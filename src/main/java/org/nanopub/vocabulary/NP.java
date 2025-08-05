package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * This class defines the NP (Nanopub) vocabulary.
 */
public class NP {

    public static final String NAMESPACE = "http://www.nanopub.org/nschema#";

    public static final String PREFIX = "np";

    public static final Namespace NS = Utils.createNamespace(PREFIX, NAMESPACE);

    public static final IRI NANOPUBLICATION = Utils.createIRI(NAMESPACE, "Nanopublication");

    public static final IRI HAS_ASSERTION = Utils.createIRI(NAMESPACE, "hasAssertion");

    public static final IRI HAS_PROVENANCE = Utils.createIRI(NAMESPACE, "hasProvenance");

    public static final IRI HAS_PUBINFO = Utils.createIRI(NAMESPACE, "hasPublicationInfo");

}
