package dk.network42.osmfocus;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.Log;

public class PaintConfig {
    private static final String TAG = "PaintConfig";
    
    public static final float DEFAULT_ZOOM = 2.5f;
    Paint debughud = new Paint();
    Paint bounds = new Paint();
    Paint paint = new Paint();
    Paint waystroke = new Paint();
    Paint waystroke2 = new Paint();
    Paint wayfill2 = new Paint();
    Paint service = new Paint();
    Paint service2 = new Paint();
    Paint waystrokesec = new Paint(); // secondary+primary roads
    Paint waystrokesec2 = new Paint();
    Paint waystroketert = new Paint(); // tertiary+unclassifed roads
    Paint waystroketert2 = new Paint();
    Paint waystroke_undef = new Paint();
    Paint rail = new Paint(); // tertiary+unclassifed roads
    Paint rail2 = new Paint();
    Paint track = new Paint();
    Paint path = new Paint();
    Paint water = new Paint();
    Paint buildings = new Paint();
    Paint buildings2 = new Paint();
    Paint nature = new Paint();
    Paint gps = new Paint();
    Paint gps2 = new Paint();
    Paint tag = new Paint();
    Paint tagback = new Paint();
    Paint tagback2 = new Paint();
    Paint tagback2stroke = new Paint();
    Paint tagback3 = new Paint();
    Paint tagframe = new Paint();
    Paint horizon = new Paint();
    Paint[] focus = new Paint[8];
    Paint[] focus2 = new Paint[8];
    Paint fade = new Paint();
    Paint tileerr = new Paint();
    Paint attrib = new Paint();
    int mLayer;
    
    Matrix viewmatrix = new Matrix();
    Matrix inv = new Matrix();
    float[] pts = new float[2]; // Scratchpad
    float[] pts2 = new float[2]; // Scratchpad
    
    boolean wireframe = false;
    float tagtextsize = 18;
	boolean antialias = true;    
    float mHUDspacing, mListSpacing;
    float mScale = DEFAULT_ZOOM;
    float densityScale;

	//int mBackMapType = MAPTYPE_OSM;
    int mBackMapType = MapLayer.MAPTYPE_INTERNAL;


    public void setLayer(int layer) { mLayer = layer; }
    public int getLayer() { return mLayer; }
    public void setScale(float scale, Context ctx) {
    	mScale = scale;
    	update(ctx);
    }
    public float getScale() { return mScale; }

    public void update(Context ctx) {
        densityScale = ctx.getResources().getDisplayMetrics().density;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        //Log.d(TAG, "Pref:LabelSize=" + sharedPrefs.getString("pref_labelsize", "15"));
        
    	tagtextsize = densityScale * Float.parseFloat(sharedPrefs.getString("pref_labelsize", "15.0"));
    	//wireframe = sharedPrefs.getBoolean("pref_wireframe", false);
    	
    	//Toast toast = Toast.makeText(ctx, "Textsize="+tagtextsize, Toast.LENGTH_SHORT);
        //toast.show();
    	
        debughud.setColor(Color.BLACK);
        debughud.setAntiAlias(antialias);
        debughud.setStrokeWidth(0);
        debughud.setTextSize(tagtextsize);
        mHUDspacing = tagtextsize;

        attrib.setColor(Color.GRAY);
        attrib.setAntiAlias(antialias);
        attrib.setStrokeWidth(0);
        attrib.setTextSize(densityScale*10);

        bounds.setColor(Color.BLACK);
        bounds.setStrokeWidth(0);
        bounds.setStyle(Paint.Style.STROKE);

        paint.setColor(Color.BLACK);
        paint.setAntiAlias(antialias);
        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.STROKE);
        paint.setTextSize(tagtextsize);
        mListSpacing = paint.getTextSize()+1;

        fade.setColor(Color.BLACK);
        fade.setStrokeWidth(0);
        fade.setAlpha(42); // Larger means less opaque

        tileerr.setColor(Color.RED);
        tileerr.setAlpha(42); // Larger means less opaque

