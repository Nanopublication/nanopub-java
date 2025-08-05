package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.fdo.FdoRecord.SCHEMA_ID;
import static org.nanopub.fdo.FdoUtils.FDO_URI_PREFIX;

class FdoRecordTest {

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

}