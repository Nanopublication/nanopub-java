package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * This class defines the NPX vocabulary, which defines extensions to the Nanopublication model.
 */
public class NPX {

    public static final String NAMESPACE = "http://purl.org/nanopub/x/";

    public static final String PREFIX = "npx";

    public static final Namespace NS = VocabUtils.createNamespace(PREFIX, NAMESPACE);

    public static final IRI RETRACTS = VocabUtils.createIRI(NAMESPACE, "retracts");

    public static final IRI INVALIDATES = VocabUtils.createIRI(NAMESPACE, "invalidates");

    public static final IRI SUPERSEDES = VocabUtils.createIRI(NAMESPACE, "supersedes");

    public static final IRI INTRODUCES = VocabUtils.createIRI(NAMESPACE, "introduces");

    public static final IRI DESCRIBES = VocabUtils.createIRI(NAMESPACE, "describes");

    public static final IRI EMBEDS = VocabUtils.createIRI(NAMESPACE, "embeds");

    public static final IRI HAS_AGENTS = VocabUtils.createIRI(NAMESPACE, "hasAgents");

    public static final IRI HAS_SERVICES = VocabUtils.createIRI(NAMESPACE, "hasServices");

    public static final IRI HAS_BOOTSTRAP_SERVICE = VocabUtils.createIRI(NAMESPACE, "hasBootstrapService");

    public static final IRI HAS_TRUST_RANGE_ALGORITHM = VocabUtils.createIRI(NAMESPACE, "hasTrustRangeAlgorithm");

    public static final IRI HAS_UPDATE_STRATEGY = VocabUtils.createIRI(NAMESPACE, "hasUpdateStrategy");

    public static final IRI NANOPUB_INDEX = VocabUtils.createIRI(NAMESPACE, "NanopubIndex");

    public static final IRI NANOPUB_SERVICE = VocabUtils.createIRI(NAMESPACE, "NanopubService");

    public static final IRI INCOMPLETE_INDEX = VocabUtils.createIRI(NAMESPACE, "IncompleteIndex");

    public static final IRI INDEX_ASSERTION = VocabUtils.createIRI(NAMESPACE, "IndexAssertion");

    public static final IRI INCLUDES_ELEMENT = VocabUtils.createIRI(NAMESPACE, "includesElement");

    public static final IRI INCLUDES_SUBINDEX = VocabUtils.createIRI(NAMESPACE, "includesSubindex");

    public static final IRI APPENDS_INDEX = VocabUtils.createIRI(NAMESPACE, "appendsIndex");

    public static final IRI HAS_NANOPUB_TYPE = VocabUtils.createIRI(NAMESPACE, "hasNanopubType");

    public static IRI EXAMPLE_NANOPUB = VocabUtils.createIRI(NAMESPACE, "ExampleNanopub");

    public static final IRI AS_SENTENCE = VocabUtils.createIRI(NAMESPACE, "asSentence");

    public static final IRI DECLARED_BY = VocabUtils.createIRI(NAMESPACE, "declaredBy");

    public static final IRI HAS_KEY_LOCATION = VocabUtils.createIRI(NAMESPACE, "hasKeyLocation");

    public static final IRI PROTECTED_NANOPUB = VocabUtils.createIRI(NAMESPACE, "ProtectedNanopub");

    public static final IRI CRYPTO_ELEMENT = VocabUtils.createIRI(NAMESPACE, "CryptoElement");

    public static final IRI HAS_ALGORITHM = VocabUtils.createIRI(NAMESPACE, "hasAlgorithm");

    public static final IRI HAS_PUBLIC_KEY = VocabUtils.createIRI(NAMESPACE, "hasPublicKey");

    public static final IRI NANOPUB_SIGNATURE_ELEMENT = VocabUtils.createIRI(NAMESPACE, "NanopubSignatureElement");

    public static final IRI HAS_SIGNATURE_TARGET = VocabUtils.createIRI(NAMESPACE, "hasSignatureTarget");

    public static final IRI HAS_SIGNATURE = VocabUtils.createIRI(NAMESPACE, "hasSignature");

    public static final IRI SIGNED_BY = VocabUtils.createIRI(NAMESPACE, "signedBy");

    // Deprecated; used for legacy signatures
    public static final IRI HAS_SIGNATURE_ELEMENT = VocabUtils.createIRI(NAMESPACE, "hasSignatureElement");

    public static final IRI WAS_CREATED_AT = VocabUtils.createIRI(NAMESPACE, "wasCreatedAt");

    public static final IRI UPDATES_BY_CREATOR = VocabUtils.createIRI(NAMESPACE, "UpdatesByCreator");

    public static final IRI TRANSITIVE_TRUST = VocabUtils.createIRI(NAMESPACE, "TransitiveTrust");

}
