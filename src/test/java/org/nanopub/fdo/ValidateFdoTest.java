package org.nanopub.fdo;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ValidateFdoTest {

    public final static String MOCKED_NANOPUB_PATH = ValidateFdoTest.class.getResource("/fdo/").getPath().toString();

    @Test
    void validateValidFdo() throws Exception {
        String handle = "21.T11966/82045bd97a0acce88378"; // actually not used any more
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
                mocked.when(() -> HttpClient.newHttpClient()).thenReturn(mockClient);

                assertTrue(ValidateFdo.validate(new FdoRecord(nanopub)).isValid());
            }
        }
    }

    // TODO implement this test with mocks
//    @Test
//    void validateInvalidFdo() throws Exception {
//        String id = "21.T11967/39b0ec87d17a4856c5f7";
//        FdoRecord record = RetrieveFdo.resolveId(id);
//        assertFalse(ValidateFdo.validate(record).isValid());
//    }

}