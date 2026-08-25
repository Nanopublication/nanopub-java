package org.nanopub;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import static org.eclipse.rdf4j.model.util.Values.literal;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.nanopub.trusty.TempUriReplacer;
import org.nanopub.trusty.TrustyNanopubUtils;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.KPXL_GRLC;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;
import org.nanopub.vocabulary.NPX;

import net.trustyuri.TrustyUriUtils;

public class NanopubUtilsTest {

    @Test
    void getDefaultNamespaces() {
        assertFalse(NanopubUtils.getDefaultNamespaces().isEmpty());
    }

    @Test
    void getStatementsMinimal() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(vf.createIRI(TestUtils.NANOPUB_URI));

        // Create valid nanopub
        Statement assertionStatement = vf.createStatement(anyIri, anyIri, anyIri);
        creator.addAssertionStatements(assertionStatement);

        Statement provenanceStatement = vf.createStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addProvenanceStatements(provenanceStatement);

        Statement pubinfoStatement = vf.createStatement(creator.getNanopubUri(), anyIri, anyIri);
        creator.addPubinfoStatements(pubinfoStatement);

        Nanopub nanopub = creator.finalizeNanopub();

        // Cannot use equals because the statement in the nanopub has a different context, therefore, the test would fail
        assertTrue(NanopubUtils.getStatements(nanopub).stream()
                .anyMatch(st -> st.getSubject().equals(provenanceStatement.getSubject())
                && st.getPredicate().equals(provenanceStatement.getPredicate())
                && st.getObject().equals(provenanceStatement.getObject())));

        assertTrue(NanopubUtils.getStatements(nanopub).stream()
                .anyMatch(st -> st.getSubject().equals(assertionStatement.getSubject())
                && st.getPredicate().equals(assertionStatement.getPredicate())
                && st.getObject().equals(assertionStatement.getObject())));

        assertTrue(NanopubUtils.getStatements(nanopub).stream()
                .anyMatch(st -> st.getSubject().equals(pubinfoStatement.getSubject())
                && st.getPredicate().equals(pubinfoStatement.getPredicate())
                && st.getObject().equals(pubinfoStatement.getObject())));

