package org.nanopub.extra.security;

import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryAccess;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Checks that the key a nanopublication is about to be signed with is the one the network knows
 * the signer by (issue #150).
 *
 * <p>A signature made with a key that no introduction declares for its signer is cryptographically
 * fine, and the nanopublication publishes and verifies. What it cannot do is be attributed: the
 * registry has nothing tying the key to the person, so the nanopublication shows up under an
 * unapproved agent. Since a nanopublication cannot be edited afterwards, the only remedy is
 * publishing it again under a declared key and retracting the first.
 *
 * <p>The check is advisory. It reads the network, so it fails open: an unreachable query service
 * reports {@link Status#NOT_CHECKED} rather than blocking anyone from signing.
 */
public class ProfileKeyCheck {

    private static final Logger logger = LoggerFactory.getLogger(ProfileKeyCheck.class);

    /**
     * The query listing every introduction published on the network, with the user it introduces
     * and the public key it declares for them.
     */
    public static final String GET_ALL_USER_INTROS = "RAjHh6P11QFUaoPiMRBavdAnTq4YMJW4PB85oVFSBfYjU/get-all-user-intros";

    private ProfileKeyCheck() {
    }

    /**
     * What the network says about a signer and the key they are about to sign with.
     */
    public enum Status {

        /** An introduction signed with authority declares this key for this signer. */
        DECLARED(true),

        /**
         * An introduction declares this key for this signer, but none that carries authority.
         * Anyone can publish an introduction naming someone else, so such a declaration does not
         * establish that the key is theirs, and the registry need not treat it as approved.
         */
        DECLARED_WITHOUT_AUTHORITY(false),

        /** The signer is introduced on the network, but by some other key than this one. */
        KEY_NOT_DECLARED(false),

        /** Nothing on the network introduces this signer at all. */
        SIGNER_NOT_INTRODUCED(false),

        /** The network could not be asked, so nothing is known either way. */
        NOT_CHECKED(true);

        private final boolean acceptable;

        Status(boolean acceptable) {
            this.acceptable = acceptable;
        }

        /**
         * @return true if signing may proceed without complaint, including when nothing could be checked
         */
        public boolean isAcceptable() {
            return acceptable;
        }

    }

    /**
     * The outcome of a check, with a description written for whoever is about to sign.
     *
     * @param status  what the network says
     * @param message the description to show
     */
    public record Result(Status status, String message) {

        /**
         * @return true if signing may proceed without complaint
         */
        public boolean isAcceptable() {
            return status.isAcceptable();
        }

    }

    /**
     * Asks the network whether the given key is the one it knows the given signer by.
     *
     * @param signer          the signer IRI, typically an ORCID IRI
     * @param publicKeyString the base64 public key of the key pair that will sign, as
     *                        {@link SignatureUtils#encodePublicKey} writes it
     * @return the outcome, never null
     */
    public static Result check(IRI signer, String publicKeyString) {
        if (signer == null || publicKeyString == null || publicKeyString.isEmpty()) {
            return new Result(Status.NOT_CHECKED, "No signer or key to check.");
        }
        ApiResponse introductions;
        try {
            introductions = fetchIntroductions();
        } catch (Exception ex) {
            logger.debug("Could not fetch the introductions to check the signing key", ex);
            return new Result(Status.NOT_CHECKED,
                    "Could not check the signing key against the network: " + ex.getMessage()
                            + ". Signing goes ahead unchecked.");
        }
        return describe(classify(introductions, signer, publicKeyString), signer);
    }

    /**
     * Asks the network whether a signed nanopublication was signed with the key its signer is known
     * by, reading both from its signature.
     *
     * @param nanopub the signed nanopublication
     * @return the outcome, {@link Status#NOT_CHECKED} for a nanopublication whose signature does not
     * name exactly one signer and a key
     */
    public static Result check(Nanopub nanopub) {
        NanopubSignatureElement signature;
        try {
            signature = SignatureUtils.getSignatureElement(nanopub);
        } catch (MalformedCryptoElementException ex) {
            logger.debug("Signature element of nanopub {} is malformed", nanopub.getUri(), ex);
            return new Result(Status.NOT_CHECKED, "The signature could not be read, so its key was not checked.");
        }
        if (signature == null) {
            return new Result(Status.NOT_CHECKED, "The nanopublication is not signed, so no key was checked.");
        }
        Set<IRI> signers = signature.getSigners();
        if (signers == null || signers.size() != 1) {
            return new Result(Status.NOT_CHECKED, "The signature does not name exactly one signer, so its key was not checked.");
        }
        return check(signers.iterator().next(), signature.getPublicKeyString());
    }

    // How long a fetched list of introductions is reused. Publishing a file of nanopublications asks
    // the same question once per nanopublication, and the answer does not change between them; a few
    // minutes also keeps a newly published introduction from staying invisible for long.
    private static final long INTRODUCTIONS_MAX_AGE_MS = 5 * 60 * 1000;

    private static ApiResponse cachedIntroductions;
    private static long cachedAt;

    private static synchronized ApiResponse fetchIntroductions() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedIntroductions != null && now - cachedAt < INTRODUCTIONS_MAX_AGE_MS) {
            return cachedIntroductions;
        }
        ApiResponse introductions = QueryAccess.get(new QueryRef(GET_ALL_USER_INTROS));
        cachedIntroductions = introductions;
        cachedAt = now;
        return introductions;
    }

    /**
     * Forgets the introductions read from the network, so that the next check fetches them again.
     */
    public static synchronized void clearCache() {
        cachedIntroductions = null;
        cachedAt = 0;
    }

    /**
     * Works out what the given introductions say about a signer and key, without going near the
     * network, so that the classification can be exercised on its own.
     *
     * @param introductions   the response of {@link #GET_ALL_USER_INTROS}
     * @param signer          the signer IRI
     * @param publicKeyString the base64 public key that will sign
     * @return the status
     */
    static Status classify(ApiResponse introductions, IRI signer, String publicKeyString) {
        if (introductions == null) return Status.NOT_CHECKED;
        boolean signerIsIntroduced = false;
        boolean keyIsDeclared = false;
        for (ApiResponseEntry introduction : introductions.getData()) {
            if (!signer.stringValue().equals(introduction.get("user"))) continue;
            signerIsIntroduced = true;
            if (!publicKeyString.equals(introduction.get("pubkey"))) continue;
            keyIsDeclared = true;
            if ("true".equalsIgnoreCase(introduction.get("authoritative"))) return Status.DECLARED;
        }
        if (keyIsDeclared) return Status.DECLARED_WITHOUT_AUTHORITY;
        if (signerIsIntroduced) return Status.KEY_NOT_DECLARED;
        return Status.SIGNER_NOT_INTRODUCED;
    }

    private static Result describe(Status status, IRI signer) {
        return new Result(status, switch (status) {
            case DECLARED -> "The signing key is the one the network knows " + signer + " by.";
            case DECLARED_WITHOUT_AUTHORITY -> "The signing key is declared for " + signer
                    + ", but by an introduction that carries no authority, which anyone could have"
                    + " published. Nanopublications signed with it may show as coming from an"
                    + " unapproved agent.";
            case KEY_NOT_DECLARED -> signer + " is introduced on the network, but by a different key"
                    + " than the one about to sign. Nanopublications signed with this key cannot be"
                    + " attributed, and will show as coming from an unapproved agent. Publish an"
                    + " introduction declaring this key, or sign with the declared one.";
            case SIGNER_NOT_INTRODUCED -> "Nothing on the network introduces " + signer + "."
                    + " Nanopublications signed for them cannot be attributed, and will show as"
                    + " coming from an unapproved agent. Publish an introduction first.";
            case NOT_CHECKED -> "The signing key was not checked against the network.";
        });
    }

}
