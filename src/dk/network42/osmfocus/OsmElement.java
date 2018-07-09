package dk.network42.osmfocus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

abstract public class OsmElement implements Comparable<OsmElement> {
    private static final String TAG = "OsmElement";
	protected long mOsmId;
    protected long mOsmVersion;
    protected SortedMap<String, String> tags;
    protected double mCompare=0; // Cached compare value, only calculate once
    boolean mFiltered = false;
    boolean mIsArea = false;
    OsmDB mDb = null;

    enum Compare implements Comparator<OsmElement> {
    	X_COORD {
    		public int compare(OsmElement e1, OsmElement e2) {
    			if (e1.getX() >= e2.getX()) return 1;
    			else return -1;
    		}
    	},
    	X_COORD_DESCENDING {
    		public int compare(OsmElement e1, OsmElement e2) {
    			if (e2.getX() >= e1.getX()) return 1;
    			else return -1;
    		}
    	},
    	Y_COORD {
    		public int compare(OsmElement e1, OsmElement e2) {
    			if (e1.getY() >= e2.getY()) return 1;
    			else return -1;
    		}
    	},
    	Y_COORD_DESCENDING {
    		public int compare(OsmElement e1, OsmElement e2) {
    			if (e2.getY() >= e1.getY()) return 1;
    			else return -1;
    		}
    	},
    }

    OsmElement(final long id, final long version) {
        this.mOsmId = id;
        this.mOsmVersion = version;
        this.tags = new TreeMap<String, String>();
    }
    
    void addOrUpdateTag(final String tag, final String value) {
        tags.put(tag, value);
    }
    public int getTagCnt() {
        return tags.size();
    }
    public boolean hasTag(final String key, final String value) {
        String keyValue = tags.get(key);
        return keyValue != null && keyValue.equals(value);
    }

    public String getTagWithKey(final String key) {
        return tags.get(key);
    }

    public boolean hasTagKey(final String key) {
        return getTagWithKey(key) != null;
    }

    public boolean tagsIdentical(OsmElement other) {
    	if (getTagCnt() != other.getTagCnt())
    		return false;
    	for(Map.Entry<String,String> entry : tags.entrySet()) {
    		String key = entry.getKey();
    		String value = entry.getValue();
    		String othertag = other.getTagWithKey(key);
    		if (othertag == null || value.equalsIgnoreCase(othertag))
    			return false;
    	}
    	return true;
    }
    
    public long getOsmId() {
        return mOsmId;
    }

    public String toString() {
    	return "OsmId="+mOsmId;
    }

    void updateVisuals(OsmDB db) {
    	/* See also notes on http://wiki.openstreetmap.org/wiki/The_Future_of_Areas */
    	mIsArea = hasTag("area", "yes") || hasTagKey("landuse") || hasTagKey("building") || hasTagKey("natural");
    }
    
    public boolean tagIsFiltered(String key, String value) {
    	//return false;
		return key.startsWith("kms:")  || key.startsWith("osak:") || key.equals("created_by")
				 || key.equals("addr:country") || key.equals("addr:postcode") || key.equals("source");
    }

    // Number of lines a given text needs given a maximum width
    protected int getTagTextLines(String text, Paint paint, float maxw, float indentw) {
    	float w = paint.measureText(text);
    	if (w<= maxw)
    		return 1;
    	return 1 + (int)((w-maxw)/(maxw-indentw));  // FIXME: Approximation, needs char algo
    }

    protected float formatWrappedText(ArrayList<String> lines, String text, Paint paint, float maxw) {
    	float w = paint.measureText(text);
    	if (w <= maxw) {
    		lines.add(text);
    		return w;
    	}
    	while (!text.isEmpty()) {
    		int ch = paint.breakText(text, true, maxw, null);
    		lines.add(text.substring(0, ch));
    		text = text.substring(ch);
    		if (!text.isEmpty())
    			text = "\u21AA "+text;
    	}
    	return maxw;
    }

    // Special headlines where two tags are combined, currently only highwayType : NameValue
    protected int headLines() {
    	int lines = 0;
    	if (hasTagKey("highway") && hasTagKey("name"))
    		lines += 1;
    	//if (hasTagKey("amenity") && hasTagKey("name"))
    	//	lines += 1;
    	return lines;
    }

