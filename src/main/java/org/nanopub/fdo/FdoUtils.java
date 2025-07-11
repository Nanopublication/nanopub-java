package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;

/**
 * Utility class for handling FAIR Digital Objects (FDOs).
 */
public final class FdoUtils {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * The prefix for FDOs.
     */
    public static final String FDO_URI_PREFIX = "https://hdl.handle.net/";

    /**
     * The IRI for the RDF type of a FAIR Digital Object (FDO).
     */
    public static final IRI RDF_TYPE_FDO = vf.createIRI("https://w3id.org/fdof/ontology#FAIRDigitalObject");

    /**
     * The IRI for the FDO has part relation.
     */
    public static final IRI FDO_HAS_PART = vf.createIRI("https://purl.org/dc/terms/hasPart");

    /**
     * The IRI for the FDO derived from relation.
     */
    public static final IRI FDO_DERIVES_FROM = vf.createIRI("https://www.w3.org/ns/prov#wasDerivedFrom");

    /**
     * The IRI for the FDO profile.
     */
    public static final IRI PROFILE_CLASS_IRI = vf.createIRI("https://w3id.org/fdoc/o/terms/FdoProfile");

    /**
     * The IRI for the FDO profile conformsTo relation.
     */
    public static final IRI PROFILE_IRI = vf.createIRI("http://purl.org/dc/terms/conformsTo");

    /**
     * The handle for the FDO profile.
     */
    public static final String PROFILE_HANDLE_2 = "0.FDO/Profile";

    /**
     * Then handle for the FDO profile.
     */
    public static final String PROFILE_HANDLE_1 = "FdoProfile";

    /**
     * The handle for the FDO profile.
     */
    public static final String PROFILE_HANDLE = "21.T11966/FdoProfile";

    /**
     * The IRI for the FDO shape link relation.
     */
    public static final IRI SHAPE_LINK_IRI = vf.createIRI("https://w3id.org/fdoc/o/terms/hasShape");

    /**
     * The IRI for the FDO data reference relation.
     */
    public static final IRI DATA_REF_IRI = vf.createIRI("https://w3id.org/fdof/ontology#isMaterializedBy");

    /**
     * The handle for the FDO data reference.
     */
    public static final String DATA_REF_HANDLE = "21.T11966/06a6c27e3e2ef27779ec";

    private FdoUtils() {
        // no instances
    }

    /**
     * Add the prefix "<a href="https://hdl.handle.net/">...</a>" to the fdoHandle and returns it as IRI.
     *
     * @param fdoHandle the handle of the FDO
     * @return the IRI of the FDO
     */
    public static IRI toIri(String fdoHandle) {
        return vf.createIRI(FDO_URI_PREFIX + fdoHandle);
    }

    /**
     * Remove the prefix "<a href="https://hdl.handle.net/">...</a>" from the IRI and returns the handle as string.
     * returns the iri as string otherwise.
     *
     * @param iri the IRI of the FDO
     * @return the handle of the FDO as string
     */
    public static String extractHandle(Resource iri) {
        String iriString = iri.toString();
        if (iriString.startsWith(FDO_URI_PREFIX)) {
            return iriString.substring(FDO_URI_PREFIX.length());
        }
        return iriString;
    }

    /**
     * Test if the iri starts with "<a href="https://hdl.handle.net/">...</a>" followed by a handle.
     *
     * @param iri the IRI to check
     * @return true if the IRI is a handle, false otherwise
     */
    public static boolean isHandleIri(Resource iri) {
        String potentialHandle = extractHandle(iri);
        return looksLikeHandle(potentialHandle);
    }

    /**
     * We here assume that a handle starts with 2 digits (minimal prefix),
     * no whitespaces, and contains at least one "/".
     *
     * @param potentialHandle the string to check
     * @return true if the string looks like a handle, false otherwise
     */
    public static boolean looksLikeHandle(String potentialHandle) {
        return potentialHandle.matches("\\d\\d\\S*/+\\S*");
    }

    /**
     * Check if the given string looks like a URL.
     *
     * @param potentialUrl the string to check
     * @return true if the string looks like a URL, false otherwise
     */
    public static boolean looksLikeUrl(String potentialUrl) {
        return potentialUrl.matches("http(s)?://\\S+\\.[a-z]{2,}.*");
    }

    /**
     * Create an IRI by prefixing a handle with <a href="https://hdl.handle.net/">...</a> if it's a handle,
     * or by just converting an url.
     *
     * @param handleOrUrl the handle or URL to convert
     * @return the IRI of the FDO
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
     *
     * @param np the Nanopub to check
     * @return true if the Nanopub is a FDO Nanopub, false otherwise
     */
    public static boolean isFdoNanopub(Nanopub np) {
        for (Statement st : np.getAssertion()) {
            if (st.getPredicate().equals(RDF.TYPE) && st.getObject().equals(RDF_TYPE_FDO)) {
                return true;
            }
        }
        return false;
    }

}
