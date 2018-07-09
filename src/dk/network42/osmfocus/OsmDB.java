package dk.network42.osmfocus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

public class OsmDB extends MapLayer {
    private static final String TAG = "OsmDB";
    private long mWayId = 0;
    private long mNodeId = 0;
    private Map<Long, OsmNode> mNodes;
    private Map<Long, OsmWay> mWays;
    private Map<Long, OsmRelation> mRels;
    private ArrayList<OsmBounds> mBounds;
    private Filter mFilter = new Filter();

    float mX, mY, mYmerc; // Current view position, latitude is Mercator corrected

    OsmDB() {
    	mNodes = new HashMap<Long, OsmNode>();
    	mWays = new HashMap<Long, OsmWay>();
    	mRels = new HashMap<Long, OsmRelation>();
        mBounds = new ArrayList<OsmBounds>();
    }
    
    public OsmBounds getBounds() { // FIXME: Not really correct with multiple bounds
    	return mBounds.get(0);
    }

    //void setBBox(final GeoBBox bbox) {
    //   this.box = bbox;
    //}

    // True if we have any bounds defined. May contain zero nodes/ways
    public boolean isEmpty() {
    	return (mBounds.isEmpty() || mBounds.get(0).isEmpty());
    }
    
    public OsmNode getNode(final long osmId) {
    	return mNodes.get(osmId);
    }

    public OsmWay getWay(final long osmId) {
    	return mWays.get(osmId);
    }

    public OsmWay getRelation(final long osmId) {
    	return mWays.get(osmId);
    }

    public OsmElement getElement(final long osmId) {
    	OsmElement elem = mWays.get(osmId);
    	if (elem != null)
    		return elem;
    	elem = mNodes.get(osmId);
    	if (elem != null)
    		return elem;
    	elem = mRels.get(osmId);
    	if (elem != null)
    		return elem;
    	return null;
    }
    
    public Map<Long, OsmNode> getNodes() {
        return mNodes;
    }

    public Map<Long, OsmWay> getWays() {
        return mWays;
    }

    public Map<Long, OsmRelation> getRelations() {
        return mRels;
    }
   
    void insertNode(final OsmNode node) {
        mNodes.put(node.mOsmId, node);
        node.mDb = this;
    }

    void insertWay(final OsmWay way) {
        mWays.put(way.mOsmId, way);
        way.mDb = this;
    }

    void insertRelation(final OsmRelation rel) {
        mRels.put(rel.mOsmId, rel);
        rel.mDb = this;
    }

    void insertBounds(final OsmBounds box) {
        mBounds.add(box);
        if (mBounds.size()==1) {
			mX = (float) (getBounds().getLeft()/1e7f);
	    	mY = (float) (getBounds().getTop()/1e7f);
	    	mYmerc = (float) GeoMath.latToMercator(mY);
    		//Log.d(TAG, "DB ref frozen to "+mX+", "+mY+"("+mYmerc+")");
        }
    }

	public static OsmNode createNode(long osmId, long osmVersion, int lat, int lon) {
		return new OsmNode(osmId, osmVersion, lat, lon);
    }

	public OsmNode createNodeWithNewId(int lat, int lon) {
		return createNode(--mNodeId, 1, lat, lon);
	}

	public static OsmWay createWay(long osmId, long osmVersion) {
        return new OsmWay(osmId, osmVersion);
	}

	public OsmWay createWayWithNewId() {
        return createWay(--mWayId, 1);
	}

	public static OsmRelation createRelation(long osmId, long osmVersion) {
		return new OsmRelation(osmId, osmVersion);
	}

	public OsmRelation createRelationWithNewId() {
        return createRelation(--mWayId, 1);
	}

	public static OsmBounds createBounds(int minlon, int minlat, int maxlon, int maxlat) throws OsmException {
		return new OsmBounds(minlon, minlat, maxlon, maxlat);
    }

