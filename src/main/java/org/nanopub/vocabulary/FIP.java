package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * This class defines the FAIR Implementation Profile (FIP) vocabulary.
 */
public class FIP {

    public static final String NAMESPACE = "https://w3id.org/fair/fip/terms/";

    public static final String PREFIX = "fip";

    public static final Namespace NS = VocabUtils.createNamespace(PREFIX, NAMESPACE);

    public static IRI AVAILABLE_FAIR_ENABLING_RESOURCE = VocabUtils.createIRI(NAMESPACE, "Available-FAIR-Enabling-Resource");

    public static IRI AVAILABLE_FAIR_SUPPORTING_RESOURCE = VocabUtils.createIRI(NAMESPACE, "Available-FAIR-Supporting-Resource");

    public static IRI FAIR_ENABLING_RESOURCE_TO_BE_DEVELOPED = VocabUtils.createIRI(NAMESPACE, "FAIR-Enabling-Resource-to-be-Developed");

    public static IRI FAIR_SUPPORTING_RESOURCE_TO_BE_DEVELOPED = VocabUtils.createIRI(NAMESPACE, "FAIR-Supporting-Resource-to-be-Developed");

    public static IRI FAIR_ENABLING_RESOURCE = VocabUtils.createIRI(NAMESPACE, "FAIR-Enabling-Resource");

    public static IRI FAIR_SUPPORTING_RESOURCE = VocabUtils.createIRI(NAMESPACE, "FAIR-Supporting-Resource");

    public static IRI FAIR_IMPLEMENTATION_PROFILE = VocabUtils.createIRI(NAMESPACE, "FAIR-Implementation-Profile");

}
