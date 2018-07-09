package dk.network42.osmfocus;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.sql.Timestamp;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class CustomExceptionHandler implements UncaughtExceptionHandler {
	private static String TAG = "CustomExceptionHandler";

	private UncaughtExceptionHandler mDefaultHandler;
	private SharedData mG;
	private String mUrl;
	
	public CustomExceptionHandler(SharedData g, String url) {
		mG = g;
		mUrl = url;
		mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
	}

	public void uncaughtException(Thread t, Throwable e) {
		Log.e(TAG, "Exception:"+e);
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		e.printStackTrace(printWriter);
		mG.printState(printWriter);
		String data = result.toString();
        printWriter.close();
        String filename = timestamp + ".txt";
        
        writeFile(filename, data);

        mDefaultHandler.uncaughtException(t, e);
	}

	private void writeFile(String filename, String data) {
		String state = Environment.getExternalStorageState();
	    if ( ! Environment.MEDIA_MOUNTED.equals(state)) {
			Log.e(TAG, "External media not available");	        
	    }
		String filenm = mG.mCtx.getExternalFilesDir(null)+ "/" + filename;
		Log.e(TAG, "Writing crash data to file '"+filenm+"' = "+data);
		try {
			FileOutputStream os = new FileOutputStream(filenm);
            os.write(data.getBytes());
            os.close();
        } catch (IOException e) {
        	Log.e(TAG, "Error writing " + filenm, e);
        }
		Log.e(TAG, "Done writing crash data to file");
	}
}
