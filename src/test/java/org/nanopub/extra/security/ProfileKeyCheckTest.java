package org.nanopub.extra.security;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A signature made with a key no introduction declares for its signer is valid, publishes and
 * verifies -- and cannot be attributed, so the nanopublication shows up under an unapproved agent.
 * Since it cannot be corrected afterwards, the network is asked about the key first (issue #150).
 *
 * <p>A declaration counts only when the introduction carrying it is signed by one of the keys that
 * introduction declares: the first introduction of a signer is self-signed, and one adding a key is
 * signed by an existing key it restates alongside the new one.
 */
class ProfileKeyCheckTest {

    private static final IRI SIGNER = Values.iri("https://orcid.org/0000-0002-1267-0234");
    private static final IRI SOMEBODY_ELSE = Values.iri("https://orcid.org/0000-0001-8492-0354");
    private static final String KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtheKeyThatSigns";
    private static final String ANOTHER_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsomeOtherKey";
    private static final IRI FIRST_INTRO = Values.iri("https://w3id.org/np/RAfirstIntro");
    private static final IRI SECOND_INTRO = Values.iri("https://w3id.org/np/RAsecondIntro");

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

    /**
     * One key declaration: who it is for, which key it declares, which introduction carries it, and
     * whether that introduction is signed with the key this row declares.
     */
    private static String[] declaration(IRI user, String pubkey, IRI introduction, boolean signedWithThisKey) {
        return new String[]{user.stringValue(), "A Name", "false", introduction.stringValue(),
                "2026-09-01T00:00:00Z", pubkey, "hash", Boolean.toString(signedWithThisKey), ""};
    }

    private static String[] selfSignedDeclaration(IRI user, String pubkey) {
        return declaration(user, pubkey, FIRST_INTRO, true);
    }

    @Test
    void aSelfSignedIntroductionDeclaringThisKeyIsWhatWeWant() {
        ProfileKeyCheck.Classification classification =
                ProfileKeyCheck.classify(introductions(selfSignedDeclaration(SIGNER, KEY)), SIGNER, KEY);
        assertEquals(ProfileKeyCheck.Status.DECLARED, classification.status());
        assertEquals(List.of(FIRST_INTRO), classification.introductions());
    }

    /**
     * The way a second key is added: a further introduction, signed with the key the signer already
     * has, declaring that key again alongside the new one.
     */
    @Test
    void aKeyAddedByAnIntroductionSignedWithAnExistingKeyIsDeclared() {
        ProfileKeyCheck.Classification classification = ProfileKeyCheck.classify(introductions(
                selfSignedDeclaration(SIGNER, ANOTHER_KEY),
                declaration(SIGNER, ANOTHER_KEY, SECOND_INTRO, true),
                declaration(SIGNER, KEY, SECOND_INTRO, false)), SIGNER, KEY);
        assertEquals(ProfileKeyCheck.Status.DECLARED, classification.status());
        assertEquals(List.of(SECOND_INTRO), classification.introductions());
    }

    /**
     * Anyone can publish an introduction naming someone else. Such an introduction declares neither
     * the key that signed it nor, therefore, anything at all.
     */
    @Test
    void anIntroductionNotSignedWithAKeyItDeclaresIsNotEnough() {
        ProfileKeyCheck.Classification classification = ProfileKeyCheck.classify(introductions(
                selfSignedDeclaration(SIGNER, ANOTHER_KEY),
                declaration(SIGNER, KEY, SECOND_INTRO, false)), SIGNER, KEY);
        assertEquals(ProfileKeyCheck.Status.DECLARED_WITHOUT_AUTHORITY, classification.status());
        assertFalse(classification.status().isAcceptable());
        assertEquals(List.of(SECOND_INTRO), classification.introductions());
    }

    /**
     * People hold several keys, one per place they sign from, and the introduction that carries
     * authority is not necessarily the first row that matches.
     */
    @Test
    void anIntroductionWithAuthorityCountsWhereverItSitsInTheAnswer() {
        assertEquals(ProfileKeyCheck.Status.DECLARED, ProfileKeyCheck.classify(introductions(
                declaration(SIGNER, KEY, SECOND_INTRO, false),
                selfSignedDeclaration(SIGNER, KEY)), SIGNER, KEY).status());
    }

    @Test
    void everyIntroductionDeclaringTheKeyIsNamed() {
        ProfileKeyCheck.Classification classification = ProfileKeyCheck.classify(introductions(
                selfSignedDeclaration(SIGNER, KEY),
                declaration(SIGNER, KEY, SECOND_INTRO, true)), SIGNER, KEY);
        assertEquals(List.of(FIRST_INTRO, SECOND_INTRO), classification.introductions());
    }

    /**
     * The case that loses a nanopublication: a key regenerated, or a profile copied between machines
     * without its introduction.
     */
    @Test
    void anIntroducedSignerSigningWithAnUndeclaredKeyIsReported() {
        ProfileKeyCheck.Classification classification =
                ProfileKeyCheck.classify(introductions(selfSignedDeclaration(SIGNER, ANOTHER_KEY)), SIGNER, KEY);
        assertEquals(ProfileKeyCheck.Status.KEY_NOT_DECLARED, classification.status());
        assertTrue(classification.introductions().isEmpty());
    }

    @Test
    void aSignerNobodyHasIntroducedIsReported() {
        assertEquals(ProfileKeyCheck.Status.SIGNER_NOT_INTRODUCED,
                ProfileKeyCheck.classify(introductions(selfSignedDeclaration(SOMEBODY_ELSE, KEY)), SIGNER, KEY).status());
    }

    /**
     * Somebody else's introduction of the same key says nothing about this signer.
     */
    @Test
    void anIntroductionOfAnotherUserIsNotRead() {
        assertEquals(ProfileKeyCheck.Status.SIGNER_NOT_INTRODUCED,
                ProfileKeyCheck.classify(introductions(), SIGNER, KEY).status());
    }

    /**
     * An introduction declaring keys for two signers is signed by one key, which carries authority
     * only for the signer it is declared for.
     */
    @Test
    void authorityFromAnotherSignerInTheSameIntroductionDoesNotCount() {
        assertEquals(ProfileKeyCheck.Status.DECLARED_WITHOUT_AUTHORITY, ProfileKeyCheck.classify(introductions(
                declaration(SOMEBODY_ELSE, ANOTHER_KEY, FIRST_INTRO, true),
                declaration(SIGNER, KEY, FIRST_INTRO, false)), SIGNER, KEY).status());
    }

    /**
     * The check reads the network, and an unreachable query service is no reason to stop someone
     * signing what they were going to sign anyway.
     */
    @Test
    void anAnswerlessNetworkLeavesSigningAlone() {
        ProfileKeyCheck.Status status = ProfileKeyCheck.classify(null, SIGNER, KEY).status();
        assertEquals(ProfileKeyCheck.Status.NOT_CHECKED, status);
        assertTrue(status.isAcceptable());
    }

    @Test
    void nothingToCheckIsNotAComplaint() {
        assertTrue(ProfileKeyCheck.check(null, KEY).isAcceptable());
        assertTrue(ProfileKeyCheck.check(SIGNER, null).isAcceptable());
        assertTrue(ProfileKeyCheck.check(SIGNER, "").isAcceptable());
    }

    /**
     * Nothing to check is nothing to attribute either: the yes-or-no form answers for what the
     * network says, not for what it was not asked.
     */
    @Test
    void nothingToCheckIsNotAValidIntroduction() {
        assertFalse(ProfileKeyCheck.hasValidIntroduction(null, KEY));
        assertFalse(ProfileKeyCheck.hasValidIntroduction(SIGNER, null));
    }

    @Test
    void aResultNamesNoIntroductionWhenNoneWasRead() {
        assertTrue(ProfileKeyCheck.check(SIGNER, null).introductions().isEmpty());
    }

    /**
     * Every outcome names the signer and says what to do about it, since the whole point is that
     * whoever is signing can act before publishing rather than after.
     */
    @Test
    void everyComplaintExplainsItself() {
        String message = ProfileKeyCheck.check(SIGNER, null).message();
        assertFalse(message.isBlank());
    }

}