    protected float formatWrappedText(ArrayList<String> lines, Paint paint, float maxw) {
    	
    	String head = null;
    	float w = 0.0f;

		// Compact headlines like  "ValueOfHighway : ValueOfName"
    	if (hasTagKey("highway") && hasTagKey("name")) {
    		head = getTagWithKey("highway") + " : '" + getTagWithKey("name") + "'";
    		w = formatWrappedText(lines, head, paint, maxw);
    	}
//    	if (hasTagKey("amenity") && hasTagKey("name")) {
//    		head = getTagWithKey("amenity") + " : '" + getTagWithKey("name") + "'";
//    		float tmp = formatWrappedText(lines, head, paint, maxw);
//			if (w<tmp)
//				w = tmp;
//    	}

    	for(Map.Entry<String,String> entry : tags.entrySet()) {
    		String key = entry.getKey();
    		String value = entry.getValue();
    		if (head != null && (key.equalsIgnoreCase("highway") || key.equalsIgnoreCase("name")))
    			continue;
    		if (!tagIsFiltered(key, value)) {
    			String txt = key+" = "+value;
    			float tmp = formatWrappedText(lines, txt, paint, maxw);
    			if (w<tmp)
    				w = tmp;
    		}
    	}
    	return w;
    }

    protected void clipLine(ArrayList<String> lines, int idx, Paint paint, float maxw) {
    	//final String sym = "\u21a9"; // bend arrow
    	final String sym = "  \u21df";
    	final String spc = " ";
    	String txt = lines.get(idx);
    	float txtw = paint.measureText(txt);
    	float symw = paint.measureText(sym);
    	float spcw = paint.measureText(spc);
		if (txtw+symw < maxw) {
			int spcs = (int)((maxw-(txtw+symw))/spcw);
			for (int ii=0; ii<spcs; ii++)
				txt += spc;
			txt += sym;
		} else {
			int ch = paint.breakText(txt, true, maxw-symw, null);
			txt = txt.substring(0, ch)+sym;
		}
    	lines.set(idx, txt);
    }

    public RectF drawTags(Canvas canvas, RectF viewrect, boolean poi_lines,
    					Paint.Align xalign, Paint.Align yalign,
    					PaintConfig pcfg, int style) {

    	ArrayList<String> list = new ArrayList<String>();
    	final int inset = 3; // inset relative to viewbox
    	final int xborder = 4; int yborder = 4;  // margin between text and surrounding box
    	float x,y, w;
    	int lines;
    	float round = 1.0f;
        float lspace = pcfg.tag.getTextSize()+1;

    	float maxx = viewrect.width() * 0.49f;
    	float maxy = viewrect.height() * 0.49f;
    	float maxw = maxx-2*xborder-inset;
    	int maxl = (int)(maxy/lspace);

    	w = formatWrappedText(list, pcfg.tag, maxw);
    	lines = list.size();

    	if (lines>maxl) {
    		lines=maxl;
    		clipLine(list, maxl-1, pcfg.tag, maxw);
    	}

    	if (xalign == Paint.Align.LEFT) { // x,y is upper left corner of box
    		x = viewrect.left+inset;
    	} else if (xalign == Paint.Align.RIGHT) {
    		x = viewrect.right-w-2*xborder-inset;
    	} else {
    		x = (viewrect.right-viewrect.left)/2-w/2-inset;
    	}
    	if (yalign == Paint.Align.LEFT) {
    		y = viewrect.top+inset;
    	} else if (yalign == Paint.Align.RIGHT) {
    		y = viewrect.bottom-(2*yborder+lines*lspace)-inset;
    	} else {
    		y = (viewrect.bottom-viewrect.top)/2-(lines*lspace)/2-inset;
    	}

    	// Draw box
    	RectF rrect = new RectF(x,y,x+w+2*xborder,y+2*yborder+lines*lspace);
    	
    	if (headLines() > 0) {
    		drawFancyFrame(canvas, pcfg.tagback, pcfg.tagback3, rrect, lines-1, lspace);
    	} else {
    		canvas.drawRoundRect(rrect, 2*round, 2*round, pcfg.tagback3);
    	}
    	canvas.drawRoundRect(rrect, 2*round, 2*round, pcfg.focus2[style]);

    	// Draw text
    	for (int ii=0; ii<lines; ii++) {
			canvas.drawText(list.get(ii), x+xborder, y+lspace, pcfg.tag);
			y += lspace;
    		
    	}

    	if (poi_lines) {
	    	// Draw line to point on map
	    	pcfg.pts[0] = getX();
	    	pcfg.pts[1] = getY();
	    	pcfg.viewmatrix.mapPoints(pcfg.pts);
	    	//x = rrect.centerX();
	    	x = xalign == Paint.Align.LEFT ? rrect.right : (xalign == Paint.Align.RIGHT ? rrect.left : viewrect.centerX());
	    	y = yalign == Paint.Align.LEFT ? rrect.bottom : (yalign == Paint.Align.RIGHT ? rrect.top : viewrect.centerY());
	    	double [] newpts = GeoMath.shortenLine(pcfg.pts[0], pcfg.pts[1], x, y, 7.5f*pcfg.densityScale);
	    	//Log.d(TAG, "Tag: pts="+ee.getX()+","+ee.getY()+" screenpts="+pcfg.pts[0]+","+pcfg.pts[1]);
	    	canvas.drawLine(x, y, (float)newpts[0], (float)newpts[1], pcfg.focus2[style]);
    	}
    	return rrect;
    }

