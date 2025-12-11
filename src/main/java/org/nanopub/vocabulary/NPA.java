package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Vocabulary for Nanopub Administration (NPA).
 */
public class NPA {

    public static final String NAMESPACE = "http://purl.org/nanopub/admin/";

    public static final String PREFIX = "npa";

    public static final Namespace NS = VocabUtils.createNamespace(PREFIX, NAMESPACE);

    /**
     * IRI for the predicate that indicates that a hash value is associated with an object.
     */
    public static final IRI IS_HASH_OF = VocabUtils.createIRI(NAMESPACE, "http://purl.org/nanopub/admin/isHashOf");

    /**
     * Prefix for the hash values stored in the admin graph.
     */
    public static final IRI HASH = VocabUtils.createIRI(NAMESPACE, "hash");

    /**
     * IRI for the repo init ID.
     */
    public static final IRI HAS_REPO_INIT_ID = VocabUtils.createIRI(NAMESPACE, "hasRepoInitId");

    /**
     * IRI for the nanopub count.
     */
    public static final IRI HAS_NANOPUB_COUNT = VocabUtils.createIRI(NAMESPACE, "hasNanopubCount");

    /**
     * IRI for the nanopub checksum.
     */
    public static final IRI HAS_NANOPUB_CHECKSUM = VocabUtils.createIRI(NAMESPACE, "hasNanopubChecksum");

    /**
     * IRI for the nanopub load number.
     */
    public static final IRI HAS_LOAD_NUMBER = VocabUtils.createIRI(NAMESPACE, "hasLoadNumber");

    /**
     * IRI for the nanopub load checksum.
     */
    public static final IRI HAS_LOAD_CHECKSUM = VocabUtils.createIRI(NAMESPACE, "hasLoadChecksum");

    /**
     * IRI for the nanopub load timestamp.
     */
    public static final IRI HAS_LOAD_TIMESTAMP = VocabUtils.createIRI(NAMESPACE, "hasLoadTimestamp");

    /**
     * IRI for the nanopub load status.
     */
    public static final IRI HAS_STATUS = VocabUtils.createIRI(NAMESPACE, "hasStatus");

    /**
     * IRI for the nanopub registry load counter.
     */
    public static final IRI HAS_REGISTRY_LOAD_COUNTER = VocabUtils.createIRI(NAMESPACE, "hasRegistryLoadCounter");

    /**
     * IRI for the nanopub repository ID.
     */
    public static final IRI THIS_REPO = VocabUtils.createIRI(NAMESPACE, "thisRepo");

    /**
     * IRI for the nanopub coverage item.
     */
    public static final IRI HAS_COVERAGE_ITEM = VocabUtils.createIRI(NAMESPACE, "hasCoverageItem");

    /**
     * IRI for the nanopub coverage hash.
     */
    public static final IRI HAS_COVERAGE_HASH = VocabUtils.createIRI(NAMESPACE, "hasCoverageHash");

    /**
     * IRI for the nanopub coverage filter.
     */
    public static final IRI HAS_COVERAGE_FILTER = VocabUtils.createIRI(NAMESPACE, "hasCoverageFilter");

    /**
     * Admin graph IRI.
     */
    public static final IRI GRAPH = VocabUtils.createIRI(NAMESPACE, "graph");

    /**
     * Admin network graph IRI.
     */
    public static final IRI NETWORK_GRAPH = VocabUtils.createIRI(NAMESPACE, "networkGraph");

    /**
     * IRI for the head graph of a nanopub.
     */
    public static final IRI HAS_HEAD_GRAPH = VocabUtils.createIRI(NAMESPACE, "hasHeadGraph");

    /**
     * IRI for the graph of a nanopub.
     */
    public static final IRI HAS_GRAPH = VocabUtils.createIRI(NAMESPACE, "hasGraph");

    /**
     * IRI for the note about a nanopub.
     */
    public static final IRI NOTE = VocabUtils.createIRI(NAMESPACE, "note");

    /**
     * IRI for the subIRI of a nanopub.
     */
    public static final IRI HAS_SUB_IRI = VocabUtils.createIRI(NAMESPACE, "hasSubIri");

    /**
     * IRI for the refers to nanopub relation.
     */
    public static final IRI REFERS_TO_NANOPUB = VocabUtils.createIRI(NAMESPACE, "refersToNanopub");

    /**
     * IRI for the has valid signature for public key relation.
     */
    public static final IRI HAS_VALID_SIGNATURE_FOR_PUBLIC_KEY = VocabUtils.createIRI(NAMESPACE, "hasValidSignatureForPublicKey");

    /**
     * IRI for the has valid signature for public key hash relation.
     */
    public static final IRI HAS_VALID_SIGNATURE_FOR_PUBLIC_KEY_HASH = VocabUtils.createIRI(NAMESPACE, "hasValidSignatureForPublicKeyHash");

    /**
     * IRI for the has artifact code relation.
     */
    public static final IRI ARTIFACT_CODE = VocabUtils.createIRI(NAMESPACE, "artifactCode");

    /**
     * IRI for the is introduction of relation.
     */
    public static final IRI IS_INTRODUCTION_OF = VocabUtils.createIRI(NAMESPACE, "isIntroductionOf");

    /**
     * IRI for the declares pubkey relation.
     */
    public static final IRI DECLARES_PUBKEY = VocabUtils.createIRI(NAMESPACE, "declaresPubkey");

    /**
     * IRI for the has filter literal relation.
     */
    public static final IRI HAS_FILTER_LITERAL = VocabUtils.createIRI(NAMESPACE, "hasFilterLiteral");

}
