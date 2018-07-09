package dk.network42.osmfocus;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Path;
import android.util.Log;

public class OsmRelation extends OsmElement {
    private static final String TAG = "OsmRelation";
    private class RelMember {
    	String mRole;
    	String mType;
    	long mRef;
    	OsmElement mElem;
    	boolean mRender;
    }
    protected final ArrayList<RelMember> mElems;
    protected Path mPath = new Path();
    boolean mDrawInners = false; // Set if inners have tags different from relation/outer
    OsmElement mVisualDefiningElem; // Normally relation, but may be outer way
    
    OsmRelation(final long osmId, final long osmVersion) {
        super(osmId, osmVersion);
        mElems = new ArrayList<RelMember>();
    }

    boolean hasRole(String role) {
    	for (RelMember member: mElems) {
        	if (member.mRole.equalsIgnoreCase(role)) {
        		return true;
        	}
        }
        return false;
    }
    
    long getRoleRef(String role) {
    	for (RelMember member: mElems) {
        	if (member.mRole.equalsIgnoreCase(role)) {
        		if (member.mElem == null) {
        			Log.d(TAG, "Relation w NULL elem, ref="+member.mRef);
        			return 0; // FIXME
        		}
        		return member.mElem.getOsmId();
        	}
        }
    	return 0; // FIXME
    }
    
    void addMember(final String type, final String role, long ref, OsmElement e) {
    	RelMember elem = new RelMember();
    	elem.mRole = role;
    	elem.mType = type;
    	elem.mElem = e;
    	elem.mRef = ref;
    	elem.mRender = false;
        mElems.add(elem);
    }
    
    /* Detect which object define how the relation should be rendered.
     * In principle tags should be on relations, but sometimes they are
     * on the outer element(s) */
    void updateVisualDefiningElem() {
    	if (!hasTag("type", "multipolygon")) {
    		Log.d(TAG, "Relation "+mOsmId+" is not a multipolygon");
    		return;
    	}
    	mVisualDefiningElem = this;
    	for (RelMember member: mElems) {
        	if (member.mRole.equalsIgnoreCase("outer") &&
        			member.mElem != null &&
        			member.mElem.getTagCnt() > 0 &&
        			getTagCnt() == 1) {
        		// If relation only has one tag ('type') and we have an outer
        		// with tags, use the outer for defining the visuals
            	mVisualDefiningElem = member.mElem;        		
        	}
    	}
    	for (RelMember member: mElems) {
    		// If we have an inner that differs from the element defining
    		// the visuals of the relation, then we render it
    		if (member.mRole.equalsIgnoreCase("inner") &&
    				member.mElem != null &&
    				member.mElem.getTagCnt()>0 &&
    				!mVisualDefiningElem.tagsIdentical(member.mElem)) {
    			mDrawInners = true;
    			member.mRender = true;
    		}    		
    	}
    }
    
    void updateVisuals(OsmDB db) {
    	if (!hasTag("type", "multipolygon")) {
    		Log.d(TAG, "Relation "+mOsmId+" is not a multipolygon");
    		return;
    	}
    	super.updateVisuals(db);
    	mPath.rewind();
    	mPath.setFillType(Path.FillType.EVEN_ODD);
    	for (RelMember member: mElems) {
    		OsmWay way = (OsmWay) member.mElem;
    		if (way != null) {
    			if (this.mOsmId==446026) { Log.d(TAG, "Add Path "+way.mOsmId); }
    			mPath.addPath(way.mPath); // FIXME: Polygons may be build from several ways
    		} else {
    			Log.d(TAG, "Relation w id="+mOsmId+" is missing way with id="+member.mRef+" for role="+member.mRole);
    		}
        }
    	updateVisualDefiningElem();
    }

    public void draw(Canvas canvas, OsmDB db, PaintConfig pcfg) {
    	//Log.d(TAG, "draw Relation: id="+mOsmId);
    	if (!pcfg.wireframe) {
    		pcfg.getWayPaints(this, mVisualDefiningElem);
    		if (pcfg.look != null) {
    			canvas.drawPath(mPath, pcfg.look);
    		}
    		if (pcfg.look2 != null) {
    			canvas.drawPath(mPath, pcfg.look2);
    		}
//    		if (pcfg.nodelook != null) {
//    			for (int ii = 0, size = mNodes.size(); ii<size; ii++)
//    				canvas.drawPoint(mNodes.get(ii).getX(db), mNodes.get(ii).getY(db), pcfg.nodelook);
//	    	}
//    		if (mDrawInners) {
//    	    	for (RelMember member: mElems) {
//    	    		if (member.mRender) {
//    	    			member.mElem.draw(canvas, db, pcfg);
//    	    		}    	    		
//    	    	}
//    		}
    	} else { // Wireframe
    		if (pcfg.getLayer()==1) {
				canvas.drawPath(mPath, pcfg.paint);
    		}
    	}
    }

    // FIXME
    public double distTo(double lon, double lat_merc) { return Double.POSITIVE_INFINITY; }
    public double angleTo(double lon, double lat_merc) { return 0; }
}
