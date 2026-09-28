package org.nanopub.extra.setting;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPX;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class IntroNanopubTest {

    @Test
    void getNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        String userName = "John Doe";
        IRI userUri = iri(TestUtils.ORCID);
        IntroNanopub introNanopub = new IntroNanopub(nanopub, userUri);
        assertEquals(nanopub, introNanopub.getNanopub());
    }

    @Test
    void getUser() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        String userName = "John Doe";
        IRI userUri = iri(TestUtils.ORCID);
        IntroNanopub introNanopub = new IntroNanopub(nanopub, userUri);
        assertEquals(userUri, introNanopub.getUser());
    }

    @Test
    void getNameNotSet() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        String userName = "John Doe";
        IRI userUri = iri(TestUtils.ORCID);
        IntroNanopub introNanopub = new IntroNanopub(nanopub, userUri);
        assertNull(introNanopub.getName());
    }

    @Test
    void getNameSet() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        IRI userUri = iri(TestUtils.ORCID);
        String userName = "John Doe";

        NanopubCreator nanopubCreator = TestUtils.getNanopubCreator();
        nanopubCreator.addAssertionStatement(userUri, FOAF.NAME, literal(userName));
        nanopubCreator.addProvenanceStatement(nanopubCreator.getAssertionUri(), TestUtils.anyIri, TestUtils.anyIri);
        nanopubCreator.addPubinfoStatement(nanopubCreator.getNanopubUri(), TestUtils.anyIri, TestUtils.anyIri);

        Nanopub nanopub = nanopubCreator.finalizeNanopub();
        IntroNanopub introNanopub = new IntroNanopub(nanopub, userUri);

        assertEquals(userName, introNanopub.getName());
    }

    @Test
    void constructWithNullName() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        IRI userUri = iri(TestUtils.ORCID);
        String userName = "John Doe";

        NanopubCreator nanopubCreator = TestUtils.getNanopubCreator();
        nanopubCreator.addAssertionStatement(userUri, FOAF.NAME, literal(userName));
        nanopubCreator.addProvenanceStatement(nanopubCreator.getAssertionUri(), TestUtils.anyIri, TestUtils.anyIri);
        nanopubCreator.addPubinfoStatement(nanopubCreator.getNanopubUri(), TestUtils.anyIri, TestUtils.anyIri);

        Nanopub nanopub = nanopubCreator.finalizeNanopub();
        IntroNanopub introNanopub = new IntroNanopub(nanopub, userUri);

        assertEquals(userName, introNanopub.getName());
    }

    // ------------------------------------------------------ key declarations

    private static final IRI USER = iri(TestUtils.ORCID);
    private static final IRI KEY_DECLARATION = iri("https://example.org/keyDeclaration");
    private static final IRI KEY_LOCATION = iri("https://example.org/keys/");

    /**
     * A nanopub whose assertion declares a key, with the parts the test asks for.
     */
    private static Nanopub introNanopubWith(String publicKey, IRI declarer, String algorithm) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        if (declarer != null) {
            creator.addAssertionStatement(KEY_DECLARATION, NPX.DECLARED_BY, declarer);
        }
        creator.addAssertionStatement(KEY_DECLARATION, NPX.HAS_KEY_LOCATION, KEY_LOCATION);
        if (publicKey != null) {
            creator.addAssertionStatement(KEY_DECLARATION, NPX.HAS_PUBLIC_KEY, literal(publicKey));
        }
        if (algorithm != null) {
            creator.addAssertionStatement(KEY_DECLARATION, NPX.HAS_ALGORITHM, literal(algorithm));
        }
        creator.addProvenanceStatement(creator.getAssertionUri(), TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(creator.getNanopubUri(), TestUtils.anyIri, TestUtils.anyIri);
        return creator.finalizeNanopub();
    }

    @Test
    void readsAKeyDeclaration() throws Exception {
        IntroNanopub intro = new IntroNanopub(introNanopubWith("a public key", USER, "RSA"), USER);

        assertEquals(1, intro.getKeyDeclarations().size());
        KeyDeclaration declaration = intro.getKeyDeclarations().getFirst();
        assertEquals("a public key", declaration.getPublicKeyString());
        assertEquals(SignatureAlgorithm.RSA, declaration.getAlgorithm());
        assertEquals(KEY_LOCATION, declaration.getKeyLocation());
        assertTrue(declaration.getDeclarers().contains(USER));
    }

    @Test
    void takesTheUserFromTheDeclarationWhenNoneIsGiven() throws Exception {
        IntroNanopub intro = new IntroNanopub(introNanopubWith("a public key", USER, "RSA"));

        assertEquals(USER, intro.getUser());
        assertEquals(1, intro.getKeyDeclarations().size());
    }

    @Test
    void dropsKeyDeclarationsWithoutAPublicKey() throws Exception {
        IntroNanopub intro = new IntroNanopub(introNanopubWith(null, USER, "RSA"), USER);

        assertTrue(intro.getKeyDeclarations().isEmpty());
    }

    @Test
    void dropsKeyDeclarationsWithAnEmptyPublicKey() throws Exception {
        IntroNanopub intro = new IntroNanopub(introNanopubWith("", USER, "RSA"), USER);

        assertTrue(intro.getKeyDeclarations().isEmpty());
    }

    @Test
    void dropsKeyDeclarationsOfAnotherUser() throws Exception {
        IRI otherUser = iri("https://orcid.org/0000-0000-0000-0009");

        IntroNanopub intro = new IntroNanopub(introNanopubWith("a public key", otherUser, "RSA"), USER);

        assertTrue(intro.getKeyDeclarations().isEmpty());
    }

    @Test
    void ignoresAMalformedAlgorithm() throws Exception {
        IntroNanopub intro = new IntroNanopub(introNanopubWith("a public key", USER, "magic"), USER);

        assertEquals(1, intro.getKeyDeclarations().size());
        assertNull(intro.getKeyDeclarations().getFirst().getAlgorithm());
    }

    @Test
    void ignoresASecondPublicKey() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(KEY_DECLARATION, NPX.DECLARED_BY, USER);
        creator.addAssertionStatement(KEY_DECLARATION, NPX.HAS_PUBLIC_KEY, literal("first key"));
        creator.addAssertionStatement(KEY_DECLARATION, NPX.HAS_PUBLIC_KEY, literal("second key"));
        creator.addProvenanceStatement(creator.getAssertionUri(), TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(creator.getNanopubUri(), TestUtils.anyIri, TestUtils.anyIri);

        IntroNanopub intro = new IntroNanopub(creator.finalizeNanopub(), USER);

        assertEquals(1, intro.getKeyDeclarations().size());
        assertNotNull(intro.getKeyDeclarations().getFirst().getPublicKeyString());
    }

    // ------------------------------------------------------------- extraction

    private static HttpClient httpClientReturning(int statusCode, String turtle) throws IOException {
        HttpClient client = mock(HttpClient.class);
        HttpResponse response = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(statusLine.toString()).thenReturn("HTTP/1.1 " + statusCode);
        when(response.getStatusLine()).thenReturn(statusLine);
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(turtle.getBytes(StandardCharsets.UTF_8)));
        when(response.getEntity()).thenReturn(entity);
        when(client.execute(any(HttpGet.class))).thenReturn(response);
        return client;
    }

    @Test
    void extractReadsTheUserPage() throws Exception {
        String userId = "https://example.org/user";
        String turtle = "<" + userId + "> <" + RDFS.LABEL + "> \"Jane Doe\" .\n";

        IntroNanopub.IntroExtractor extractor = IntroNanopub.extract(userId, httpClientReturning(200, turtle));

        assertEquals("Jane Doe", extractor.getName());
        assertNull(extractor.getIntroNanopub());
    }

    @Test
    void extractFollowsTheFoafPageToTheIntroNanopub() throws Exception {
        String userId = "https://example.org/user";
        String npUri = "https://w3id.org/np/RAO30EliKt55zd1CjWpKBE9q3KeJfoy9q0Q5x-XaSNxRk";
        String turtle = "<" + userId + "> <" + FOAF.PAGE + "> <" + npUri + "> .\n";
        Nanopub introNanopub = TestUtils.createNanopub();

        try (MockedStatic<GetNanopub> getNanopub = mockStatic(GetNanopub.class)) {
            getNanopub.when(() -> GetNanopub.get(npUri)).thenReturn(introNanopub);

            IntroNanopub.IntroExtractor extractor = IntroNanopub.extract(userId, httpClientReturning(200, turtle));

            assertEquals(introNanopub, extractor.getIntroNanopub());
        }
    }

    @Test
    void extractIgnoresPagesThatAreNotNanopubs() throws Exception {
        String userId = "https://example.org/user";
        String turtle = "<" + userId + "> <" + FOAF.PAGE + "> <https://example.org/homepage> .\n";

        IntroNanopub.IntroExtractor extractor = IntroNanopub.extract(userId, httpClientReturning(200, turtle));

        assertNull(extractor.getIntroNanopub());
    }

    @Test
    void extractIgnoresStatementsAboutSomebodyElse() throws Exception {
        String userId = "https://example.org/user";
        String turtle = "<https://example.org/other> <" + RDFS.LABEL + "> \"Someone else\" .\n";

        IntroNanopub.IntroExtractor extractor = IntroNanopub.extract(userId, httpClientReturning(200, turtle));

        assertNull(extractor.getName());
    }

    @Test
    void extractReportsAnUnsuccessfulResponse() throws Exception {
        HttpClient client = httpClientReturning(404, "");

        assertThrows(IOException.class, () -> IntroNanopub.extract("https://example.org/user", client));
    }

    @Test
    @SuppressWarnings("deprecation")
    void extractReportsAnUnusableUserId() {
        IOException ex = assertThrows(IOException.class,
                () -> IntroNanopub.extract("https://example.org/{user}", mock(HttpClient.class)));
        assertTrue(ex.getMessage().startsWith("invalid URL: "), ex.getMessage());
    }

    @Test
    void getBuildsAnIntroNanopubFromTheUserPage() throws Exception {
        String userId = "https://example.org/user";
        String npUri = "https://w3id.org/np/RAO30EliKt55zd1CjWpKBE9q3KeJfoy9q0Q5x-XaSNxRk";
        String turtle = "<" + userId + "> <" + FOAF.PAGE + "> <" + npUri + "> .\n";
        Nanopub introNanopub = TestUtils.createNanopub();

        try (MockedStatic<GetNanopub> getNanopub = mockStatic(GetNanopub.class)) {
            getNanopub.when(() -> GetNanopub.get(npUri)).thenReturn(introNanopub);

            IntroNanopub intro = IntroNanopub.get(userId, httpClientReturning(200, turtle));

            assertNotNull(intro);
            assertEquals(introNanopub, intro.getNanopub());
            assertEquals(iri(userId), intro.getUser());
        }
    }

    @Test
    void getBuildsAnIntroNanopubFromAnExtractor() throws Exception {
        String userId = "https://example.org/user";
        Nanopub nanopub = TestUtils.createNanopub();
        IntroNanopub.IntroExtractor extractor = mock(IntroNanopub.IntroExtractor.class);
        when(extractor.getIntroNanopub()).thenReturn(nanopub);

        IntroNanopub intro = IntroNanopub.get(userId, extractor);

        assertEquals(nanopub, intro.getNanopub());
        assertEquals(iri(userId), intro.getUser());
    }

}