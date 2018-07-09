
package dk.network42.osmfocus;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import android.util.Log;

// See http://wiki.openstreetmap.org/wiki/API_v0.6
public class OsmServer {
    private static final String TAG = "OsmServer";
    
    static private final String API_VERSION = "0.6";
    static private final int SERVER_CONNECT_TIMEOUT_MS = 30*1000;
    static private final int API_TIMEOUT_MS = 10*1000;
    static final int API_MAX_DOWNLOAD_DEGREES = (int) 1E7/4;
    static private final String DEFAULT_API_URL = "http://api.openstreetmap.org/api/"+API_VERSION+"/";

    private final String mApiUrl;
    private final String mAgent;
    // See also http://wiki.openstreetmap.org/index.php/Getting_Data#Construct_an_URL_for_the_HTTP_API
    //  "The server may reject your region if it is larger than 1/4 degree in either dimension."
    
    public OsmServer(String apiUrl, String agent) {
    	assert (agent!=null && agent.isEmpty());
    	mAgent = agent;
    	if (apiUrl==null || apiUrl.isEmpty()) {
    		mApiUrl = DEFAULT_API_URL;
    	} else {
    		mApiUrl = apiUrl;
    	}
    }

    // GET /api/0.6/map?bbox=left,bottom,right,top
    // where:
    //	left is the longitude of the left (westernmost) side of the bounding box.
    //	bottom is the latitude of the bottom (southernmost) side of the bounding box.
    //	right is the longitude of the right (easternmost) side of the bounding box.
    //	top is the latitude of the top (northernmost) side of the bounding box.
    //
    // Example:
    //  http://api.openstreetmap.org/api/0.6/map?bbox=11.54,48.14,11.543,48.145
    //
    // Error codes
    // HTTP status code 400 (Bad Request)
    //  When any of the node/way/relation limits are crossed
    // HTTP status code 509 (Bandwidth Limit Exceeded)
    //  "Error: You have downloaded too much data. Please try again later."
    //
    static public InputStream getStreamForArea(String apiUrl, String agent, final GeoBBox area) throws OsmServerException, IOException {
    	assert (agent!=null && agent.isEmpty());
    	if (apiUrl==null || apiUrl.isEmpty()) {
    		apiUrl = DEFAULT_API_URL;
    	}
    	URL url = new URL(apiUrl+"map?bbox="+getApiString(area));
		Log.d(TAG, "URL='"+url+"'");
    	HttpURLConnection con = (HttpURLConnection) url.openConnection();
    	con.setConnectTimeout(SERVER_CONNECT_TIMEOUT_MS);
    	con.setReadTimeout(API_TIMEOUT_MS);
    	con.setRequestProperty("Accept-Encoding", "gzip"); // Default, but set it explicitly anyway
    	con.setRequestProperty("User-Agent", agent);

    	if (con.getResponseCode() == -1) {
    		Log.w(TAG, "No response code for '"+url+"'");
    		throw new OsmServerException(-1, "No response code for '"+url+"'");
    		// TODO: Retry?
    	}
    	if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
    		Log.w(TAG, "Request NACK for '"+url+"'");
    		throw new OsmServerException(-1, "Request NACK for '"+url+
    				"', code "+con.getResponseCode()+
    				", message '"+con.getResponseMessage()+"'");
    	}
    	
    	if ("gzip".equals(con.getHeaderField("Content-encoding"))) {
    		return new GZIPInputStream(new BufferedInputStream(con.getInputStream()));
    	} else {
    		return new BufferedInputStream(con.getInputStream());
    	}
    }

    public InputStream getStreamForArea(final GeoBBox area) throws OsmServerException, IOException {
    	return getStreamForArea(mApiUrl, mAgent, area);
    }

    // See also http://wiki.openstreetmap.org/index.php/Getting_Data#Construct_an_URL_for_the_HTTP_API
    //  "The server may reject your region if it is larger than 1/4 degree in either dimension."
    static public boolean isValidSizeForApi(GeoBBox box) {
		int wx = box.right-box.left;
		int wy = box.top-box.bottom;
		return (wx<=API_MAX_DOWNLOAD_DEGREES/2 && wy <= API_MAX_DOWNLOAD_DEGREES/2);
    }

    public static void makeBboxSizeValidForApi(GeoBBox box) {
    	if (! isValidSizeForApi(box)) {
    		int cx = box.right/2+box.left/2;
    		int cy = box.top/2+box.bottom/2;
    		box.left = cx-API_MAX_DOWNLOAD_DEGREES/2;
    		box.bottom = cy-API_MAX_DOWNLOAD_DEGREES/2;
    		box.right = cx+API_MAX_DOWNLOAD_DEGREES/2;
    		box.top = cy+API_MAX_DOWNLOAD_DEGREES/2;
    		box.check();
    	}
    }

	// Get BBox centered at point with given size.  API maximums are enforced and box shrunk accordingly
	// Size ('meters') measured to both sides of central point, i.e. box will be 2*meters wide
	public static GeoBBox getBoxForPoint(final double lat, final double lon, final double meters) {
		GeoBBox box = GeoBBox.getBoxForPoint(lat, lon, meters);
		makeBboxSizeValidForApi(box);
		return box;
	}

    // See also http://wiki.openstreetmap.org/index.php/Getting_Data#Construct_an_URL_for_the_HTTP_API
    // Example: http://api.openstreetmap.org/api/0.6/map?bbox=11.54,48.14,11.543,48.145
	public static String getApiString(GeoBBox box) {
		return ""+box.left/1E7+","+box.bottom/1E7+","+box.right/1E7+","+box.top/1E7;
	}
}
