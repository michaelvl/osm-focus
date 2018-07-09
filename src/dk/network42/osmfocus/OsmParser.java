package dk.network42.osmfocus;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class OsmParser extends DefaultHandler {
	private static final String TAG = "OsmParser";
	private OsmDB mDb;
    private OsmNode mCurrentNode;
    private OsmWay mCurrentWay;
    private OsmRelation mCurrentRel;
    private OsmBounds mCurrentBound;
    private final ArrayList<Exception> exceptions;

    public OsmParser() {
        super();
        mDb = new OsmDB();
        mCurrentNode = null;
        mCurrentWay = null;
        mCurrentRel = null;
        mCurrentBound = null;
        exceptions = new ArrayList<Exception>();
	}
    
    public OsmDB getStorage() {
        return mDb;
    }

	public void startElement(final String uri, final String name, final String qName, final Attributes atts) {
		//Log.d(TAG, "startElement(), name="+name);
        try {
            if (isOsmElement(name)) {
                parseOsmElement(name, atts);
            } else if (isWayNode(name)) {
            	parseWayNode(atts);
            } else if (isTag(name)) {
                parseTag(atts);
            } else if (isRelationMember(name)) {
                parseRelationElement(atts);
            } else if (isBounds(name)) {
            	parseBounds(atts);
            }
        } catch (OsmParseException e) {
            Log.e(TAG, "OsmParseException", e);
            exceptions.add(e);
        }
	}
	
	public void endElement(final String uri, final String name, final String qName) {
		//Log.d(TAG, "endElement(), name="+name);
        if (isNode(name)) {
            mDb.insertNode(mCurrentNode);
            mCurrentNode = null;
        } else if (isWay(name)) {
            mDb.insertWay(mCurrentWay);
            mCurrentWay = null;
        } else if (isRelation(name)) {
            mDb.insertRelation(mCurrentRel);
            mCurrentRel = null;
        } else if (isBounds(name)) {
            mDb.insertBounds(mCurrentBound);
            mCurrentBound = null;
        }
	}

    private void parseOsmElement(final String name, final Attributes atts) throws OsmParseException {
        try {
        	long osmId = Long.parseLong(atts.getValue("id"));
        	long osmVersion = Long.parseLong(atts.getValue("version"));
                
        	if (isNode(name)) {
        		int lon = (int) (Double.parseDouble(atts.getValue("lon")) * 1E7);
        		int lat = (int) (Double.parseDouble(atts.getValue("lat")) * 1E7);
        		mCurrentNode = mDb.createNode(osmId, osmVersion/*, status*/, lon, lat);
        		//Log.d(TAG, "Node: id="+osmId+" pos="+lon+","+lat);
            } else if (isWay(name)) {
            	mCurrentWay = mDb.createWay(osmId, osmVersion/*, status*/);
            	//Log.d(TAG, "Way: id="+osmId);
            } else if (isRelation(name)) {
            	mCurrentRel = mDb.createRelation(osmId, osmVersion/*, status*/);
            	//Log.d(TAG, "Relation: id="+osmId);
            }
        } catch (NumberFormatException e) {
        	throw new OsmParseException("Element unparsable");
        }
    }

    private void parseBounds(final Attributes atts) throws OsmParseException {
        try {
        	int minlat = (int) (Double.parseDouble(atts.getValue("minlat")) * 1E7);
        	int maxlat = (int) (Double.parseDouble(atts.getValue("maxlat")) * 1E7);
        	int minlon = (int) (Double.parseDouble(atts.getValue("minlon")) * 1E7);
        	int maxlon = (int) (Double.parseDouble(atts.getValue("maxlon")) * 1E7);
        	try {
        		mCurrentBound = mDb.createBounds(minlon, minlat, maxlon, maxlat);
        	} catch (OsmException e) {
        		throw new OsmParseException("Bounds are not correct");
            }
        } catch (NumberFormatException e) {
        	throw new OsmParseException("Bounds unparsable");
        }
    }

    private void parseTag(final Attributes atts) {
        OsmElement currentOsmElement = getCurrentOsmElement();
        if (currentOsmElement == null) {
                Log.e(TAG, "Parsing Error: no mCurrentOsmElement set!");
        } else {
                String k = atts.getValue("k");
                String v = atts.getValue("v");
                currentOsmElement.addOrUpdateTag(k, v);
            	//Log.d(TAG, "Tag, "+k+"="+v);
        }
    }

    private void parseWayNode(final Attributes atts) throws OsmParseException {
        try {
        	if (mCurrentWay == null) {
        		Log.e(TAG, "No mCurrentWay set!");
            } else {
            	long nodeOsmId = Long.parseLong(atts.getValue("ref"));
            	OsmNode node = mDb.getNode(nodeOsmId);
            	mCurrentWay.addNode(node);
            }
        } catch (NumberFormatException e) {
        	throw new OsmParseException("WayNode unparsable");
        }
    }

    private void parseRelationElement(final Attributes atts) throws OsmParseException {
        try {
        	if (mCurrentRel == null) {
        		Log.e(TAG, "No mCurrentRel set!");
            } else {
            	String type = atts.getValue("type");
            	String role = atts.getValue("role");
            	long ref = Long.parseLong(atts.getValue("ref"));
            	mCurrentRel.addMember(type, role, ref, mDb.getElement(ref));
            	//Log.d(TAG, "Rel, type="+type+" role="+role+" ref="+ref);
            }
        } catch (NumberFormatException e) {
        	throw new OsmParseException("RelationElement unparsable");
        }
    }
    
    private OsmElement getCurrentOsmElement() {
        if (mCurrentNode != null) {
        	return mCurrentNode;
        }
        if (mCurrentWay != null) {
        	return mCurrentWay;
        }
        if (mCurrentRel != null) {
        	return mCurrentRel;
        }
        return null;
    }

    private static boolean isNode(final String name) { return name.equalsIgnoreCase("node"); }
	private static boolean isWay(final String name) { return name.equalsIgnoreCase("way"); }
	private static boolean isWayNode(final String name) { return name.equalsIgnoreCase("nd"); }
    private static boolean isBounds(final String name) { return name.equalsIgnoreCase("bounds"); }
    private static boolean isRelation(final String name) { return name.equalsIgnoreCase("relation"); }
    private static boolean isRelationMember(final String name) { return name.equalsIgnoreCase("member"); }
    private static boolean isTag(final String name) { return name.equalsIgnoreCase("tag"); }
	private static boolean isOsmElement(final String name) { return isNode(name) || isWay(name) || isRelation(name); }
}
