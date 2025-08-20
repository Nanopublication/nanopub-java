package org.nanopub.extra.server;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.extra.setting.NanopubSetting;
import org.nanopub.vocabulary.NPX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling nanopub server-related operations.
 */
public class NanopubServerUtils {

    /**
     * Singleton, no instances.
     */
    private NanopubServerUtils() {
    }

    private static final List<String> bootstrapServerList = new ArrayList<>();

    /**
     * Returns a list of bootstrap servers.
     *
     * @return a list of bootstrap server URIs
     */
    public static List<String> getBootstrapServerList() {
        if (bootstrapServerList.isEmpty()) {
            try {
                for (IRI iri : NanopubSetting.getLocalSetting().getBootstrapServices()) {
                    bootstrapServerList.add(iri.stringValue());
                }
            } catch (RDF4JException | MalformedNanopubException | IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return bootstrapServerList;
    }

    /**
     * Checks if the given nanopub is a protected nanopub.
     *
     * @param np the nanopub to check
     * @return true if the nanopub is protected, false otherwise
     */
    public static boolean isProtectedNanopub(Nanopub np) {
        for (Statement st : np.getPubinfo()) {
            if (!st.getSubject().equals(np.getUri())) continue;
            if (!st.getPredicate().equals(RDF.TYPE)) continue;
            if (st.getObject().equals(NPX.PROTECTED_NANOPUB)) return true;
        }
        return false;
    }

}
