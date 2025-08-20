package org.nanopub.extra.index;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPX;

import static org.eclipse.rdf4j.model.util.Values.triple;
import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.utils.TestUtils.anyIri;

class IndexUtilsTest {

    @Test
    void isIndex() throws MalformedNanopubException {
        NanopubCreator creator1 = new NanopubCreator(Values.iri(TestUtils.NANOPUB_URI + "1"));
        Statement assertionStatement = Statements.statement(triple(anyIri, anyIri, anyIri));
        creator1.addAssertionStatement(assertionStatement);
        creator1.addProvenanceStatement(anyIri, anyIri);
        creator1.addPubinfoStatement(RDF.TYPE, anyIri);

        NanopubCreator creator2 = new NanopubCreator(Values.iri(TestUtils.NANOPUB_URI + "2"));
        creator2.addAssertionStatement(Statements.statement(triple(anyIri, anyIri, anyIri)));
        creator2.addProvenanceStatement(anyIri, anyIri);
        creator2.addPubinfoStatement(Statements.statement(triple(Values.iri(anyIri.stringValue() + "/another"), anyIri, anyIri)));
        creator2.addPubinfoStatement(RDF.TYPE, NPX.NANOPUB_INDEX);

        Nanopub nanopub1 = creator1.finalizeNanopub(true);
        Nanopub nanopub2 = creator2.finalizeNanopub(true);

        assertFalse(IndexUtils.isIndex(nanopub1));
        assertTrue(IndexUtils.isIndex(nanopub2));
    }

    @Test
    void castToIndexWithNanopubIndex() throws MalformedNanopubException {
        NanopubCreator creator2 = new NanopubCreator(Values.iri(TestUtils.NANOPUB_URI + "2"));
        creator2.addAssertionStatement(Statements.statement(triple(anyIri, anyIri, anyIri)));
        creator2.addProvenanceStatement(anyIri, anyIri);
        creator2.addPubinfoStatement(RDF.TYPE, NPX.NANOPUB_INDEX);

        Nanopub nanopub2 = creator2.finalizeNanopub(true);
        NanopubIndex index = new NanopubIndexImpl(nanopub2);

        assertEquals(IndexUtils.castToIndex(index), index);
    }

    @Test
    void castToIndexWithNanopub() throws MalformedNanopubException {
        NanopubCreator creator2 = new NanopubCreator(Values.iri(TestUtils.NANOPUB_URI + "2"));
        creator2.addAssertionStatement(Statements.statement(triple(anyIri, anyIri, anyIri)));
        creator2.addProvenanceStatement(anyIri, anyIri);
        creator2.addPubinfoStatement(RDF.TYPE, NPX.NANOPUB_INDEX);

        Nanopub nanopub2 = creator2.finalizeNanopub(true);
        assertInstanceOf(NanopubIndexImpl.class, IndexUtils.castToIndex(nanopub2));
    }

}