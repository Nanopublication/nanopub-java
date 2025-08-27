package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.Namespace;

public class RDFG {

    public static final String NAMESPACE = "http://www.w3.org/2004/03/trix/rdfg-1/";

    public static final String PREFIX = "rdfg";

    public static final Namespace NS = Utils.createNamespace(PREFIX, NAMESPACE);

}
