package dk.network42.osmfocus;

import java.io.IOException;

public class OsmException extends IOException {

	public OsmException(final String string) {
		super(string);
    }

    public OsmException(final String string, final Throwable e) {
        super(string);
        initCause(e);
    }
}