        waystroke.setColor(Color.rgb(190, 190, 190));
        waystroke.setAntiAlias(antialias);
        waystroke.setStrokeWidth(6);
        waystroke.setStrokeJoin(Paint.Join.ROUND);
        waystroke.setStrokeCap(Paint.Cap.ROUND);
        waystroke.setStyle(Paint.Style.STROKE);
        waystroke2.setColor(Color.rgb(254, 254, 254));
        waystroke2.setAntiAlias(antialias);
        waystroke2.setStrokeWidth(5);
        waystroke2.setStrokeJoin(Paint.Join.ROUND);
        waystroke2.setStrokeCap(Paint.Cap.ROUND);
        waystroke2.setStyle(Paint.Style.STROKE);
        wayfill2.set(waystroke2);
        wayfill2.setStyle(Paint.Style.FILL_AND_STROKE);
        
        service.set(waystroke);
        service.setStrokeWidth(3);
        service2.set(waystroke2);
        service2.setStrokeWidth(2.4f);

        waystrokesec.setColor(Color.rgb(163, 123, 72));
        waystrokesec.setAntiAlias(antialias);
        waystrokesec.setStrokeWidth(10);
        waystrokesec.setStrokeJoin(Paint.Join.ROUND);
        waystrokesec.setStrokeCap(Paint.Cap.ROUND);
        waystrokesec.setStyle(Paint.Style.STROKE);
        waystrokesec2.setColor(Color.rgb(237, 237, 201));
        waystrokesec2.setAntiAlias(antialias);
        waystrokesec2.setStrokeWidth(8);
        waystrokesec2.setStrokeJoin(Paint.Join.ROUND);
        waystrokesec2.setStrokeCap(Paint.Cap.ROUND);
        waystrokesec2.setStyle(Paint.Style.STROKE);

        waystroketert.setColor(Color.rgb(190, 190, 190));
        waystroketert.setAntiAlias(antialias);
        waystroketert.setStrokeWidth(10);
        waystroketert.setStrokeJoin(Paint.Join.ROUND);
        waystroketert.setStrokeCap(Paint.Cap.ROUND);
        waystroketert.setStyle(Paint.Style.STROKE);
        waystroketert2.setColor(Color.rgb(254, 254, 254));
        waystroketert2.setAntiAlias(antialias);
        waystroketert2.setStrokeWidth(8);
        waystroketert2.setStrokeJoin(Paint.Join.ROUND);
        waystroketert2.setStrokeCap(Paint.Cap.ROUND);
        waystroketert2.setStyle(Paint.Style.STROKE);

        waystroke_undef.setColor(Color.rgb(210,210,210));
        waystroke_undef.setAntiAlias(antialias);
        waystroke_undef.setStrokeWidth(0);
        waystroke_undef.setStyle(Paint.Style.STROKE);
        waystroke_undef.setTextSize(16);
        //waypaint_undef.setPathEffect(new DashPathEffect(new float[] {1,2}, 0));

        rail.setColor(Color.rgb(190, 190, 190));
        rail.setAntiAlias(antialias);
        rail.setStrokeWidth(2.5f);
        rail.setStyle(Paint.Style.STROKE);
        rail2.setColor(Color.rgb(254, 254, 254));
        rail2.setAntiAlias(antialias);
        rail2.setStrokeWidth(2);
        rail2.setStyle(Paint.Style.STROKE);
        rail2.setPathEffect(new DashPathEffect(new float[] {2f,2f}, 0));

        path.setColor(Color.rgb(190, 108, 108));  // Paths and cycleways
        path.setAntiAlias(antialias);
        path.setStrokeWidth(1.5f);
        path.setStyle(Paint.Style.STROKE);
        path.setPathEffect(new DashPathEffect(new float[] {4f,1.5f}, 0));
        
        track.set(path);
        track.setColor(Color.rgb(156, 107, 8));
        track.setStrokeWidth(2.5f);

        buildings.setColor(Color.rgb(224, 224, 224)); // Fill
        buildings.setAntiAlias(antialias);
        buildings.setStrokeWidth(0);
        buildings2.setColor(Color.rgb(145, 145, 145)); // Outline
        buildings2.setAntiAlias(antialias);
        buildings2.setStrokeWidth(0);
        buildings2.setStyle(Paint.Style.STROKE);

        nature.setColor(Color.rgb(212, 228, 196));
        nature.setAntiAlias(antialias);
        nature.setStrokeWidth(0);
        water.setColor(Color.rgb(181, 208, 208));
        water.setAntiAlias(antialias);
        water.setStrokeWidth(0);

