package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Service-type IRIs used in service intro nanopubs (the secondary type next to {@code npx:NanopubService}).
 */
public class NPS {

    public static final String NAMESPACE = "https://w3id.org/np/o/service/terms/";

    public static final String PREFIX = "nps";

    public static final Namespace NS = VocabUtils.createNamespace(PREFIX, NAMESPACE);

    public static final IRI NANOPUB_QUERY_1_1 = VocabUtils.createIRI(NAMESPACE, "nanopub-query-1.1");
    public static final IRI NANOPUB_REGISTRY_1_0 = VocabUtils.createIRI(NAMESPACE, "nanopub-registry-1.0");
    public static final IRI NANODASH_2_X = VocabUtils.createIRI(NAMESPACE, "nanodash-2.x");

}
