package org.nanopub.fdo;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.security.*;
import org.nanopub.trusty.TempUriReplacer;
import org.nanopub.vocabulary.NPX;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Set;

import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.nanopub.extra.security.SignatureAlgorithm.RSA;
import static org.nanopub.fdo.FdoRecord.SCHEMA_ID;
import static org.nanopub.fdo.FdoUtils.FDO_URI_PREFIX;
import static org.nanopub.utils.TestUtils.vf;

class FdoRecordTest {

    private static final String TEST_KEY_PATH = "~/.nanopub/testkey/";
    private static final String TEST_KEY_NAME = "id";
    private static final String testKeysDirPath = SignatureUtils.getFullFilePath(TEST_KEY_PATH);

    @BeforeAll
    static void setUp() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        File testKeysDir = new File(testKeysDirPath);
        testKeysDir.mkdirs();
        MakeKeys.make(testKeysDirPath + TEST_KEY_NAME, SignatureAlgorithm.RSA);
        assertTrue(new File(testKeysDirPath + TEST_KEY_NAME + "_" + SignatureAlgorithm.RSA.name().toLowerCase()).exists());
        assertTrue(new File(testKeysDirPath + TEST_KEY_NAME + "_" + SignatureAlgorithm.RSA.name().toLowerCase() + ".pub").exists());

        // mock TransformContext.makeDefault() to get test keys
        KeyPair key = SignNanopub.loadKey(TEST_KEY_PATH + "/id_rsa", RSA);