        // there are 4 header statements, we do not check them here
        assertEquals(7, NanopubUtils.getStatements(nanopub).size());
    }

    @Test
    void getIllTypedLiteralStatementsFindsThemInEveryGraph() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, anyIri, vf.createLiteral("two", XSD.INTEGER));
        creator.addProvenanceStatement(creator.getAssertionUri(), anyIri, vf.createLiteral("of course", XSD.BOOLEAN));
        creator.addPubinfoStatement(anyIri, vf.createLiteral("2019-02-26", XSD.DATETIME));
        Nanopub nanopub = creator.finalizeNanopub();

        List<Statement> illTyped = NanopubUtils.getIllTypedLiteralStatements(nanopub);

        assertEquals(3, illTyped.size());
        assertTrue(NanopubUtils.describeIllTypedLiteral(illTyped.getFirst()).startsWith("Invalid value for datatype "));
    }

    @Test
    void getIllTypedLiteralStatementsIgnoresNonSchemaDatatypes() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, anyIri, vf.createLiteral("42", XSD.INTEGER));
        creator.addAssertionStatement(anyIri, anyIri, vf.createLiteral("plain string"));
        // not an XML Schema datatype, so its lexical space is unknown to us and not checked
        creator.addAssertionStatement(anyIri, anyIri, vf.createLiteral("anything", vf.createIRI("https://example.org/myDatatype")));
        creator.addProvenanceStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);

        assertTrue(NanopubUtils.getIllTypedLiteralStatements(creator.finalizeNanopub()).isEmpty());
    }

    @Test
    void getInvalidSparqlStatementsFindsOnlyTheBrokenQueries() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, KPXL_GRLC.SPARQL, vf.createLiteral("SELECT ?x WHERE { ?x ?y ?z }"));
        creator.addAssertionStatement(anyIri, KPXL_GRLC.SPARQL, vf.createLiteral("SELECT ?x WHERE { ?x ?y }"));
        creator.addProvenanceStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        Nanopub nanopub = creator.finalizeNanopub();

        List<Statement> invalid = NanopubUtils.getInvalidSparqlStatements(nanopub);

        assertEquals(1, invalid.size());
        assertTrue(NanopubUtils.describeInvalidSparql(invalid.getFirst())
                .startsWith("Invalid SPARQL as object of " + KPXL_GRLC.SPARQL.stringValue()));
    }

    @Test
    void getInvalidSparqlStatementsIgnoresOtherPredicatesAndNonLiterals() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        // the same broken query text under a predicate that does not declare it as SPARQL
        creator.addAssertionStatement(anyIri, anyIri, vf.createLiteral("SELECT ?x WHERE { ?x ?y }"));
        creator.addAssertionStatement(anyIri, KPXL_GRLC.SPARQL, anyIri);
        creator.addProvenanceStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);

        assertTrue(NanopubUtils.getInvalidSparqlStatements(creator.finalizeNanopub()).isEmpty());
    }

    @Test
    void writeToStream() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        RDFFormat format = RDFFormat.TURTLE; // TODO TrustyNanopubUtils.STNP_FORMAT
        NanopubUtils.writeToStream(nanopub, os, format);

        String output = os.toString();
        assertTrue(output.contains(nanopub.getUri().toString()));
    }

    @Test
    void testEquality() throws Exception {
        Nanopub np1 = TestUtils.createNanopub();
        Nanopub np2 = TestUtils.createNanopub();
        assertEquals(np1, np2);
    }

    @Test
    void writeToString() throws MalformedNanopubException, IOException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();

        RDFFormat format = RDFFormat.JSONLD; // TODO NullPointerException with TrustyNanopubUtils.STNP_FORMAT
        String output = NanopubUtils.writeToString(nanopub, format);

        assertTrue(output.contains(nanopub.getUri().toString()));
    }

    @Test
    void getLabelWithoutLabelAssertionReturnsNull() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(vf.createIRI(TestUtils.NANOPUB_URI));

        // Create nanopub with Label
        creator.addAssertionStatement(
                vf.createStatement(
                        vf.createIRI("https://knowledgepixels.com/nanopubIri#titleassertion"),
                        anyIri,
                        anyIri)
        );

        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        Nanopub nanopub = creator.finalizeNanopub();

        String retrievedLabel = NanopubUtils.getLabel(nanopub);
        assertNull(retrievedLabel);
    }

    @Test
    void getLabelWithIntroLabel() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String label = "My Label";
        String introducedObject = "https://knowledgepixels.com/nanopubIri#introducedObject";
        NanopubCreator creator = new NanopubCreator(vf.createIRI(TestUtils.NANOPUB_URI));

        // Create nanopub with Label
        creator.addAssertionStatement(
                vf.createStatement(
                        vf.createIRI(introducedObject),
                        RDFS.LABEL,
                        vf.createLiteral(label)
                )
        );

        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(NPX.INTRODUCES, vf.createIRI(introducedObject));
        Nanopub nanopub = creator.finalizeNanopub();

        String retrievedLabel = NanopubUtils.getLabel(nanopub);
        assertEquals(label, retrievedLabel);
    }

    @Test
    void getLabelWithLabelInAssertionWithRDFS() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String label = "My Label";
        NanopubCreator creator = new NanopubCreator(vf.createIRI(TestUtils.NANOPUB_URI));

        // Create nanopub with Label
        creator.addAssertionStatement(
                vf.createStatement(
                        vf.createIRI("https://knowledgepixels.com/nanopubIri#titleassertion"),
                        RDFS.LABEL,
                        vf.createLiteral(label))
        );

        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        Nanopub nanopub = creator.finalizeNanopub();

        String retrievedLabel = NanopubUtils.getLabel(nanopub);
        assertEquals(label, retrievedLabel);
    }

    @Test
    void getLabelWithLabelInAssertionWithDC() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String label = "My Label";
        NanopubCreator creator = new NanopubCreator(vf.createIRI(TestUtils.NANOPUB_URI));

        creator.addAssertionStatements(
                vf.createStatement(
                        vf.createIRI("https://knowledgepixels.com/nanopubIri#titleassertion"),
                        DCTERMS.TITLE,
                        vf.createLiteral(label)),
                vf.createStatement(
                        vf.createIRI("https://knowledgepixels.com/nanopubIri#titleassertion"),
                        DC.TITLE,
                        vf.createLiteral(label))
        );

        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        Nanopub nanopub = creator.finalizeNanopub();

        String retrievedLabel = NanopubUtils.getLabel(nanopub);
        assertEquals(label + " " + label, retrievedLabel);
    }

    @Test
    void getLabelWithLabelInProvenanceWithRDFS() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String label = "My Label";
        NanopubCreator creator = new NanopubCreator(vf.createIRI(TestUtils.NANOPUB_URI));

        // Create nanopub with Label
        creator.addAssertionStatement(
                vf.createStatement(
                        anyIri, anyIri, anyIri
                )
        );

        creator.addProvenanceStatement(RDFS.LABEL, vf.createLiteral(label));
        creator.addPubinfoStatement(anyIri, anyIri);

        Nanopub nanopub = creator.finalizeNanopub();

        String retrievedLabel = NanopubUtils.getLabel(nanopub);
        assertEquals(label, retrievedLabel);
    }

    @Test
    void getLabelWithLabelInProvenanceWithDC() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String label = "My Label";
        NanopubCreator creator = new NanopubCreator(vf.createIRI(TestUtils.NANOPUB_URI));

        // Create nanopub with Label
        creator.addAssertionStatement(
                vf.createStatement(
                        anyIri, anyIri, anyIri
                )
        );

        creator.addProvenanceStatement(DCTERMS.TITLE, vf.createLiteral(label));
        creator.addPubinfoStatement(anyIri, anyIri);

        Nanopub nanopub = creator.finalizeNanopub();

        String retrievedLabel = NanopubUtils.getLabel(nanopub);
        assertEquals(label, retrievedLabel);
    }

    @Test
    void getLabelWithLabelInPubInfoWithRDFS() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String label = "My Label";
        NanopubCreator creator = new NanopubCreator(vf.createIRI(TestUtils.NANOPUB_URI));

        // Create nanopub with Label
        creator.addAssertionStatement(
                vf.createStatement(
                        anyIri, anyIri, anyIri
                )
        );

        creator.addProvenanceStatement(anyIri, anyIri);

        creator.addPubinfoStatement(RDFS.LABEL, vf.createLiteral(label));

        Nanopub nanopub = creator.finalizeNanopub();

        String retrievedLabel = NanopubUtils.getLabel(nanopub);
        assertEquals(label, retrievedLabel);
    }

    @Test
    void getLabelWithLabelInPubInfoWithDC() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String label = "My Label";
        NanopubCreator creator = new NanopubCreator(vf.createIRI(TestUtils.NANOPUB_URI));

        // Create nanopub with Label
        creator.addAssertionStatement(
                vf.createStatement(
                        anyIri, anyIri, anyIri
                )
        );

        creator.addProvenanceStatement(anyIri, anyIri);

        creator.addPubinfoStatement(DCTERMS.TITLE, vf.createLiteral(label));
        creator.addPubinfoStatement(DC.TITLE, vf.createLiteral(label));

        Nanopub nanopub = creator.finalizeNanopub();

        String retrievedLabel = NanopubUtils.getLabel(nanopub);
        assertEquals(label + " " + label, retrievedLabel);
    }

    @Test
    void getDescription() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(vf.createIRI(TestUtils.NANOPUB_URI));

        String description = "My Description";

        // Create nanopub with Description
        Statement assertionStatement = vf.createStatement(vf.createIRI("https://knowledgepixels.com/nanopubIri#titleassertion"), DCTERMS.DESCRIPTION, literal(description));
        creator.addAssertionStatements(assertionStatement);

        Statement provenanceStatement = vf.createStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addProvenanceStatements(provenanceStatement);

        Statement pubinfoStatement = vf.createStatement(creator.getNanopubUri(), anyIri, anyIri);
        creator.addPubinfoStatements(pubinfoStatement);
        Nanopub nanopub = creator.finalizeNanopub();

        String retrievedDescription = NanopubUtils.getDescription(nanopub);
        assertEquals(description, retrievedDescription);
    }

    @Test
    void getTypes() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        Set<IRI> types = NanopubUtils.getTypes(nanopub);
        // This is an extremely minimal test, some more assertions were nice
        assertTrue(types.contains(anyIri));
    }

    @Test
    void updateXorChecksum() {
        String anyChecksum = "This is any checksum with a length more than 32 characters for testing";
        IRI anyIri = vf.createIRI("http://www.tkuhn.org/pub/sempub/sempub.trig#np2.RA8tL7TWDOtL6oz3dhhYZ6JIBB9YlroOFIMKcQk7nFEr8");

        String updatedChecksum = NanopubUtils.updateXorChecksum(anyIri, anyChecksum);

        String res = "vMpXx6ZpfXb2vTxPHo7Xotfmd1ENAlbltQ7nSnGfvxgtersfortestin";
        //assertNotEquals(res, updatedChecksum);

        /*IRI nanopubId = vf.createIRI("http://example.org/nanopub#artifactCode");
        String initialChecksum = TrustyUriUtils.getBase64(new byte[32]);
        assertNotNull(updatedChecksum);
        assertNotEquals(initialChecksum, updatedChecksum);*/
    }

    @Test
    void updateXorChecksumThrowsExceptionForNullNanopubId() {
        String initialChecksum = TrustyUriUtils.getBase64(new byte[32]);
        assertThrows(NullPointerException.class, () -> NanopubUtils.updateXorChecksum(null, initialChecksum));
    }

    @Test
    void updateXorChecksumThrowsExceptionForNullChecksum() {
        IRI nanopubId = vf.createIRI("http://www.tkuhn.org/pub/sempub/sempub.trig#np2.RA8tL7TWDOtL6oz3dhhYZ6JIBB9YlroOFIMKcQk7nFEr8");
        assertThrows(NullPointerException.class, () -> NanopubUtils.updateXorChecksum(nanopubId, null));
    }

    @Test
    void updateXorChecksumThrowsExceptionForInvalidChecksumLength() {
        IRI nanopubId = vf.createIRI("http://www.tkuhn.org/pub/sempub/sempub.trig#np2.RA8tL7TWDOtL6oz3dhhYZ6JIBB9YlroOFIMKcQk7nFEr8");
        String invalidChecksum = "shortChecksum";
        assertThrows(IllegalArgumentException.class, () -> NanopubUtils.updateXorChecksum(nanopubId, invalidChecksum));
    }

    @Test
    void getHttpClient() {
        CloseableHttpClient client = NanopubUtils.getHttpClient();
        assertNotNull(client);

        // We do not care if it's the same client, but it must be there
        client = NanopubUtils.getHttpClient();
        assertNotNull(client);
    }

    @Test
    void createTempNanopubIri() {
        IRI tempNanopubIri = NanopubUtils.createTempNanopubIri();
        assertTrue(tempNanopubIri.stringValue().startsWith(TempUriReplacer.tempUri));

        IRI tempNanopubIri2 = NanopubUtils.createTempNanopubIri();
        assertNotEquals(tempNanopubIri, tempNanopubIri2);
    }

    @Test
    void getParserReturnsNonNullParserForValidFormat() {
        RDFParser parser = NanopubUtils.getParser(RDFFormat.TURTLE);
        assertNotNull(parser);
    }

    @Test
    void getParserThrowsExceptionForNullFormat() {
        assertThrows(NullPointerException.class, () -> NanopubUtils.getParser(null));
    }

    @Test
    void getParserConfiguresNamespacesSetting() {
        RDFParser parser = NanopubUtils.getParser(RDFFormat.JSONLD);
        assertNotNull(parser.getParserConfig().get(BasicParserSettings.NAMESPACES));
        assertInstanceOf(Set.class, parser.getParserConfig().get(BasicParserSettings.NAMESPACES));
    }

    @Test
    void propagateToHandlerHandlesNamespacesForNanopubWithNs() {
        NanopubWithNs nanopub = mock(NanopubWithNs.class);
        RDFHandler handler = mock(RDFHandler.class);

        when(nanopub.getNsPrefixes()).thenReturn(List.of("ex"));
        when(nanopub.getNamespace("ex")).thenReturn("https://example.org/");

        NanopubUtils.propagateToHandler(nanopub, handler);

        verify(handler).startRDF();
        verify(handler).handleNamespace("ex", "https://example.org/");
        verify(handler).endRDF();
    }

    @Test
    void propagateToHandlerHandlesDefaultNamespacesForNanopubWithoutNs() {
        NanopubWithNs nanopub = mock(NanopubWithNs.class);
        RDFHandler handler = mock(RDFHandler.class);

        when(nanopub.getNsPrefixes()).thenReturn(List.of());
        when(nanopub.getUri()).thenReturn(TestUtils.anyIri);

        NanopubUtils.propagateToHandler(nanopub, handler);

        verify(handler).startRDF();
        verify(handler).handleNamespace("this", nanopub.getUri().toString());
        for (Pair<String, String> nsEntry : NanopubUtils.getDefaultNamespaces()) {
            verify(handler).handleNamespace(nsEntry.getLeft(), nsEntry.getRight());
        }
        verify(handler).endRDF();
    }

    @Test
    void propagateToHandlerHandlesDefaultNamespacesForNanopubWithoutNamespaceSupport() {
        // a plain Nanopub (not a NanopubWithNs) always gets the default namespaces
        Nanopub nanopub = mock(Nanopub.class);
        RDFHandler handler = mock(RDFHandler.class);

        when(nanopub.getUri()).thenReturn(TestUtils.anyIri);

        NanopubUtils.propagateToHandler(nanopub, handler);

        verify(handler).startRDF();
        verify(handler).handleNamespace("this", TestUtils.anyIri.toString());
        for (Pair<String, String> nsEntry : NanopubUtils.getDefaultNamespaces()) {
            verify(handler).handleNamespace(nsEntry.getLeft(), nsEntry.getRight());
        }
        verify(handler).endRDF();
    }

    @Test
    void writeToStringInTrustyDigestFormat() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, anyIri, anyIri);
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        Nanopub nanopub = creator.finalizeTrustyNanopub();

        String output = NanopubUtils.writeToString(nanopub, TrustyNanopubUtils.STNP_FORMAT);

        assertEquals(TrustyNanopubUtils.getTrustyDigestString(nanopub), output);
    }

    @Test
    void writeToStreamWrapsIoErrors() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, anyIri, anyIri);
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        Nanopub nanopub = creator.finalizeTrustyNanopub();

        OutputStream failing = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("nowhere to write to");
            }
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> NanopubUtils.writeToStream(nanopub, failing, TrustyNanopubUtils.STNP_FORMAT));
        assertInstanceOf(IOException.class, ex.getCause());
    }

    @Test
    void getUsedPrefixesCollectsThePrefixesOfTheNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, RDF.TYPE, anyIri);
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        creator.addDefaultNamespaces();
        creator.addNamespace("unused", "https://example.org/unused/");

        Set<String> usedPrefixes = NanopubUtils.getUsedPrefixes((NanopubWithNs) creator.finalizeNanopub());

        // the head graph refers to np:hasAssertion and friends
        assertTrue(usedPrefixes.contains("np"));
        assertFalse(usedPrefixes.contains("unused"));
    }

    @Test
    void getUsedPrefixesReturnsWhatItHasWhenWritingFails() {
        NanopubWithNs nanopub = mock(NanopubWithNs.class);
        when(nanopub.getNsPrefixes()).thenReturn(List.of("ex"));
        when(nanopub.getNamespace("ex")).thenReturn("https://example.org/");
        when(nanopub.getHead()).thenThrow(new RDFHandlerException("cannot write this nanopub"));

        assertNotNull(NanopubUtils.getUsedPrefixes(nanopub));
    }

    /**
     * Builds a nanopub in which every label-, description- and type-carrying
     * predicate is present, both with the object types that are picked up and
     * with the ones that are ignored.
     */
    private Nanopub createRichNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        IRI npId = creator.getNanopubUri();
        IRI aId = creator.getAssertionUri();
        IRI introduced = vf.createIRI("https://example.org/introduced");
        IRI described = vf.createIRI("https://example.org/described");
        IRI embedded = vf.createIRI("https://example.org/embedded");
        IRI other = vf.createIRI("https://example.org/other");
        IRI aType = vf.createIRI("https://example.org/AssertionType");

        // Pubinfo: the introduced/described/embedded IRIs, plus statements that must be ignored
        creator.addPubinfoStatement(NPX.INTRODUCES, introduced);
        creator.addPubinfoStatement(NPX.DESCRIBES, described);
        creator.addPubinfoStatement(NPX.EMBEDS, embedded);
        creator.addPubinfoStatement(NPX.INTRODUCES, literal("not an IRI"));
        creator.addPubinfoStatement(NPX.EMBEDS, literal("not an IRI"));
        creator.addPubinfoStatement(vf.createStatement(other, NPX.INTRODUCES, introduced));
        creator.addPubinfoStatement(anyIri, anyIri);
        // Pubinfo: labels, titles, descriptions and types of the nanopub itself
        creator.addPubinfoStatement(RDFS.LABEL, literal("np label"));
        creator.addPubinfoStatement(DCTERMS.TITLE, literal("np title"));
        creator.addPubinfoStatement(DC.TITLE, literal("np dc title"));
        creator.addPubinfoStatement(DCTERMS.DESCRIPTION, literal("np description"));
        creator.addPubinfoStatement(DC.DESCRIPTION, literal("np dc description"));
        creator.addPubinfoStatement(RDFS.COMMENT, literal("np comment"));
        creator.addPubinfoStatement(SKOS.DEFINITION, literal("np definition"));
        creator.addPubinfoStatement(RDF.TYPE, vf.createIRI("https://example.org/NpType"));
        creator.addPubinfoStatement(NPX.HAS_NANOPUB_TYPE, vf.createIRI("https://example.org/NpType2"));
        // Pubinfo: the same predicates with an object type that is not picked up
        creator.addPubinfoStatement(RDFS.LABEL, other);
        creator.addPubinfoStatement(DCTERMS.TITLE, other);
        creator.addPubinfoStatement(RDF.TYPE, literal("not an IRI"));
        creator.addPubinfoStatement(NPX.HAS_NANOPUB_TYPE, literal("not an IRI"));

        // Provenance: labels, titles, descriptions and types of the assertion
        creator.addProvenanceStatement(RDFS.LABEL, literal("a prov label"));
        creator.addProvenanceStatement(DCTERMS.TITLE, literal("a prov title"));
        creator.addProvenanceStatement(DCTERMS.DESCRIPTION, literal("a prov description"));
        creator.addProvenanceStatement(DC.DESCRIPTION, literal("a prov dc description"));
        creator.addProvenanceStatement(RDFS.COMMENT, literal("a prov comment"));
        creator.addProvenanceStatement(SKOS.DEFINITION, literal("a prov definition"));
        creator.addProvenanceStatement(RDF.TYPE, aType);
        // Provenance: statements that must be ignored
        creator.addProvenanceStatement(RDFS.LABEL, other);
        creator.addProvenanceStatement(DCTERMS.TITLE, other);
        creator.addProvenanceStatement(RDF.TYPE, literal("not an IRI"));
        creator.addProvenanceStatement(vf.createStatement(other, DCTERMS.DESCRIPTION, literal("ignored")));
        creator.addProvenanceStatement(vf.createStatement(other, RDF.TYPE, aType));

        // Assertion: labels, titles and descriptions of the assertion itself
        creator.addAssertionStatement(aId, RDFS.LABEL, literal("a label"));
        creator.addAssertionStatement(aId, DCTERMS.TITLE, literal("a title"));
        creator.addAssertionStatement(aId, DC.TITLE, literal("a dc title"));
        creator.addAssertionStatement(aId, DCTERMS.DESCRIPTION, literal("a description"));
        creator.addAssertionStatement(aId, DC.DESCRIPTION, literal("a dc description"));
        creator.addAssertionStatement(aId, RDFS.COMMENT, literal("a comment"));
        creator.addAssertionStatement(aId, SKOS.DEFINITION, literal("a definition"));
        creator.addAssertionStatement(aId, RDF.TYPE, aType);
        // Assertion: the same predicates with an object type that is not picked up
        creator.addAssertionStatement(aId, RDFS.LABEL, other);
        creator.addAssertionStatement(aId, DCTERMS.TITLE, other);
        // Assertion: labels and descriptions of the introduced IRI
        creator.addAssertionStatement(introduced, RDFS.LABEL, literal("i label"));
        creator.addAssertionStatement(introduced, DCTERMS.DESCRIPTION, literal("i description"));
        creator.addAssertionStatement(introduced, DC.DESCRIPTION, literal("i dc description"));
        creator.addAssertionStatement(introduced, RDFS.COMMENT, literal("i comment"));
        creator.addAssertionStatement(introduced, SKOS.DEFINITION, literal("i definition"));
        creator.addAssertionStatement(introduced, RDF.TYPE, vf.createIRI("https://example.org/IntroType"));
        creator.addAssertionStatement(introduced, RDFS.LABEL, other);
        // Assertion: statements about something that is neither the assertion nor introduced
        creator.addAssertionStatement(other, RDFS.LABEL, literal("ignored"));
        creator.addAssertionStatement(other, NPX.DECLARED_BY, anyIri);

        return creator.finalizeNanopub();
    }

    @Test
    void getLabelPrefersTheNanopubLabel() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        assertEquals("np label", NanopubUtils.getLabel(createRichNanopub()));
    }

    @Test
    void getLabelIgnoresNonLiteralObjects() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        IRI introduced = vf.createIRI("https://example.org/introduced");

        creator.addPubinfoStatement(NPX.INTRODUCES, introduced);
        creator.addPubinfoStatement(NPX.DESCRIBES, vf.createIRI("https://example.org/described"));
        creator.addPubinfoStatement(NPX.EMBEDS, vf.createIRI("https://example.org/embedded"));
        creator.addPubinfoStatement(NPX.INTRODUCES, literal("not an IRI"));
        creator.addPubinfoStatement(RDFS.LABEL, anyIri);
        creator.addPubinfoStatement(DCTERMS.TITLE, anyIri);
        creator.addProvenanceStatement(RDFS.LABEL, anyIri);
        creator.addProvenanceStatement(DCTERMS.TITLE, anyIri);
        creator.addAssertionStatement(creator.getAssertionUri(), RDFS.LABEL, anyIri);
        creator.addAssertionStatement(creator.getAssertionUri(), DCTERMS.TITLE, anyIri);
        creator.addAssertionStatement(creator.getAssertionUri(), DC.TITLE, anyIri);
        creator.addAssertionStatement(introduced, RDFS.LABEL, anyIri);

        assertNull(NanopubUtils.getLabel(creator.finalizeNanopub()));
    }

    @Test
    void getLabelFallsBackToTheAssertionTitle() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(creator.getAssertionUri(), DCTERMS.TITLE, literal("a title"));
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);

        assertEquals("a title", NanopubUtils.getLabel(creator.finalizeNanopub()));
    }

    @Test
    void getDescriptionConcatenatesEveryFlavour() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String description = NanopubUtils.getDescription(createRichNanopub());

        assertNotNull(description);
        for (String expected : List.of("np description", "np dc description", "np comment", "np definition",
                "a prov description", "a prov dc description", "a prov comment", "a prov definition",
                "a description", "a dc description", "a comment", "a definition",
                "i description", "i dc description", "i comment", "i definition")) {
            assertTrue(description.contains(expected), "missing '" + expected + "' in: " + description);
        }
    }

    @Test
    void getDescriptionWithoutAnyDescriptionReturnsNull() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        assertNull(NanopubUtils.getDescription(TestUtils.createNanopub()));
    }

    @Test
    void getTypesCollectsTypesFromEveryGraph() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Set<IRI> types = NanopubUtils.getTypes(createRichNanopub());

        assertTrue(types.contains(vf.createIRI("https://example.org/NpType")));
        assertTrue(types.contains(vf.createIRI("https://example.org/NpType2")));
        assertTrue(types.contains(vf.createIRI("https://example.org/AssertionType")));
        assertTrue(types.contains(vf.createIRI("https://example.org/IntroType")));
        assertTrue(types.contains(NPX.DECLARED_BY));
    }

    @Test
    void getIntroducedIriIds() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Set<String> introduced = NanopubUtils.getIntroducedIriIds(createRichNanopub());

        assertEquals(Set.of("https://example.org/introduced", "https://example.org/described",
                "https://example.org/embedded"), introduced);
    }

    @Test
    void getIntroducedIriIdsWithoutIntroductionsIsEmpty() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        assertTrue(NanopubUtils.getIntroducedIriIds(TestUtils.createNanopub()).isEmpty());
    }

    @Test
    void getEmbeddedIriIds() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Set<String> embedded = NanopubUtils.getEmbeddedIriIds(createRichNanopub());

        assertEquals(Set.of("https://example.org/embedded"), embedded);
    }

    @Test
    void getEmbeddedIriIdsWithoutEmbeddingsIsEmpty() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        assertTrue(NanopubUtils.getEmbeddedIriIds(TestUtils.createNanopub()).isEmpty());
    }

