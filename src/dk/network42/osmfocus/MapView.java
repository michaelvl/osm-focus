package dk.network42.osmfocus;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.GpsStatus;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

public class MapView extends View {
    private static final String TAG = "MapView";
    String mLocProvider, mTmpStr;
    GpsStatus mGpsStatus;
    long mLastRedraw;
    SharedData mG;
    ArrayList<OsmElement> mNearest = new ArrayList<OsmElement>(10);
    Rect viewrect = new Rect();
    RectF viewrectf = new RectF();
    RectF worldrect = new RectF();
    int mRedraws=0;
    Context context;
    static public final float prescale = 150000.0f;
    
    public MapView(Context ctx) {
        super(ctx);
        setFocusable(true);
        setFocusableInTouchMode(true);
        context = ctx;
        mLastRedraw = SystemClock.elapsedRealtime();
    }

    @Override
    public void onDraw(Canvas canvas) {
    	long now = SystemClock.elapsedRealtime();

    	canvas.drawARGB(255, 238, 241, 233);
    	
        canvas.getClipBounds(viewrect);
        int h = viewrect.height();
        int w = viewrect.width();
        viewrectf.set(viewrect);
        //Log.d(TAG, "Viewrectf: "+viewrectf.toString());
        mG.mPcfg.viewmatrix.reset();
        canvas.save();
        //canvas.getMatrix().mapRect(viewrectf);
        //Log.d(TAG, "2 Viewrectf: "+viewrectf.toString());
        canvas.translate(w/2,  h/2);
        mG.mPcfg.viewmatrix.preTranslate(w/2,  h/2);
        if (mG.mUseCompass) {
        	canvas.rotate((float)-mG.mAzimuth);
        }

        canvas.save();

        canvas.scale(1.0f, -1.0f); // Up is down
        mG.mPcfg.viewmatrix.preScale(1.0f, -1.0f);
        
        canvas.scale(mG.mPcfg.mScale, mG.mPcfg.mScale); // Current zoom
        mG.mPcfg.viewmatrix.preScale(mG.mPcfg.mScale, mG.mPcfg.mScale);
        
        canvas.translate((float)(-mG.mLon*MapView.prescale), (float)(-GeoMath.latToMercator(mG.mLat)*MapView.prescale)); // Current pos
        mG.mPcfg.viewmatrix.preTranslate((float)(-mG.mLon*MapView.prescale), (float)(-GeoMath.latToMercator(mG.mLat)*MapView.prescale)); // Current pos
        worldrect.set(viewrectf);
        mG.mPcfg.view2world(worldrect);
        //Log.d(TAG, "Viewport="+viewrectf+", world viewport="+worldrect);

        if (mG.mLocationUpdates > 0) {
	        if (mG.mTileLayer != null && (mG.mPcfg.mBackMapType == MapLayer.MAPTYPE_OSM || mG.mPcfg.mBackMapType == MapLayer.MAPTYPE_OSMCYCLEMAP)) {
	        	mG.mTileLayer.draw(canvas, worldrect, mG.mPcfg);
	        }
	        if (mG.mVectorLayer != null) {
	        	mG.mVectorLayer.draw(canvas, worldrect, mG.mPcfg);
	        }
        }
                
        // GPS position
        if (mG.mPhyLocation != null) {
        	canvas.drawCircle((float)(mG.mPhyLocation.getLongitude()*MapView.prescale),
        			(float)(GeoMath.latToMercator(mG.mPhyLocation.getLatitude())*MapView.prescale),
        			(float)(GeoMath.convertMetersToGeoDistanceMed((float)mG.mPhyLocation.getAccuracy())*MapView.prescale), mG.mPcfg.gps);
        }

        canvas.restore(); // Restores zoom scaling, current position translation and 'up is down'

        if (mG.mPhyLocation == null) {
        	String txt = context.getString(R.string.info_locationunknown);
        	canvas.drawText(txt, -mG.mPcfg.debughud.measureText(txt)/2, -3*mG.mPcfg.tagtextsize, mG.mPcfg.debughud);
        	txt = "Satelites: Visible:"+mG.mSatelitesVisible+" used:"+mG.mSatelitesUsed;
        	canvas.drawText(txt, -mG.mPcfg.debughud.measureText(txt)/2, -2*mG.mPcfg.tagtextsize, mG.mPcfg.debughud);
        } else {
            // Compass
//          canvas.save();
//          canvas.translate(w/2,  h/2);
//          canvas.scale(w/2, h/2); //FIXME
//          //canvas.drawLine(0, 0, FloatMath.cos(aarad), FloatMath.sin(aarad), mG.mPcfg.paint);
//          //canvas.drawCircle(FloatMath.cos(aarad), FloatMath.sin(aarad), 5, mG.mPcfg.paint);
//          //canvas.drawCircle(0, 0, 1, mG.mPcfg.paint);
        	
            // Cross-hair
        	float len1 = 2.0f*mG.mPcfg.densityScale;
        	float len2 = 10.0f*mG.mPcfg.densityScale;
            canvas.drawLine(0, -len1, 0, -len2, mG.mPcfg.gps2);
            canvas.drawLine(0,  len1, 0,  len2, mG.mPcfg.gps2);
            canvas.drawLine(-len1, 0, -len2, 0, mG.mPcfg.gps2);
            canvas.drawLine( len1, 0,  len2, 0, mG.mPcfg.gps2);
        }
        
        canvas.restore(); // Restores center (0,0)
        
    	// Tags
        if (mG.mLocationUpdates > 0) {
	        if (mG.mTileLayer != null) {
	        	mG.mTileLayer.drawLegends(canvas, viewrectf, mG.mPcfg);
	        }
	        if (mG.mVectorLayer != null) {
	        	mG.mVectorLayer.drawLegends(canvas, viewrectf, mG.mPcfg);
	        }
        }
        
        canvas.restore();
        long endtime = SystemClock.elapsedRealtime();

        // Debug info
        float dbghudY = h/2;
    	if (mG.mDebugHUD) {
    		canvas.drawText(String.format("View Loc: %.8f,%.8f, upd %d, sats %d/%d, scale %.4f",
    						mG.mLon, mG.mLat, mG.mLocationUpdates, mG.mSatelitesVisible, mG.mSatelitesUsed, mG.mPcfg.mScale),
    					0, dbghudY+mG.mPcfg.mHUDspacing, mG.mPcfg.debughud);
    		if (mG.mPhyLocation != null) {
    			canvas.drawText(String.format("Phy Loc: %.8f,%.8f, Accuracy: %.4f (%s)",
    						mG.mPhyLocation.getLongitude(), mG.mPhyLocation.getLatitude(), mG.mPhyLocation.getAccuracy(), mLocProvider),
    					0, dbghudY+2*mG.mPcfg.mHUDspacing, mG.mPcfg.debughud);
    		} else {
    			canvas.drawText("No phy location yet",
    					0, dbghudY+2*mG.mPcfg.mHUDspacing, mG.mPcfg.debughud);
    		}
    		canvas.drawText(String.format("Azimuth: %.4f, Pitch: %.4f, Roll: %.4f", mG.mAzimuth, mG.mPitch, mG.mRoll),
    				0, dbghudY+3*mG.mPcfg.mHUDspacing, mG.mPcfg.debughud);
    		if (mG.mDb != null) {
    			canvas.drawText(String.format("Nodes: %d, Ways: %d, Relations: %d",
    							mG.mDb.getNodes().size(), mG.mDb.getWays().size(), mG.mDb.getRelations().size()),
    						0, dbghudY+4*mG.mPcfg.mHUDspacing, mG.mPcfg.debughud);
    		}
    		canvas.drawText(String.format("RedrawPeriod: %dms, RedrawTime: %dms, Redraws:%d",
					now-mLastRedraw, endtime-now, ++mRedraws), 0, dbghudY+5*mG.mPcfg.mHUDspacing, mG.mPcfg.debughud);
    		int memTile=0, memXml=0;
    		if (mG.mTileLayer!=null)
    			memTile = mG.mTileLayer.getMemorySize();
    		if (mG.mVectorLayer!=null)
    			memXml = mG.mVectorLayer.getMemorySize();
    		canvas.drawText(String.format("MemoryUse: Tiles=%dk, Vector: %dk",
    					memTile/1024, memXml/1024), 0, dbghudY+6*mG.mPcfg.mHUDspacing, mG.mPcfg.debughud);
    	}
        //Log.d(TAG, "h="+h+"spacing="+mG.mPcfg.mListSpacing);
        mLastRedraw = now;
    }

    public void setSharedData(SharedData data) {
    	mG = data;
    }
}
