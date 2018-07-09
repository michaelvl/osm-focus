package dk.network42.osmfocus;

//import org.apache.http.HttpStatus;

public class OsmServerException extends OsmException {

    // See e.g. http://wiki.openstreetmap.org/wiki/API_v0.6
    private final int mHttpErr;

    OsmServerException(final int httpErr, final String info) {
        super(info);
        mHttpErr = httpErr;
    }
}
