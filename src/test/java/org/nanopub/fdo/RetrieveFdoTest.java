package org.nanopub.fdo;

import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.QueryAccess;
import org.nanopub.utils.MockFileService;
import org.nanopub.utils.MockFileServiceExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockFileServiceExtension.class)
class RetrieveFdoTest {

    @Test
    void resolveInNanopubNetworkWithValidHandle() throws FailedApiCallException, MalformedNanopubException, IOException {
        String handle = "21.T11967/39b0ec87d17a4856c5f7";
        Nanopub nanopub = new NanopubImpl(new File(Objects.requireNonNull(MockFileService.getFdoNanopubFromHandle(handle))));

        try (MockedStatic<GetNanopub> mockedStatic = mockStatic(GetNanopub.class, CALLS_REAL_METHODS);
             MockedStatic<QueryAccess> mockedQueryAccess = mockStatic(QueryAccess.class)) {
            ApiResponse mockedApiResponse = mock();

            List<ApiResponseEntry> responseEntryList = new ArrayList<>();
            ApiResponseEntry apiResponseEntry = new ApiResponseEntry();
            apiResponseEntry.add("np", nanopub.getUri().toString());
            responseEntryList.add(apiResponseEntry);

            when(mockedApiResponse.getData()).thenReturn(responseEntryList);
            mockedQueryAccess.when(() -> QueryAccess.get(any(), any())).thenReturn(mockedApiResponse);

            Nanopub nanopubFromId = new NanopubImpl(new File(MockFileService.getValidAndSignedNanopubFromId(TrustyUriUtils.getArtifactCode(mockedApiResponse.getData().getFirst().get("np")))));
            mockedStatic.when(() -> GetNanopub.get(nanopub.getUri().toString())).thenReturn(nanopubFromId);

            Nanopub retrievedNanopub = RetrieveFdo.resolveInNanopubNetwork(handle);
            assertEquals(nanopub, retrievedNanopub);
        }
    }

    @Test
    void resolveInNanopubNetworkWithInvalidHandle() throws FailedApiCallException {
        String handle = "notAValidHandle";
        try (MockedStatic<QueryAccess> mockedQueryAccess = mockStatic(QueryAccess.class, CALLS_REAL_METHODS)) {
            mockedQueryAccess.when(() -> QueryAccess.get(any(), any())).thenReturn(mock(ApiResponse.class));

            Nanopub np = RetrieveFdo.resolveInNanopubNetwork(handle);
            assertNull(np);
        }
    }

    @Test
    void resolveIdWithInvalidHandle() {
        String handle = "notAValidHandle";
        assertThrows(FdoNotFoundException.class, () -> RetrieveFdo.resolveId(handle));
    }

    @Test
    void resolveIdWithValidHandleFromNetwork() throws FdoNotFoundException, MalformedNanopubException, IOException {
        String handle = "21.T11967/39b0ec87d17a4856c5f7";
        Nanopub nanopub = new NanopubImpl(new File(Objects.requireNonNull(MockFileService.getFdoNanopubFromHandle(handle))));

        try (MockedStatic<RetrieveFdo> mockedStatic = mockStatic(RetrieveFdo.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> RetrieveFdo.resolveInNanopubNetwork(handle)).thenReturn(nanopub);
            FdoRecord fdoRecord = RetrieveFdo.resolveId(handle);
            assertNotNull(fdoRecord);
            assertEquals(handle, FdoUtils.extractHandle(fdoRecord.getId()));
        }
    }

