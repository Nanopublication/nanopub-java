package org.nanopub;

import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.extra.security.MakeKeys;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.fdo.FdoNanopubTest;
import org.nanopub.fdo.RetrieveFdo;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

/**
 * This class contains a bunch of examples on how to use the library.
 */
public class UsageExamples {

    final ValueFactory vf = SimpleValueFactory.getInstance();

    void createKey() throws Exception {
        MakeKeys.make("~/.nanopub/id", SignatureAlgorithm.RSA);
    }

    void createAndPublishNanopub() throws Exception {
        System.err.println("# Creating nanopub...");
        NanopubCreator npCreator = new NanopubCreator(true);

        final IRI anne = vf.createIRI("https://example.com/anne");
        npCreator.addAssertionStatement(anne, RDF.TYPE, vf.createIRI("https://schema.org/Person"));
        npCreator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, anne);
        npCreator.addPubinfoStatement(RDF.TYPE, vf.createIRI("http://purl.org/nanopub/x/ExampleNanopub"));
        Nanopub np = npCreator.finalizeNanopub(true);
        System.err.println("# Nanopub before signing:");
        NanopubUtils.writeToStream(np, System.err, RDFFormat.TRIG);

        System.err.println("# Signing nanopub...");
        Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
        System.err.println("# Final nanopub after signing:");
        NanopubUtils.writeToStream(signedNp, System.err, RDFFormat.TRIG);

        System.err.println("# Publishing to test server...");
        PublishNanopub.publishToTestServer(signedNp);
        //System.err.println("# Publishing to real server...");
        //PublishNanopub.publish(signedNp);
        System.err.println("# Published");
    }

    void retrieveContentFromNpNetwork() throws Exception {
        String id = "https://w3id.org/np/RAsSeIyT03LnZt3QvtwUqIHSCJHWW1YeLkyu66Lg4FeBk/nanodash-readme";
        InputStream in = RetrieveFdo.retrieveContentFromId(id);
        in.transferTo(System.out);
    }

    void createFdoNanopubManuallyWithoutHandleSystem() throws MalformedNanopubException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, TrustyUriException, SignatureException, InvalidKeyException {
        new FdoNanopubTest().exampleCreateFdoNanopubManuallyWithoutHandleSystem();
    }

    void createNanopubWithFdoIri() throws MalformedNanopubException {
        new FdoNanopubTest().exampleCreateWithFdoIri();
    }

    void createNanopubWithFdoIriSuffix() throws MalformedNanopubException {
        new FdoNanopubTest().exampleCreateWithFdoIriSuffix();
    }

    void shaclValidation() {
        // look at
        new ShaclValidationTest();
    }

    void examplesWithHandleSystem() throws MalformedNanopubException {
        // look at
        new TheseTestsRequireOtherSystemsIT();
    }
}
