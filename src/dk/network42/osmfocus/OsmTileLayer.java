package dk.network42.osmfocus;

import android.content.ComponentCallbacks2;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;

public abstract class OsmTileLayer extends MapLayer {

	private static final String TAG = "OsmTileLayer";
	TileLruCache mTiles;
	OsmTileProvider mProvider;
	//View mView;
	Handler mHandler;
	Handler mMainHandler=null;
	String mAttrib=null;
	int mLastZoom = -1;
	
	public OsmTileLayer(OsmTileProvider provider, int maxCacheSize) {
		mProvider = provider;
		mTiles = new TileLruCache(maxCacheSize);
		mHandler = new Handler(Looper.getMainLooper()) {
			@Override
	        public void handleMessage(Message inputMessage) {
				//OsmTile t = (OsmTile) inputMessage.obj;
				switch (inputMessage.what) {
				case OsmTileProvider.TASK_DOWNLOAD_START:
				case OsmTileProvider.TASK_DOWNLOAD_COMPLETE:
					//Log.d(TAG, "Got download complete, tile"+t+" tile cache size="+mTiles.size());
					if (mMainHandler != null)
						mMainHandler.obtainMessage(MainActivity.INVALIDATE_VIEW, this).sendToTarget();
					break;
				}
			}
		};
	}

	static String urlFromType(int type) {
		if (type==MapLayer.MAPTYPE_OSM) {
			return "http://a.tile.openstreetmap.org";
		} else if (type==MapLayer.MAPTYPE_OSMCYCLEMAP) {
			return "http://a.tile.opencyclemap.org/cycle";    			
		}
		return "";
	}

	public void setMainHandler(Handler handler) {
		mMainHandler = handler;
	}

	public void draw(Canvas canvas, RectF worldport, PaintConfig pcfg) {
		for (int ii=0; ii<getNumLayers(); ii++) { // Sub-layers
			drawTiles(canvas, worldport, pcfg, ii);
		}
	}

	public void drawTiles(Canvas canvas, RectF worldport, PaintConfig pcfg, int layer) {
		int zoom = getZoom(pcfg);
		Rect vrect = OsmTile.worldport2tileport(worldport, zoom);
		//Log.d(TAG, "TilePort="+vrect);

		if (zoom != mLastZoom) {
			mProvider.clearPending();
			mLastZoom = zoom;
		}

		// RectF's are reversed top-bottom wise
		int circles = (Math.max(vrect.right-vrect.left, vrect.bottom-vrect.top)+1)/2;
		int cx = (vrect.right+vrect.left+1)/2;
		int cy = (vrect.bottom+vrect.top+1)/2;
		//Log.d(TAG, "Circles: "+circles+" around ("+cx+","+cy+")");
		
		drawTile(canvas, pcfg, cx, cy, zoom, layer);
		if (circles==0)
			return;
		int xt, yt;
		for (int cc=1; cc<=circles; cc++) {
			//Log.d(TAG, "cc="+cc);
			for (xt=cx-cc; xt<=cx+cc; xt++) {
				yt = cy-cc;
				//Log.d(TAG, "1: "+xt+","+yt);
				if (xt>=vrect.left && xt <= vrect.right && yt >= vrect.top && yt <= vrect.bottom)
					drawTile(canvas, pcfg, xt, yt, zoom, layer);
			}
			for (yt=cy-(cc-1); yt<=cy+cc-1; yt++) {
				xt = cx+cc;
				//Log.d(TAG, "2: "+xt+","+yt);
				if (xt>=vrect.left && xt <= vrect.right && yt >= vrect.top && yt <= vrect.bottom)
					drawTile(canvas, pcfg, xt, yt, zoom, layer);
			}
			for (xt=cx+cc; xt>=cx-cc; xt--) {
				yt = cy+cc;
				//Log.d(TAG, "3: "+xt+","+yt);
				if (xt>=vrect.left && xt <= vrect.right && yt >= vrect.top && yt <= vrect.bottom)
					drawTile(canvas, pcfg, xt, yt, zoom, layer);
			}
			for (yt=cy+cc-1; yt>=cy-(cc-1); yt--) {
				xt = cx-cc;
				//Log.d(TAG, "4: "+xt+","+yt);
				if (xt>=vrect.left && xt <= vrect.right && yt >= vrect.top && yt <= vrect.bottom)
					drawTile(canvas, pcfg, xt, yt, zoom, layer);
			}
		}
		//Log.d(TAG, "Tile layer draw finished ");
	}
	public void drawLegends(Canvas canvas, RectF viewrect, PaintConfig pcfg) {
		if (mAttrib != null) {
			canvas.drawText(mAttrib, viewrect.centerX()-pcfg.attrib.measureText(mAttrib)/2, 
					viewrect.centerY()+viewrect.height()*0.4f, pcfg.attrib);
		}
	}

	protected abstract String getTileId(int xt, int yt, int zoom);
	
	protected abstract OsmTile createTile(int xt, int yt, int zoom);

	protected abstract void handleMissingTile(OsmTile t, int layer);

	protected void drawTile(Canvas canvas, PaintConfig pcfg, int xt, int yt, int zoom, int layer) {
		//Log.d(TAG, "Draw tile: "+xt+","+yt+" zoom="+zoom);
		String tid = getTileId(xt, yt, zoom);
		OsmTile t = mTiles.get(tid);
		if (t == null) {
			t = createTile(xt, yt, zoom);
			mTiles.put(tid, t);
			//Log.d(TAG, "New tile, TID="+tid+" LruCache size="+mTiles.size());
			handleMissingTile(t, layer);
			t.drawX(canvas, pcfg);
		} else {
			if (! t.draw(canvas, pcfg)) {
				t.drawX(canvas, pcfg);
			}
		}
	}

//	private void drawTileCoords(Canvas canvas, PaintConfig pcfg, OsmTile t) {
//		String st = "";
//		st.format("%d,%d,%d", t.mX, t.mY, t.mZoom);
//		t.drawText(canvas, pcfg.paint, st);
//	}

	protected abstract void downloadTile(OsmTile t);

	protected int getZoom(PaintConfig pcfg) {
		return view2OsmZoom(pcfg.mScale);
	}

	// Convert from internal view scale value to OSM zoom levels
	protected int view2OsmZoom(float scale) {
		int zoom = (int) (16+(1.0+Math.log((double)scale)/Math.log(2.0)));
		if (zoom > OsmTile.MAX_ZOOM)
			zoom = OsmTile.MAX_ZOOM;
		//Log.d(TAG, "Scale="+scale+" OsmZoom="+zoom);
		return zoom;
	}

	public void onTrimMemory(int level) {
		if (level == ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
			mTiles.trimToSize(mTiles.maxSize()/2);
		} else if (level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
			mTiles.trimToSize(0);
		}
	}

	public int getMemorySize() {
		return mTiles.size();
	}

	public void flushCache() {
		mTiles.evictAll();
	}

	protected int getNumLayers() {
		return 1;
	}

	// Attribute, like OSM data attribution
	public void setAttrib(String attrib) {
		mAttrib = attrib;
	}

	protected class TileLruCache extends LruCache<String, OsmTile> {
		public TileLruCache(int maxSize) {
			super(maxSize);
		}
		protected int sizeOf(String k, OsmTile v) {
			return v.getByteSize();
		}
	};
}
