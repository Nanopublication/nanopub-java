package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * This class defines the FDO Connect namespace (FDOC) vocabulary.
 */
public class FDOC {

    public static final String NAMESPACE = "https://w3id.org/fdoc/o/terms/";

    public static final String PREFIX = "fdoc";

    public static final Namespace NS = Utils.createNamespace(PREFIX, NAMESPACE);

    public static final IRI FDO_PROFILE = Utils.createIRI(NAMESPACE, "FdoProfile");

    public static final IRI HAS_SHAPE = Utils.createIRI(NAMESPACE, "hasShape");
}
