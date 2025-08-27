package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.FDOF;
import org.nanopub.vocabulary.HDL;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

class FdoUtilsTest {

    private static final String VALID_HANDLE = "21.T11966/FdoProfile";

    @Test
    void extractHandleWithValidHandleIri() {
        String suffix = VALID_HANDLE;
        Resource iri = iri(HDL.NAMESPACE + suffix);
        String result = FdoUtils.extractHandle(iri);
        assertEquals(suffix, result);
    }

    @Test
    void extractHandleWithNonHandleIri() {
        Resource iri = iri("https://example.com/fdo/resource");
        String result = FdoUtils.extractHandle(iri);
        assertEquals("https://example.com/fdo/resource", result);
    }

    @Test
    void looksLikeHandleWithValidHandle() {
        boolean result = FdoUtils.looksLikeHandle(VALID_HANDLE);
        assertTrue(result);
    }

    @Test
    void looksLikeHandleWithInvalidHandle() {
        String handle = "21.T11966FdoProfile";
        boolean result = FdoUtils.looksLikeHandle(handle);
        assertFalse(result);
    }

    @Test
    void looksLikeUrlWithValidUrl() {
        String url = HDL.NAMESPACE + VALID_HANDLE;
        boolean result = FdoUtils.looksLikeUrl(url);
        assertTrue(result);
    }

    @Test
    void looksLikeUrlWithInvalidUrl() {
        String url = "hdl.handle.net/21.T11966/FdoProfile";
        boolean result = FdoUtils.looksLikeUrl(url);
        assertFalse(result);
    }

    @Test
    void isHandleIriWithValidHandleIri() {
        Resource iri = iri(HDL.NAMESPACE + VALID_HANDLE);
        boolean result = FdoUtils.isHandleIri(iri);
        assertTrue(result);
    }

    @Test
    void isHandleIriWithNonHandleIri() {
        Resource iri = iri("https://example.com/fdo/resource");
        boolean result = FdoUtils.isHandleIri(iri);
        assertFalse(result);
    }

    @Test
    void createIriWithHandle() {
        String handle = VALID_HANDLE;
        IRI result = FdoUtils.createIri(handle);
        assertEquals(HDL.NAMESPACE + handle, result.stringValue());
    }

    @Test
    void createIriWithUrl() {
        String url = HDL.NAMESPACE + VALID_HANDLE;
        IRI result = FdoUtils.createIri(url);
        assertEquals(HDL.NAMESPACE + VALID_HANDLE, result.stringValue());
    }

    @Test
    void createIriWithInvalidInput() {
        String invalidInput = "invalid-handle";
        assertThrows(IllegalArgumentException.class, () -> FdoUtils.createIri(invalidInput));
    }

    @Test
    void toIriWithHandle() {
        String handle = VALID_HANDLE;
        IRI result = FdoUtils.toIri(handle);
        assertEquals(iri(HDL.NAMESPACE + handle), result);
    }

    @Test
    void isFdoNanopubWithValidFdoNanopub() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(TestUtils.anyIri, RDF.TYPE, FDOF.FAIR_DIGITAL_OBJECT);
        creator.addProvenanceStatement(TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(TestUtils.anyIri, TestUtils.anyIri);

        Nanopub np = creator.finalizeNanopub(true);
        assertTrue(FdoUtils.isFdoNanopub(np));
    }

    @Test
    void isFdoNanopubWithInvalidFdoNanopub() throws MalformedNanopubException {
        Nanopub np = TestUtils.createNanopub();
        assertFalse(FdoUtils.isFdoNanopub(np));
    }

}