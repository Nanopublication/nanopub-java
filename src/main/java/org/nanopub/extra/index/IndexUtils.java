package org.nanopub.extra.index;

import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.openrdf.model.Statement;
import org.openrdf.model.vocabulary.RDF;

public class IndexUtils {

	private IndexUtils() {}  // no instances allowed

	public static boolean isIndex(Nanopub np) {
		for (Statement st : np.getPubinfo()) {
			if (!st.getSubject().equals(np.getUri())) continue;
			if (!st.getPredicate().equals(RDF.TYPE)) continue;
			if (!st.getObject().equals(NanopubIndex.NANOPUB_INDEX_URI)) continue;
			return true;
		}
		return false;
	}

	public static NanopubIndex castToIndex(Nanopub np) throws MalformedNanopubException {
		if (np instanceof NanopubIndex) {
			return (NanopubIndex) np;
		} else {
			return new NanopubIndexImpl(np);
		}
	}

}
