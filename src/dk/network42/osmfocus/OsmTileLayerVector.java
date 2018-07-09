package dk.network42.osmfocus;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

public class OsmTileLayerVector extends OsmTileLayer {
	private static final String TAG = "OsmTileLayerVector";
	private static final int TILEZOOM = 16;  // Vector tile zoom level
	private static final int MAX_POI = 8;
	private static final float MIN_SCALE = 0.5f;
	String mProviderArg;
    SharedData mG;
    ArrayList<OsmElement> mNearest = new ArrayList<OsmElement>(MAX_POI);
    RectF [] mUsedArea = new RectF[MAX_POI];
    boolean filterWayNodes = false;

	public OsmTileLayerVector(OsmTileProvider provider, int maxCacheSize) {
		super(provider, maxCacheSize);
		mProviderArg = "";
	}

    public void setSharedData(SharedData data) {
    	mG = data;
    }

	protected void tryAutoDownload() {
		if (mG.mVectorAutoDownload == SharedData.AUTODOWNLOAD_MANUAL)
			return;

		int xt = OsmTile.lon2tile(mG.mLon, TILEZOOM);
    	int yt = OsmTile.lat2tile(mG.mLat, TILEZOOM);
    	int x, y, c=0;
    	String tid;

    	if (mG.mVectorAutoDownload == SharedData.AUTODOWNLOAD_AUTOMATIC2)
			c=1;

    	for (x=xt-c; x<=xt+c; x++) {
        	for (y=yt-c; y<=yt+c; y++) {
        		tid = OsmTile.tileId(mProviderArg, x, y, TILEZOOM);
        		OsmTileVector tile = (OsmTileVector) mTiles.get(tid);
        		if (tile != null && tile.mDb == null && !tile.isQueuedForDownload()) {
        			downloadTile(tile);
        		}
        	}
    	}
	}

	protected OsmTile createTile(int xt, int yt, int zoom) {
		return new OsmTileVector(xt, yt, zoom);
	}

	// Called once immediately after createTile()
	protected void handleMissingTile(OsmTile t, int layer) {
		tryAutoDownload();
	}

	// Shading are only drawn on layer 1
	protected void drawTile(Canvas canvas, PaintConfig pcfg, int xt, int yt, int zoom, int layer) {
		//Log.d(TAG, "Draw tile: "+xt+","+yt+" zoom="+zoom);
		String tid = getTileId(xt, yt, zoom);
		OsmTile t = mTiles.get(tid);
		if (t == null) {
			t = createTile(xt, yt, zoom);
			mTiles.put(tid, t);
			//Log.d(TAG, "New tile, TID="+tid+" LruCache size="+mTiles.size());
			handleMissingTile(t, layer);
			if (layer==1)
				t.drawX(canvas, pcfg);
		} else {
			if (layer==0) {
				t.draw(canvas, pcfg);
			}
			if (layer==1 && !t.canDraw(canvas, pcfg)) {
				t.drawX(canvas, pcfg);
			}
		}
	}

	protected void downloadTile(OsmTile t) {
		if (t.mDownloadErrs < 2) {
			mProvider.downloadTile(mProviderArg, t, mHandler);
		} else {
			//Log.d(TAG, "Skipping download, errs="+t.mDownloadErrs);
		}
	}

	protected String getTileId(int xt, int yt, int zoom) {
		return OsmTile.tileId(mProviderArg, xt, yt, zoom);
	}

	// See: http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
	@Override
	protected int view2OsmZoom(float scale) {
		return 16;
	}

	public void download(double lon, double lat) {
		int xt = OsmTile.lon2tile(lon, TILEZOOM);
		int yt = OsmTile.lat2tile(lat, TILEZOOM);
		String tid = OsmTile.tileId(mProviderArg, xt, yt, TILEZOOM);
		OsmTile t = mTiles.get(tid);
		if (t == null) {
			t = createTile(xt, yt, TILEZOOM);
		}
		downloadTile(t);
	}

    public int closestElements(double lon, double lat, int max, ArrayList<OsmElement> near) {

    	near.clear();
    	int xt = OsmTile.lon2tile(lon, TILEZOOM);
    	int yt = OsmTile.lat2tile(lat, TILEZOOM);
    	int x, y, c=1;
    	String tid;

    	for (x=xt-c; x<=xt+c; x++) {
        	for (y=yt-c; y<=yt+c; y++) {
        		tid = OsmTile.tileId(mProviderArg, x, y, TILEZOOM);
        		OsmTileVector t = (OsmTileVector) mTiles.get(tid);
        		if (t != null && t.mDb != null) {
        			t.mDb.closestElements(lon, lat, max, near);
        		}
        	}
        }
        return near.size();
    }

