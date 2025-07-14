package org.nanopub.extra.index;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;

class IndexUtilsTest {

    private static final String TEST_NANOPUB_URI = "https://knowledgepixels.com/nanopubIri#test";
    private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();
    private static final IRI anyIri = vf.createIRI("https://knowledgepixels.com/nanopubIri#any");

    @Test
    void isIndex() throws MalformedNanopubException {
        NanopubCreator creator1 = new NanopubCreator(vf.createIRI(TEST_NANOPUB_URI + "1"));
        Statement assertionStatement = vf.createStatement(anyIri, anyIri, anyIri);
        creator1.addAssertionStatements(assertionStatement);
        creator1.addProvenanceStatements(vf.createStatement(creator1.getAssertionUri(), anyIri, anyIri));
        creator1.addPubinfoStatements(vf.createStatement(creator1.getNanopubUri(), anyIri, anyIri));

        NanopubCreator creator2 = new NanopubCreator(vf.createIRI(TEST_NANOPUB_URI + "2"));
        creator2.addAssertionStatements(vf.createStatement(anyIri, anyIri, anyIri));
        creator2.addProvenanceStatements(vf.createStatement(creator2.getAssertionUri(), anyIri, anyIri));
        creator2.addPubinfoStatements(vf.createStatement(creator2.getNanopubUri(), RDF.TYPE, NanopubIndex.NANOPUB_INDEX_URI));

        Nanopub nanopub1 = creator1.finalizeNanopub(true);
        Nanopub nanopub2 = creator2.finalizeNanopub(true);

        Assertions.assertFalse(IndexUtils.isIndex(nanopub1));
        Assertions.assertTrue(IndexUtils.isIndex(nanopub2));
    }

}