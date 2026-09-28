package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Vocabulary for grlc-style SPARQL query templates published as nanopubs.
 * The namespace is <a href="https://w3id.org/kpxl/grlc/">https://w3id.org/kpxl/grlc/</a>.
 */
public final class KPXL_GRLC {

    private KPXL_GRLC() {
    }

    public static final String NAMESPACE = "https://w3id.org/kpxl/grlc/";
    public static final String PREFIX = "kpxl_grlc";
    public static final Namespace NS = VocabUtils.createNamespace(PREFIX, NAMESPACE);

    /**
     * IRI for the class of grlc-based SPARQL query templates.
     */
    public static final IRI GRLC_QUERY = VocabUtils.createIRI(NAMESPACE, "grlc-query");

    /**
     * IRI for the relation linking a query template to its SPARQL endpoint URL.
     */
    public static final IRI ENDPOINT = VocabUtils.createIRI(NAMESPACE, "endpoint");

    /**
     * IRI for the relation linking a query template to its SPARQL template string.
     */
    public static final IRI SPARQL = VocabUtils.createIRI(NAMESPACE, "sparql");

}
