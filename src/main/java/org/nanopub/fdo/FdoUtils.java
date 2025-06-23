package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;

public class FdoUtils {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    public static final String FDO_URI_PREFIX = "https://hdl.handle.net/";
    public static final IRI RDF_TYPE_FDO = vf.createIRI("https://w3id.org/fdof/ontology#FAIRDigitalObject");

    public static final IRI PROFILE_IRI = vf.createIRI("http://purl.org/dc/terms/conformsTo");
    public static final String PROFILE_HANDLE_2 = "0.FDO/Profile";
    public static final String PROFILE_HANDLE_1 = "FdoProfile";
    public static final String PROFILE_HANDLE = "21.T11966/FdoProfile";

    public static final IRI DATA_REF_IRI = vf.createIRI("https://w3id.org/fdof/ontology#isMaterializedBy");
    public static final String DATA_REF_HANDLE = "21.T11966/06a6c27e3e2ef27779ec";

    /**
     * Add the prefix "https://hdl.handle.net/" to the fdoHandle and returns it as IRI.
     */
    public static IRI toIri(String fdoHandle) {
        return vf.createIRI(FDO_URI_PREFIX + fdoHandle);
    }

    /**
     * Remove the prefix "https://hdl.handle.net/" from the IRI and returns the handle as string.
     * returns the iri as string otherwise.
     */
    public static String extractHandle(Resource iri) {
        String iriString = iri.toString();
        if (iriString.startsWith(FDO_URI_PREFIX)) {
            return iriString.substring(FDO_URI_PREFIX.length());
        }
        return iriString;
    }

    /**
     * Test if the iri starts with "https://hdl.handle.net/" followed by a handle.
     */
    public static boolean isHandleIri(Resource iri) {
        String potentialHandle = extractHandle(iri);
        return looksLikeHandle(potentialHandle);
    }

    /**
     * We here assume that a handle starts with 2 digits (minimal prefix),
     * no whitespaces, and contains at least one "/".
     */
    public static boolean looksLikeHandle(String potentialHandle) {
        if (potentialHandle.matches("\\d\\d\\S*/+\\S*")){
            return true;
        }
        return false;
    }

    public static boolean looksLikeUrl(String potentialUrl) {
        if (potentialUrl.matches("http(s)?://\\S+\\.[a-z]{2,}.*")){
            return true;
        }
        return false;
    }

    /**
     * Create an IRI by prefixing a handle with https://hdl.handle.net/ if it's a handle,
     * or by just converting an url.
     */
    public static IRI createIri(String handleOrUrl) {
        if (looksLikeHandle(handleOrUrl)) {
            return vf.createIRI(FDO_URI_PREFIX + handleOrUrl);
        } else if (looksLikeUrl(handleOrUrl)) {
            return vf.createIRI(handleOrUrl);
        }
        throw new IllegalArgumentException("Neither handle nor url found: " + handleOrUrl);
    }

    /**
     * Check if the given Nanopub is a FDO Nanopub.
     */
    public static boolean isFdoNanopub(Nanopub np) {
        for (Statement st: np.getAssertion()) {
            if (st.getPredicate().equals(RDF.TYPE) && st.getObject().equals(RDF_TYPE_FDO)) {
                return true;
            }
        }
        return false;
    }

}
