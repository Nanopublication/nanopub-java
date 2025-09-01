package org.nanopub;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nanopub.utils.TestUtils;

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

}