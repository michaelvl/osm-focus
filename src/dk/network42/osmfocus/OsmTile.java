package dk.network42.osmfocus;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
 
public class OsmTile {
	private static final String TAG = "OsmTile";
	
	public static final int MIN_ZOOM = 0;
	public static final int MAX_ZOOM = 18; // Standard OSM and mapnik
	public static final int ZOOM_LEVELS = MAX_ZOOM-MIN_ZOOM;
	// See http://wiki.openstreetmap.org/wiki/Tile_usage_policy
	public static final int MAX_DOWNLOAD_THREADS = 2;
	
	public int mX, mY, mZoom;

	int mDownloadErrs = 0;
	boolean mDownloadOngoing = false;

	// FIXME: This should not be part of OsmTile, but rather an inherited class
	private Bitmap mBitmap = null;

	// Geo coords
	protected GeoBBox mBox;
	protected RectF mBox2 = new RectF(); // FIXME: Merge bboxes
	
	// Screen coords (scaled and Mercator latitudes)
	protected RectF mViewBox = new RectF();

	public OsmTile(int x, int y, int zoom) {
		mX = x;
		mY = y;
		mZoom = zoom;
		mBox = tile2BBox(x, y, zoom);
		mBox2.left   = (float)(((float)mBox.left)/1E7);
		mBox2.right  = (float)(((float)mBox.right)/1E7);
		mBox2.top    = (float)(((float)mBox.bottom)/1E7);
		mBox2.bottom = (float)(((float)mBox.top)/1E7);
		mViewBox.left   = (float) MapView.prescale * mBox2.left;
		mViewBox.right  = (float) MapView.prescale * mBox2.right;
		mViewBox.top    = (float)(MapView.prescale * GeoMath.latToMercator(mBox2.top));
		mViewBox.bottom = (float)(MapView.prescale * GeoMath.latToMercator(mBox2.bottom));
	}

	public OsmTile(double lon, double lat, int zoom) {
		this(lon2tile(lon, zoom), lat2tile(lat, zoom), zoom);
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}

	public int getByteSize() {
		// FIXME: Wrong for vector tiles
		return 4*256*256; // Fixed tile size for OSM in ARGB_8888
//		Log.d(TAG, "Get size of tile "+this);
//		Bitmap bm = getBitmap();
//		if (bm==null) {
//			Log.d(TAG, " = 0");
//			return 0;
//		}
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//	        return getBitmap().getAllocationByteCount();
//	    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
//	        return bm.getRowBytes() * bm.getHeight();
//	    }
	}

	public boolean canDraw(Canvas canvas, PaintConfig pcfg) {
		if (mBitmap != null) {
			return true;
		}
		return false;
	}

	public boolean draw(Canvas canvas, PaintConfig pcfg) {
		if (mBitmap != null) {
			//Log.d(TAG, "Draw bitmap at "+mBox2+" ("+mViewBox+") zoom="+mZoom);
			canvas.drawBitmap(mBitmap, null, mViewBox, pcfg.paint);
			return true;
		} else if (mDownloadErrs>0) {
			drawError(canvas, pcfg);
		}
		return false;
	}

	public void drawX(Canvas canvas, PaintConfig pcfg) {
		canvas.drawLine(mViewBox.left, mViewBox.bottom, mViewBox.right, mViewBox.top, pcfg.paint);
		canvas.drawLine(mViewBox.left, mViewBox.top, mViewBox.right, mViewBox.bottom, pcfg.paint);
	}

	public void drawP(Canvas canvas, PaintConfig pcfg) {
		float xc = (mViewBox.left+mViewBox.right)/2;
		float yc = (mViewBox.bottom+mViewBox.top)/2;
		canvas.drawLine(mViewBox.left, yc, mViewBox.right, yc, pcfg.paint);
		canvas.drawLine(xc, mViewBox.top, xc, mViewBox.bottom, pcfg.paint);
	}

	public void drawFade(Canvas canvas, PaintConfig pcfg) {
		canvas.drawRect(mViewBox, pcfg.fade);
	}

	public void drawError(Canvas canvas, PaintConfig pcfg) {
		canvas.drawRect(mViewBox, pcfg.tileerr);
	}

	//	public void drawText(Canvas canvas, Paint paint, String st) {
//		float w = paint.measureText(st);
//		canvas.drawText(st, mViewBox.centerX(), mViewBox.centerY(), paint);
//	}

	public void setQueuedForDownload() {
		assert(mDownloadOngoing==false);
		mDownloadOngoing = true;
	}

	public void clearQueuedForDownload() {
		assert(mDownloadOngoing==true);
		mDownloadOngoing = false;
	}

