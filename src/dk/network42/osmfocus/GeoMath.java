package dk.network42.osmfocus;

public class GeoMath {
	
	// Convenience constants
    private static final double _180_PI = 180.0/Math.PI;
    private static final double _PI_2   = Math.PI/2.0;
    private static final double _PI_4   = Math.PI/4.0;
    private static final double _PI_180 = Math.PI/180.0;
    private static final double _PI_360 = Math.PI/360.0;

    // Maximum latitude due to Mercator projection.
    public static final double MAX_LAT = _180_PI * Math.atan(Math.sinh(Math.PI));

    // See http://en.wikipedia.org/wiki/Earth_ellipsoid
    public static final double EARTH_POLAR_RADIUS = 6356752;
    public static final double EARTH_EQUATORIAL_RADIUS = 6378137;
    // Reference Earth radius in meters (mean between equatorial and polar WGS84 radii).
    public static final double EARTH_RADIUS = (EARTH_EQUATORIAL_RADIUS+EARTH_POLAR_RADIUS)/2;

    // Approximate conversions between 'on the ground' meter measure to angle [degrees]
    // One equator
    public static double convertMetersToGeoDistanceEq(double meters) {
        return _180_PI*meters/EARTH_EQUATORIAL_RADIUS;
    }
    // On parallel at a given latitude
    public static double convertMetersToGeoDistancePar(double meters, double lat) {
        return _180_PI*meters/EARTH_EQUATORIAL_RADIUS/Math.cos(lat*_PI_180);
    }
    // On median
    public static double convertMetersToGeoDistanceMed(double meters) {
        return _180_PI*meters/EARTH_POLAR_RADIUS;
    }

    // Mercator projection.  Truncated to MAX_LAT if lat is outside +-MAX_LAT.
    // http://en.wikipedia.org/wiki/Mercator_projection#Mathematics_of_the_projection
    public static double latToMercator(double lat) {
        lat = Math.min(MAX_LAT, lat);
        lat = Math.max(-MAX_LAT, lat);
        return _180_PI * Math.log(Math.tan(lat*_PI_360 + _PI_4));
    }

    // Inverse mercator projection
    public static double mercatorToLat(double merc) {
        return _180_PI * (2.0 * Math.atan(Math.exp(merc*_PI_180)) - _PI_2);
    }

    public static double getMercatorScale(double lat) {
    	return latToMercator(lat)/lat;
    }

    // Closest point on finite line (l1 to l2) from point (p)
    // Algorithm, see http://paulbourke.net/geometry/pointlineplane/
    public static double[] nearestPoint(double px, double py, double l1x, double l1y, double l2x, double l2y) {
        double x, y;
        double dx = l2x - l1x;
        double dy = l2y - l1y;
        if (dx == 0.0 && dy == 0.0) {
            x = l1x;
            y = l1y;
        } else {
            double u;
            u = ((px-l1x)*dx + (py-l1y)*dy) / (dx*dx+dy*dy);
            if (u <= 0.0) {
                x = l1x;
                y = l1y;
            } else if (u >= 1.0) {
                x = l2x;
                y = l2y;
            } else {
                x = l1x + u*dx;
                y = l1y + u*dy;
            }
        }
        return new double []{x, y};
    }

    // Distance from point (p) to finite line (l1 to l2)
    public static double getLineDistance(double px, double py, double l1x, double l1y, double l2x, double l2y) {
        double x, y;
        double dx = l2x - l1x;
        double dy = l2y - l1y;
        if (dx == 0.0 && dy == 0.0) {
            x = l1x;
            y = l1y;
        } else {
            double u;
            u = ((px-l1x)*dx + (py-l1y)*dy) / (dx*dx+dy*dy);
            if (u <= 0.0) {
                x = l1x;
                y = l1y;
            } else if (u >= 1.0) {
                x = l2x;
                y = l2y;
            } else {
                x = l1x + u*dx;
                y = l1y + u*dy;
            }
        }
        return Math.hypot(px-x, py-y);
    }

    // Shorten line with 'by' from (l1x,l1y)
    public static double[] shortenLine(double l1x, double l1y, double l2x, double l2y, double by) {
        double dx = l2x - l1x;
        double dy = l2y - l1y;
        if ((dx == 0.0 && dy == 0.0) || (by <= 0)) {
    		return new double []{l1x, l1y};
    	}
    	double len = Math.hypot(dx, dy);
    	if (len <= by) {
    		return new double []{l2x, l2y};
    	}
        double x, y, u;
        u = by/len;
        x = l1x + u*dx;
        y = l1y + u*dy;
        return new double []{x, y};
    }
}