// TODO: Using this as quickstart code in the README. Should probably be made executable somewhere, but not sure where...
//    @Test
//    void demoNanopubCreationExample() throws Exception {
//    	System.err.println("==========");
//    	System.err.println("# Creating nanopub...");
//    	NanopubCreator npCreator = new NanopubCreator(true);
//    	final IRI anne = vf.createIRI("https://example.com/anne");
//    	npCreator.addAssertionStatement(anne, RDF.TYPE, SCHEMA.PERSON);
//    	npCreator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, anne);
//    	npCreator.addPubinfoStatement(RDF.TYPE, NPX.EXAMPLE_NANOPUB);
//    	Nanopub np = npCreator.finalizeNanopub(true);
//    	System.err.println("# Nanopub before signing:");
//    	NanopubUtils.writeToStream(np, System.err, RDFFormat.TRIG);
//    	Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
//    	System.err.println("# Final nanopub after signing:");
//    	NanopubUtils.writeToStream(signedNp, System.err, RDFFormat.TRIG);
//    	System.err.println("# Publishing to test server...");
//    	PublishNanopub.publishToTestServer(signedNp);
//    	//System.err.println("# Publishing to real server...");
//    	//PublishNanopub.publish(signedNp);
//    	System.err.println("==========");
//    }
}