        gps.setColor(Color.rgb(0, 0, 192));  // Circle defining physical location and accuracy
        gps.setAntiAlias(antialias);
        gps.setStrokeWidth(0);
        gps.setAlpha(32);
        
        gps2.setColor(Color.rgb(0, 0, 200));
        gps2.setAntiAlias(antialias);
        gps2.setStrokeWidth(0);
        gps2.setStyle(Paint.Style.STROKE);

        tag.setColor(Color.rgb(0, 0, 0));
        tag.setAntiAlias(antialias);
        tag.setStrokeWidth(0);
        tag.setTextSize(tagtextsize);
        tagback.setColor(Color.rgb(230, 230, 140));
        tagback.setAntiAlias(antialias);
        tagback.setStrokeWidth(0);
        //tagback.setAlpha(190);
        tagback2.setColor(Color.rgb(160, 240, 160));
        tagback2.setAntiAlias(antialias);
        tagback2.setStrokeWidth(0);
        //tagback2stroke.setAlpha(190);
        tagback2stroke.setColor(Color.rgb(160, 240, 160));
        tagback2stroke.setAntiAlias(antialias);
        tagback2stroke.setStrokeWidth(4);
        //tagback2stroke.setAlpha(200);
        tagback3.setColor(Color.WHITE);
        tagback3.setAntiAlias(antialias);
        tagback3.setStrokeWidth(0);
        //tagback3.setAlpha(220);
        tagframe.setColor(Color.BLACK);
        tagframe.setAntiAlias(antialias);
        tagframe.setStrokeWidth(1);
        tagframe.setStyle(Paint.Style.STROKE);

        horizon.setColor(Color.rgb(0, 0, 200));
        horizon.setAntiAlias(antialias);
        horizon.setStrokeWidth(0);
        horizon.setStyle(Paint.Style.STROKE);