	public boolean isQueuedForDownload() {
		return mDownloadOngoing;
	}

	public boolean download(String useragent, String provider) {
		Bitmap bm = dlFromUrl(tileUrl(provider));
		if (bm != null) {
			mBitmap = flipBitmap(bm);
			//Log.d(TAG, "Download done");
			return true;
		} else {
			Log.e(TAG, "Download failed, tile="+this);
			return false;
		}
	}

	private Bitmap dlFromUrl(String tileurl) {
		try {
			//Log.d(TAG, "Loading tile "+tileurl);
			URL url = new URL(tileurl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			//Log.d(TAG, "Connect for tile download");
			conn.connect();
			InputStream input = conn.getInputStream();
			//Log.d(TAG, "Decode bitmap");
			Bitmap bm = BitmapFactory.decodeStream(input);
			//Log.d(TAG, "Downloaded tile "+tileurl+", bbox="+mBox.toString());
			return bm;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Bitmap flipBitmap(Bitmap bm) {
		Matrix m = new Matrix();
		m.preScale(1, -1);
		Bitmap bmf = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, false);
		if (bmf!=null)
			bmf.setDensity(DisplayMetrics.DENSITY_DEFAULT);
		return bmf;
	}

	static public int lon2tile(double lon, int zoom) {
		int xtile = (int)Math.floor((lon + 180.0)/360.0 * (1<<zoom));
		if (xtile < 0)
			xtile = 0;
		else if (xtile >= (1<<zoom))
			xtile = (1<<zoom)-1;
		return xtile;
	}

	static public int lat2tile(double lat, int zoom) {
		int ytile = (int)Math.floor((1.0 - Math.log(Math.tan(Math.toRadians(lat)) + 1.0/Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom));
		if (ytile < 0)
			ytile = 0;
		else if (ytile >= (1<<zoom))
			ytile = (1<<zoom)-1;
		return ytile;
	}

	static public double tile2lon(int x, int zoom) {
		return x/Math.pow(2.0, zoom)*360.0 - 180.0;
	}
	
	static public double tile2lat(int y, int zoom) {
		double n = Math.PI - (2.0*Math.PI * y)/Math.pow(2.0, zoom);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}

	// Convert world viewport coords to tile 'indexes'
	static public Rect worldport2tileport(final RectF worldport, int zoom) {
		Rect r = new Rect();
		r.left = lon2tile(worldport.left, zoom);
		r.right = lon2tile(worldport.right, zoom);
		r.top = lat2tile(worldport.top, zoom);
		r.bottom = lat2tile(worldport.bottom, zoom);
		return r;
	}

	// See http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
	static public String xy2Url(String provider, int x, int y, int zoom) {
		String name;
		name = String.format(Locale.US, "%s/%d/%d/%d.png", provider, zoom, x, y);
		return name;
	}

	public String tileId(String provider) {
		String name;
		name = String.format(Locale.US, "%s/%d/%d/%d", provider, mZoom, mX, mY);
		return name;
	}

	static public String tileId(String provider, int x, int y, int zoom) {
		String name;
		name = String.format(Locale.US, "%s/%d/%d/%d", provider, zoom, x, y);
		return name;
	}

	static public String tileId(String provider, double lon, double lat, int zoom) {
		String name;
		name = String.format(Locale.US, "%s/%d/%d/%d", provider, zoom, lon2tile(lon, zoom), lat2tile(lat, zoom));
		return name;
	}

	public String tileUrl(String provider) {
		return xy2Url(provider, mX, mY, mZoom);
	}

	static public String tileUrl(String provider, double lon, double lat, int zoom) {
		return xy2Url(provider, lon2tile(lon, zoom), lat2tile(lat, zoom), zoom);
	}

	static public String id2Url(String provider, String id) {
		return String.format("%s/%s.png", provider, id);
	}

	static public GeoBBox pos2BBox(double lon, double lat, int zoom) {
		return tile2BBox(lon2tile(lon, zoom), lat2tile(lat, zoom), zoom);
	}

	static public GeoBBox tile2BBox(int tilex, int tiley, int zoom) {
		int left = (int)(tile2lon(tilex, zoom)* 1E7);
		int right = (int)(tile2lon(tilex+1, zoom)* 1E7);
		int top = (int)(tile2lat(tiley, zoom)* 1E7);
		int bottom = (int)(tile2lat(tiley+1, zoom)* 1E7);
		GeoBBox box = new GeoBBox(left, bottom, right, top);
		return box;
	}

	public String toString() {
		return String.format(Locale.US, "%d/%d/%d", mZoom, mX, mY);
	}
}
