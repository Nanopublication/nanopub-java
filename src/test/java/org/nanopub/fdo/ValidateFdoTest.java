package org.nanopub.fdo;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ValidateFdoTest {

    public final static String MOCKED_NANOPUB_PATH = Objects.requireNonNull(ValidateFdoTest.class.getResource("/fdo/")).getPath();

    @Test
    void validateValidFdo() throws Exception {
        String handle = "21.T11966/82045bd97a0acce88378"; // Please leave it here for documentation.
        String profileId = "https://hdl.handle.net/21.T11966/996c38676da9ee56f8ab";
        String schemaUrl = "https://typeapi.lab.pidconsortium.net/v1/types/schema/21.T11966/996c38676da9ee56f8ab";
        String jsonResponse = "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"@id\":\"hdl:21.T11966/996c38676da9ee56f8ab\",\"additionalProperties\":true,\"description\":\"The profile which is attached to all Profile-FDOs.\",\"properties\":{\"21.T11966/FdoProfile\":{\"@id\":\"hdl:21.T11966/FdoProfile\",\"title\":\"FdoProfile\",\"type\":\"string\"},\"21.T11966/JsonSchema\":{\"@id\":\"hdl:21.T11966/JsonSchema\",\"title\":\"JsonSchema\",\"type\":\"object\"},\"21.T11966/b5b58656b1fa5aff0505\":{\"@id\":\"hdl:21.T11966/b5b58656b1fa5aff0505\",\"pattern\":\"^([0-9,A-Z,a-z])+(\\\\.([0-9,A-Z,a-z])+)*\\\\/([!-~])+$\",\"title\":\"FdoService\",\"type\":\"string\"}},\"required\":[\"21.T11966/FdoProfile\",\"21.T11966/JsonSchema\",\"21.T11966/b5b58656b1fa5aff0505\"],\"title\":\"FdoProfileProfile\",\"type\":\"object\"}";

        Nanopub nanopub = new NanopubImpl(new File(MOCKED_NANOPUB_PATH + "validFdo.trig"));

        try (MockedStatic<RetrieveFdo> mocked = mockStatic(RetrieveFdo.class)) {
            FdoRecord mockSchemaRecord = mock();
            when(mockSchemaRecord.getSchemaUrl()).thenReturn(schemaUrl);
            mocked.when(() -> RetrieveFdo.resolveId(profileId)).thenReturn(mockSchemaRecord);
            HttpResponse<String> mockJsonResponse = mock(HttpResponse.class);
            when(mockJsonResponse.body()).thenReturn(jsonResponse);
            try (MockedStatic<HttpClient> httpClientStaticMock = mockStatic(HttpClient.class)) {
                HttpClient mockClient = mock();
                when(mockClient.send(Mockito.any(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockJsonResponse);
                mocked.when(HttpClient::newHttpClient).thenReturn(mockClient);

                assertTrue(ValidateFdo.validate(new FdoRecord(nanopub)).isValid());
            }
        }
    }

    @Test
    void validateInvalidFdo() throws Exception {
        String handle = "21.T11967/39b0ec87d17a4856c5f7";  // Please leave it here for documentation.
        String profileId = "https://hdl.handle.net/21.T11966/82045bd97a0acce88378";
        String schemaUrl = "https://typeapi.lab.pidconsortium.net/v1/types/schema/21.T11966/82045bd97a0acce88378";
        String jsonResponse = "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"@id\":\"hdl:21.T11966/82045bd97a0acce88378\",\"additionalProperties\":true,\"description\":\"The profile for an FDO that follows configuration type 4.\",\"properties\":{\"21.T11966/1639bb8709dda583d357\":{\"@id\":\"hdl:21.T11966/1639bb8709dda583d357\",\"items\":{\"@id\":\"hdl:21.T11966/06a6c27e3e2ef27779ec\",\"pattern\":\"^([0-9,A-Z,a-z])+(\\\\.([0-9,A-Z,a-z])+)*\\\\/([!-~])+$\",\"type\":\"string\"},\"title\":\"DataRefs\",\"type\":\"array\"},\"21.T11966/FdoProfile\":{\"@id\":\"hdl:21.T11966/FdoProfile\",\"type\":\"string\"},\"21.T11966/b5b58656b1fa5aff0505\":{\"@id\":\"hdl:21.T11966/b5b58656b1fa5aff0505\",\"pattern\":\"^([0-9,A-Z,a-z])+(\\\\.([0-9,A-Z,a-z])+)*\\\\/([!-~])+$\",\"type\":\"string\"},\"21.T11966/d3da8ecbafdc54485a40\":{\"@id\":\"hdl:21.T11966/d3da8ecbafdc54485a40\",\"items\":{\"@id\":\"hdl:21.T11966/68763ca08f0783e44efa\",\"pattern\":\"^([0-9,A-Z,a-z])+(\\\\.([0-9,A-Z,a-z])+)*\\\\/([!-~])+$\",\"type\":\"string\"},\"title\":\"MetadataRefs\",\"type\":\"array\"}},\"required\":[\"21.T11966/FdoProfile\",\"21.T11966/b5b58656b1fa5aff0505\"],\"title\":\"FdoConfigType4Profile\",\"type\":\"object\"}";

        Nanopub nanopub = new NanopubImpl(new File(MOCKED_NANOPUB_PATH + "invalidFdo.trig"));

        try (MockedStatic<RetrieveFdo> mocked = mockStatic(RetrieveFdo.class)) {
            FdoRecord mockSchemaRecord = mock();
            when(mockSchemaRecord.getSchemaUrl()).thenReturn(schemaUrl);
            mocked.when(() -> RetrieveFdo.resolveId(profileId)).thenReturn(mockSchemaRecord);
            HttpResponse<String> mockJsonResponse = mock(HttpResponse.class);
            when(mockJsonResponse.body()).thenReturn(jsonResponse);
            try (MockedStatic<HttpClient> httpClientStaticMock = mockStatic(HttpClient.class)) {
                HttpClient mockClient = mock();
                when(mockClient.send(Mockito.any(), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockJsonResponse);
                mocked.when(HttpClient::newHttpClient).thenReturn(mockClient);

                assertFalse(ValidateFdo.validate(new FdoRecord(nanopub)).isValid());
            }
        }
    }

}