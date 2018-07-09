package dk.network42.osmfocus;

import android.graphics.Canvas;

public class OsmBounds {
    private static final String TAG = "OsmBounds";

    public
    OsmNode min = null, max = null;

    public boolean isEmpty() {
    	return (min==null || max==null);
    }

    public OsmBounds() {
    }

    public OsmBounds(int minlon, int minlat, int maxlon, int maxlat) {
    	// Normalize by swapping
    	int tmp;
    	if (minlon>maxlon) {
    		tmp = minlon;
    		minlon = maxlon;
    		maxlon = tmp;
    	}
    	if (minlat>maxlat) {
    		tmp = minlat;
    		minlat = maxlat;
    		maxlat = tmp;
    	}
    	
    	min = new OsmNode(minlon, minlat);
    	max = new OsmNode(maxlon, maxlat);
    }
 
    // Osm geopoints, lon/lat * 1e7
    int getLeft() { return min.getOsmLon(); }
    int getTop() { return min.getOsmLat(); }
    int getRight() { return max.getOsmLon(); }
    int getBottom() { return max.getOsmLat(); }
    
    public void draw(Canvas canvas, OsmDB db, PaintConfig pcfg) {
		canvas.drawRect(min.getX(db), min.getY(db), max.getX(db), max.getY(db), pcfg.bounds);
    }
}
