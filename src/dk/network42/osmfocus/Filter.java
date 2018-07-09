package dk.network42.osmfocus;

public class Filter {
	public void filter(OsmElement e) {
//		if (e.getTagCnt() == 21) { // FIXME
//			e.mFiltered = true;
//		}
		if (e.getTagCnt() == 1 && e.hasTag("building", "yes")) { // FIXME
			e.mFiltered = true;
		}
//		if (e.hasTag("landuse", "residential")) { // FIXME
//			e.mFiltered = true;
//		}
	}
}