	public void draw(Canvas canvas, RectF worldport, PaintConfig pcfg) {

		if (pcfg.mScale<MIN_SCALE)
			return;
		
		for (int ii=0; ii<getNumLayers(); ii++) { // Sub-layers
			drawTiles(canvas, worldport, pcfg, ii);
		}
		tryAutoDownload();

		if (closestElements(mG.mLon, mG.mLat, mG.mPoisToShow, mNearest)==0)
			return;

		OsmDB.quadSort(mNearest);
		//OsmDB.radianSortCW(mNearest, mG.mLon, GeoMath.latToMercator(mG.mLat), 0); //Math.PI*3.0/4.0);

		int pos=0;
		for (int ii = 0; ii<mNearest.size(); ++ii) {
			OsmElement e = mNearest.get(ii);
//			if (!filterWayNodes || !((OsmNode)mNearest.get(ii)).mIsWayNode) {
				//Log.d(TAG, "Hightlight "+e);
				e.mDb.highlightElem(canvas, mG.mPcfg, e, ii);
				pos++;
				if (pos==mG.mPoisToShow) break;
//			}
		}
		// FIXME: Needs scaling to screen coords
		//canvas.drawCircle((float)mG.mLon, (float)GeoMath.latToMercator(mG.mLat),
		//		(float) mNearest.get(mNearest.size()-1).distTo(mG.mLon, mG.mLat), mG.mPcfg.horizon);
	}

	private Paint.Align[] xloc4 = {Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.LEFT, Paint.Align.RIGHT};
	private Paint.Align[] yloc4 = {Paint.Align.LEFT, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT};
	private Paint.Align[] xloc8 = {Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.RIGHT};
	private Paint.Align[] yloc8 = {Paint.Align.LEFT, Paint.Align.LEFT, Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT};

	public void drawLegends(Canvas canvas, RectF viewrect, PaintConfig pcfg) {
		if (pcfg.mScale<MIN_SCALE) {
			String txt;
			if (pcfg.mBackMapType == MAPTYPE_INTERNAL) {
				txt = mG.mCtx.getString(R.string.info_notshowvectoratzoomlevel);
			} else {
				txt = mG.mCtx.getString(R.string.info_notshowpoiatzoomlevel);
			}
        	canvas.drawText(txt, viewrect.centerX()-mG.mPcfg.debughud.measureText(txt)/2, 
        			viewrect.centerY()-mG.mPcfg.mHUDspacing, mG.mPcfg.debughud);
			return;
		}
    	int pos=0;
    	Paint.Align[] xloc, yloc;
    	if (mG.mPoisToShow==8) {
    		xloc = xloc8; yloc = yloc8;
    	} else {
    		xloc = xloc4; yloc = yloc4;
    	}
		for (int ii = 0; ii<mNearest.size(); ++ii) {
        	if (!filterWayNodes || !((OsmNode)mNearest.get(ii)).mIsWayNode) {
        		mUsedArea[ii] = mNearest.get(ii).drawTags(canvas, viewrect, mG.mShowPoiLines,
        								xloc[pos], yloc[pos], mG.mPcfg, ii);
        		pos++;
        		if (pos==mG.mPoisToShow) break;
        	}
        }
		String tid = OsmTile.tileId(mProviderArg, mG.mLon, mG.mLat, TILEZOOM);
		OsmTileVector t = (OsmTileVector) mTiles.get(tid);
		if (t == null || t.mDb == null) {
			//Log.d(TAG, "No focus tile (id="+tid+") at "+mG.mLon+","+mG.mLat);
			tryAutoDownload();
			String txt;
			if (t != null && t.mDownloadErrs > 0) {
				txt = mG.mCtx.getString(R.string.info_errdownloadarea);
			} else {
				if (mG.mVectorAutoDownload==SharedData.AUTODOWNLOAD_MANUAL) {
					txt = mG.mCtx.getString(R.string.info_locationoutside);
				} else {
					txt = mG.mCtx.getString(R.string.info_downloading);
				}
			}
        	canvas.drawText(txt, viewrect.centerX()-mG.mPcfg.debughud.measureText(txt)/2, 
        			viewrect.centerY()-mG.mPcfg.mHUDspacing, mG.mPcfg.debughud);
		}
	}

	protected int getNumLayers() {
		return 2; // Vector and shaded
	}

}