        for (int ii=0; ii<8; ii++) { // highlighted objects
        	focus[ii] = new Paint();
        	switch (ii%8) {
        	// See http://developer.android.com/design/style/color.html
        	case 0:
	        	focus[ii].setColor(Color.rgb(0xff, 0x44, 0x44));
	        	break;
        	case 1:
	        	focus[ii].setColor(Color.rgb(0xff, 0xbb, 0x33));
	        	break;
        	case 2:
	        	focus[ii].setColor(Color.rgb(0x99, 0xcc, 0));
	        	break;
        	case 3:
	        	focus[ii].setColor(Color.rgb(0xaa, 0x66, 0xcc));
	        	break;
        	case 4:
	        	focus[ii].setColor(Color.rgb(0x33, 0xb5, 0xe5));
	        	break;
	        // Darker variants below
        	case 5:
	        	focus[ii].setColor(Color.rgb(0xcc, 0, 0));
	        	break;
        	case 6:
	        	focus[ii].setColor(Color.rgb(0xff, 0x88, 0));
	        	break;
        	case 7:
	        	focus[ii].setColor(Color.rgb(0x66, 0x99, 0));
	        	break;
        	}
	        focus[ii].setAntiAlias(antialias);
	        focus[ii].setStrokeWidth(densityScale * 3/mScale); // Used with global scaling
	        focus[ii].setStrokeCap(Paint.Cap.ROUND);
	        focus[ii].setStyle(Paint.Style.STROKE);
	        //focus[ii].setAlpha(42); // Larger means less opaque
        	focus2[ii] = new Paint(focus[ii]);
	        focus2[ii].setStrokeWidth(densityScale * 1.5f);  // Used with screen scaling
        }
    }

	// FIXME: Autoset when modifying viewmatrix, add methods for updating viewmatrix and autoset in these
    private void setInvertM() {
    	assert viewmatrix.invert(inv);
    	viewmatrix.invert(inv);
    }
    
    // Convert from world coords (not mercator projected) to screen coords
    public float[] world2view(double lon, double lat) {
    	float [] pts = new float[]{(float)lon*MapView.prescale, (float)GeoMath.latToMercator(lat)*MapView.prescale};
    	viewmatrix.mapPoints(pts);
    	return pts;
    }

    // Convert from view coords to world coords (not mercator projected)
    public double[] view2world(float x, float y) {
    	pts[0] = x;
    	pts[1] = y;
    	double [] ptsd = new double[2];
    	setInvertM();
    	inv.mapPoints(pts);
    	ptsd[0] = pts[0]/MapView.prescale;
    	ptsd[1] = GeoMath.mercatorToLat(pts[1]/MapView.prescale);
    	return ptsd;
    }

    public void view2world(RectF rect) {
    	setInvertM();
    	pts[0] = rect.left;
    	pts[1] = rect.top;
    	inv.mapPoints(pts);
    	rect.left = pts[0]/MapView.prescale;
    	rect.top = (float) GeoMath.mercatorToLat(pts[1]/MapView.prescale);
    	pts[0] = rect.right;
    	pts[1] = rect.bottom;
    	inv.mapPoints(pts);
    	rect.right = pts[0]/MapView.prescale;
    	rect.bottom = (float) GeoMath.mercatorToLat(pts[1]/MapView.prescale);
    }

    boolean hasTag(OsmElement e, OsmElement multiPolyOuter, String key, String val) {
    	return ((multiPolyOuter==null && e.hasTag(key, val)) || (multiPolyOuter!=null && multiPolyOuter.hasTag(key, val)));
    }
    
    boolean hasTagKey(OsmElement e, OsmElement multiPolyOuter, String key) {
    	return ((multiPolyOuter==null && e.hasTagKey(key)) || (multiPolyOuter!=null && multiPolyOuter.hasTagKey(key)));
    }

    // Small hack
    public Paint look, look2, nodelook;
    public void getWayPaints(OsmElement e, OsmElement multiPolyOuter) {
		look = look2 = nodelook = null;
		if (hasTag(e, multiPolyOuter, "natural", "water")) {
			if (getLayer()==0) {
				look = water;
			}
		} else if (hasTag(e, multiPolyOuter, "natural", "scrub") || hasTag(e, multiPolyOuter, "landuse", "forest") ||
                hasTag(e, multiPolyOuter, "landuse", "grass") || hasTagKey(e, multiPolyOuter, "natural")) {
			if (getLayer()==0) {
				look = nature;
			}
		} else if (hasTagKey(e, multiPolyOuter, "building")) {
			if (getLayer()==0) {
				look = buildings;
				look2 = buildings2;
            }
		} else if (hasTag(e, multiPolyOuter, "highway", "residential") ||
				hasTag(e, multiPolyOuter, "highway", "living_street") ||
				hasTag(e, multiPolyOuter, "highway", "pedestrian")) {
            if (getLayer()==1) {
				look = waystroke;
			} else if (getLayer()==2) {
				if (e.mIsArea) {
					look = wayfill2;
				} else {
					look = waystroke2;
				}
			}
		} else if (hasTag(e, multiPolyOuter, "highway", "service")) {
			if (getLayer()==1) {
				look = service;
			} else if (getLayer()==2) {
				look = service2;
			}
		} else if (hasTag(e, multiPolyOuter, "highway", "secondary") || hasTag(e, multiPolyOuter, "highway", "primary") ||
				hasTag(e, multiPolyOuter, "highway", "trunk") || hasTag(e, multiPolyOuter, "highway", "motorway")) {
			if (getLayer()==1) {
				look = waystrokesec;
			} else if (getLayer()==2) {
				look = waystrokesec2;
			}
		} else if (hasTag(e, multiPolyOuter, "highway", "tertiary") || hasTag(e, multiPolyOuter, "highway", "unclassified")) {
			if (getLayer()==1) {
				look = waystroketert;
			} else if (getLayer()==2) {
				look = waystroketert2;
			}
		} else if (hasTag(e, multiPolyOuter, "highway", "path") || hasTag(e, multiPolyOuter, "highway", "cycleway") ||
				hasTag(e, multiPolyOuter, "highway", "footway")) {
			if (getLayer()==1) {
				look = path;
			}
		} else if (hasTag(e, multiPolyOuter, "railway", "rail")) {
			if (getLayer()==1) {
				look = rail;
			} else if (getLayer()==1) {
				look = rail2;
			}
		} else if (hasTag(e, multiPolyOuter, "highway", "track")) {
			if (getLayer()==1) {
				look = track;
			}
		} else if (multiPolyOuter==null) {
			if (getLayer()==1) {
				look = waystroke_undef;
				nodelook = paint;
			}
		}
    }
}
