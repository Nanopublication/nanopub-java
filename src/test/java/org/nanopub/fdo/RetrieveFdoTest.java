package org.nanopub.fdo;

import net.trustyuri.TrustyUriUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
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

        InputStream content = IOUtils.toInputStream(getMockedContentFromIdWithDataRef(), StandardCharsets.UTF_8);

        FdoRecord record = new FdoRecord(fdoNanopub);
        assertEquals(dataRef, record.getDataRef());

        try (MockedStatic<RetrieveFdo> mocked = mockStatic(RetrieveFdo.class, CALLS_REAL_METHODS)) {
            try (MockedStatic<HttpClient> httpClientStaticMock = mockStatic(HttpClient.class)) {
                mocked.when(() -> HttpClient.newHttpClient()).thenThrow(RuntimeException.class); // workaround, since the mocked method from next line is executed anyway.
                mocked.when(() -> RetrieveFdo.resolveId(fdoNanopubId)).thenReturn(record);
            }

            // mock the HttpRequest and the HttpResponse
            try (MockedStatic<HttpClient> httpClientStaticMock = mockStatic(HttpClient.class)) {
                HttpClient mockClient = mock();
                HttpResponse<InputStream> httpResponse = mock();
                when(httpResponse.body()).thenReturn(content);
                when(mockClient.send(Mockito.any(),  ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any())).thenReturn(httpResponse);
                mocked.when(() -> HttpClient.newHttpClient()).thenReturn(mockClient);

                InputStream contentStream = RetrieveFdo.retrieveContentFromId(fdoNanopubId);
                assertNotNull(contentStream);

                String contentString = new String(contentStream.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(contentString.contains("![logo](nanodash.png)"));
            }
        }
    }

    private String getMockedContentFromIdWithDataRef() {
        return "Nanodash\n" +
                "========\n" +
                "\n" +
                "![logo](nanodash.png)\n" +
                "\n" +
                "Nanodash was previously called Nanobench.\n" +
                "\n" +
                "Nanodash is a client to browse and publish nanopublications.\n" +
                "\n" +
                "\n" +
                "### Online Instances\n" +
                "\n" +
                "You can use Nanodash by login in via ORCID in one of the online instances:\n" +
                "\n" +
                "- https://nanodash.petapico.org/\n" +
                "- https://nanodash.knowledgepixels.com/\n" +
                "- https://nanodash.np.trustyuri.net/\n" +
                "\n" +
                "\n" +
                "### Local Installation\n" +
                "\n" +
                "To use Nanodash locally, see the [installation instructions with Docker](INSTALL-with-Docker.md).\n" +
                "\n" +
                "\n" +
                "### Screenshot\n" +
                "\n" +
                "This screenshot of Nanodash is showing its publishing feature with auto-complete-powered forms generated from semantic templates:\n" +
                "\n" +
                "![screenshot of Nanodash showing the publishing feature](screenshot.png)\n" +
                "\n" +
                "\n" +
                "### Tutorials\n" +
                "\n" +
                "[This demo](https://knowledgepixels.com/nanopub-demo/) gives you a quick hands-on introduction into nanopublications via the Nanodash interface, including a video:\n" +
                "\n" +
                "- [Demo video on nanopublications and Nanodash](https://youtu.be/_wmXHgC706I)\n" +
                "\n" +
                "You can also check out these older video tutorials to learn more about Nanodash and how it can be used (the videos are from the time when Nanodash was still called Nanobench):\n" +
                "\n" +
                "- [Nanobench Tutorial 1: The Nanopublication Ecosystem](https://youtu.be/wPAd9wPkvEg)\n" +
                "- (This second video is a bit outdated. You no longer need to install Nanobench/Nanodash locally, but you can use the link of the online instance above and skip this video.)\n" +
                "  [Nanobench Tutorial 2: Setting up Nanobench](https://youtu.be/GG21BhzxaQk)\n" +
                "- [Nanobench Tutorial 3: Browsing and Publishing Nanopublications](https://youtu.be/-UB28HVEO38)\n" +
                "- [Nanobench Tutorial 4: Creating Templates](https://youtu.be/gQk8ItHr38U)\n" +
                "- [Nanobench Tutorial 5: Using the Query Services](https://youtu.be/U200GuqOBso)\n" +
                "\n" +
                "\n" +
                "### License\n" +
                "\n" +
                "Copyright (C) 2022-2024 Knowledge Pixels\n" +
                "\n" +
                "This program is free software: you can redistribute it and/or modify\n" +
                "it under the terms of the GNU Affero General Public License as\n" +
                "published by the Free Software Foundation, either version 3 of the\n" +
                "License, or (at your option) any later version.\n" +
                "\n" +
                "This program is distributed in the hope that it will be useful,\n" +
                "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
                "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
                "GNU Affero General Public License for more details.\n" +
                "\n" +
                "You should have received a copy of the GNU Affero General Public License\n" +
                "along with this program.  If not, see https://www.gnu.org/licenses/.\n";
    }

}