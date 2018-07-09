package dk.network42.osmfocus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class MainActivity extends Activity implements
				GooglePlayServicesClient.ConnectionCallbacks,
				GooglePlayServicesClient.OnConnectionFailedListener,
				LocationListener, GpsStatus.Listener,
				SensorEventListener {
	
	static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    static final int PREFERENCE_REQUEST   = 9001;
	static final int INVALIDATE_VIEW      = 1000;
	static final int POLL_NOTIFICATIONS   = 1001;

	private static final String TAG = "OsmFocusActivity";
	public static final String PREFS_NAME = "OSMFocusPrefsFile";

	LocationManager mLocationManager;
	LocationClient mLocationClient;
	LocationRequest mLocationRequest;
	
	SensorManager sensorManager;
	private Sensor sensorAccelerometer;
	private Sensor sensorMagneticField;   
	private float[] valuesAccelerometer;
	private float[] valuesMagneticField;
	private float[] matrixR;
	private float[] matrixI;
	private float[] matrixValues;
	private MapView mapView;
    double mPanLon, mPanLat;
	private OsmServer mOsmServer = new OsmServer(null, "Editor");
	private GestureDetectorCompat mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;
	private boolean mScaleInProgress = false;
    SharedData mG = null;
	public double mLonLastUpd=0, mLatLastUpd=0;  // Where mapview was last updated

    NotificationManager mNotificationManager;
	Handler mHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedData.checkAppUpdate(getApplicationContext());
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        
        //setContentView(R.layout.activity_main);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
        //        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        getActionBar().setDisplayShowTitleEnabled(false);

        Log.i(TAG, "API level "+Build.VERSION.SDK_INT);

		mHandler = new Handler(Looper.getMainLooper()) {
			int mNotifId;
			boolean mNotifActive;
			@Override
	        public void handleMessage(Message inputMessage) {
				switch (inputMessage.what) {
				case INVALIDATE_VIEW:
					//Log.d(TAG, "Invalidate view");
					mapView.invalidate();
					// Fall-through
				case POLL_NOTIFICATIONS:
					int dlsb = mG.mTileLayerProvider.getActiveDownloads();
					int dlsv = mG.mVectorLayerProvider.getActiveDownloads();
					Log.d(TAG, "Downloads active="+dlsb+"+"+dlsv);
					if (dlsb>0 || dlsv>0) {
						if (! mNotifActive) {
							String st = "Downloading...";
							mNotifId = setOsmLoadNotif(st);
							mNotifActive = true;
						} else {
							Log.d(TAG, "Notification already active");
							mHandler.sendMessageDelayed(mHandler.obtainMessage(POLL_NOTIFICATIONS, this), 1000);
						}
					} else if (mNotifActive) {
						cancelOsmLoadNotif(mNotifId);
						mNotifActive = false;
					}
					break;
				}
			}
		};

        mapView = new MapView(this);
        //mG.mPcfg.update(getBaseContext());
        setContentView(mapView);
        mapView.requestFocus();
        if (getLastNonConfigurationInstance() != null) {
        	Log.d(TAG, "getLastNonConfigurationInstance() != null");
        	mG = (SharedData) getLastNonConfigurationInstance();
            mapView.setSharedData(mG);
            mG.mTileLayer.setMainHandler(mHandler);
            mG.mVectorLayer.setMainHandler(mHandler);
            mG.update(getApplicationContext());
        } else {
	        long maxMemL = Runtime.getRuntime().maxMemory();
	        int maxMem = (int) Math.min(maxMemL, Integer.MAX_VALUE);
	        Log.i(TAG, "maxMemory="+maxMem);
	        mG = new SharedData();
            mapView.setSharedData(mG);
	        mG.mTileLayerProvider = new OsmTileProvider(mG.mOsmServerAgentName, OsmTile.MAX_DOWNLOAD_THREADS);
	        mG.mTileLayer = new OsmTileLayerBm(mG.mTileLayerProvider, maxMem/4);
	        mG.mTileLayer.setAttrib(getApplicationContext().getString(R.string.info_osm_copyright));
            mG.mTileLayer.setMainHandler(mHandler);
            mG.mTileLayer.setProviderUrl(OsmTileLayer.urlFromType(mG.mPcfg.mBackMapType));
	        mG.mVectorLayerProvider = new OsmTileProvider(mG.mOsmServerAgentName);
	        mG.mVectorLayer = new OsmTileLayerVector(mG.mVectorLayerProvider, maxMem/8);
	        mG.mVectorLayer.setSharedData(mG);
            mG.mVectorLayer.setMainHandler(mHandler);
	        mG.update(getApplicationContext());
        }

        if (mG.mDeveloperMode) {
	        if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
	            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(mG, ""));
	        }
        }

        mGestureDetector = new GestureDetectorCompat(this, mGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(this, mScaleGestureListener);
        /*mDetector.setOnDoubleTapListener(this);*/
        
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.addGpsStatusListener(this);
        Location loc = getMostRecentKnownLocation();
        if (loc != null)
        	this.onLocationChanged(loc);  
        
        mLocationClient = new LocationClient(this, this, this);
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000/*ms*/);
        mLocationRequest.setFastestInterval(500/*ms*/);
        
        //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        //	Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        //	startActivity(intent);
        //}
        //Criteria criteria = new Criteria();
        //mG.mLocProvider = locationManager.getBestProvider(criteria, false);
        //mG.mLocProvider = locationManager.GPS_PROVIDER;
        //Location location = locationManager.getLastKnownLocation(mG.mLocProvider);
        //if (location != null) {
        //    Toast toast = Toast.makeText(getApplicationContext(), "Location provider "+mG.mLocProvider, Toast.LENGTH_SHORT);
        //    toast.show();
        //    //onLocationChanged(location);
        //}

        //Location location = new Location("FIXME");
        //Location location = mLocationClient.getLastLocation();

        if (mG.mUseCompass) {
        	sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        	sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        	sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        	valuesAccelerometer = new float[3];
        	valuesMagneticField = new float[3];
        	matrixR = new float[9];
        	matrixI = new float[9];
        	matrixValues = new float[3];
        }
        
        // Initialize the location fields
        //if (location != null) {
        //  Log.d(TAG, "Provider " + mG.mLocProvider + " has been selected.");
        //  onLocationChanged(location);
        //} else {
          //latituteField.setText("Location not available");
          //longitudeField.setText("Location not available");
        //}
        
        //registerForContextMenu(mapView);
        
        registerComponentCallbacks(new ComponentCallbacks2() {
        	@ Override
        	public void onTrimMemory(int level)  {
        		appTrimMemory(level);
        	}
        	public void onLowMemory() {
                appTrimMemory(TRIM_MEMORY_COMPLETE);
        	}
        	public void onConfigurationChanged(Configuration newConfig) {
        		//
        	}
        });
    }

    public void appTrimMemory(int level) {
        Log.d(TAG, "appTrimMemory("+level+")");
        if (mG.mTileLayer != null)
        	mG.mTileLayer.onTrimMemory(level);
        if (mG.mVectorLayer != null)
        	mG.mVectorLayer.onTrimMemory(level);    	
    }

    public Object onRetainNonConfigurationInstance() {
    	if (mG != null)
    		return mG;
    	return super.onRetainNonConfigurationInstance();
    }
    
    /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
    	if (mLocationClient.isConnected()) {
    		mLocationClient.removeLocationUpdates(this);
        }
        mLocationClient.disconnect();
        super.onStop();
    }
    
    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
                switch (resultCode) {
                    case Activity.RESULT_OK:
                    break;
                }
            case PREFERENCE_REQUEST:
    			mG.update(getBaseContext());
    			mapView.postInvalidate();                          
            	break;
        }
    }

	private boolean servicesConnected() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d("Location Updates", "Google Play services is available.");
            return true;
        } else {
            //int errorCode = GooglePlayServicesUtil.getErrorCode();
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                    							CONNECTION_FAILURE_RESOLUTION_REQUEST);
            if (errorDialog != null) {
            	errorDialog.show();
            } else {
            	showOkDialog(this, "Something went wrong with Google Play Services");
            }
            return false;
        }
    }
    
    public static void showOkDialog(Context context, String txt)
    {
        Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(txt);
        builder.setCancelable(true);
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    @Override
    public void onConnected(Bundle dataBundle) {
    	Log.d(TAG, "onConnected");
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            //showErrorDialog(connectionResult.getErrorCode());
        }
    }
    
    @Override 
    public boolean onTouchEvent(MotionEvent event){
        boolean h = mScaleGestureDetector.onTouchEvent(event);
        h |= mGestureDetector.onTouchEvent(event);
        h |= super.onTouchEvent(event);
        return h;
    }

    private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
    = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
    	@Override
	    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();
	    	Log.d(TAG, "ScaleBegin, focus="+focusX+","+focusY);
	    	return true;
	    }
    	@Override
    	public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();
            float scale = scaleGestureDetector.getScaleFactor();
	    	Log.d(TAG, "Scale, scale="+scale+", focus="+focusX+","+focusY);
	    	mG.mPcfg.setScale(mG.mPcfg.getScale()*scale, mG.mCtx);
	    	mScaleInProgress = true;
	    	mapView.postInvalidate();
	    	return true;
	    }
    	@Override
	    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
	    	Log.d(TAG, "ScaleEnd");
	    }
    };
    
    private final GestureDetector.SimpleOnGestureListener mGestureListener
    = new GestureDetector.SimpleOnGestureListener() {
    	@Override
    	public boolean onDown(MotionEvent event) { 
            //Log.d(TAG,"onDown: " + event.toString());
            mPanLon = mG.mLon; mPanLat = mG.mLat;
	    	mScaleInProgress = false;
	    	return true;
        }
    	@Override
    	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
    		if (mScaleInProgress) {
                Log.d(TAG, "Scroll skipped");
    			return false;
    		}
            //Log.d(TAG, "onScroll: " + e1.toString()+e2.toString());
    		float dx, dy;
    		dx = e2.getX()-e1.getX();
    		dy = e2.getY()-e1.getY();
            Log.d(TAG, "Scroll pixels: x="+dx+" y="+dy);
            mG.mLon = mPanLon - dx/(mG.mPcfg.mScale*MapView.prescale);
            double panMercLat = dy/(mG.mPcfg.mScale*MapView.prescale);
            mG.mLat = GeoMath.mercatorToLat((GeoMath.latToMercator(mPanLat)+panMercLat));
            Log.d(TAG, "Scroll to lon="+mG.mLon+" lat="+mG.mLat+" ("+GeoMath.latToMercator(mPanLat)+"+"+panMercLat+")");
            mG.mFollowGPS = false;
            mapView.postInvalidate();
            return true;
        }
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, 
                float velocityX, float velocityY) {
            Log.d(TAG, "onFling: " + event1.toString()+event2.toString());
            return true;
        }

