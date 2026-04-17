package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Vocabulary for schema.org.
 * <p>
 * This class provides constants for commonly used schema.org properties.
 */
public class SCHEMA {

    public static final String NAMESPACE = "http://schema.org/";

    public static final String PREFIX = "schema";

    public static final Namespace NS = VocabUtils.createNamespace(PREFIX, NAMESPACE);

    public static final IRI DESCRIPTION = VocabUtils.createIRI(NAMESPACE, "description");

    public static final IRI NAME = VocabUtils.createIRI(NAMESPACE, "name");

    public static final IRI IS_BASED_ON = VocabUtils.createIRI(NAMESPACE, "isBasedOn");

    public static final IRI PERSON = VocabUtils.createIRI(NAMESPACE, "Person");

    public static final IRI RO_CRATE_IDENTIFIER = VocabUtils.createIRI(NAMESPACE, "identifier");

    public static final IRI RO_CRATE_HAS_PART = VocabUtils.createIRI(NAMESPACE, "hasPart");

}
