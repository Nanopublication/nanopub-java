package org.nanopub;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nanopub.trusty.TempUriReplacer;
import org.nanopub.trusty.TrustyNanopubUtils;
import org.nanopub.utils.TestUtils;

import java.util.List;
import java.util.Set;

import static org.eclipse.rdf4j.model.util.Values.namespace;
import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

class NanopubCreatorTest {

    private NanopubCreator createNanopubCreator() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(vf.createIRI(TestUtils.NANOPUB_URI));
        Statement assertionStatement = vf.createStatement(anyIri, anyIri, anyIri);
        creator.addAssertionStatements(assertionStatement);

        Statement provenanceStatement = vf.createStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addProvenanceStatements(provenanceStatement);

        Statement pubinfoStatement = vf.createStatement(creator.getNanopubUri(), anyIri, anyIri);
        creator.addPubinfoStatements(pubinfoStatement);
        return creator;
    }

    @Test
    void createEmpty() {
        NanopubCreator creator = new NanopubCreator();
        Assertions.assertNotNull(creator);
        Assertions.assertNull(creator.getAssertionUri());
        Assertions.assertNull(creator.getPubinfoUri());
        Assertions.assertNull(creator.getProvenanceUri());
        Assertions.assertNull(creator.getNanopubUri());
        Assertions.assertTrue(creator.getCurrentAssertionStatements().isEmpty());
        Assertions.assertTrue(creator.getCurrentPubinfoStatements().isEmpty());
        Assertions.assertTrue(creator.getCurrentProvenanceStatements().isEmpty());
    }

    @Test
    void setNanopubUri() throws NanopubAlreadyFinalizedException {
        // Test with String
        NanopubCreator creator = new NanopubCreator();
        Assertions.assertNull(creator.getNanopubUri());
        creator.setNanopubUri(TestUtils.NANOPUB_URI);
        Assertions.assertEquals(TestUtils.NANOPUB_URI, creator.getNanopubUri().toString());

        // Test with IRI
        String testIri = "https://knowledgepixels.com/nanopubIri#test2";
        creator.setNanopubUri(vf.createIRI(testIri));
        Assertions.assertEquals(testIri, creator.getNanopubUri().toString());
    }

    @Test
    void setNanopubUriWithFinalized() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createNanopubCreator();
        creator.finalizeNanopub();
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.setNanopubUri(TestUtils.NANOPUB_URI));
    }

    @Test
    void finalizeOnFinalized() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createNanopubCreator();
        Nanopub nanopub = creator.finalizeNanopub();
        assertNotNull(nanopub);
        Nanopub nanopub1 = creator.finalizeNanopub();
        assertEquals(nanopub, nanopub1);
    }

    @Test
    void finalizeNanopubThrowsExceptionWhenUriIsNull() {
        NanopubCreator creator = new NanopubCreator();
        assertThrows(MalformedNanopubException.class, () -> creator.finalizeNanopub(false));
    }

    @Test
    void finalizeNanopubWithoutTimestamp() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createNanopubCreator();
        Nanopub nanopub = creator.finalizeNanopub();
        assertNotNull(nanopub);
        assertEquals(TestUtils.NANOPUB_URI, nanopub.getUri().toString());
        assertFalse(nanopub.getPubinfo().stream().anyMatch(st -> st.getPredicate().equals(DCTERMS.CREATED)));
    }

    @Test
    void finalizeNanopubWithTimestamp() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createNanopubCreator();
        Nanopub nanopub = creator.finalizeNanopub(true);
        assertNotNull(nanopub);
        assertEquals(TestUtils.NANOPUB_URI, nanopub.getUri().toString());
        assertTrue(nanopub.getPubinfo().stream().anyMatch(st -> st.getPredicate().equals(DCTERMS.CREATED)));
    }

    private NanopubCreator createFinalizedNanopubCreator() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createNanopubCreator();
        creator.finalizeNanopub();
        return creator;
    }

    @Test
    void createWithTempNanopubIris() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(true);
        assertNotNull(creator.getNanopubUri());
        assertTrue(creator.getNanopubUri().stringValue().startsWith(TempUriReplacer.tempUri));
        assertTrue(creator.getAssertionUri().stringValue().startsWith(creator.getNanopubUri().stringValue()));
    }

    @Test
    void createWithoutTempNanopubIris() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(false);
        assertNull(creator.getNanopubUri());
    }

    @Test
    void setNanopubUriAfterItHasBeenUsed() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(TestUtils.NANOPUB_URI);
        // this fixes the nanopub URI, as it is now referred to from a pubinfo statement
        creator.addPubinfoStatement(anyIri, anyIri);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> creator.setNanopubUri("https://knowledgepixels.com/nanopubIri#other"));
        assertEquals("Cannot change nanopublication URI anymore: has already been used", ex.getMessage());
    }

    @Test
    void setAssertionUri() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(TestUtils.NANOPUB_URI);

        String withString = TestUtils.NANOPUB_URI + "assertion1";
        creator.setAssertionUri(withString);
        assertEquals(withString, creator.getAssertionUri().toString());

        IRI withIri = vf.createIRI(TestUtils.NANOPUB_URI + "assertion2");
        creator.setAssertionUri(withIri);
        assertEquals(withIri, creator.getAssertionUri());
    }

    @Test
    void setAssertionUriAfterItHasBeenUsed() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(TestUtils.NANOPUB_URI);
        // this fixes the assertion URI, as it is now referred to from a provenance statement
        creator.addProvenanceStatement(anyIri, anyIri);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> creator.setAssertionUri(TestUtils.NANOPUB_URI + "other"));
        assertEquals("Cannot change assertion URI anymore: has already been used", ex.getMessage());
    }

    @Test
    void setAssertionUriWithFinalized() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createFinalizedNanopubCreator();
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.setAssertionUri(anyIri));
    }

    @Test
    void setProvenanceUri() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(TestUtils.NANOPUB_URI);

        String withString = TestUtils.NANOPUB_URI + "provenance1";
        creator.setProvenanceUri(withString);
        assertEquals(withString, creator.getProvenanceUri().toString());

        IRI withIri = vf.createIRI(TestUtils.NANOPUB_URI + "provenance2");
        creator.setProvenanceUri(withIri);
        assertEquals(withIri, creator.getProvenanceUri());
    }

    @Test
    void setProvenanceUriWithFinalized() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createFinalizedNanopubCreator();
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.setProvenanceUri(anyIri));
    }

    @Test
    void setPubinfoUri() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(TestUtils.NANOPUB_URI);

        String withString = TestUtils.NANOPUB_URI + "pubinfo1";
        creator.setPubinfoUri(withString);
        assertEquals(withString, creator.getPubinfoUri().toString());

        IRI withIri = vf.createIRI(TestUtils.NANOPUB_URI + "pubinfo2");
        creator.setPubinfoUri(withIri);
        assertEquals(withIri, creator.getPubinfoUri());
    }

    @Test
    void setPubinfoUriWithFinalized() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createFinalizedNanopubCreator();
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.setPubinfoUri(anyIri));
    }

    @Test
    void addAssertionStatementsFromIterable() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(TestUtils.NANOPUB_URI);
        Statement st1 = vf.createStatement(anyIri, anyIri, anyIri);
        Statement st2 = vf.createStatement(anyIri, RDFS.LABEL, vf.createLiteral("label"));

        creator.addAssertionStatements(List.of(st1, st2));

        assertEquals(List.of(st1, st2), creator.getCurrentAssertionStatements());
    }

    @Test
    void addAssertionStatementsWithFinalized() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createFinalizedNanopubCreator();
        Statement st = vf.createStatement(anyIri, anyIri, anyIri);

        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addAssertionStatements(st));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addAssertionStatements(List.of(st)));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addAssertionStatement(st));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addAssertionStatement(anyIri, anyIri, anyIri));
    }

    @Test
    void addProvenanceStatementsFromIterable() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(TestUtils.NANOPUB_URI);
        Statement st1 = vf.createStatement(creator.getAssertionUri(), anyIri, anyIri);
        Statement st2 = vf.createStatement(creator.getAssertionUri(), RDFS.LABEL, vf.createLiteral("label"));

        creator.addProvenanceStatements(List.of(st1, st2));

        assertEquals(List.of(st1, st2), creator.getCurrentProvenanceStatements());
    }

    @Test
    void addProvenanceStatementFromStatement() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(TestUtils.NANOPUB_URI);
        Statement st = vf.createStatement(creator.getAssertionUri(), anyIri, anyIri);

        creator.addProvenanceStatement(st);

        assertEquals(List.of(st), creator.getCurrentProvenanceStatements());
    }

    @Test
    void addProvenanceStatementWithoutAssertionUri() {
        NanopubCreator creator = new NanopubCreator();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> creator.addProvenanceStatement(anyIri, anyIri));
        assertEquals("Assertion URI not yet set", ex.getMessage());
    }

    @Test
    void addProvenanceStatementsWithFinalized() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createFinalizedNanopubCreator();
        Statement st = vf.createStatement(creator.getAssertionUri(), anyIri, anyIri);

        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addProvenanceStatements(st));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addProvenanceStatements(List.of(st)));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addProvenanceStatement(st));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addProvenanceStatement(anyIri, anyIri));
    }

    @Test
    void addPubinfoStatementsFromIterable() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(TestUtils.NANOPUB_URI);
        Statement st1 = vf.createStatement(creator.getNanopubUri(), anyIri, anyIri);
        Statement st2 = vf.createStatement(creator.getNanopubUri(), RDFS.LABEL, vf.createLiteral("label"));

        creator.addPubinfoStatements(List.of(st1, st2));

        assertEquals(List.of(st1, st2), creator.getCurrentPubinfoStatements());
    }

    @Test
    void addPubinfoStatementWithoutNanopubUri() {
        NanopubCreator creator = new NanopubCreator();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> creator.addPubinfoStatement(anyIri, anyIri));
        assertEquals("Nanopublication URI not yet set", ex.getMessage());
    }

    @Test
    void addPubinfoStatementsWithFinalized() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createFinalizedNanopubCreator();
        Statement st = vf.createStatement(creator.getNanopubUri(), anyIri, anyIri);

        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addPubinfoStatements(st));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addPubinfoStatements(List.of(st)));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addPubinfoStatement(st));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addPubinfoStatement(anyIri, anyIri));
    }

    @Test
    void addTimestampNow() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createNanopubCreator();
        creator.addTimestampNow();

        assertTrue(creator.finalizeNanopub().getPubinfo().stream()
                .anyMatch(st -> st.getPredicate().equals(DCTERMS.CREATED)));
    }

    @Test
    void addCreatorAndAuthorWithIri() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        IRI creatorIri = vf.createIRI("http://orcid.org/0000-0000-0000-0001");
        IRI authorIri = vf.createIRI("http://orcid.org/0000-0000-0000-0002");

        NanopubCreator creator = createNanopubCreator();
        creator.addCreator(creatorIri);
        creator.addAuthor(authorIri);
        Nanopub nanopub = creator.finalizeNanopub();

        assertEquals(Set.of(creatorIri), nanopub.getCreators());
        assertEquals(Set.of(authorIri), nanopub.getAuthors());
    }

    @Test
    void addCreatorAndAuthorWithFullOrcidUri() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createNanopubCreator();
        creator.addCreator("http://orcid.org/0000-0000-0000-0001");
        creator.addAuthor("http://orcid.org/0000-0000-0000-0002");
        Nanopub nanopub = creator.finalizeNanopub();

        assertEquals(Set.of(vf.createIRI("http://orcid.org/0000-0000-0000-0001")), nanopub.getCreators());
        assertEquals(Set.of(vf.createIRI("http://orcid.org/0000-0000-0000-0002")), nanopub.getAuthors());
    }

    @Test
    void addCreatorAndAuthorWithBareOrcidIdentifier() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createNanopubCreator();
        creator.addCreator("0000-0000-0000-0001");
        creator.addAuthor("0000-0000-0000-0002");
        Nanopub nanopub = creator.finalizeNanopub();

        assertEquals(Set.of(vf.createIRI("http://orcid.org/0000-0000-0000-0001")), nanopub.getCreators());
        assertEquals(Set.of(vf.createIRI("http://orcid.org/0000-0000-0000-0002")), nanopub.getAuthors());
    }

    @Test
    void addNamespaceInAllFlavours() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createNanopubCreator();
        creator.addNamespace("ex1", "https://example.org/ex1/");
        creator.addNamespace("ex2", vf.createIRI("https://example.org/ex2/"));
        creator.addNamespace(namespace("ex3", "https://example.org/ex3/"));
        creator.addNamespaces(namespace("ex4", "https://example.org/ex4/"),
                namespace("ex5", "https://example.org/ex5/"));
        creator.addNamespaces(List.of(namespace("ex6", "https://example.org/ex6/")));

        NanopubWithNs nanopub = (NanopubWithNs) creator.finalizeNanopub();

        for (int i = 1; i <= 6; i++) {
            assertEquals("https://example.org/ex" + i + "/", nanopub.getNamespace("ex" + i));
        }
    }

    @Test
    void addDefaultNamespaces() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createNanopubCreator();
        creator.addDefaultNamespaces();

        NanopubWithNs nanopub = (NanopubWithNs) creator.finalizeNanopub();

        assertEquals(TestUtils.NANOPUB_URI, nanopub.getNamespace("this"));
        for (Pair<String, String> p : NanopubUtils.getDefaultNamespaces()) {
            assertEquals(p.getRight(), nanopub.getNamespace(p.getLeft()));
        }
    }

    @Test
    void addNamespacesWithFinalized() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createFinalizedNanopubCreator();
        Namespace namespace = namespace("ex", "https://example.org/");

        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addNamespace("ex", "https://example.org/"));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addNamespaces(namespace));
        assertThrows(NanopubAlreadyFinalizedException.class, () -> creator.addNamespaces(List.of(namespace)));
    }

    @Test
    void finalizeNanopubRemovingUnusedPrefixes() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createNanopubCreator();
        creator.addDefaultNamespaces();
        creator.addNamespace("unused", "https://example.org/unused/");
        creator.setRemoveUnusedPrefixesEnabled(true);

        NanopubWithNs nanopub = (NanopubWithNs) creator.finalizeNanopub();

        assertFalse(nanopub.getNsPrefixes().contains("unused"));
        assertNull(nanopub.getNamespace("unused"));
    }

    @Test
    void finalizeNanopubKeepingUnusedPrefixes() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = createNanopubCreator();
        creator.addDefaultNamespaces();
        creator.addNamespace("unused", "https://example.org/unused/");
        creator.setRemoveUnusedPrefixesEnabled(false);

        NanopubWithNs nanopub = (NanopubWithNs) creator.finalizeNanopub();

        assertTrue(nanopub.getNsPrefixes().contains("unused"));
        assertEquals("https://example.org/unused/", nanopub.getNamespace("unused"));
    }

    @Test
    void finalizeTrustyNanopub() throws Exception {
        Nanopub nanopub = createNanopubCreator().finalizeTrustyNanopub();

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(nanopub));
    }

    @Test
    void finalizeTrustyNanopubWithTimestamp() throws Exception {
        Nanopub nanopub = createNanopubCreator().finalizeTrustyNanopub(true);

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(nanopub));
        assertTrue(nanopub.getPubinfo().stream().anyMatch(st -> st.getPredicate().equals(DCTERMS.CREATED)));
    }

}