	// FIXME handle overlaps, merge newest
    void merge(OsmDB with) {
    	if (with==null)
    		return;
    	if (mNodes.size()==0)
    		mNodes = with.mNodes;
    	if (mWays.size()==0)
    		mWays = with.mWays;
    	
    	with.mX = mX;  // Update with relative origin of dest db
    	with.mY = mY;
    	with.mYmerc = mYmerc;
    	with.updateVisuals();
    	
    	Iterator it;
    	it = with.mNodes.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry pair = (Map.Entry) it.next();
            if (!mNodes.containsKey(pair.getKey())) {
            	insertNode((OsmNode)pair.getValue());
            }
        }
    	it = with.mWays.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry pair = (Map.Entry) it.next();
            if (!mWays.containsKey(pair.getKey())) {
            	insertWay((OsmWay)pair.getValue());
            }
        }
    }

    void updateAndVacuum() {
    	Iterator it = mWays.entrySet().iterator();
    	while (it.hasNext()) {
        	Map.Entry pair = (Map.Entry) it.next();
        	OsmWay ee = (OsmWay)pair.getValue();
        	mFilter.filter(ee);
        	Iterator nit = ee.getNodes().iterator();
        	while (nit.hasNext()) {
            	OsmNode nn = (OsmNode) nit.next();
            	nn.mIsWayNode = true;
            }
        }
    	it = mNodes.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry pair = (Map.Entry) it.next();
        	mFilter.filter((OsmElement)pair.getValue());
        }
        updateAndVacuumRelations();
        vacuumWays();
        updateVisuals();
    }
    
    // Remove relation that we do not use
    void updateAndVacuumRelations() {
    	Iterator it = mRels.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry pair = (Map.Entry) it.next();
        	OsmRelation rel = (OsmRelation) pair.getValue();
        	if (rel.hasTag("type", "multipolygon")) {
            	rel.updateVisualDefiningElem();
        	} else {
            	//Log.d(TAG, "Vacuum Rel, id="+rel.mOsmId);
        		it.remove();
        	}
        }
    }

    // Remove e.g. ways tagged with building=yes and which are part of a multipolygon with role=outer
    void vacuumWays() {
    	Iterator it = mRels.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry pair = (Map.Entry) it.next();
        	OsmRelation rel = (OsmRelation) pair.getValue();
        	if (rel.hasTag("type", "multipolygon") && rel.hasRole("outer")) {
        		long ref = rel.getRoleRef("outer");
        		//Log.d(TAG, "Rel id="+rel.mOsmId+", outer id="+ref);
        		OsmWay way = mWays.get(ref);
        		if (way != null) {
        			way.mIsMultipolyOuter = true;
        		}
        	}
        }
    }
    
    void updateVisuals() {
        Iterator it = mWays.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry pair = (Map.Entry) it.next();
        	OsmWay way = (OsmWay) pair.getValue();
        	way.updateVisuals(this);
        }    	
    	it = mRels.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry pair = (Map.Entry) it.next();
        	OsmRelation rel = (OsmRelation) pair.getValue();
        	rel.updateVisuals(this);
        }    	
    }

    public static boolean idIsInList(long id, ArrayList<OsmElement> list) {
		for (int ii = 0; ii<list.size(); ii++) {
			if (list.get(ii).getOsmId() == id)
				return true;
		}
		return false;
    }

    public int closestElements(double lon, double lat, int max, ArrayList<OsmElement> near) {
    	double lat_merc = GeoMath.latToMercator(lat);
		//Log.d(TAG, "Find elements close to ("+lon+","+lat+"(merc="+lat_merc+"))");
    	OsmElement e;
    	Map.Entry pair;
    	Iterator it = mNodes.entrySet().iterator();
    	while (it.hasNext()) {
    		pair = (Map.Entry) it.next();
        	e = (OsmNode) pair.getValue();
        	if (e.getTagCnt() > 0 && !e.mFiltered && !idIsInList(e.getOsmId(), near)) {
        		e.compareSetDistTo(lon, lat_merc);
        		if (near.size() < max) {
            		near.add(e);        			
            		//Log.d(TAG, "Node(Id="+e.mOsmId+"): Add, near size="+near.size()+" near=["+dumpNearArray(near)+"]");
                    Collections.sort(near);
        		} else if (near.get(near.size()-1).compareGet() > e.compareGet()) {
        			near.set(near.size()-1, e);
                    Collections.sort(near);
        		}
        		//Log.d(TAG, "Node(Id="+e.mOsmId+"): near size="+near.size()+" near=["+dumpNearArray(near)+"]");
        	}
        }
    	it = mWays.entrySet().iterator();
    	while (it.hasNext()) {
    		pair = (Map.Entry) it.next();
        	e = (OsmWay) pair.getValue();
        	if (e.getTagCnt() > 0 && !e.mFiltered && !idIsInList(e.getOsmId(), near)) {
        		e.compareSetDistTo(lon, lat_merc);
        		if (near.size() < max) {
        			near.add(e);
            		//Log.d(TAG, "Way(Id="+e.mOsmId+"): Add, near size="+near.size()+" near=["+dumpNearArray(near)+"]");
        			Collections.sort(near);
        		} else if (near.get(near.size()-1).compareGet() > e.compareGet()) {
            		near.set(near.size()-1, e);
            		//Log.d(TAG, "Way(Id="+e.mOsmId+"): insert at "+(near.size()-1)+" near=["+dumpNearArray(near)+"]");
                    Collections.sort(near);
            	}
        	}
        }
		//Log.d(TAG, "Final near=["+dumpNearArray(near)+"]");
        return 1;
    }

