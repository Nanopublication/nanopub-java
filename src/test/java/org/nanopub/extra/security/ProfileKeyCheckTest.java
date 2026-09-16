package org.nanopub.extra.security;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A signature made with a key no introduction declares for its signer is valid, publishes and
 * verifies -- and cannot be attributed, so the nanopublication shows up under an unapproved agent.
 * Since it cannot be corrected afterwards, the network is asked about the key first (issue #150).
 */
class ProfileKeyCheckTest {

    private static final IRI SIGNER = Values.iri("https://orcid.org/0000-0002-1267-0234");
    private static final IRI SOMEBODY_ELSE = Values.iri("https://orcid.org/0000-0001-8492-0354");
    private static final String KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtheKeyThatSigns";
    private static final String ANOTHER_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsomeOtherKey";

    /**
     * Builds a response shaped like the one the introductions query answers with.
     */
    private static ApiResponse introductions(String[]... rows) {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"user", "name", "isSoftware", "intronp", "date", "pubkey", "pubkeyHash", "authoritative", "keyLocation"});
        for (String[] row : rows) {
            response.add(row);
        }
        return response;
    }

    private static String[] introduction(IRI user, String pubkey, String authoritative) {
        return new String[]{user.stringValue(), "A Name", "false", "https://w3id.org/np/RAintro", "2026-09-01T00:00:00Z", pubkey, "hash", authoritative, ""};
    }

    @Test
    void anAuthoritativeIntroductionForThisKeyIsWhatWeWant() {
        assertEquals(ProfileKeyCheck.Status.DECLARED,
                ProfileKeyCheck.classify(introductions(introduction(SIGNER, KEY, "true")), SIGNER, KEY));
    }

    // Anyone can publish an introduction naming someone else, so one without authority does not
    // establish that the key is theirs.
    @Test
    void anIntroductionWithoutAuthorityIsNotEnough() {
        ProfileKeyCheck.Status status =
                ProfileKeyCheck.classify(introductions(introduction(SIGNER, KEY, "false")), SIGNER, KEY);
        assertEquals(ProfileKeyCheck.Status.DECLARED_WITHOUT_AUTHORITY, status);
        assertFalse(status.isAcceptable());
    }

    // People hold several keys, one per place they sign from, and the authoritative declaration is
    // not necessarily the first row that matches.
    @Test
    void anAuthoritativeIntroductionCountsWhereverItSitsInTheAnswer() {
        assertEquals(ProfileKeyCheck.Status.DECLARED, ProfileKeyCheck.classify(introductions(
                introduction(SIGNER, ANOTHER_KEY, "true"),
                introduction(SIGNER, KEY, "false"),
                introduction(SIGNER, KEY, "true")), SIGNER, KEY));
    }

    // The case that loses a nanopublication: a key regenerated, or a profile copied between machines
    // without its introduction.
    @Test
    void anIntroducedSignerSigningWithAnUndeclaredKeyIsReported() {
        assertEquals(ProfileKeyCheck.Status.KEY_NOT_DECLARED,
                ProfileKeyCheck.classify(introductions(introduction(SIGNER, ANOTHER_KEY, "true")), SIGNER, KEY));
    }

    @Test
    void aSignerNobodyHasIntroducedIsReported() {
        assertEquals(ProfileKeyCheck.Status.SIGNER_NOT_INTRODUCED,
                ProfileKeyCheck.classify(introductions(introduction(SOMEBODY_ELSE, KEY, "true")), SIGNER, KEY));
    }

    // Somebody else's introduction of the same key says nothing about this signer.
    @Test
    void anIntroductionOfAnotherUserIsNotRead() {
        assertEquals(ProfileKeyCheck.Status.SIGNER_NOT_INTRODUCED,
                ProfileKeyCheck.classify(introductions(), SIGNER, KEY));
    }

    // The check reads the network, and an unreachable query service is no reason to stop someone
    // signing what they were going to sign anyway.
    @Test
    void anAnswerlessNetworkLeavesSigningAlone() {
        ProfileKeyCheck.Status status = ProfileKeyCheck.classify(null, SIGNER, KEY);
        assertEquals(ProfileKeyCheck.Status.NOT_CHECKED, status);
        assertTrue(status.isAcceptable());
    }

    @Test
    void nothingToCheckIsNotAComplaint() {
        assertTrue(ProfileKeyCheck.check(null, KEY).isAcceptable());
        assertTrue(ProfileKeyCheck.check(SIGNER, null).isAcceptable());
        assertTrue(ProfileKeyCheck.check(SIGNER, "").isAcceptable());
    }

    // Every outcome names the signer and says what to do about it, since the whole point is that
    // whoever is signing can act before publishing rather than after.
    @Test
    void everyComplaintExplainsItself() {
        String message = ProfileKeyCheck.check(SIGNER, null).message();
        assertFalse(message.isBlank());
    }

}
