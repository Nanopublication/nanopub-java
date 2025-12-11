package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * This class defines the FAIR Digital Object Framework (FDOF) vocabulary.
 */
public class FDOF {

    public static final String NAMESPACE = "https://w3id.org/fdof/ontology#";

    public static final String PREFIX = "fdof";

    public static final Namespace NS = VocabUtils.createNamespace(PREFIX, NAMESPACE);

    public static final IRI FAIR_DIGITAL_OBJECT = VocabUtils.createIRI(NAMESPACE, "FAIRDigitalObject");

    public static final IRI IS_MATERIALIZED_BY = VocabUtils.createIRI(NAMESPACE, "isMaterializedBy");

    public static final IRI HAS_ENCODING_FORMAT = VocabUtils.createIRI(NAMESPACE, "hasEncodingFormat");

    public static final IRI HAS_METADATA = VocabUtils.createIRI(NAMESPACE, "hasMetadata");

}
