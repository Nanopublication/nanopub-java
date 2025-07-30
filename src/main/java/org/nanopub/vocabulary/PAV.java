package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.Namespace;

/**
 * This class defines the PAV (Provenance, Authoring, and Versioning) vocabulary.
 */
public class PAV {

    public static final String NAMESPACE = "http://purl.org/pav/";

    public static final String PREFIX = "pav";

    public static final Namespace NS = Utils.createNamespace(PREFIX, NAMESPACE);

}
