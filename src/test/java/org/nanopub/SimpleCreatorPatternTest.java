package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.PAV;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

class SimpleCreatorPatternTest {

    private final SimpleCreatorPattern pattern = new SimpleCreatorPattern();

    private static final IRI ALICE = vf.createIRI("https://example.org/alice");
    private static final IRI BOB = vf.createIRI("https://example.org/bob");
    private static final IRI CAROL = vf.createIRI("https://example.org/carol");
    private static final IRI AUTHOR_LIST = vf.createIRI("https://example.org/authorList");

    private static NanopubCreator creator() throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, anyIri, anyIri);
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        return creator;
    }

    private static IRI rdfElement(int index) {
        return vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#_" + index);
    }

    @Test
    void getName() {
        assertEquals("Basic creator information", pattern.getName());
    }

    @Test
    void appliesToEveryNanopub() throws Exception {
        assertTrue(pattern.appliesTo(TestUtils.createNanopub()));
    }

    @Test
    void getPatternInfoUrl() throws Exception {
        assertEquals("https://github.com/Nanopublication/nanopub-java/blob/master/src/main/java/org/nanopub/SimpleCreatorPattern.java",
                pattern.getPatternInfoUrl().toString());
    }

    @Test
    void isCorrectlyUsedByANanopubWithACreator() throws Exception {
        NanopubCreator creator = creator();
        creator.addPubinfoStatement(DCTERMS.CREATOR, ALICE);

        assertTrue(pattern.isCorrectlyUsedBy(creator.finalizeNanopub()));
    }

    @Test
    void isCorrectlyUsedByANanopubWithOnlyAnAuthor() throws Exception {
        NanopubCreator creator = creator();
        creator.addPubinfoStatement(PAV.AUTHORED_BY, ALICE);

        assertTrue(pattern.isCorrectlyUsedBy(creator.finalizeNanopub()));
    }

    @Test
    void isNotCorrectlyUsedByANanopubWithoutEither() throws Exception {
        assertFalse(pattern.isCorrectlyUsedBy(TestUtils.createNanopub()));
    }

    @Test
    void getDescriptionForANanopubWithoutEither() throws Exception {
        assertEquals("No authors or creators found", pattern.getDescriptionFor(TestUtils.createNanopub()));
    }

    @Test
    void getDescriptionForASingleAuthorAndCreator() throws Exception {
        NanopubCreator creator = creator();
        creator.addPubinfoStatement(PAV.AUTHORED_BY, ALICE);
        creator.addPubinfoStatement(DCTERMS.CREATOR, BOB);

        assertEquals("1 author; 1 creator", pattern.getDescriptionFor(creator.finalizeNanopub()));
    }

    @Test
    void getDescriptionForACreatorWithoutAnAuthor() throws Exception {
        NanopubCreator creator = creator();
        creator.addPubinfoStatement(DCTERMS.CREATOR, ALICE);

        assertEquals("0 authors; 1 creator", pattern.getDescriptionFor(creator.finalizeNanopub()));
    }

    @Test
    void getDescriptionForSeveralAuthorsAndCreators() throws Exception {
        NanopubCreator creator = creator();
        creator.addPubinfoStatement(PAV.AUTHORED_BY, ALICE);
        creator.addPubinfoStatement(PAV.AUTHORED_BY, BOB);
        creator.addPubinfoStatement(DCTERMS.CREATOR, ALICE);
        creator.addPubinfoStatement(DCTERMS.CREATOR, BOB);

        assertEquals("2 creator", pattern.getDescriptionFor(creator.finalizeNanopub()));
    }

    @Test
    void getCreatorsAcceptsEveryCreatorProperty() throws Exception {
        NanopubCreator creator = creator();
        creator.addPubinfoStatement(PAV.CREATED_BY, ALICE);
        creator.addPubinfoStatement(SimpleCreatorPattern.PAV_CREATEDBY_1, BOB);
        creator.addPubinfoStatement(PAV.CREATED_BY_V2, CAROL);
        creator.addPubinfoStatement(DCTERMS.CREATOR, vf.createIRI("https://example.org/dave"));
        creator.addPubinfoStatement(DC.CREATOR, vf.createIRI("https://example.org/erin"));
        creator.addPubinfoStatement(PROV.WAS_ATTRIBUTED_TO, vf.createIRI("https://example.org/frank"));

        assertEquals(6, SimpleCreatorPattern.getCreators(creator.finalizeNanopub()).size());
    }

    @Test
    void getCreatorsIgnoresStatementsThatDoNotQualify() throws Exception {
        NanopubCreator creator = creator();
        // not about the nanopub itself
        creator.addPubinfoStatement(vf.createStatement(anyIri, DCTERMS.CREATOR, ALICE));
        // not a creator property
        creator.addPubinfoStatement(RDFS.LABEL, BOB);
        // not an IRI
        creator.addPubinfoStatement(DCTERMS.CREATOR, vf.createLiteral("Carol"));

        assertTrue(SimpleCreatorPattern.getCreators(creator.finalizeNanopub()).isEmpty());
    }

    @Test
    void getAuthorsAcceptsEveryAuthorProperty() throws Exception {
        NanopubCreator creator = creator();
        creator.addPubinfoStatement(PAV.AUTHORED_BY, ALICE);
        creator.addPubinfoStatement(SimpleCreatorPattern.PAV_AUTHOREDBY_1, BOB);
        creator.addPubinfoStatement(PAV.AUTHORED_BY_V2, CAROL);

        assertEquals(Set.of(ALICE, BOB, CAROL), SimpleCreatorPattern.getAuthors(creator.finalizeNanopub()));
    }

    @Test
    void getAuthorsIgnoresStatementsThatDoNotQualify() throws Exception {
        NanopubCreator creator = creator();
        // not about the nanopub itself
        creator.addPubinfoStatement(vf.createStatement(anyIri, PAV.AUTHORED_BY, ALICE));
        // not an author property
        creator.addPubinfoStatement(RDFS.LABEL, BOB);
        // not an IRI
        creator.addPubinfoStatement(PAV.AUTHORED_BY, vf.createLiteral("Carol"));
        // an author list that is not an IRI either
        creator.addPubinfoStatement(SimpleCreatorPattern.BIBO_AUTHOR_LIST, vf.createLiteral("not a list"));

        assertTrue(SimpleCreatorPattern.getAuthors(creator.finalizeNanopub()).isEmpty());
    }

    @Test
    void getAuthorsReadsTheAuthorList() throws Exception {
        NanopubCreator creator = creator();
        creator.addPubinfoStatement(SimpleCreatorPattern.BIBO_AUTHOR_LIST, AUTHOR_LIST);
        creator.addPubinfoStatement(vf.createStatement(AUTHOR_LIST, rdfElement(1), ALICE));
        creator.addPubinfoStatement(vf.createStatement(AUTHOR_LIST, rdfElement(2), BOB));
        // entries that are skipped: wrong subject, wrong predicate, non-IRI object
        creator.addPubinfoStatement(vf.createStatement(anyIri, rdfElement(3), CAROL));
        creator.addPubinfoStatement(vf.createStatement(AUTHOR_LIST, RDFS.LABEL, CAROL));
        creator.addPubinfoStatement(vf.createStatement(AUTHOR_LIST, rdfElement(4), vf.createLiteral("Carol")));

        assertEquals(Set.of(ALICE, BOB), SimpleCreatorPattern.getAuthors(creator.finalizeNanopub()));
    }

    @Test
    void getAuthorListKeepsTheDeclaredOrder() throws Exception {
        NanopubCreator creator = creator();
        creator.addPubinfoStatement(SimpleCreatorPattern.BIBO_AUTHOR_LIST, AUTHOR_LIST);
        creator.addPubinfoStatement(vf.createStatement(AUTHOR_LIST, rdfElement(2), BOB));
        creator.addPubinfoStatement(vf.createStatement(AUTHOR_LIST, rdfElement(1), ALICE));

        assertEquals(List.of(ALICE, BOB), SimpleCreatorPattern.getAuthorList(creator.finalizeNanopub()));
    }

    @Test
    void getAuthorListAppendsAuthorsThatAreNotInTheList() throws Exception {
        NanopubCreator creator = creator();
        creator.addPubinfoStatement(SimpleCreatorPattern.BIBO_AUTHOR_LIST, AUTHOR_LIST);
        creator.addPubinfoStatement(vf.createStatement(AUTHOR_LIST, rdfElement(1), ALICE));
        // Alice is in the list already, Carol is not
        creator.addPubinfoStatement(PAV.AUTHORED_BY, ALICE);
        creator.addPubinfoStatement(PAV.AUTHORED_BY, CAROL);
        // entries that are skipped
        creator.addPubinfoStatement(vf.createStatement(anyIri, rdfElement(2), BOB));
        creator.addPubinfoStatement(vf.createStatement(AUTHOR_LIST, RDFS.LABEL, BOB));
        creator.addPubinfoStatement(vf.createStatement(AUTHOR_LIST, rdfElement(3), vf.createLiteral("Bob")));
        // an author and an author list that are not IRIs
        creator.addPubinfoStatement(PAV.AUTHORED_BY, vf.createLiteral("Bob"));
        creator.addPubinfoStatement(SimpleCreatorPattern.BIBO_AUTHOR_LIST, vf.createLiteral("not a list"));

        assertEquals(List.of(ALICE, CAROL), SimpleCreatorPattern.getAuthorList(creator.finalizeNanopub()));
    }

    @Test
    void getAuthorListWithoutAnAuthorListIsEmpty() throws Exception {
        assertTrue(SimpleCreatorPattern.getAuthorList(TestUtils.createNanopub()).isEmpty());
    }

    @Test
    void isCreatorProperty() {
        assertTrue(SimpleCreatorPattern.isCreatorProperty(PAV.CREATED_BY));
        assertTrue(SimpleCreatorPattern.isCreatorProperty(SimpleCreatorPattern.PAV_CREATEDBY_1));
        assertTrue(SimpleCreatorPattern.isCreatorProperty(PAV.CREATED_BY_V2));
        assertTrue(SimpleCreatorPattern.isCreatorProperty(DCTERMS.CREATOR));
        assertTrue(SimpleCreatorPattern.isCreatorProperty(DC.CREATOR));
        assertTrue(SimpleCreatorPattern.isCreatorProperty(PROV.WAS_ATTRIBUTED_TO));
        assertFalse(SimpleCreatorPattern.isCreatorProperty(RDFS.LABEL));
    }

    @Test
    void isAuthorProperty() {
        assertTrue(SimpleCreatorPattern.isAuthorProperty(PAV.AUTHORED_BY));
        assertTrue(SimpleCreatorPattern.isAuthorProperty(SimpleCreatorPattern.PAV_AUTHOREDBY_1));
        assertTrue(SimpleCreatorPattern.isAuthorProperty(PAV.AUTHORED_BY_V2));
        assertFalse(SimpleCreatorPattern.isAuthorProperty(RDFS.LABEL));
    }

}
