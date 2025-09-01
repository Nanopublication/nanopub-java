package org.nanopub.extra.setting;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.utils.TestUtils;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

}