package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * This class defines the NPX vocabulary, which defines extensions to the Nanopublication model.
 */
public class NPX {

    public static final String NAMESPACE = "http://purl.org/nanopub/x/";

    public static final String PREFIX = "npx";

    public static final Namespace NS = Utils.createNamespace(PREFIX, NAMESPACE);

    public static final IRI RETRACTS = Utils.createIRI(NAMESPACE, "retracts");

    public static final IRI INVALIDATES = Utils.createIRI(NAMESPACE, "invalidates");

    public static final IRI SUPERSEDES = Utils.createIRI(NAMESPACE, "supersedes");

    public static final IRI INTRODUCES = Utils.createIRI(NAMESPACE, "introduces");

    public static final IRI DESCRIBES = Utils.createIRI(NAMESPACE, "describes");

    public static final IRI EMBEDS = Utils.createIRI(NAMESPACE, "embeds");

    public static final IRI HAS_AGENTS = Utils.createIRI(NAMESPACE, "hasAgents");

    public static final IRI HAS_SERVICES = Utils.createIRI(NAMESPACE, "hasServices");

    public static final IRI HAS_BOOTSTRAP_SERVICE = Utils.createIRI(NAMESPACE, "hasBootstrapService");

    public static final IRI HAS_TRUST_RANGE_ALGORITHM = Utils.createIRI(NAMESPACE, "hasTrustRangeAlgorithm");

    public static final IRI HAS_UPDATE_STRATEGY = Utils.createIRI(NAMESPACE, "hasUpdateStrategy");

    public static final IRI NANOPUB_INDEX = Utils.createIRI(NAMESPACE, "NanopubIndex");

    public static final IRI NANOPUB_SERVICE = Utils.createIRI(NAMESPACE, "NanopubService");

    public static final IRI INCOMPLETE_INDEX = Utils.createIRI(NAMESPACE, "IncompleteIndex");

    public static final IRI INDEX_ASSERTION = Utils.createIRI(NAMESPACE, "IndexAssertion");

    public static final IRI INCLUDES_ELEMENT = Utils.createIRI(NAMESPACE, "includesElement");

    public static final IRI INCLUDES_SUBINDEX = Utils.createIRI(NAMESPACE, "includesSubindex");

    public static final IRI APPENDS_INDEX = Utils.createIRI(NAMESPACE, "appendsIndex");

    public static final IRI HAS_NANOPUB_TYPE = Utils.createIRI(NAMESPACE, "hasNanopubType");

    public static IRI EXAMPLE_NANOPUB = Utils.createIRI(NAMESPACE, "ExampleNanopub");

    public static final IRI AS_SENTENCE = Utils.createIRI(NAMESPACE, "asSentence");

    public static final IRI DECLARED_BY = Utils.createIRI(NAMESPACE, "declaredBy");

    public static final IRI HAS_KEY_LOCATION = Utils.createIRI(NAMESPACE, "hasKeyLocation");

    public static final IRI PROTECTED_NANOPUB = Utils.createIRI(NAMESPACE, "ProtectedNanopub");

    public static final IRI CRYPTO_ELEMENT = Utils.createIRI(NAMESPACE, "CryptoElement");

    public static final IRI HAS_ALGORITHM = Utils.createIRI(NAMESPACE, "hasAlgorithm");

    public static final IRI HAS_PUBLIC_KEY = Utils.createIRI(NAMESPACE, "hasPublicKey");

    public static final IRI NANOPUB_SIGNATURE_ELEMENT = Utils.createIRI(NAMESPACE, "NanopubSignatureElement");

    public static final IRI HAS_SIGNATURE_TARGET = Utils.createIRI(NAMESPACE, "hasSignatureTarget");

    public static final IRI HAS_SIGNATURE = Utils.createIRI(NAMESPACE, "hasSignature");

    public static final IRI SIGNED_BY = Utils.createIRI(NAMESPACE, "signedBy");

    // Deprecated; used for legacy signatures
    public static final IRI HAS_SIGNATURE_ELEMENT = Utils.createIRI(NAMESPACE, "hasSignatureElement");

    public static final IRI WAS_CREATED_AT = Utils.createIRI(NAMESPACE, "wasCreatedAt");

    public static final IRI UPDATES_BY_CREATOR = Utils.createIRI(NAMESPACE, "UpdatesByCreator");

    public static final IRI TRANSITIVE_TRUST = Utils.createIRI(NAMESPACE, "TransitiveTrust");

}
