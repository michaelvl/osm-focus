package dk.network42.osmfocus;

import java.io.PrintWriter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

public class SharedData {
    private static final String TAG = "SharedData";

    private static final String PREF_VER_KEY = "prefVersion";
    
	public OsmDB mDb = new OsmDB();
	public OsmTileProvider mTileLayerProvider;
	public OsmTileLayerBm mTileLayer = null;
	public OsmTileProvider mVectorLayerProvider;
	public OsmTileLayerVector mVectorLayer = null;
	
	public boolean mUseCompass = false;
    boolean mDebugHUD = false;
	public boolean mFollowGPS = true;
    public double mLon, mLat;  // View location
    long mLocationUpdates = 0;
    int mSatelitesVisible = 0;
    int mSatelitesUsed = 0;
    Location mPhyLocation = null; // Physical location
    public String mLocProvider;
    public double mAzimuth, mPitch, mRoll;
    PaintConfig mPcfg = new PaintConfig();

    final boolean mDeveloperMode = false;
    
    static final int AUTODOWNLOAD_MANUAL        = 1;
    static final int AUTODOWNLOAD_AUTOMATIC1    = 2;
    static final int AUTODOWNLOAD_AUTOMATIC2    = 3;
    int mVectorAutoDownload = AUTODOWNLOAD_AUTOMATIC1;

    boolean mShowPoiLines = true;
    int mPoisToShow = 0;
    
    String mOsmServerAgentName = new String("OSMfocus");

    Context mCtx;

    public void update(Context ctx) {
    	mCtx = ctx;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	mVectorAutoDownload = Integer.parseInt(sharedPrefs.getString("pref_autoload", "2"));
    	mShowPoiLines = sharedPrefs.getBoolean("pref_poilines", false);

    	int numpoi = Integer.parseInt(sharedPrefs.getString("pref_poinum", "0"));
    	boolean largescreen = (ctx.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
        boolean landscape = (ctx.getResources().getConfiguration().orientation & Configuration.ORIENTATION_LANDSCAPE) != 0;
    	//Log.d(TAG, "Pref:POI num=" + numpoi+" isLargeScreen="+largescreen+" landscape="+landscape);
    	if (numpoi == 0) { // Auto
	    	if (largescreen || landscape) {
	    		mPoisToShow = 8;
	    	} else {
	    		mPoisToShow = 4;    		
	    	}
	    	Log.d(TAG, "Auto POI num=" + mPoisToShow);
    	} else {
    		mPoisToShow = numpoi;
    	}

    	int backtype = Integer.parseInt(sharedPrefs.getString("pref_backmaptype", "1"));
    	if (mPcfg.mBackMapType != backtype) {
    		mPcfg.mBackMapType = backtype;
            mTileLayer.setProviderUrl(OsmTileLayer.urlFromType(mPcfg.mBackMapType));
    	}
        mPcfg.update(ctx);
    }

    public void printState(PrintWriter pw) {
    	pw.print("Loc="+mLon+","+mLat);
    	pw.print("PhyLoc="+mPhyLocation);
    }

    static void checkAppUpdate(Context ctx) {
//    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
//    	int ver = getAppVersion(ctx);
//    	int prefver = prefs.getInt(PREF_VER_KEY, 0);
//    	SharedPreferences.Editor ed = prefs.edit();
//    	if (prefver >= 0) {
//    		if (ver==2) {
//    			Log.d(TAG, "Do pref update, have "+prefver+" wants "+ver);
//    			ed.remove("pref_autoload");
//    			ed.apply();
//    		}
//    	}
//    	ed.putInt(PREF_VER_KEY, prefver);
//    	ed.apply();
    }

    static int getAppVersion(Context ctx) {
    	int ver = -1;
    	try {
    		ver = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
    	} catch (NameNotFoundException e) {
    		Log.e(TAG, "Could not find package name");
    	}
    	return ver;
    }
}