    @Test
    void resolveIdWithValidHandleFromSystem() throws FdoNotFoundException, MalformedNanopubException, IOException {
        String handle = "21.T11967/39b0ec87d17a4856c5f7";
        Nanopub nanopub = new NanopubImpl(new File(Objects.requireNonNull(MockFileService.getFdoNanopubFromHandle(handle))));
        FdoRecord fdoRecord = new FdoRecord(nanopub);

        try (MockedStatic<RetrieveFdo> mocked = mockStatic(RetrieveFdo.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> RetrieveFdo.resolveInNanopubNetwork(handle)).thenReturn(null);
            mocked.when(() -> RetrieveFdo.resolveInHandleSystem(handle)).thenReturn(fdoRecord);

            FdoRecord retrievedFdoRecord = RetrieveFdo.resolveId(handle);
            assertEquals(fdoRecord, retrievedFdoRecord);
        }
    }

    @Test
    void resolveIdWithValidHandleIriFromSystem() throws FdoNotFoundException, MalformedNanopubException, IOException {
        String handle = "21.T11967/39b0ec87d17a4856c5f7";
        String handleIri = FdoUtils.FDO_URI_PREFIX + handle;
        Nanopub nanopub = new NanopubImpl(new File(Objects.requireNonNull(MockFileService.getFdoNanopubFromHandle(handle))));
        FdoRecord fdoRecord = new FdoRecord(nanopub);

        try (MockedStatic<RetrieveFdo> mocked = mockStatic(RetrieveFdo.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> RetrieveFdo.resolveInNanopubNetwork(handleIri)).thenReturn(null);
            mocked.when(() -> RetrieveFdo.resolveInHandleSystem(handle)).thenReturn(fdoRecord);

            FdoRecord retrievedFdoRecord = RetrieveFdo.resolveId(handleIri);
            assertEquals(fdoRecord, retrievedFdoRecord);
        }
    }

    @Test
    void resolveIdNotFoundAnywhere() {
        String handle = "21.T11967/39b0ec87d17a4856c5f7";
        try (MockedStatic<RetrieveFdo> retrieveFdoMock = mockStatic(RetrieveFdo.class, CALLS_REAL_METHODS);
             MockedStatic<FdoUtils> fdoUtilsMock = mockStatic(FdoUtils.class)) {
            retrieveFdoMock.when(() -> RetrieveFdo.resolveInNanopubNetwork(any())).thenReturn(null);
            fdoUtilsMock.when(() -> FdoUtils.looksLikeHandle(any())).thenReturn(false);
            fdoUtilsMock.when(() -> FdoUtils.isHandleIri(any())).thenReturn(false);
            assertThrows(FdoNotFoundException.class, () -> RetrieveFdo.resolveId(handle));
        }
    }

    @Test
    void retrieveContentFromIdWithoutDataRef() {
        String handle = "21.T11967/39b0ec87d17a4856c5f7";
        assertThrows(FdoNotFoundException.class, () -> RetrieveFdo.retrieveContentFromId(handle));
    }

    @Test
    void retrieveContentFromIdWithDataRef() throws FdoNotFoundException, URISyntaxException, IOException, InterruptedException, MalformedNanopubException {
        String fdoNanopubId = "https://w3id.org/np/RA1KlMiWjiJtQiU2R6twcLtvZv93KOqJGoXuk-HjkgiNE";
        Nanopub fdoNanopub = new NanopubImpl(new File(Objects.requireNonNull(MockFileService.getValidAndSignedNanopubFromId(TrustyUriUtils.getArtifactCode(fdoNanopubId)))));
        IRI dataRef = iri("https://raw.githubusercontent.com/knowledgepixels/nanodash/refs/heads/master/README.md");

        FdoRecord record = new FdoRecord(fdoNanopub);
        assertEquals(dataRef, record.getDataRef());

        try (MockedStatic<RetrieveFdo> mocked = mockStatic(RetrieveFdo.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> RetrieveFdo.resolveId(fdoNanopubId)).thenReturn(record);

            // TODO mock the HttpRequest and the HttpResponse

            InputStream contentStream = RetrieveFdo.retrieveContentFromId(fdoNanopubId);
            assertNotNull(contentStream);

            String contentString = new String(contentStream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(contentString.contains("![logo](nanodash.png)"));
        }
    }

}