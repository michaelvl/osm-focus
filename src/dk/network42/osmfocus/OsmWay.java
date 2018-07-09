package dk.network42.osmfocus;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

public class OsmWay extends OsmElement {
    private static final String TAG = "OsmWay";

    protected final ArrayList<OsmNode> mNodes;
    protected boolean mIsClosed = false;
    protected Path mPath = new Path();
    double mLonClose, mLatMercClose;
    int mCloseStartNode;
    boolean mIsMultipolyOuter = false; // Drawn as relation multipolygon instead if true
	
    OsmWay(final long osmId, final long osmVersion) {
        super(osmId, osmVersion);
        mNodes = new ArrayList<OsmNode>();
    }

    void addNode(final OsmNode node) {
    	//Log.d(TAG, "id="+mOsmId+": ("+node.getLat()+","+node.getLon()+"), nodes="+mNodes.size());
    	//mPath.moveTo(node.getX(), node.getY());
        mNodes.add(node);
    }

    public List getNodes() {
        return mNodes;
    }

    // For screen mapping when db translation is not active
	public float getX() { return ((float) mLonClose*MapView.prescale); }
    public float getY() { return ((float) mLatMercClose*MapView.prescale); }

    public double distTo(double lon, double lat_merc) {
    	int size = mNodes.size();
    	double dd, dst = Double.POSITIVE_INFINITY;
		for (int ii=0; ii<size-1; ii++) {
			dd = GeoMath.getLineDistance((float)lon, (float)lat_merc,
					(float)mNodes.get(ii).getLon(), (float)mNodes.get(ii).getLatMerc(),
					(float)mNodes.get(ii+1).getLon(), (float)mNodes.get(ii+1).getLatMerc());
			if (dd<dst) {
				dst = dd;
				double[] pts = GeoMath.nearestPoint(lon, lat_merc,
						mNodes.get(ii).getLon(), mNodes.get(ii).getLatMerc(),
						mNodes.get(ii+1).getLon(), mNodes.get(ii+1).getLatMerc());
				mLonClose = pts[0];
				mLatMercClose = pts[1];
				mCloseStartNode = ii;
//				Log.d(TAG, "OSMid="+mOsmId+" lon="+lon+", latMerc="+lat_merc+
//						" ii="+ii+
//						" p1("+mNodes.get(ii).mOsmId+")=("+(float)mNodes.get(ii).getLon()+","+(float)mNodes.get(ii).getLatMerc()+")"+
//						" p2("+mNodes.get(ii+1).mOsmId+")=("+(float)mNodes.get(ii+1).getLon()+","+(float)mNodes.get(ii+1).getLatMerc()+")"+
//						", ii="+ii+", dist="+dst);
			}
		}
    	//return Math.hypot(getLon()-lon, GeoMath.latToMercator(getLat())-GeoMath.latToMercator(lat));
		return dst;
    }

    // Assumes distTo() has been called
    public double angleTo(double lon, double lat_merc) {
    	return Math.atan2(mLatMercClose-lat_merc, mLonClose-lon);
    }

    void updateVisuals(OsmDB db)
    {
    	super.updateVisuals(db);
    	updatePath(db);
    }
    
    private void updatePath(OsmDB db)
    {
    	//Log.d(TAG, "updatePath() for way: name="+getTagWithKey("name")+" nodes="+mNodes.size());
    	int size = mNodes.size();
    	mPath.rewind();
    	mPath.moveTo(mNodes.get(0).getX(db), mNodes.get(0).getY(db));
		for (int ii=1; ii<size; ii++) {
			mPath.lineTo(mNodes.get(ii).getX(db), mNodes.get(ii).getY(db));
		}
    }
    
	public void draw(Canvas canvas, OsmDB db, PaintConfig pcfg)
	{
    	//Log.d(TAG, "draw Way: name="+getTagWithKey("name")+" nodes="+mNodes.size());
    	Paint look, look2;
    	Paint nodelook;
    	if (!pcfg.wireframe) {
    		if (mIsMultipolyOuter)
    			return;
    		pcfg.getWayPaints(this, null);
    		look = pcfg.look;
    		look2 = pcfg.look2;
    		nodelook = pcfg.nodelook;
    		if (mOsmId==52009742 || mOsmId==52009746 || mOsmId==52009741 || mOsmId==52009739) {
    			Log.d(TAG, "Id="+mOsmId+": Multio="+mIsMultipolyOuter+" look="+look+" look2="+look2+" nodelook="+nodelook);
    		}
    		if (look != null) {
    			canvas.drawPath(mPath, look);
    		}
    		if (look2 != null) {
    			canvas.drawPath(mPath, look2);
    		}
    		if (nodelook != null) {
    			for (int ii = 0, size = mNodes.size(); ii<size; ii++)
    				canvas.drawPoint(mNodes.get(ii).getX(db), mNodes.get(ii).getY(db), nodelook);
	    	}
    	} else { // Wireframe
    		if (pcfg.getLayer()==0) {
    	    	canvas.drawPath(mPath, pcfg.paint);
//    			int size = mNodes.size();
//    			for (int ii=0; ii<size-1; ii++) {
//    				canvas.drawLine(mNodes.get(ii).getX(), mNodes.get(ii).getY(),
//    						mNodes.get(ii+1).getX(), mNodes.get(ii+1).getY(), pcfg.paint);
//    			}
    	    	for (int ii = 0, size = mNodes.size(); ii<size; ii++) {
    	    		//canvas.drawPoint(mNodes.get(ii).getX(db), mNodes.get(ii).getY(db), pcfg.paint);
    	    		canvas.drawCircle(mNodes.get(ii).getX(db), mNodes.get(ii).getY(db), 1.0f/pcfg.mScale, pcfg.paint);
    			}
        	}
    	}
    }
    
    public void highlight(Canvas canvas, OsmDB db, PaintConfig pcfg, int style)
    {
//    	Log.d(TAG, "Highlight("+mOsmId+") mCloseStartNode="+mCloseStartNode+" = ("+
//    				mNodes.get(mCloseStartNode).getX(db)+","+mNodes.get(mCloseStartNode).getY(db)+")-("+
//    				mNodes.get(mCloseStartNode+1).getX(db)+","+mNodes.get(mCloseStartNode+1).getY(db)+")");
    	//canvas.drawLine(mNodes.get(mCloseStartNode).getX(db), mNodes.get(mCloseStartNode).getY(db),
		//			    mNodes.get(mCloseStartNode+1).getX(db), mNodes.get(mCloseStartNode+1).getY(db), pcfg.focus[style]);
    	canvas.drawPath(mPath, pcfg.focus[style]);
    }
}