        TransformContext testTC = new TransformContext(RSA, key, null, false, false, false);
        MockedStatic<TransformContext> transformContextMock = mockStatic(TransformContext.class, CALLS_REAL_METHODS);
        transformContextMock
                .when(() -> TransformContext.makeDefault())
                .thenAnswer(invocation -> testTC);

    }

    @AfterAll
    static void tearDown() throws IOException {
        File testKeysDir = new File(testKeysDirPath);
        FileUtils.deleteDirectory(testKeysDir);
        assertFalse(testKeysDir.exists());
    }


    @Test
    void constructFdoRecordWithoutDataRef() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";

        FdoRecord record = new FdoRecord(fdoProfile, label, null);

        assertNotNull(record);
        assertEquals(FdoUtils.RDF_TYPE_FDO, record.getAttribute(RDF.TYPE));
        assertEquals(record.getAttribute(FdoUtils.PROFILE_IRI), fdoProfile);
        assertEquals(label, record.getAttribute(RDFS.LABEL).stringValue());
        assertNull(record.getAttribute(FdoUtils.DATA_REF_IRI));
        assertNull(record.getId());
        assertNull(record.getOriginalNanopub());
    }

    @Test
    void constructFdoRecordWithDataRef() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        String dataRef = "https://example.org/data-ref";

        FdoRecord record = new FdoRecord(fdoProfile, label, Values.iri(dataRef));

        assertNotNull(record);
        assertEquals(FdoUtils.RDF_TYPE_FDO, record.getAttribute(RDF.TYPE));
        assertEquals(record.getAttribute(FdoUtils.PROFILE_IRI), fdoProfile);
        assertEquals(label, record.getAttribute(RDFS.LABEL).stringValue());
        assertEquals(record.getAttribute(FdoUtils.DATA_REF_IRI), Values.iri(dataRef));
        assertNull(record.getId());
        assertNull(record.getOriginalNanopub());
    }

    @Test
    void constructFdoRecordWithoutLabel() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String dataRef = "https://example.org/data-ref";

        FdoRecord record = new FdoRecord(fdoProfile, null, Values.iri(dataRef));

        assertNotNull(record);
        assertEquals(FdoUtils.RDF_TYPE_FDO, record.getAttribute(RDF.TYPE));
        assertEquals(record.getAttribute(FdoUtils.PROFILE_IRI), fdoProfile);
        assertNull(record.getAttribute(RDFS.LABEL));
        assertEquals(record.getAttribute(FdoUtils.DATA_REF_IRI), Values.iri(dataRef));
        assertNull(record.getId());
        assertNull(record.getOriginalNanopub());
    }

    @Test
    void getLabelWhenSet() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);
        assertEquals(label, record.getLabel());
    }

    @Test
    void getLabelWhenNull() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        FdoRecord record = new FdoRecord(fdoProfile, null, null);
        assertNull(record.getLabel());
    }

    @Test
    void getId() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);
        assertNull(record.getId());
    }

    @Test
    void setId() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);
        assertNull(record.getId());

        IRI recordId = Values.iri("https://example.org/fdo-record-id");
        record.setId(recordId);
        assertNotNull(record.getId());
        assertEquals(recordId, record.getId());
    }

    @Test
    void getDataRef() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);
        assertNull(record.getDataRef());

        FdoRecord record2 = new FdoRecord(fdoProfile, label, Values.iri("https://example.org/data-ref"));
        assertEquals(Values.iri("https://example.org/data-ref"), record2.getDataRef());
    }

    @Test
    void setDataRef() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);
        assertNull(record.getDataRef());

        IRI dataRef = Values.iri("https://example.org/data-ref");
        record.setDataRef(dataRef.stringValue());
        assertEquals(dataRef, record.getDataRef());
    }

    @Test
    void removeAttribute() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);

        assertNotNull(record.getAttribute(RDFS.LABEL));

        FdoRecord returnRecord = record.removeAttribute(RDFS.LABEL);
        assertNull(returnRecord.getAttribute(RDFS.LABEL));
        assertNull(record.getAttribute(RDFS.LABEL));
    }

    @Test
    void getAttribute() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);

        assertEquals(label, record.getAttribute(RDFS.LABEL).stringValue());
        assertEquals(fdoProfile, record.getAttribute(FdoUtils.PROFILE_IRI));
        assertNull(record.getAttribute(FdoUtils.DATA_REF_IRI));
    }

    @Test
    void setAttribute() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);

        IRI newAttribute = Values.iri("https://example.org/new-attribute");
        record.setAttribute(newAttribute, literal("New Value"));

        assertEquals("New Value", record.getAttribute(newAttribute).stringValue());
    }

    @Test
    void getSchemaUrlWhenSet() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String schemaUrl = "https://example.org/schema.json";
        FdoRecord record = new FdoRecord(fdoProfile, null, Values.iri(schemaUrl));

        record.setAttribute(Values.iri(FDO_URI_PREFIX + SCHEMA_ID),
                literal("{\"$ref\": \"" + schemaUrl + "\"}"));

        assertEquals(schemaUrl, record.getSchemaUrl());
    }

    @Test
    void getSchemaUrlWhenNotSet() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String schemaUrl = "https://example.org/schema.json";
        FdoRecord record = new FdoRecord(fdoProfile, null, Values.iri(schemaUrl));

        assertNull(record.getSchemaUrl());
    }


    @Test
    void buildStatementsWithDataRefAndAggregates() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";

        FdoRecord record = new FdoRecord(fdoProfile, label, Values.iri("https://example.org/data-ref"));
        record.setId(Values.iri("https://example.org/fdo-record-id"));
        record.addAggregatedFdo("https://example.org/aggregated-fdo");
        assertThrows(RuntimeException.class, record::buildStatements);
    }

    @Test
    void buildStatementsWithoutIdSet() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);
        assertThrows(RuntimeException.class, record::buildStatements);
    }

    @Test
    void addAggregatedFdoWithInvalidUriOrHandle() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);
        String aggregateFdo = "not-a-valid-uri-or-handle";
        assertThrows(RuntimeException.class, () -> record.addAggregatedFdo(aggregateFdo));
    }

    @Test
    void addAggregatedFdoWithValidUriAndBuildStatements() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);

        String aggregate1 = "https://w3id.org/np/RAbb0pvoFGiNwcY8nL-qSR93O4AAcfsQRS_TNvLqt0VHg/FdoExample";
        String aggregate2 = "https://w3id.org/np/RAwCj8sM9FkB8Wyz3-i0Fh9Dcq1NniH1sErJBVEkoRQ-o/FdoExample";
        String aggregate3 = "21.T11966/365ff9576c26ca6053db";

        record.addAggregatedFdo(aggregate1);
        record.addAggregatedFdo(aggregate2);
        record.addAggregatedFdo(aggregate3);

        record.setId(Values.iri("https://example.org/fdo-record-id"));

        Set<Statement> statements = record.buildStatements();
        assertTrue(statements.contains(vf.createStatement(
                record.getId(),
                FdoUtils.FDO_HAS_PART,
                Values.iri(aggregate1)
        )));

        assertTrue(statements.contains(vf.createStatement(
                record.getId(),
                FdoUtils.FDO_HAS_PART,
                Values.iri(aggregate2)
        )));

        assertTrue(statements.contains(vf.createStatement(
                record.getId(),
                FdoUtils.FDO_HAS_PART,
                Values.iri(FDO_URI_PREFIX + aggregate3)
        )));
    }

    @Test
    void addDerivedFromFdoAndBuildStatements() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);

        IRI derives1 = Values.iri("https://w3id.org/np/RAbb0pvoFGiNwcY8nL-qSR93O4AAcfsQRS_TNvLqt0VHg/FdoExample");
        IRI derives2 = Values.iri("https://w3id.org/np/RAwCj8sM9FkB8Wyz3-i0Fh9Dcq1NniH1sErJBVEkoRQ-o/FdoExample");

        record.addDerivedFromFdo(derives1);
        record.addDerivedFromFdo(derives2);

        record.setId(Values.iri("https://example.org/fdo-record-id"));
        Set<Statement> statements = record.buildStatements();
        assertTrue(statements.contains(vf.createStatement(
                record.getId(),
                FdoUtils.FDO_DERIVES_FROM,
                derives1
        )));

        assertTrue(statements.contains(vf.createStatement(
                record.getId(),
                FdoUtils.FDO_DERIVES_FROM,
                derives2
        )));
    }

    @Test
    void createUpdatedNanopubRecordWithoutNanopub() {
        IRI fdoProfile = Values.iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "Example FDO Record";
        FdoRecord record = new FdoRecord(fdoProfile, label, null);
        assertThrows(MalformedCryptoElementException.class, record::createUpdatedNanopub);
    }

    @Test
    void createUpdatedNanopubRecordWithNanopub() throws FdoNotFoundException, MalformedCryptoElementException, IOException {
        String id = "https://w3id.org/np/RAproAPfRNhcGoaa0zJ1lsZ_-fRsnlDLLC3nv5guyUWRo/FdoExample";
        FdoRecord record = RetrieveFdo.resolveId(id);

        NanopubCreator creator;

        try (MockedStatic<SignatureUtils> signatureUtilsMock = mockStatic(SignatureUtils.class, CALLS_REAL_METHODS)) {
            signatureUtilsMock
                    .when(() -> SignatureUtils.assertMatchingPubkeys(any(TransformContext.class), any()))
                    .thenAnswer(invocation -> null);

            creator = record.createUpdatedNanopub();
        }

        assertNotNull(creator);
        assertTrue(creator.getAssertionUri().stringValue().matches(TempUriReplacer.tempUri + "\\d+" + "/assertion"));

        Nanopub np = record.getOriginalNanopub();
        assertTrue(creator.getCurrentPubinfoStatements().stream().anyMatch(st ->
                NPX.SUPERSEDES.equals(st.getPredicate()) &&
                        np.getUri().equals(st.getObject())
        ));
        assertEquals(np.getProvenance().size(), creator.getCurrentProvenanceStatements().size());
    }


}