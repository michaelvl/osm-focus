package dk.network42.osmfocus;

import android.view.View;

public class OsmTileLayerBm extends OsmTileLayer {
	private static final String TAG = "OsmTileLayerBm";
	String mProviderArg = "";

	public OsmTileLayerBm(OsmTileProvider provider, int maxCacheSize) {
		super(provider, maxCacheSize);
		//mProviderArg = "http://a.tile.openstreetmap.org";
		//mProviderArg = "http://a.tile.opencyclemap.org/cycle";
	}

	public void setProviderUrl(String url) {
		mProviderArg = url;
		flushCache();
	}

	protected OsmTile createTile(int xt, int yt, int zoom) {
		return new OsmTile(xt, yt, zoom);
	}

	protected void handleMissingTile(OsmTile t, int layer) {
		downloadTile(t);
	}

	protected void downloadTile(OsmTile t) {
		mProvider.downloadTile(mProviderArg, t, mHandler);
	}

	protected String getTileId(int xt, int yt, int zoom) {
		return OsmTile.tileId(mProviderArg, xt, yt, zoom);
	}
}
