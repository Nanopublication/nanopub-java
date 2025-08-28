package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Vocabulary for Nanopub Administration (NPA).
 */
public class NPA {

    public static final String NAMESPACE = "http://purl.org/nanopub/admin/";

    public static final String PREFIX = "npa";

    public static final Namespace NS = Utils.createNamespace(PREFIX, NAMESPACE);

    /**
     * IRI for the predicate that indicates that a hash value is associated with an object.
     */
    public static final IRI IS_HASH_OF = Utils.createIRI(NAMESPACE, "http://purl.org/nanopub/admin/isHashOf");

    /**
     * Prefix for the hash values stored in the admin graph.
     */
    public static final IRI HASH = Utils.createIRI(NAMESPACE, "hash");

    /**
     * IRI for the repo init ID.
     */
    public static final IRI HAS_REPO_INIT_ID = Utils.createIRI(NAMESPACE, "hasRepoInitId");

    /**
     * IRI for the nanopub count.
     */
    public static final IRI HAS_NANOPUB_COUNT = Utils.createIRI(NAMESPACE, "hasNanopubCount");

    /**
     * IRI for the nanopub checksum.
     */
    public static final IRI HAS_NANOPUB_CHECKSUM = Utils.createIRI(NAMESPACE, "hasNanopubChecksum");

    /**
     * IRI for the nanopub load number.
     */
    public static final IRI HAS_LOAD_NUMBER = Utils.createIRI(NAMESPACE, "hasLoadNumber");

    /**
     * IRI for the nanopub load checksum.
     */
    public static final IRI HAS_LOAD_CHECKSUM = Utils.createIRI(NAMESPACE, "hasLoadChecksum");

    /**
     * IRI for the nanopub load timestamp.
     */
    public static final IRI HAS_LOAD_TIMESTAMP = Utils.createIRI(NAMESPACE, "hasLoadTimestamp");

    /**
     * IRI for the nanopub load status.
     */
    public static final IRI HAS_STATUS = Utils.createIRI(NAMESPACE, "hasStatus");

    /**
     * IRI for the nanopub registry load counter.
     */
    public static final IRI HAS_REGISTRY_LOAD_COUNTER = Utils.createIRI(NAMESPACE, "hasRegistryLoadCounter");

    /**
     * IRI for the nanopub repository ID.
     */
    public static final IRI THIS_REPO = Utils.createIRI(NAMESPACE, "thisRepo");

    /**
     * IRI for the nanopub coverage item.
     */
    public static final IRI HAS_COVERAGE_ITEM = Utils.createIRI(NAMESPACE, "hasCoverageItem");

    /**
     * IRI for the nanopub coverage hash.
     */
    public static final IRI HAS_COVERAGE_HASH = Utils.createIRI(NAMESPACE, "hasCoverageHash");

    /**
     * IRI for the nanopub coverage filter.
     */
    public static final IRI HAS_COVERAGE_FILTER = Utils.createIRI(NAMESPACE, "hasCoverageFilter");

    /**
     * Admin graph IRI.
     */
    public static final IRI GRAPH = Utils.createIRI(NAMESPACE, "graph");

    /**
     * Admin network graph IRI.
     */
    public static final IRI NETWORK_GRAPH = Utils.createIRI(NAMESPACE, "networkGraph");

    /**
     * IRI for the head graph of a nanopub.
     */
    public static final IRI HAS_HEAD_GRAPH = Utils.createIRI(NAMESPACE, "hasHeadGraph");

    /**
     * IRI for the graph of a nanopub.
     */
    public static final IRI HAS_GRAPH = Utils.createIRI(NAMESPACE, "hasGraph");

    /**
     * IRI for the note about a nanopub.
     */
    public static final IRI NOTE = Utils.createIRI(NAMESPACE, "note");

    /**
     * IRI for the subIRI of a nanopub.
     */
    public static final IRI HAS_SUB_IRI = Utils.createIRI(NAMESPACE, "hasSubIri");

    /**
     * IRI for the refers to nanopub relation.
     */
    public static final IRI REFERS_TO_NANOPUB = Utils.createIRI(NAMESPACE, "refersToNanopub");

    /**
     * IRI for the has valid signature for public key relation.
     */
    public static final IRI HAS_VALID_SIGNATURE_FOR_PUBLIC_KEY = Utils.createIRI(NAMESPACE, "hasValidSignatureForPublicKey");

    /**
     * IRI for the has valid signature for public key hash relation.
     */
    public static final IRI HAS_VALID_SIGNATURE_FOR_PUBLIC_KEY_HASH = Utils.createIRI(NAMESPACE, "hasValidSignatureForPublicKeyHash");

    /**
     * IRI for the has artifact code relation.
     */
    public static final IRI ARTIFACT_CODE = Utils.createIRI(NAMESPACE, "artifactCode");

    /**
     * IRI for the is introduction of relation.
     */
    public static final IRI IS_INTRODUCTION_OF = Utils.createIRI(NAMESPACE, "isIntroductionOf");

    /**
     * IRI for the declares pubkey relation.
     */
    public static final IRI DECLARES_PUBKEY = Utils.createIRI(NAMESPACE, "declaresPubkey");

    /**
     * IRI for the has filter literal relation.
     */
    public static final IRI HAS_FILTER_LITERAL = Utils.createIRI(NAMESPACE, "hasFilterLiteral");

}