//  private static String dumpNearArray(ArrayList<OsmElement> near) {
//	String st = "";
//	for (int ii=0; ii<near.size(); ii++) {
//		st += near.get(ii).mOsmId + "(C="+near.get(ii).compareGet()+") ";
//	}
//	return st;
//}
//	  private static String dumpNearArray(ArrayList<OsmElement> near) {
//		String st = "";
//		for (int ii=0; ii<near.size(); ii++) {
//			st += " x="+near.get(ii).getX()+" y="+near.get(ii).getY();
//		}
//		return st;
//	}

    public static void quadSort(ArrayList<OsmElement> near) {
    	Collections.sort(near, OsmElement.Compare.Y_COORD_DESCENDING);
    	if (near.size()==4) {
	    	Collections.sort(near.subList(0, 2), OsmElement.Compare.X_COORD);
	    	Collections.sort(near.subList(2, 4), OsmElement.Compare.X_COORD);
    	}
    	if (near.size()==8) {
	    	Collections.sort(near.subList(0, 3), OsmElement.Compare.X_COORD);
	    	Collections.sort(near.subList(3, 5), OsmElement.Compare.X_COORD);
	    	Collections.sort(near.subList(5, 8), OsmElement.Compare.X_COORD);
    	}
    	//Log.d(TAG, "After sort:"+dumpNearArray(near));
    }

    // Clock-wise
    public static void radianSortCW(ArrayList<OsmElement> near, double lon, double latMerc, double angleOffset) {
		for (int ii = 0; ii<near.size(); ++ii) {
			near.get(ii).compareSetAngleTo(lon, latMerc, angleOffset);
		}
		//Log.d(TAG, "Before sort:"+dumpNearArray(near));
    	Collections.sort(near, new Comparator<OsmElement>(){
    		@Override
    		public int compare(OsmElement e1, OsmElement e2) {
    			return (int) e2.compareTo(e1);
    		}
    	});
		//Log.d(TAG, "After sort:"+dumpNearArray(near));
    }

    // Counter clock-wise
    public static void radianSortCCW(ArrayList<OsmElement> near, double lon, double latMerc, double angleOffset) {
		for (int ii = 0; ii<near.size(); ++ii) {
			near.get(ii).compareSetAngleTo(lon, latMerc, angleOffset);
		}
    	Collections.sort(near);
    }
    
    public void draw(Canvas canvas, RectF worldport, PaintConfig pcfg) {
    	if (! isEmpty()) {
    		canvas.save();
    		//Log.d(TAG, "DB ref coords lon="+mX+", lat="+mYmerc+"(merc)");
            canvas.translate(mX*MapView.prescale, mYmerc*MapView.prescale);
    		// Elements
    		int layers = 3;
	    	for (int layer=0; layer<layers; layer++) {
	    		pcfg.setLayer(layer);
	    		Iterator it = getNodes().entrySet().iterator();
	    		while (it.hasNext()) {
	    			Map.Entry pair = (Map.Entry) it.next();
	    			((OsmNode)pair.getValue()).draw(canvas, this, pcfg);
	    		}
				it = getWays().entrySet().iterator();
	    		while (it.hasNext()) {
		        	Map.Entry pair = (Map.Entry) it.next();
	    			((OsmWay)pair.getValue()).draw(canvas, this, pcfg);
	    		}
				it = getRelations().entrySet().iterator();
	    		while (it.hasNext()) {
		        	Map.Entry pair = (Map.Entry) it.next();
	    			((OsmRelation)pair.getValue()).draw(canvas, this, pcfg);
	    		}
	    	}
    		canvas.restore();
    	}
    }

    public void drawBounds(Canvas canvas, RectF worldport, PaintConfig pcfg) {
		canvas.save();
        canvas.translate(mX*MapView.prescale, mYmerc*MapView.prescale);
		int size = mBounds.size();
		for (int ii=0; ii<size; ii++) {  // FIXME
			mBounds.get(ii).draw(canvas, this, pcfg);
    	}
		canvas.restore();
    }

    public void highlightElem(Canvas canvas, PaintConfig pcfg, OsmElement elem, int style) {
    	canvas.save();
    	canvas.translate(mX*MapView.prescale, mYmerc*MapView.prescale);
    	elem.highlight(canvas, this, pcfg, style);
    	canvas.restore();
    }

//    public void drawElem(Canvas canvas, PaintConfig pcfg, OsmElement elem) {
//		canvas.save();
//		Log.d(TAG, "DB ref coords lon="+mX+", lat="+mYmerc+"(merc)");
//        canvas.translate(mX, mYmerc);
//        elem.draw(canvas, this, pcfg);
//		canvas.restore();
//    }

    public String toString() {
    	return "DB={"+mNodes.size()+"/"+mWays.size()+"/"+mRels.size()+"}";
    }
}
