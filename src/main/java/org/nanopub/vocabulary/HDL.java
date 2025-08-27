package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.Namespace;

/**
 * This class defines the Handle (HDL) vocabulary.
 */
public class HDL {

    public static final String NAMESPACE = "https://hdl.handle.net/";

    public static final String PREFIX = "hdl";

    public static final Namespace NS = Utils.createNamespace(PREFIX, NAMESPACE);
}
