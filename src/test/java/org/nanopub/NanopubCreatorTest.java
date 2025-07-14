package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NanopubCreatorTest {

    private static final String TEST_NANOPUB_URI = "https://knowledgepixels.com/nanopubIri#test";
    private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();
    private static final IRI anyIri = vf.createIRI("https://knowledgepixels.com/nanopubIri#any");

    private NanopubCreator createNanopubCreator() {
        NanopubCreator creator = new NanopubCreator(vf.createIRI(TEST_NANOPUB_URI));
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
    void setNanopubUri() {
        // Test with String
        NanopubCreator creator = new NanopubCreator();
        Assertions.assertNull(creator.getNanopubUri());
        creator.setNanopubUri(TEST_NANOPUB_URI);
        Assertions.assertEquals(TEST_NANOPUB_URI, creator.getNanopubUri().toString());

        // Test with IRI
        String testIri = "https://knowledgepixels.com/nanopubIri#test2";
        creator.setNanopubUri(vf.createIRI(testIri));
        Assertions.assertEquals(testIri, creator.getNanopubUri().toString());
    }

    @Test
    void setNanopubUriWithFinalized() throws MalformedNanopubException {
        NanopubCreator creator = createNanopubCreator();
        creator.finalizeNanopub();
        assertThrows(RuntimeException.class, () -> creator.setNanopubUri(TEST_NANOPUB_URI));
    }

    @Test
    void finalizeOnFinalized() throws MalformedNanopubException {
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
    void finalizeNanopubWithoutTimestamp() throws MalformedNanopubException {
        NanopubCreator creator = createNanopubCreator();
        Nanopub nanopub = creator.finalizeNanopub();
        assertNotNull(nanopub);
        assertEquals(TEST_NANOPUB_URI, nanopub.getUri().toString());
        assertFalse(nanopub.getPubinfo().stream().anyMatch(st -> st.getPredicate().equals(SimpleTimestampPattern.DCT_CREATED)));
    }

    @Test
    void finalizeNanopubWithTimestamp() throws MalformedNanopubException {
        NanopubCreator creator = createNanopubCreator();
        Nanopub nanopub = creator.finalizeNanopub(true);
        assertNotNull(nanopub);
        assertEquals(TEST_NANOPUB_URI, nanopub.getUri().toString());
        assertTrue(nanopub.getPubinfo().stream().anyMatch(st -> st.getPredicate().equals(SimpleTimestampPattern.DCT_CREATED)));
    }

}