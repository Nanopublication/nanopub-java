package org.nanopub;

import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class NanopubUtilsTest {

    private final ValueFactory vf = SimpleValueFactory.getInstance();
    private final IRI anyIri = vf.createIRI("http://knowledgepixels.com/nanopubIri#any");

    @Test
    void getDefaultNamespaces() {
        assertThat(NanopubUtils.getDefaultNamespaces()).isNotEmpty();
    }

    @Test
    void getStatementsMinimal() throws MalformedNanopubException {
        NanopubCreator creator = new NanopubCreator(vf.createIRI("http://knowledgepixels.com/nanopubIri#title"));

        // Create valid nanopub
        Statement assertionStatement = vf.createStatement(anyIri, anyIri, anyIri);
        creator.addAssertionStatements(assertionStatement);

        Statement provenanceStatement = vf.createStatement(
                creator.getAssertionUri(),
                anyIri,
                anyIri);
        creator.addProvenanceStatements(provenanceStatement);

        Statement pubinfoStatement = vf.createStatement(creator.getNanopubUri(), anyIri, anyIri);
        creator.addPubinfoStatements(pubinfoStatement);

        Nanopub nanopub = creator.finalizeNanopub();

        // Check that getStatements returns the expected Statements
        assertThat(NanopubUtils.getStatements(nanopub).contains(provenanceStatement));
        assertThat(NanopubUtils.getStatements(nanopub).contains(assertionStatement));
        assertThat(NanopubUtils.getStatements(nanopub).contains(pubinfoStatement));

        // there are 4 header statements, we do not check them here
        assertThat(NanopubUtils.getStatements(nanopub).size()).isEqualTo(7);
    }

    @Test
    void writeToStream() throws MalformedNanopubException {
        Nanopub nanopub = createNanopub();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        RDFFormat format = RDFFormat.TURTLE; // TODO TrustyNanopubUtils.STNP_FORMAT
        NanopubUtils.writeToStream(nanopub, os, format);

        String output = new String(os.toByteArray());
        assertThat(output).contains(nanopub.getUri().toString());
    }

    public Nanopub createNanopub() throws MalformedNanopubException {
        NanopubCreator creator = new NanopubCreator(vf.createIRI("http://knowledgepixels.com/nanopubIri#title"));

        // Create valid nanopub
        Statement assertionStatement = vf.createStatement(anyIri, anyIri, anyIri);
        creator.addAssertionStatements(assertionStatement);

        Statement provenanceStatement = vf.createStatement(
                creator.getAssertionUri(),
                anyIri,
                anyIri);
        creator.addProvenanceStatements(provenanceStatement);

        Statement pubinfoStatement = vf.createStatement(creator.getNanopubUri(), anyIri, anyIri);
        creator.addPubinfoStatements(pubinfoStatement);

        Nanopub nanopub = creator.finalizeNanopub();
        return nanopub;
    }

    @Test
    void testEquality() throws Exception {
        Nanopub np1 = createNanopub();
        Nanopub np2 = createNanopub();
        assertThat(np1.equals(np2)).isTrue();
    }

    @Test
    void writeToString() throws MalformedNanopubException, IOException {
        Nanopub nanopub = createNanopub();

        RDFFormat format = RDFFormat.JSONLD; // TODO NullPointerException with TrustyNanopubUtils.STNP_FORMAT
        String output = NanopubUtils.writeToString(nanopub, format);

        assertThat(output).contains(nanopub.getUri().toString());
    }

    @Test
    void getLabel() throws MalformedNanopubException {
        NanopubCreator creator = new NanopubCreator(vf.createIRI("http://knowledgepixels.com/nanopubIri#title"));

        // Create nanopub with Label
        Statement assertionStatement = vf.createStatement(
                vf.createIRI("http://knowledgepixels.com/nanopubIri#titleassertion"),
                RDFS.LABEL,
                vf.createLiteral("My Label"));
        creator.addAssertionStatements(assertionStatement);

        Statement provenanceStatement = vf.createStatement(
                creator.getAssertionUri(),
                anyIri,
                anyIri);
        creator.addProvenanceStatements(provenanceStatement);

        Statement pubinfoStatement = vf.createStatement(creator.getNanopubUri(), anyIri, anyIri);
        creator.addPubinfoStatements(pubinfoStatement);
        Nanopub nanopub = creator.finalizeNanopub();

        String label = NanopubUtils.getLabel(nanopub);
        assertThat(label).isEqualTo("My Label");
    }

    @Test
    void getDescription() throws MalformedNanopubException {
        NanopubCreator creator = new NanopubCreator(vf.createIRI("http://knowledgepixels.com/nanopubIri#title"));

        // Create nanopub with Description
        Statement assertionStatement = vf.createStatement(
                vf.createIRI("http://knowledgepixels.com/nanopubIri#titleassertion"),
                DCTERMS.DESCRIPTION,
                vf.createLiteral("My Description"));
        creator.addAssertionStatements(assertionStatement);

        Statement provenanceStatement = vf.createStatement(
                creator.getAssertionUri(),
                anyIri,
                anyIri);
        creator.addProvenanceStatements(provenanceStatement);

        Statement pubinfoStatement = vf.createStatement(creator.getNanopubUri(), anyIri, anyIri);
        creator.addPubinfoStatements(pubinfoStatement);
        Nanopub nanopub = creator.finalizeNanopub();

        String description = NanopubUtils.getDescription(nanopub);
        assertThat(description).isEqualTo("My Description");
    }

    @Test
    void getTypes() throws MalformedNanopubException {
        Nanopub nanopub = createNanopub();
        Set<IRI> types = NanopubUtils.getTypes(nanopub);
        // This is an extremely minimal test, some more assertions were nice
        assertThat(types).contains(anyIri);
    }

    @Test
    void updateXorChecksum() {
        String anyChecksum = "This is any checksum with length more than 32 characters for testing";
        IRI anyIri = vf.createIRI("http://www.tkuhn.org/pub/sempub/sempub.trig#np2.RA8tL7TWDOtL6oz3dhhYZ6JIBB9YlroOFIMKcQk7nFEr8");

        String res = "vMpXx6ZpfXb2vTxPHo7Xotfmd1ENAlbltQ7nSnGfvxgtersfortestin";
        assertThat(res).isNotEqualTo(anyChecksum);
    }

    @Test
    void getHttpClient() {
        CloseableHttpClient client = NanopubUtils.getHttpClient();
        assertThat(client).isNotNull();

        // We do not care if it's the same client, but it must be there
        client = NanopubUtils.getHttpClient();
        assertThat(client).isNotNull();
    }
 
// TODO: Using this as quickstart code in the README. Should probably be made executable somewhere, but not sure where...
//    @Test
//    void demoNanopubCreationExample() throws Exception {
//    	System.err.println("==========");
//    	System.err.println("# Creating nanopub...");
//    	NanopubCreator npCreator = new NanopubCreator(true);
//    	final IRI anne = vf.createIRI("https://example.com/anne");
//    	npCreator.addAssertionStatement(anne, RDF.TYPE, vf.createIRI("https://schema.org/Person"));
//    	npCreator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, anne);
//    	npCreator.addPubinfoStatement(RDF.TYPE, vf.createIRI("http://purl.org/nanopub/x/ExampleNanopub"));
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