    public void drawFancyFrame(Canvas canvas, Paint style1, Paint style2, RectF rrect, int lines, float lspace)
    {
    	float x = rrect.left;
    	float y = rrect.top;
    	float w = rrect.right-rrect.left;
    	float round = 1.0f;
    	RectF roundrr = new RectF();
    	Path pp = new Path();
    	Path pp2 = new Path();
    	pp.moveTo(x, y+lspace+2); // Top
    	pp.lineTo(x+w, y+lspace+2);
    	pp.lineTo(x+w, y+round);
    	//roundrr.set(rrect.left, rrect.top, rrect.right, rrect.bottom);
    	roundrr.set(rrect.right-2*round, rrect.top, rrect.right, rrect.top+2*round);
    	pp.arcTo(roundrr,  0, -90);
    	pp.lineTo(x+round, y);
    	roundrr.set(rrect.left, rrect.top, rrect.left+2*round, rrect.top+2*round);
    	pp.arcTo(roundrr,  -90, -90);
    	canvas.drawPath(pp, style1);
    	pp2.moveTo(x, y+lspace+2); // Bottom
    	pp2.lineTo(x+w, y+lspace+2); // Start upper-right
    	//pp2.lineTo(x+w, y+lines*lspace-round);
    	roundrr.set(rrect.right-2*round, rrect.bottom-2*round, rrect.right, rrect.bottom);
    	pp2.arcTo(roundrr,  0, 90);
    	//pp2.lineTo(x+round, y+lines*lspace);
    	roundrr.set(rrect.left, rrect.bottom-2*round, rrect.left+2*round, rrect.bottom);
    	pp2.arcTo(roundrr,  90, 90);
    	canvas.drawPath(pp2, style2);
    }
    
    public void draw(Canvas canvas, OsmDB db, PaintConfig pcfg) {}
    public void highlight(Canvas canvas, OsmDB db, PaintConfig pcfg, int style) {}
    public abstract double distTo(double lon, double lat_merc);
    public abstract double angleTo(double lon, double lat_merc);
    
    // For non-point object these coords are 'close points' - see e.g. compareSetDistTo() for OsmWay
	float getX() { return 0; };
    float getY() { return 0; };

    public int compareTo(OsmElement e) {
    	// int subtraction does not work with small deltas
    	if (mCompare >= e.mCompare) {
    		return 1;
    	} else {
    		return -1;
    	}
    }
    public void compareSetDistTo(double lon, double lat_merc) { mCompare = distTo(lon, lat_merc); }
    public void compareSetAngleTo(double lon, double lat_merc, double offset) { mCompare = angleTo(lon, lat_merc)+offset; }
    public double compareGet() { return mCompare; }
}
