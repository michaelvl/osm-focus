package dk.network42.osmfocus;

import android.graphics.Canvas;

public class OsmNode extends OsmElement {
    private static final String TAG = "OsmNode";

	int mLon, mLat; // OSM geopoint, WGS84 times 1e7
	float mLatMerc;
	public boolean mIsWayNode = false;
	
    OsmNode(final int lon, final int lat) {
        super(0, 0);
        mLon = lon;
        mLat = lat;
        mLatMerc = (float) GeoMath.latToMercator(getLat());
    }

    OsmNode(final long osmId, final long osmVersion, final int lon, final int lat) {
        super(osmId, osmVersion);
        mLon = lon;
        mLat = lat;
        mLatMerc = (float) GeoMath.latToMercator(getLat());
    }

	public double getLon() { return ((double)mLon)/1e7; }
    public double getLat() { return ((double)mLat)/1e7; }
    public double getLatMerc() { return mLatMerc; }
	public int getOsmLon() { return mLon; }
    public int getOsmLat() { return mLat; }
    
    // For screen mapping when db translation is not active
	public float getX() { return ((float) getLon())*MapView.prescale; }
    public float getY() { return mLatMerc*MapView.prescale; }
    
    // For use when db translation is active
	public float getX(OsmDB db) { return ((float) ((getLon()-db.mX)*MapView.prescale)); }
    public float getY(OsmDB db) { return ((float) ((mLatMerc-db.mYmerc)*MapView.prescale)); }

    //static double getLatMercator(final double lat) { return Math.toDegrees(Math.log(Math.tan(Math.PI/4+Math.toRadians(lat)/2))); }
    
	//public double getLambertLat() {
	//		return ((double)mLat)/1e7;
	//}
	//public double getLambertLon() { return ((double)mLon)/1e7; }

	// Lambert projection
	/*public NodePoint convertToXY(String lat, String lon){        
        float phi = (float) GPS.radians(Float.parseFloat(lat));
        float lambda = (float) GPS.radians(Float.parseFloat(lon));
        float q = (float) (2*Math.sin( ((Math.PI/2) - phi)/2 ));
        float x = (float)(q*Math.sin(lambda));
        float y = (float)(q*Math.cos(lambda));
        return new NodePoint(x,y);
        
    }*/

    public void draw(Canvas canvas, OsmDB db, PaintConfig pcfg) {
    	if (! mIsWayNode) {
    		if (pcfg.getLayer()==1) {
    	    	//Log.d(TAG, "Node: xy=("+getX(db)+","+getY(db)+
    	    	//		"), latlon=("+getLat()+","+getLon()+
    	    	//			"), DBorig="+db.mX+","+db.mYmerc+
    	    	//			", scale="+pcfg.scale);
    			////canvas.drawCircle(getX(db), getY(db), 2.0f/pcfg.scale, pcfg.focus);
    			//canvas.drawPoint(getX(db), getY(db), pcfg.focus);
    		}
    	}
    }

    public void highlight(Canvas canvas, OsmDB db, PaintConfig pcfg, int style)
    {
//    	Log.d(TAG, "NodeHighlight: xy=("+getX(db)+","+getY(db)+
//    			"), latlon=("+getLat()+","+getLon()+
//    				"), DBorig="+db.mX+","+db.mYmerc);
    	canvas.drawCircle(getX(db), getY(db), 5.0f*pcfg.densityScale/pcfg.mScale, pcfg.focus[style]);
    }

    public double distTo(double lon, double lat_merc) {
    	return Math.hypot(getLon()-lon, getLatMerc()-lat_merc);
    }

    public double angleTo(double lon, double lat_merc) {
    	return Math.atan2(getLatMerc()-lat_merc, getLon()-lon);
    }
}
