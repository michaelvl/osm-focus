package dk.network42.osmfocus;

import android.util.Log;

// WGS84 based bounding box
public class GeoBBox {
	private static final String TAG = "GeoBBox";

	// primary attributes, multiplied by 1E7 (OSM style), all floats are derived from these
	public int left, right;  // left <= right
	public int bottom, top;  // bottom <= top
	public boolean empty = true;

	public void check() {
		assert(left <= right);
		assert(bottom <= top);
	}
	
	public GeoBBox(final int left, final int bottom, final int right, final int top) {
		this.left = left;
		this.bottom = bottom;
		this.right = right;
		this.top = top;
		this.empty = false;
		check();
	}

	public GeoBBox(final double left, final double bottom, final double right, final double top) {
		this((int)(left*1E7), (int)(bottom*1E7), (int)(right*1E7), (int)(top*1E7));
	}

	public String toString() {
		return "["+left/1E7+","+bottom/1E7+"--"+right/1E7+","+top/1E7+"]";
	}

	// Get BBox centered at point with given size.  Size ('meters') measured to both sides of central
	// point, i.e. box will be 2*meters wide
	public static GeoBBox getBoxForPoint(final double lat, final double lon, final double meters) {
		double dax = GeoMath.convertMetersToGeoDistancePar(meters, lat);
		double day = GeoMath.convertMetersToGeoDistanceMed(meters);
		GeoBBox box = new GeoBBox(lon-dax, lat-day, lon+dax, lat+day);
		//Log.d(TAG, "GeoBBox for point ("+lat+","+lon+") size="+meters+" -> deltaAngles="+dax+","+day+", box="+box);
		return box;
	}
}
