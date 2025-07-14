package org.nanopub.extra.index;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;

/**
 * Utility class for working with nanopublication indexes.
 */
public class IndexUtils {

    private IndexUtils() {
    }  // no instances allowed

    /**
     * Checks if the given nanopublication is an index nanopublication.
     *
     * @param np the nanopublication to check
     * @return true if the nanopublication is an index, false otherwise
     */
    public static boolean isIndex(Nanopub np) {
        for (Statement st : np.getPubinfo()) {
            if (!st.getSubject().equals(np.getUri())) continue;
            if (!st.getPredicate().equals(RDF.TYPE)) continue;
            if (!st.getObject().equals(NanopubIndex.NANOPUB_INDEX_URI)) continue;
            return true;
        }
        return false;
    }

    /**
     * Casts the given nanopublication to a NanopubIndex if it is an index.
     *
     * @param np the nanopublication to cast
     * @return the NanopubIndex instance
     * @throws MalformedNanopubException if the nanopublication is not a valid index
     */
    public static NanopubIndex castToIndex(Nanopub np) throws MalformedNanopubException {
        if (np instanceof NanopubIndex) {
            return (NanopubIndex) np;
        } else {
            return new NanopubIndexImpl(np);
        }
    }

}
