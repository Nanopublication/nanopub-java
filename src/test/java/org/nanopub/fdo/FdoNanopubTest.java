package org.nanopub.fdo;

import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.nanopub.*;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.fdo.rest.HandleResolver;
import org.nanopub.fdo.rest.ResponsePrinter;
import org.nanopub.fdo.rest.gson.ParsedJsonResponse;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import static java.lang.System.out;

public class FdoNanopubTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    @Test
    void exampleCreateWithFdoIri() throws MalformedNanopubException {
        String fdoHandle = "21.T11967/39b0ec87d17a4856c5f7";
        String fdoProfile = "21.T11966/365ff9576c26ca6053db";
        String fdoLabel = "NumberFdo1";
        NanopubCreator creator = FdoNanopubCreator.createWithFdoIri(FdoUtils.createIri(fdoHandle),
                fdoProfile, fdoLabel);

        creator.addProvenanceStatement(PROV.ATTRIBUTION, vf.createIRI("https://orcid.org/0000-0000-0000-0000"));

        Nanopub np = creator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);
    }

    @Test
    void exampleCreateWithFdoIriSuffix() throws MalformedNanopubException {
        String fdoSuffix = "abc-table";
        String fdoProfile = "21.T11966/365ff9576c26ca6053db";
        String fdoLabel = "abc-table-fdo";
        NanopubCreator creator = FdoNanopubCreator.createWithFdoSuffix(fdoSuffix,
                fdoProfile, fdoLabel, null);

        creator.addProvenanceStatement(PROV.ATTRIBUTION, vf.createIRI("https://orcid.org/0000-0000-0000-0000"));

        Nanopub np = creator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);
    }

    @Test
    void testFdoNanopubCreation() throws MalformedNanopubException {
        String fdoHandle = "21.T11967/39b0ec87d17a4856c5f7";
        String fdoProfile = "21.T11966/365ff9576c26ca6053db";
        String fdoLabel = "NumberFdo1";
        NanopubCreator creator = FdoNanopubCreator.createWithFdoIri(FdoUtils.createIri(fdoHandle),
                fdoProfile,"NumberFdo1" );

        creator.addProvenanceStatement(PROV.ATTRIBUTION, vf.createIRI("https://orcid.org/0000-0000-0000-0000"));

        Nanopub np = creator.finalizeNanopub(true);

        FdoNanopub fdoNanopub = new FdoNanopub(np);
        Assert.assertEquals(fdoProfile, fdoNanopub.getProfile());
        Assert.assertEquals(fdoLabel, fdoNanopub.getLabel());
    }

    @Test
    void testInvalidFdoNanopub() throws MalformedNanopubException {
        Nanopub np = new NanopubUtilsTest().createNanopub();
        Assert.assertThrows(IllegalArgumentException.class, () -> new FdoNanopub(np));
    }

    @Test
    void exampleRestCall() throws URISyntaxException, IOException, InterruptedException {
//        String id = "4263537/4000";
        String id = "21.T11967/39b0ec87d17a4856c5f7";
        ParsedJsonResponse response = new HandleResolver().call(id);

        ResponsePrinter.print(response);
    }

    @Test
    void exampleCreateFdoNanopubManuallyWithoutHandleSystem() throws MalformedNanopubException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, TrustyUriException, SignatureException, InvalidKeyException {
        String fdoSuffix = "example-fdo-01";
        String profile = "https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile";
        String label = "ExampleFdo01";
        String dataRef = "https://github.com/Nanopublication/nanopub-java/blob/master/README.md";
        String signer = "https://orcid.org/0000-0000-0000-0000"; // enter your orcid

        // create nanopub
        NanopubCreator creator = FdoNanopubCreator.createWithFdoSuffix(fdoSuffix, profile, label, dataRef);
        creator.addProvenanceStatement(PROV.ATTRIBUTION, vf.createIRI(signer));
        Nanopub np = creator.finalizeNanopub();

        // enter your key
        KeyPair key = SignNanopub.loadKey("src/test/resources/testsuite/transform/signed/rsa-key1/key/id_rsa", SignatureAlgorithm.RSA);
        TransformContext context = new TransformContext(SignatureAlgorithm.RSA, key, vf.createIRI(signer), true, true, true);
        Nanopub signedNp = SignNanopub.signAndTransform(np, context);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);
    }

    @Test
    void testLooksLikeHandle () {
        Assert.assertTrue(FdoUtils.looksLikeHandle("21.T11967/39b0ec87d17a4856c5f7"));
        Assert.assertTrue(FdoUtils.looksLikeHandle("21.T11966/82045bd97a0acce88378"));
        Assert.assertTrue(FdoUtils.looksLikeHandle("4263537/4000"));

        Assert.assertFalse(FdoUtils.looksLikeHandle("this is not a valid handle"));
        Assert.assertFalse(FdoUtils.looksLikeHandle("https://this_is_no_handle"));
        Assert.assertFalse(FdoUtils.looksLikeHandle("21.T11966"));
    }

    @Test
    void testLooksLikeUrl () {
        Assert.assertTrue(FdoUtils.looksLikeUrl("https://this_may_be_an_url.com"));
        Assert.assertTrue(FdoUtils.looksLikeUrl("https://www.knowledgepixesl.com"));
        Assert.assertTrue(FdoUtils.looksLikeUrl("https://hdl.handle.net/api/handles/4263537/4000"));
        Assert.assertTrue(FdoUtils.looksLikeUrl("https://hdl.handle.net"));

        Assert.assertFalse(FdoUtils.looksLikeUrl("https://this_is_no_url"));
        Assert.assertFalse(FdoUtils.looksLikeUrl("this is not a valid url"));
    }

}
