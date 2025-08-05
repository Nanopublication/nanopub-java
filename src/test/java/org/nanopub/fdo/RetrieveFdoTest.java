package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.FailedApiCallException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class RetrieveFdoTest {

    @Test
    void resolveInNanopubNetworkWithValidHandle() throws FailedApiCallException {
        String handle = "21.T11967/39b0ec87d17a4856c5f7";
        Nanopub np = RetrieveFdo.resolveInNanopubNetwork(handle);
        assertNotNull(np);
    }

    @Test
    void resolveInNanopubNetworkWithInvalidHandle() throws FailedApiCallException {
        String handle = "notAValidHandle";
        Nanopub np = RetrieveFdo.resolveInNanopubNetwork(handle);
        assertNull(np);
    }

    @Test
    void resolveIdWithInvalidHandle() throws FdoNotFoundException {
        String handle = "notAValidHandle";
        assertThrows(FdoNotFoundException.class, () -> RetrieveFdo.resolveId(handle));
    }

    @Test
    void resolveIdWithValidHandleFromNetwork() throws FdoNotFoundException {
        String handle = "21.T11967/39b0ec87d17a4856c5f7";
        FdoRecord fdoRecord = RetrieveFdo.resolveId(handle);
        assertNotNull(fdoRecord);
    }

    @Test
    void resolveIdWithValidHandleFromSystem() throws FdoNotFoundException {
        String handle = "21.T11967/39b0ec87d17a4856c5f7";
        try (MockedStatic<RetrieveFdo> mocked = mockStatic(RetrieveFdo.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> RetrieveFdo.resolveInNanopubNetwork(handle)).thenReturn(null);
            FdoRecord result = RetrieveFdo.resolveId(handle);
            assertNotNull(result);
        }
    }

    @Test
    void resolveIdWithValidHandleIriFromSystem() throws FdoNotFoundException {
        String handle = "21.T11967/39b0ec87d17a4856c5f7";
        String handleIri = FdoUtils.FDO_URI_PREFIX + handle;

        try (MockedStatic<RetrieveFdo> mocked = mockStatic(RetrieveFdo.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> RetrieveFdo.resolveInNanopubNetwork(handleIri)).thenReturn(null);
            FdoRecord result = RetrieveFdo.resolveId(handleIri);
            assertNotNull(result);
        }
    }

    @Test
    void retrieveContentFromIdWithoutDataRef() {
        String handle = "21.T11967/39b0ec87d17a4856c5f7";
        assertThrows(FdoNotFoundException.class, () -> RetrieveFdo.retrieveContentFromId(handle));
    }

    @Test
    void retrieveContentFromIdWithDataRef() throws FdoNotFoundException, URISyntaxException, IOException, InterruptedException {
        IRI profile = iri("https://w3id.org/np/RABPR2eJ7dbuf_OPDLztvRZI-el2_wBFkVBiPCLmr1Q50/test-fdo-profile");
        String label = "ExampleFdo01";
        IRI dataRef = iri("https://raw.githubusercontent.com/Nanopublication/nanopub-java/refs/heads/master/README.md");

        FdoRecord record = new FdoRecord(profile, label, dataRef);

        try (MockedStatic<RetrieveFdo> mocked = mockStatic(RetrieveFdo.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> RetrieveFdo.resolveId(any())).thenReturn(record);

            InputStream contentStream = RetrieveFdo.retrieveContentFromId(any());
            assertNotNull(contentStream);
            String contentString = new String(contentStream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(contentString.contains("# nanopub-java"));
        }
    }

}