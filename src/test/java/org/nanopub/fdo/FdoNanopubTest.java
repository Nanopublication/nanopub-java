package org.nanopub.fdo;

import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.fdo.rest.HandleResolver;
import org.nanopub.fdo.rest.ResponsePrinter;
import org.nanopub.fdo.rest.gson.ParsedJsonResponse;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPX;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FdoNanopubTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    @Test
    public void exampleCreateWithFdoIri() throws MalformedNanopubException {
        String fdoHandle = "21.T11967/39b0ec87d17a4856c5f7";
        IRI fdoProfile = vf.createIRI("https://hdl.handle.net/21.T11966/365ff9576c26ca6053db");
        String fdoLabel = "NumberFdo1";
        FdoRecord record = new FdoRecord(fdoProfile, fdoLabel, null);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoIri(record, FdoUtils.createIri(fdoHandle));

        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, vf.createIRI("https://orcid.org/0000-0000-0000-0000"));

        Nanopub np = creator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);
    }

    @Test
    public void exampleCreateWithFdoIriSuffix() throws MalformedNanopubException {
        String fdoSuffix = "abc-table";
        IRI fdoProfile = vf.createIRI("https://hdl.handle.net/21.T11966/365ff9576c26ca6053db");
        String fdoLabel = "abc-table-fdo";
        FdoRecord record = new FdoRecord(fdoProfile, fdoLabel, null);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoSuffix(record, fdoSuffix);

        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, vf.createIRI("https://orcid.org/0000-0000-0000-0000"));

        Nanopub np = creator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, Charset.forName("UTF-8")));
        NanopubUtils.propagateToHandler(np, w);
    }

    @Test
    void testFdoNanopubCreation() throws MalformedNanopubException {
        String fdoHandle = "21.T11967/39b0ec87d17a4856c5f7";
        IRI fdoProfile = vf.createIRI("https://hdl.handle.net/21.T11966/365ff9576c26ca6053db");
        String fdoLabel = "NumberFdo1";
        FdoRecord record = new FdoRecord(fdoProfile, fdoLabel, null);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoIri(record, FdoUtils.createIri(fdoHandle));
        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, vf.createIRI("https://orcid.org/0000-0000-0000-0000"));

        Nanopub np = creator.finalizeNanopub(true);
        assertTrue(FdoUtils.isFdoNanopub(np));
    }

    @Test
    void testNonFdoNanopub() throws MalformedNanopubException {
        NanopubCreator npCreator = new NanopubCreator(true);
        final IRI nonFdoNanopub = vf.createIRI("https://example.com/nonFdoNanopub");
        npCreator.addAssertionStatement(nonFdoNanopub, RDF.TYPE, TestUtils.anyIri);
        npCreator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, nonFdoNanopub);
        npCreator.addPubinfoStatement(RDF.TYPE, NPX.EXAMPLE_NANOPUB);
        Nanopub np = npCreator.finalizeNanopub(true);

        assertFalse(FdoUtils.isFdoNanopub(np));
    }

    @Test
    void exampleRestCall() throws URISyntaxException, IOException, InterruptedException {
//        String id = "4263537/4000";
        String id = "21.T11967/39b0ec87d17a4856c5f7";
        ParsedJsonResponse response = new HandleResolver().call(id);

        ResponsePrinter.print(response);
    }

    @Test
    public void exampleCreateFdoNanopubManuallyWithoutHandleSystem() throws MalformedNanopubException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, TrustyUriException, SignatureException, InvalidKeyException {
        String fdoSuffix = "example-fdo-01";
        IRI profile = vf.createIRI("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "ExampleFdo01";
        IRI dataRef = vf.createIRI("https://github.com/Nanopublication/nanopub-java/blob/master/README.md");
        String signer = "https://orcid.org/0000-0000-0000-0000"; // enter your orcid

        // create fdo record
        FdoRecord record = new FdoRecord(profile, label, dataRef);

        // create nanopub
        NanopubCreator creator = FdoNanopubCreator.createWithFdoSuffix(record, fdoSuffix);
        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, vf.createIRI(signer));
        Nanopub np = creator.finalizeNanopub();

        // enter your key
        KeyPair key = SignNanopub.loadKey("src/test/resources/testsuite/transform/signed/rsa-key1/key/id_rsa", SignatureAlgorithm.RSA);
        TransformContext context = new TransformContext(SignatureAlgorithm.RSA, key, vf.createIRI(signer), true, true, true);
        // signing
        Nanopub signedNp = SignNanopub.signAndTransform(np, context);

        // output to System.out
        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, Charset.forName("UTF-8")));

        System.out.println("\nSigned Nanopub:");
        NanopubUtils.propagateToHandler(signedNp, w);
    }

    @Test
    void testLooksLikeHandle() {
        assertTrue(FdoUtils.looksLikeHandle("21.T11967/39b0ec87d17a4856c5f7"));
        assertTrue(FdoUtils.looksLikeHandle("21.T11966/82045bd97a0acce88378"));
        assertTrue(FdoUtils.looksLikeHandle("4263537/4000"));

        assertFalse(FdoUtils.looksLikeHandle("this is not a valid handle"));
        assertFalse(FdoUtils.looksLikeHandle("https://this_is_no_handle"));
        assertFalse(FdoUtils.looksLikeHandle("21.T11966"));
    }

    @Test
    void testLooksLikeUrl() {
        assertTrue(FdoUtils.looksLikeUrl("https://this_may_be_an_url.com"));
        assertTrue(FdoUtils.looksLikeUrl("https://www.knowledgepixesl.com"));
        assertTrue(FdoUtils.looksLikeUrl("https://hdl.handle.net/api/handles/4263537/4000"));
        assertTrue(FdoUtils.looksLikeUrl("https://hdl.handle.net"));

        assertFalse(FdoUtils.looksLikeUrl("https://this_is_no_url"));
        assertFalse(FdoUtils.looksLikeUrl("this is not a valid url"));
    }

}
