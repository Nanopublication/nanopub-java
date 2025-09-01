package org.nanopub;

import net.trustyuri.TrustyUriUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.junit.jupiter.api.Test;
import org.nanopub.trusty.TempUriReplacer;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPX;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

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
                .anyMatch(st -> st.getSubject().equals(provenanceStatement.getSubject()) &&
                        st.getPredicate().equals(provenanceStatement.getPredicate())
                        && st.getObject().equals(provenanceStatement.getObject())));

        assertTrue(NanopubUtils.getStatements(nanopub).stream()
                .anyMatch(st -> st.getSubject().equals(assertionStatement.getSubject()) &&
                        st.getPredicate().equals(assertionStatement.getPredicate())
                        && st.getObject().equals(assertionStatement.getObject())));

        assertTrue(NanopubUtils.getStatements(nanopub).stream()
                .anyMatch(st -> st.getSubject().equals(pubinfoStatement.getSubject()) &&
                        st.getPredicate().equals(pubinfoStatement.getPredicate())
                        && st.getObject().equals(pubinfoStatement.getObject())));

        // there are 4 header statements, we do not check them here
        assertEquals(7, NanopubUtils.getStatements(nanopub).size());
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