package org.nanopub.fdo;

import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.nanopub.*;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.fdo.rest.HandleResolver;
import org.nanopub.fdo.rest.ResponsePrinter;
import org.nanopub.fdo.rest.gson.ParsedJsonResponse;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.SigningKeyPair;
import org.nanopub.trusty.TempUriReplacer;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.FDOF;
import org.nanopub.vocabulary.HDL;
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
import static org.mockito.Mockito.*;

public class FdoNanopubCreatorTest {

    @Test
    void prepareNanopubCreator() throws NanopubAlreadyFinalizedException {
        IRI fdoProfile = iri(HDL.NAMESPACE + "21.T11966/365ff9576c26ca6053db");
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
                                       && statement.getObject().equals(FDOF.FAIR_DIGITAL_OBJECT))
        );

        assertTrue(fdoNanopubCreator.getCurrentPubinfoStatements().stream().anyMatch(statement ->
                statement.getSubject().equals(npIri)
                && statement.getPredicate().equals(NPX.INTRODUCES)
                && statement.getObject().equals(fdoIri))
        );
    }

    public void createWithFdoIri() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String fdoHandle = "21.T11967/39b0ec87d17a4856c5f7";
        IRI fdoProfile = iri(HDL.NAMESPACE + "21.T11966/365ff9576c26ca6053db");
        String fdoLabel = "NumberFdo1";
        FdoRecord record = new FdoRecord(fdoProfile, fdoLabel, null);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoIri(record, FdoUtils.createIri(fdoHandle));

        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, iri(TestUtils.ORCID));

        Nanopub np = creator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, StandardCharsets.UTF_8));
        NanopubUtils.propagateToHandler(np, w);
    }

    @Test
    public void createWithFdoSuffix() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String fdoSuffix = "abc-table";
        IRI fdoProfile = iri(HDL.NAMESPACE + "21.T11966/365ff9576c26ca6053db");
        String fdoLabel = "abc-table-fdo";
        FdoRecord record = new FdoRecord(fdoProfile, fdoLabel, null);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoSuffix(record, fdoSuffix);

        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, iri(TestUtils.ORCID));

        Nanopub np = creator.finalizeNanopub(true);

        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(out, StandardCharsets.UTF_8));
        NanopubUtils.propagateToHandler(np, w);
    }

    @Test
    void testFdoNanopubCreation() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String fdoHandle = "21.T11967/39b0ec87d17a4856c5f7";
        IRI fdoProfile = iri(HDL.NAMESPACE + "21.T11966/365ff9576c26ca6053db");
        String fdoLabel = "NumberFdo1";
        FdoRecord record = new FdoRecord(fdoProfile, fdoLabel, null);
        NanopubCreator creator = FdoNanopubCreator.createWithFdoIri(record, FdoUtils.createIri(fdoHandle));
        creator.addProvenanceStatement(PROV.WAS_ATTRIBUTED_TO, iri(TestUtils.ORCID));

        Nanopub np = creator.finalizeNanopub(true);
        assertTrue(FdoUtils.isFdoNanopub(np));
    }

    @Test
    void testNonFdoNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
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

        try (MockedStatic<HttpClient> staticClient = mockStatic(HttpClient.class)) {
            staticClient.when(HttpClient::newHttpClient).thenReturn(mockClient);

            // call handle resolver
            ParsedJsonResponse response = new HandleResolver().call(id);
            ResponsePrinter.print(response);
        }
    }

    @Test
    public void exampleCreateFdoNanopubManuallyWithoutHandleSystem() throws MalformedNanopubException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, TrustyUriException, SignatureException, InvalidKeyException, NanopubAlreadyFinalizedException {
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
        SigningKeyPair signingKeyPair = NanopubTestSuite.getLatest().getSigningKey("rsa-key1");
        KeyPair key = SignNanopub.loadKey(signingKeyPair.getPrivateKeyFile().getPath(), SignatureAlgorithm.RSA);
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
        assertTrue(FdoUtils.looksLikeUrl(HDL.NAMESPACE + "api/handles/4263537/4000"));
        assertTrue(FdoUtils.looksLikeUrl(HDL.NAMESPACE));

        assertFalse(FdoUtils.looksLikeUrl("https://this_is_no_url"));
        assertFalse(FdoUtils.looksLikeUrl("this is not a valid url"));
    }

    @Test
    void createFdoRecordFromHandleSystem() throws URISyntaxException, IOException, InterruptedException, MalformedNanopubException {
        String handle = "21.T11966/82045bd97a0acce88378";
        String handleSystemResponse = "{\"responseCode\":1,\"handle\":\"21.T11966/82045bd97a0acce88378\",\"values\":[{\"index\":100,\"type\":\"HS_ADMIN\",\"data\":{\"format\":\"admin\",\"value\":{\"handle\":\"0.NA/21.T11966\",\"index\":300,\"permissions\":\"111111111111\"}},\"ttl\":86400,\"timestamp\":\"2025-07-02T11:03:54Z\"},{\"index\":1,\"type\":\"10320/loc\",\"data\":{\"format\":\"string\",\"value\":\"<locations>\\n<location href=\\\"http://typeregistry.testbed.pid.gwdg.de/objects/21.T11966/82045bd97a0acce88378\\\" weight=\\\"0\\\" view=\\\"json\\\" />\\n<location href=\\\"http://typeregistry.testbed.pid.gwdg.de/#objects/21.T11966/82045bd97a0acce88378\\\" weight=\\\"1\\\" view=\\\"ui\\\" />\\n</locations>\"},\"ttl\":86400,\"timestamp\":\"2025-07-02T11:03:54Z\"},{\"index\":2,\"type\":\"21.T11966/FdoProfile\",\"data\":{\"format\":\"string\",\"value\":\"21.T11966/996c38676da9ee56f8ab\"},\"ttl\":86400,\"timestamp\":\"2025-07-02T11:03:54Z\"},{\"index\":3,\"type\":\"21.T11966/JsonSchema\",\"data\":{\"format\":\"string\",\"value\":\"{\\\"$ref\\\": \\\"https://typeapi.lab.pidconsortium.net/v1/types/schema/21.T11966/82045bd97a0acce88378\\\"}\"},\"ttl\":86400,\"timestamp\":\"2025-07-02T11:03:54Z\"},{\"index\":4,\"type\":\"21.T11966/b5b58656b1fa5aff0505\",\"data\":{\"format\":\"string\",\"value\":\"21.T11966/service\"},\"ttl\":86400,\"timestamp\":\"2025-07-02T11:03:54Z\"},{\"index\":5,\"type\":\"0.TYPE/DOIPService\",\"data\":{\"format\":\"string\",\"value\":\"21.T11966/service\"},\"ttl\":86400,\"timestamp\":\"2025-07-02T11:03:54Z\"}]}";

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.body()).thenReturn(handleSystemResponse);
        try (MockedStatic<HttpClient> httpClientStaticMock = mockStatic(HttpClient.class)) {
            HttpClient mockClient = mock();
            when(mockClient.send(Mockito.any(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);
            httpClientStaticMock.when(HttpClient::newHttpClient).thenReturn(mockClient);

            FdoRecord fdoRecord = FdoNanopubCreator.createFdoRecordFromHandleSystem(handle);
            assertNotNull(fdoRecord);
        }
    }

    @Test
    void createFdoRecordFromHandleSystem_recognisesLowercaseFdoProfile() throws Exception {
        String handle = "20.5000.1025/J7E-C1H-1X2";
        String handleSystemResponse = "{\"responseCode\":1,\"handle\":\"20.5000.1025/J7E-C1H-1X2\",\"values\":["
                + "{\"index\":1,\"type\":\"fdoProfile\",\"data\":{\"format\":\"string\",\"value\":\"https://doi.org/21.T11148/2e76f544229901c5a942\"},\"ttl\":86400,\"timestamp\":\"2024-12-18T15:52:41Z\"},"
                + "{\"index\":5,\"type\":\"digitalObjectName\",\"data\":{\"format\":\"string\",\"value\":\"Annotation\"},\"ttl\":86400,\"timestamp\":\"2024-12-18T15:52:41Z\"}"
                + "]}";

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.body()).thenReturn(handleSystemResponse);
        try (MockedStatic<HttpClient> httpClientStaticMock = mockStatic(HttpClient.class)) {
            HttpClient mockClient = mock();
            when(mockClient.send(Mockito.any(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);
            httpClientStaticMock.when(HttpClient::newHttpClient).thenReturn(mockClient);

            FdoRecord record = FdoNanopubCreator.createFdoRecordFromHandleSystem(handle);

            assertEquals("https://doi.org/21.T11148/2e76f544229901c5a942", record.getProfile());
            // The recognised profile field must not leak into the generic attribute map.
            assertNull(record.getAttribute(iri(FdoNanopubCreator.FDO_TYPE_PREFIX + "fdoProfile")));
        }
    }

    @Test
    void createFdoRecordFromHandleSystem_throwsWhenProfileMissing() throws Exception {
        String handle = "20.5000.1025/NO-PROFILE";
        String handleSystemResponse = "{\"responseCode\":1,\"handle\":\"20.5000.1025/NO-PROFILE\",\"values\":["
                + "{\"index\":5,\"type\":\"digitalObjectName\",\"data\":{\"format\":\"string\",\"value\":\"Annotation\"},\"ttl\":86400,\"timestamp\":\"2024-12-18T15:52:41Z\"}"
                + "]}";

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.body()).thenReturn(handleSystemResponse);
        try (MockedStatic<HttpClient> httpClientStaticMock = mockStatic(HttpClient.class)) {
            HttpClient mockClient = mock();
            when(mockClient.send(Mockito.any(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);
            httpClientStaticMock.when(HttpClient::newHttpClient).thenReturn(mockClient);

            MalformedNanopubException ex = assertThrows(MalformedNanopubException.class,
                    () -> FdoNanopubCreator.createFdoRecordFromHandleSystem(handle));
            assertTrue(ex.getMessage().contains(handle));
            assertTrue(ex.getMessage().contains("fdoProfile"));
        }
    }

    @Test
    void createFromHandleSystem_buildsFdoNanopub() throws Exception {
        String handle = "20.5000.1025/J7E-C1H-1X2";
        String profileValue = "https://doi.org/21.T11148/2e76f544229901c5a942";
        String handleSystemResponse = "{\"responseCode\":1,\"handle\":\"20.5000.1025/J7E-C1H-1X2\",\"values\":["
                + "{\"index\":1,\"type\":\"fdoProfile\",\"data\":{\"format\":\"string\",\"value\":\"" + profileValue + "\"},\"ttl\":86400,\"timestamp\":\"2024-12-18T15:52:41Z\"},"
                + "{\"index\":5,\"type\":\"digitalObjectName\",\"data\":{\"format\":\"string\",\"value\":\"Annotation\"},\"ttl\":86400,\"timestamp\":\"2024-12-18T15:52:41Z\"}"
                + "]}";

        HttpResponse<String> mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.body()).thenReturn(handleSystemResponse);
        try (MockedStatic<HttpClient> httpClientStaticMock = mockStatic(HttpClient.class)) {
            HttpClient mockClient = mock();
            when(mockClient.send(Mockito.any(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockHttpResponse);
            httpClientStaticMock.when(HttpClient::newHttpClient).thenReturn(mockClient);

            Nanopub np = FdoNanopubCreator.createFromHandleSystem(handle);

            IRI fdoIri = FdoUtils.createIri(handle);
            IRI derivedFrom = iri(HandleResolver.BASE_URI + handle);

            boolean hasTypeStatement = np.getAssertion().stream().anyMatch(st ->
                    st.getSubject().equals(fdoIri)
                    && st.getPredicate().equals(RDF.TYPE)
                    && st.getObject().equals(FDOF.FAIR_DIGITAL_OBJECT));
            assertTrue(hasTypeStatement, "assertion must declare fdoIri a fdof:FAIRDigitalObject");

            boolean hasConformsTo = np.getAssertion().stream().anyMatch(st ->
                    st.getSubject().equals(fdoIri)
                    && st.getPredicate().equals(DCTERMS.CONFORMS_TO)
                    && st.getObject().equals(iri(profileValue)));
            assertTrue(hasConformsTo, "assertion must carry dct:conformsTo for the profile");

            boolean hasDerivedFrom = np.getProvenance().stream().anyMatch(st ->
                    st.getPredicate().equals(PROV.WAS_DERIVED_FROM)
                    && st.getObject().equals(derivedFrom));
            assertTrue(hasDerivedFrom, "provenance must trace back to the handle API URL");

            boolean introducesFdo = np.getPubinfo().stream().anyMatch(st ->
                    st.getPredicate().equals(NPX.INTRODUCES)
                    && st.getObject().equals(fdoIri));
            assertTrue(introducesFdo, "pubinfo must introduce the FDO IRI");

            // Sanity: all assertion statements have a non-null object.
            for (Statement st : np.getAssertion()) {
                assertNotNull(st.getObject(), "no null objects in assertion");
            }
        }
    }

    @Test
    void createFromHandleSystem_addsPubinfoLabelFromReferentName() throws Exception {
        String handle = "10.3535/ZJX-6N5-A5C";
        String body = "{\"responseCode\":1,\"handle\":\"" + handle + "\",\"values\":["
                + "{\"index\":1,\"type\":\"fdoProfile\",\"data\":{\"format\":\"string\",\"value\":\"https://doi.org/21.T11148/profile\"}},"
                + "{\"index\":2,\"type\":\"referentName\",\"data\":{\"format\":\"string\",\"value\":\"Rumex alpinus L.\"}}"
                + "]}";

        HttpResponse<String> resp = mock();
        when(resp.body()).thenReturn(body);

        try (MockedStatic<HttpClient> httpStatic = mockStatic(HttpClient.class)) {
            HttpClient mockClient = mock();
            when(mockClient.send(Mockito.any(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(resp);
            httpStatic.when(HttpClient::newHttpClient).thenReturn(mockClient);

            Nanopub np = FdoNanopubCreator.createFromHandleSystem(handle);

            IRI fdoIri = FdoUtils.createIri(handle);
            boolean hasReferentLabel = np.getPubinfo().stream().anyMatch(st ->
                    st.getSubject().equals(fdoIri)
                    && st.getPredicate().equals(RDFS.LABEL)
                    && st.getObject().stringValue().equals("Rumex alpinus L."));
            assertTrue(hasReferentLabel, "pubinfo must carry rdfs:label from referentName");
        }
    }

    @Test
    void createFromHandleSystem_fallsBackToHandleIdAsLabel() throws Exception {
        String handle = "10.3535/NO-REFERENT";
        String body = "{\"responseCode\":1,\"handle\":\"" + handle + "\",\"values\":["
                + "{\"index\":1,\"type\":\"fdoProfile\",\"data\":{\"format\":\"string\",\"value\":\"https://doi.org/21.T11148/profile\"}}"
                + "]}";

        HttpResponse<String> resp = mock();
        when(resp.body()).thenReturn(body);

        try (MockedStatic<HttpClient> httpStatic = mockStatic(HttpClient.class)) {
            HttpClient mockClient = mock();
            when(mockClient.send(Mockito.any(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(resp);
            httpStatic.when(HttpClient::newHttpClient).thenReturn(mockClient);

            Nanopub np = FdoNanopubCreator.createFromHandleSystem(handle);

            IRI fdoIri = FdoUtils.createIri(handle);
            boolean hasHandleLabel = np.getPubinfo().stream().anyMatch(st ->
                    st.getSubject().equals(fdoIri)
                    && st.getPredicate().equals(RDFS.LABEL)
                    && st.getObject().stringValue().equals(handle));
            assertTrue(hasHandleLabel, "pubinfo must fall back to the handle id as rdfs:label");
        }
    }

    @Test
    void createFromHandleSystem_withEnrichment_rewritesPredicatesAndAddsLabels() throws Exception {
        String handle = "20.5000.1025/J7E-C1H-1X2";
        String profileHandleId = "21.T11148/2e76f544229901c5a942";

        String fdoHandleResponse = "{\"responseCode\":1,\"handle\":\"20.5000.1025/J7E-C1H-1X2\",\"values\":["
                + "{\"index\":1,\"type\":\"fdoProfile\",\"data\":{\"format\":\"string\",\"value\":\"https://doi.org/" + profileHandleId + "\"}},"
                + "{\"index\":5,\"type\":\"digitalObjectName\",\"data\":{\"format\":\"string\",\"value\":\"Annotation\"}}"
                + "]}";
        String profileHandleResponse = "{\"responseCode\":1,\"handle\":\"" + profileHandleId + "\",\"values\":["
                + "{\"index\":1,\"type\":\"10320/loc\",\"data\":{\"format\":\"string\",\"value\":"
                + "\"<locations><location href=\\\"https://dtr.example.org/objects/" + profileHandleId + "\\\" view=\\\"json\\\" weight=\\\"1\\\"/></locations>\"}}"
                + "]}";
        String dtrBody = "{\"identifier\":\"" + profileHandleId + "\",\"properties\":["
                + "{\"name\":\"fdoProfile\",\"identifier\":\"21.T11148/21e9228a604c7b37dfdf\"},"
                + "{\"name\":\"digitalObjectName\",\"identifier\":\"21.T11148/4f2f5d61b57fb556aad9\"}"
                + "]}";

        HttpResponse<String> r1 = mock();
        when(r1.body()).thenReturn(fdoHandleResponse);
        HttpResponse<String> r2 = mock();
        when(r2.body()).thenReturn(profileHandleResponse);
        HttpResponse<String> r3 = mock();
        when(r3.body()).thenReturn(dtrBody);

        try (MockedStatic<HttpClient> httpStatic = mockStatic(HttpClient.class)) {
            HttpClient mockClient = mock();
            when(mockClient.send(Mockito.any(), eq(HttpResponse.BodyHandlers.ofString())))
                    .thenReturn(r1, r2, r3);
            httpStatic.when(HttpClient::newHttpClient).thenReturn(mockClient);

            Nanopub np = FdoNanopubCreator.createFromHandleSystem(handle, true);

            IRI fdoIri = FdoUtils.createIri(handle);
            IRI profilePredicate = iri(HDL.NAMESPACE + "21.T11148/21e9228a604c7b37dfdf");
            IRI namePredicate = iri(HDL.NAMESPACE + "21.T11148/4f2f5d61b57fb556aad9");
            IRI fallbackNamePredicate = iri(FdoNanopubCreator.FDO_TYPE_PREFIX + "digitalObjectName");

            boolean usesMappedName = np.getAssertion().stream().anyMatch(st ->
                    st.getSubject().equals(fdoIri)
                    && st.getPredicate().equals(namePredicate));
            assertTrue(usesMappedName, "digitalObjectName must be rewritten to its handle IRI");

            boolean fallbackAbsent = np.getAssertion().stream().noneMatch(st ->
                    st.getPredicate().equals(fallbackNamePredicate));
            assertTrue(fallbackAbsent, "fallback w3id kpxl predicate must not appear when mapping succeeded");

            boolean hasNameLabel = np.getPubinfo().stream().anyMatch(st ->
                    st.getSubject().equals(namePredicate)
                    && st.getPredicate().equals(RDFS.LABEL)
                    && st.getObject().stringValue().equals("digitalObjectName"));
            assertTrue(hasNameLabel, "pubinfo must carry rdfs:label 'digitalObjectName' for its mapped predicate");

            // fdoProfile's value was consumed by initFdoRecord (→ dct:conformsTo), so the mapped
            // predicate is unused — but a label may still get added only if a value was processed.
            // Ensure we didn't accidentally add a label for unused mappings.
            boolean strayProfileLabel = np.getPubinfo().stream().anyMatch(st ->
                    st.getSubject().equals(profilePredicate) && st.getPredicate().equals(RDFS.LABEL));
            assertFalse(strayProfileLabel, "no pubinfo label for mappings that produced no assertion statement");
        }
    }

}

