package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * This class defines the FAIR Digital Object Framework (FDOF) vocabulary.
 */
public class FDOF {

    public static final String NAMESPACE = "https://w3id.org/fdof/ontology#";

    public static final String PREFIX = "fdof";

    public static final Namespace NS = Utils.createNamespace(PREFIX, NAMESPACE);

    public static final IRI FAIR_DIGITAL_OBJECT = Utils.createIRI(NAMESPACE, "FAIRDigitalObject");

    public static final IRI IS_MATERIALIZED_BY = Utils.createIRI(NAMESPACE, "isMaterializedBy");

}