//        @Override
//        public void onLongPress(MotionEvent event) {
//            Log.d(TAG, "onLongPress: " + event.toString());
//            openOptionsMenu();
//        }
        //@Override
        //public void onShowPress(MotionEvent event) {
        //    Log.d(TAG, "onShowPress: " + event.toString());
        //}
        
        //@Override
        //public boolean onSingleTapUp(MotionEvent event) {
        //    Log.d(TAG, "onSingleTapUp: " + event.toString());
        //    return true;
       // }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.main_activity_actions, menu);
    	if (mG.mDeveloperMode) {
	    	menu.add(0, R.id.action_testdownload, Menu.NONE, R.string.action_testdownload);
	    	menu.add(0, R.id.action_togglehud, Menu.NONE, R.string.action_togglehud);
    	}
        return super.onCreateOptionsMenu(menu);
    }

    //@Override
    //public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    //	super.onCreateContextMenu(menu, v, menuInfo);
    //    getMenuInflater().inflate(R.menu.main, menu);
    //}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	int itemId = item.getItemId();
		if (itemId == R.id.action_download) {
			if (mG.mPhyLocation != null) {
				Log.d(TAG, "Download");
				//float horizon = 250f; //meters
				//GeoBBox bbox = OsmServer.getBoxForPoint(mG.mLat, mG.mLon, horizon);
				//GeoBBox bbox = OsmTile.pos2BBox(mG.mLon, mG.mLat, 16);
				//downloadBox(bbox);
				// FIXME
				mG.mVectorLayer.download(mG.mLon, mG.mLat);
			} else {
		    	Toast toast = Toast.makeText(getApplicationContext(), getBaseContext().getString(R.string.info_locationunknown), Toast.LENGTH_SHORT);
		        toast.show();
			}
			return true;
//		} else if (itemId == R.id.action_testdownload) {
//			Log.d(TAG, "Test Download");
//			float horizon = 250f; //meters
//			GeoBBox bbox = OsmServer.getBoxForPoint(mG.mLat, mG.mLon, horizon);
//			olddownloadBox(bbox);
//			return true;
//		} else if (itemId == R.id.action_loadcache) {
//			Log.d(TAG, "Load Cache");
//			readOSMFile("cache.osm");
//			return true;
		} else if (itemId == R.id.action_whereami) {
			Log.d(TAG, "Where am I?");
			mG.mFollowGPS = true;
			mG.mPcfg.setScale(PaintConfig.DEFAULT_ZOOM, mG.mCtx);
			if (mG.mPhyLocation != null) {
				mG.mLat = mG.mPhyLocation.getLatitude();
				mG.mLon = mG.mPhyLocation.getLongitude();
				mapView.postInvalidate();
			} else {
		    	Toast toast = Toast.makeText(getApplicationContext(), getBaseContext().getString(R.string.info_locationunknown), Toast.LENGTH_SHORT);
		        toast.show();
			}
			return true;
		} else if (itemId == R.id.action_togglehud) {
			mG.mDebugHUD = !mG.mDebugHUD;
			mapView.invalidate();
			return true;
		} else if (itemId == R.id.action_settings) {
			startActivityForResult(new Intent(this, SettingsActivity.class), PREFERENCE_REQUEST);
			mG.update(getApplicationContext());
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	//locationManager.requestLocationUpdates(mG.mLocProvider, 1000/*ms*/, 1/*meters*/, this);
    	if (mG.mUseCompass) {
    		sensorManager.registerListener(this,
    				sensorAccelerometer,
    				SensorManager.SENSOR_DELAY_NORMAL);
    		sensorManager.registerListener(this,
    				sensorMagneticField,
    				SensorManager.SENSOR_DELAY_NORMAL);
    	}
    }

    @Override
    protected void onPause() {
    	super.onPause();
    	if (mG.mUseCompass) {
    		sensorManager.unregisterListener(this,
    				sensorAccelerometer);
    		sensorManager.unregisterListener(this,
    				sensorMagneticField);
    	}
    }

    protected Location getMostRecentKnownLocation() {
    	Location loc = null;
    	long besttime = 0;
    	List<String> allp = mLocationManager.getAllProviders();
    	for (String p: allp) {
    		Location l = mLocationManager.getLastKnownLocation(p);
    		if (l != null) {
    			long time = l.getTime();
    			if (time>besttime) {
    				loc = l;
    				besttime = time;
    			}
    		}
    	}
    	return loc;
    }

    @Override
    public void onLocationChanged(Location location) {
    	mG.mPhyLocation = location;
		mG.mLocationUpdates++;
		double curr_lon = mG.mPhyLocation.getLongitude();
		double curr_lat = mG.mPhyLocation.getLatitude();
    	if (mG.mFollowGPS) {
    		mG.mLon = curr_lon;
    		mG.mLat = curr_lat;
    	}
        //Log.d(TAG, "onLocationChanged, Loc="+mG.mPhyLocation);
        final double noise = 0.00002;
        if (Math.abs(mLonLastUpd-curr_lon)>noise || Math.abs(mLatLastUpd-curr_lat)>noise) {
        	mLonLastUpd = curr_lon;
        	mLatLastUpd = curr_lat;
        	mapView.postInvalidate();
        }
    }

    public void onGpsStatusChanged(int event) {
    	boolean statchg = false;
    	switch (event) {
    	case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
	    	GpsStatus gstat = mLocationManager.getGpsStatus(null);
	        if (gstat != null) {
	        	Iterable<GpsSatellite> satellites = gstat.getSatellites();
	        	Iterator<GpsSatellite> sat = satellites.iterator();
	        	int num = 0, used = 0;
	        	while (sat.hasNext()) {
	        		num++;
	        		GpsSatellite satellite = sat.next();
	        		if (satellite.usedInFix()) {
	        			used++;
	        		}
	        	}
	        	if (mG.mSatelitesVisible != num || mG.mSatelitesUsed != used)
	        		statchg = true;
	        	mG.mSatelitesVisible = num;
	        	mG.mSatelitesUsed = used;
	        }
	        break;
    	}
    	if (statchg)
    		mapView.postInvalidate();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    	switch(event.sensor.getType()){
    	case Sensor.TYPE_ACCELEROMETER:
    		for(int i =0; i < 3; i++){
    			valuesAccelerometer[i] = event.values[i];
    		}
    		break;
    	case Sensor.TYPE_MAGNETIC_FIELD:
    		for(int i =0; i < 3; i++){
    			valuesMagneticField[i] = event.values[i];
    		}
    		break;
    	}
       
    	boolean success = SensorManager.getRotationMatrix(
    			matrixR,
    			matrixI,
    			valuesAccelerometer,
    			valuesMagneticField);
       
    	if(success){
    		SensorManager.getOrientation(matrixR, matrixValues);
        
    		mG.mAzimuth = Math.toDegrees(matrixValues[0]);
    		mG.mPitch = Math.toDegrees(matrixValues[1]);
    		mG.mRoll = Math.toDegrees(matrixValues[2]);
    		//Log.d(TAG, "Orientation: Azimuth " + azimuth + ", Pitch " + pitch + ", Roll " + roll);
    		mapView.postInvalidate();

    	}
    }

    int setOsmLoadNotif(String info) {
		int notifyID = 1;
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(/*this*/getApplicationContext())
		.setContentTitle("Downloading OSM Data")
		.setContentText(info)
		.setSmallIcon(R.drawable.ic_downloadosm);
//		Intent resultIntent = new Intent(/*this*/getApplicationContext(), MainActivity.class); //FIXME
//		PendingIntent resultPendingIntent =
//				PendingIntent.getActivity(
//						/*this*/getApplicationContext(),
//						0,
//						resultIntent,
//						PendingIntent.FLAG_UPDATE_CURRENT
//						);
//		mBuilder.setContentIntent(resultPendingIntent);
		mNotificationManager.notify(
				notifyID,
				mBuilder.build());
		Log.d(TAG, "Set OSM notif, id="+notifyID);
		return notifyID;
    }
    
    void cancelOsmLoadNotif(int notifyID) {
		Log.d(TAG, "Clear OSM notif, id="+notifyID);
    	mNotificationManager.cancel(notifyID);
    }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
}
