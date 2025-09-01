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
import org.nanopub.extra.services.APINotReachableException;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.NotEnoughAPIInstancesException;
import org.nanopub.fdo.FdoNanopubCreatorTest;
import org.nanopub.fdo.FdoQuery;
import org.nanopub.fdo.RetrieveFdo;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.SCHEMA;

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
        npCreator.addAssertionStatement(anne, RDF.TYPE, SCHEMA.PERSON);
        npCreator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, anne);
        npCreator.addPubinfoStatement(RDF.TYPE, NPX.EXAMPLE_NANOPUB);
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

    void createFdoNanopubManuallyWithoutHandleSystem() throws MalformedNanopubException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, TrustyUriException, SignatureException, InvalidKeyException, NanopubAlreadyFinalizedException {
        new FdoNanopubCreatorTest().exampleCreateFdoNanopubManuallyWithoutHandleSystem();
    }

    void createNanopubWithFdoIri() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        new FdoNanopubCreatorTest().createWithFdoIri();
    }

    void createNanopubWithFdoIriSuffix() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        new FdoNanopubCreatorTest().createWithFdoSuffix();
    }

    void shaclValidation() {
        // look at
        new ShaclValidationTest();
    }

    void examplesWithHandleSystem() throws MalformedNanopubException {
        // look at
        new GeneralIntegrationTestsIT();
    }

    void examplesForQueryingFdo() throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        // This query performs a full-text search on the FDO nanopublications.
        ApiResponse response1 = FdoQuery.textSearch("myText");

        // This query returns the FDOs whose records refer to the given PID / handle
        ApiResponse response2 = FdoQuery.findByRef("21.T11966/82045bd97a0acce88378");

        // This query returns the latest FDOs from the specified creator.
        ApiResponse response3 = FdoQuery.getFeed("https://orcid.org/0009-0008-3635-347X");

        // This query returns the things the given user has declared to be their favorites (using cito:likes).
        ApiResponse response4 = FdoQuery.getFavoriteThings("https://orcid.org/0000-0002-1267-0234");

    }
}
