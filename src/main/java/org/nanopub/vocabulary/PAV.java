package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * This class defines the PAV (Provenance, Authoring, and Versioning) vocabulary.
 */
public class PAV {

    public static final String NAMESPACE = "http://purl.org/pav/";

    public static final String PREFIX = "pav";

    public static final Namespace NS = VocabUtils.createNamespace(PREFIX, NAMESPACE);

    public static final IRI CREATED_BY = VocabUtils.createIRI(NAMESPACE, "createdBy");

    public static final IRI CREATED_ON = VocabUtils.createIRI(NAMESPACE, "createdOn");

    public static final IRI LAST_UPDATED_ON = VocabUtils.createIRI(NAMESPACE, "lastUpdatedOn");

    public static final IRI IMPORTED_ON = VocabUtils.createIRI(NAMESPACE, "importedOn");

    public static final IRI AUTHORED_BY = VocabUtils.createIRI(NAMESPACE, "authoredBy");

    public static final String NAMESPACE_V2 = "http://purl.org/pav/2.0/";

    public static final IRI CREATED_BY_V2 = VocabUtils.createIRI(NAMESPACE_V2, "createdBy");

    public static final IRI AUTHORED_BY_V2 = VocabUtils.createIRI(NAMESPACE_V2, "authoredBy");

    public static final IRI IMPORTED_ON_V2 = VocabUtils.createIRI(NAMESPACE_V2, "importedOn");

}
