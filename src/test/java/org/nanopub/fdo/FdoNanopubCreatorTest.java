package org.nanopub.fdo;

import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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
import org.nanopub.trusty.TempUriReplacer;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPX;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

import static java.lang.System.out;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FdoNanopubCreatorTest {

    @Test
    void prepareNanopubCreator() {
        IRI fdoProfile = iri("https://hdl.handle.net/21.T11966/365ff9576c26ca6053db");
        String fdoLabel = "ExampleFdoToUpdate";
        FdoRecord record = new FdoRecord(fdoProfile, fdoLabel, null);

        String npIriString = TempUriReplacer.tempUri + Math.abs(new Random().nextInt()) + "/";
        String fdoSuffix = "FdoExample";
        IRI fdoIri = iri(npIriString + fdoSuffix);
        IRI npIri = iri(npIriString);
        NanopubCreator fdoNanopubCreator = FdoNanopubCreator.prepareNanopubCreator(record, fdoIri, npIri);

        assertTrue(fdoNanopubCreator.getCurrentAssertionStatements().stream()
                .anyMatch(statement -> statement.getSubject().equals(fdoIri)
                        && statement.getPredicate().equals(RDF.TYPE)
                        && statement.getObject().equals(FdoUtils.RDF_TYPE_FDO))
        );

        assertTrue(fdoNanopubCreator.getCurrentPubinfoStatements().stream().anyMatch(statement ->
                statement.getSubject().equals(npIri)
                        && statement.getPredicate().equals(NPX.INTRODUCES)
                        && statement.getObject().equals(fdoIri))
        );
    }

    public void createWithFdoIri() throws MalformedNanopubException {
        String fdoHandle = "21.T11967/39b0ec87d17a4856c5f7";
        IRI fdoProfile = iri("https://hdl.handle.net/21.T11966/365ff9576c26ca6053db");
        String fdoLabel = "NumberFdo1";
        FdoRecord record = new FdoRecord(fdoProfile, fdoLabel, null);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoIri(record, FdoUtils.createIri(fdoHandle));

        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, iri(TestUtils.ORCID));

        Nanopub np = creator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, StandardCharsets.UTF_8));
        NanopubUtils.propagateToHandler(np, w);
    }

    @Test
    public void createWithFdoSuffix() throws MalformedNanopubException {
        String fdoSuffix = "abc-table";
        IRI fdoProfile = iri("https://hdl.handle.net/21.T11966/365ff9576c26ca6053db");
        String fdoLabel = "abc-table-fdo";
        FdoRecord record = new FdoRecord(fdoProfile, fdoLabel, null);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoSuffix(record, fdoSuffix);

        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, iri(TestUtils.ORCID));

        Nanopub np = creator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, StandardCharsets.UTF_8));
        NanopubUtils.propagateToHandler(np, w);
    }

    @Test
    void testFdoNanopubCreation() throws MalformedNanopubException {
        String fdoHandle = "21.T11967/39b0ec87d17a4856c5f7";
        IRI fdoProfile = iri("https://hdl.handle.net/21.T11966/365ff9576c26ca6053db");
        String fdoLabel = "NumberFdo1";
        FdoRecord record = new FdoRecord(fdoProfile, fdoLabel, null);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoIri(record, FdoUtils.createIri(fdoHandle));
        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, iri(TestUtils.ORCID));

        Nanopub np = creator.finalizeNanopub(true);
        assertTrue(FdoUtils.isFdoNanopub(np));
    }

    @Test
    void testNonFdoNanopub() throws MalformedNanopubException {
        NanopubCreator npCreator = new NanopubCreator(true);
        final IRI nonFdoNanopub = iri("https://example.com/nonFdoNanopub");
        npCreator.addAssertionStatement(nonFdoNanopub, RDF.TYPE, iri("https://schema.org/Any"));
        npCreator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, nonFdoNanopub);
        npCreator.addPubinfoStatement(RDF.TYPE, NPX.EXAMPLE_NANOPUB);
        Nanopub np = npCreator.finalizeNanopub(true);

        assertFalse(FdoUtils.isFdoNanopub(np));
    }

    @Test
    void exampleRestCall() throws URISyntaxException, IOException, InterruptedException, ClassNotFoundException {
//        String id = "4263537/4000";
        String id = "21.T11967/39b0ec87d17a4856c5f7";

        // Set-up mocked httpClient
        String httpResponseBody = "{\"responseCode\":1,\"handle\":\"21.T11967/39b0ec87d17a4856c5f7\",\"values\":[{\"index\":100,\"type\":\"HS_ADMIN\",\"data\":{\"format\":\"admin\",\"value\":{\"handle\":\"0.NA/21.T11967\",\"index\":300,\"permissions\":\"111111111111\"}},\"ttl\":86400,\"timestamp\":\"2025-03-30T14:53:22Z\"},{\"index\":1,\"type\":\"10320/loc\",\"data\":{\"format\":\"string\",\"value\":\"<locations>\\n<location href=\\\"https://cordra.testbed.pid.gwdg.de/objects/21.T11967/39b0ec87d17a4856c5f7\\\" weight=\\\"0\\\" view=\\\"json\\\" />\\n<location href=\\\"https://cordra.testbed.pid.gwdg.de/#objects/21.T11967/39b0ec87d17a4856c5f7\\\" weight=\\\"1\\\" view=\\\"ui\\\" />\\n</locations>\"},\"ttl\":86400,\"timestamp\":\"2025-03-30T14:53:22Z\"},{\"index\":2,\"type\":\"id\",\"data\":{\"format\":\"string\",\"value\":\"21.T11967/39b0ec87d17a4856c5f7\"},\"ttl\":86400,\"timestamp\":\"2025-03-30T14:53:22Z\"},{\"index\":3,\"type\":\"name\",\"data\":{\"format\":\"string\",\"value\":\"NumberFdo1\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":4,\"type\":\"description\",\"data\":{\"format\":\"string\",\"value\":\"This FDO is a simple data FDO to demonstrate what a ConfigType4 FDO looks like.\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":5,\"type\":\"FdoMimeType\",\"data\":{\"format\":\"string\",\"value\":\"21.T11966/f919d9f152904f6c40db\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":6,\"type\":\"FdoSemanticType\",\"data\":{\"format\":\"string\",\"value\":\"21.T11966/dde0a91075a4258a878b\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":7,\"type\":\"FdoProfile\",\"data\":{\"format\":\"string\",\"value\":\"21.T11966/82045bd97a0acce88378\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":8,\"type\":\"DataRef\",\"data\":{\"format\":\"string\",\"value\":\"21.T11967/83d2b3f39034b2ac78cd\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":9,\"type\":\"MetadataRef\",\"data\":{\"format\":\"string\",\"value\":\"21.T11967/17361829babdab0ba566\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":10,\"type\":\"FdoService\",\"data\":{\"format\":\"string\",\"value\":\"21.T11967/service\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":11,\"type\":\"FdoStatus\",\"data\":{\"format\":\"string\",\"value\":\"created\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":12,\"type\":\"creationDate\",\"data\":{\"format\":\"string\",\"value\":\"2025-03-30T14:53:22.893Z\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":13,\"type\":\"modificationDate\",\"data\":{\"format\":\"string\",\"value\":\"2025-05-09T11:33:01.196Z\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":14,\"type\":\"createdBy\",\"data\":{\"format\":\"string\",\"value\":\"admin\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":15,\"type\":\"modifiedBy\",\"data\":{\"format\":\"string\",\"value\":\"admin\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"},{\"index\":16,\"type\":\"0.TYPE/DOIPService\",\"data\":{\"format\":\"string\",\"value\":\"21.T11967/service\"},\"ttl\":86400,\"timestamp\":\"2025-05-09T11:33:01Z\"}]}";
        HttpResponse<String> mockResponse = mock();
        when(mockResponse.body()).thenReturn(httpResponseBody);

        HttpClient mockClient = mock();
        when(mockClient.send(Mockito.any(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockResponse);

        try (MockedStatic<HttpClient> staticClient = Mockito.mockStatic(HttpClient.class)) {
            staticClient.when(HttpClient::newHttpClient).thenReturn(mockClient);

            // call handle resolver
            ParsedJsonResponse response = new HandleResolver().call(id);
            ResponsePrinter.print(response);
        }
    }

    @Test
    public void exampleCreateFdoNanopubManuallyWithoutHandleSystem() throws MalformedNanopubException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, TrustyUriException, SignatureException, InvalidKeyException {
        String fdoSuffix = "example-fdo-01";
        IRI profile = iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "ExampleFdo01";
        IRI dataRef = iri("https://github.com/Nanopublication/nanopub-java/blob/master/README.md");
        String signer = TestUtils.ORCID; // enter your orcid

        // create fdo record
        FdoRecord record = new FdoRecord(profile, label, dataRef);

        // create nanopub
        NanopubCreator creator = FdoNanopubCreator.createWithFdoSuffix(record, fdoSuffix);
        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, iri(signer));
        Nanopub np = creator.finalizeNanopub();

        // enter your key
        KeyPair key = SignNanopub.loadKey(this.getClass().getResource("/testsuite/transform/signed/rsa-key1/key/id_rsa").getPath(), SignatureAlgorithm.RSA);
        TransformContext context = new TransformContext(SignatureAlgorithm.RSA, key, iri(signer), true, true, true);
        // signing
        Nanopub signedNp = SignNanopub.signAndTransform(np, context);

        // output to System.out
        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, StandardCharsets.UTF_8));

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

    @Test
    void createFdoRecordFromHandleSystem() throws URISyntaxException, IOException, InterruptedException {
        String profileId = "21.T11966/82045bd97a0acce88378";
        FdoRecord fdoRecord = FdoNanopubCreator.createFdoRecordFromHandleSystem(profileId);
        assertNotNull(fdoRecord);
    }

}

