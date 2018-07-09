package dk.network42.osmfocus;

import android.graphics.Canvas;
import android.graphics.RectF;

abstract public class MapLayer {
    static final int MAPTYPE_NONE        = 1;
	static final int MAPTYPE_OSM         = 2;
	static final int MAPTYPE_OSMCYCLEMAP = 3;
	static final int MAPTYPE_INTERNAL    = 4;

	abstract public void draw(Canvas canvas, RectF worldport, PaintConfig pcfg);
	public void onTrimMemory(int level) {}
}
