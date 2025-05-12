package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class FdoUtils {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    public static final String FDO_URI_PREFIX = "https://hdl.handle.net/";
    public static final IRI RDF_TYPE_FDO = vf.createIRI("https://w3id.org/fdof/ontology#FAIRDigitalObject");
    public static final IRI RDF_FDO_PROFILE = vf.createIRI("https://hdl.handle.net/0.FDO/Profile");

    /**
     * Add the prefix "https://hdl.handle.net/" to the fdoHandle and returns it as IRI.
     */
    public static IRI toIri(String fdoHandle) {
        return vf.createIRI(FDO_URI_PREFIX + fdoHandle);
    }

    /**
     * Remove the prefix "https://hdl.handle.net/" from the IRI and returns the handle as string.
     * returns null if the prefix is not there.
     */
    public static String extractHandle(IRI iri) {
        String iriString = iri.toString();
        if (iriString.startsWith(FDO_URI_PREFIX)) {
            return iriString.substring(FDO_URI_PREFIX.length());
        }
        return null;
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

}
