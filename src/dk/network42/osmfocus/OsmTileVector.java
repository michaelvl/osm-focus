package dk.network42.osmfocus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.graphics.Canvas;
import android.util.Log;

public class OsmTileVector extends OsmTile {
	private static final String TAG = "OsmTileVector";
	OsmDB mDb = null;

	public OsmTileVector(int x, int y, int zoom) {
		super(x, y, zoom);
	}

	public boolean canDraw(Canvas canvas, PaintConfig pcfg) {
		return (mDb != null);		
	}

	public boolean draw(Canvas canvas, PaintConfig pcfg) {
		if (mDb != null) {
			//Log.d(TAG, "Draw vector tile at "+mBox2+" ("+mViewBox+") "+mDb);
			if (pcfg.wireframe || pcfg.mBackMapType == MapLayer.MAPTYPE_INTERNAL) {
				mDb.draw(canvas, mViewBox, pcfg);
			}
			return true;
		} else if (mDownloadErrs>0) {
			drawError(canvas, pcfg);
		}
		return false;
	}

	public void drawX(Canvas canvas, PaintConfig pcfg) {
		drawFade(canvas, pcfg);
	}

	public boolean download(String useragent, String provider) {
		//Log.d(TAG, "Download vector");
		//Log.d(TAG, "Acquire Osm server stream, box="+mBox);
		try {
			InputStream istream = OsmServer.getStreamForArea(null, useragent, mBox);
			OsmDB db = loadData(istream, mBox.toString());
			if (db != null) {
				db.updateAndVacuum();
				mDb = db;
				Log.d(TAG, "Read db tile: "+mBox+" TID="+this);
				return true;
			} else {
				Log.e(TAG, "Reading of db tile failed "+mBox);
			}
		} catch (OsmServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException pce) {
			Log.e(TAG, "SAX parse error", pce);
		} catch (SAXException se) {
			Log.e(TAG, "SAX error", se);
		}
		return false;
	}


    protected OsmDB loadData(InputStream istream, String info) throws ParserConfigurationException, SAXException, IOException {
		String start = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
		//Log.d(TAG, "Start download at "+start+", info="+info);
		SAXParserFactory spf = SAXParserFactory.newInstance(); 
		SAXParser sp = spf.newSAXParser(); 
		XMLReader xr = sp.getXMLReader(); 
		OsmParser parser = new OsmParser(); 
		xr.setContentHandler(parser);
		xr.parse(new InputSource(istream));
		String end = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
		//Log.d(TAG, "Download finished at "+end+", info="+info);
		OsmDB db = parser.getStorage();
		return db;
//			db.updateAndVacuum();
//			db.merge(mG.mDb);
//			String enddb = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
//			Log.d(TAG, "Database merged at "+enddb+", info="+info);
//			mG.mDb = db;
//			mapView.postInvalidate();                          
    }

}
