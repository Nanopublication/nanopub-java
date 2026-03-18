package org.nanopub.fdo;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteEntry;
import org.nanopub.vocabulary.HDL;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValidateFdoTest {

    private final String artifactCode = "RA2A-0ojBbTr2aeXUe2Bq4Fn8VLl5Ddr82fOuegiILGkA";
    private final TestSuiteEntry entry = NanopubTestSuite.getLatest()
            .getByArtifactCode(artifactCode)
            .getFirst();
    private Nanopub nanopub;

    @BeforeEach
    void setUp() throws MalformedNanopubException, IOException {
        nanopub = new NanopubImpl(entry.toFile());
        assertNotNull(nanopub);
    }

    @Test
    void validateValidFdo() throws Exception {
        String handle = "21.T11966/82045bd97a0acce88378"; // Please leave it here for documentation.
        String profileId = HDL.NAMESPACE + "21.T11966/996c38676da9ee56f8ab";
        String schemaUrl = "https://typeapi.lab.pidconsortium.net/v1/types/schema/21.T11966/996c38676da9ee56f8ab";
        String jsonResponse = "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"@id\":\"hdl:21.T11966/996c38676da9ee56f8ab\",\"additionalProperties\":true,\"description\":\"The profile which is attached to all Profile-FDOs.\",\"properties\":{\"21.T11966/FdoProfile\":{\"@id\":\"hdl:21.T11966/FdoProfile\",\"title\":\"FdoProfile\",\"type\":\"string\"},\"21.T11966/JsonSchema\":{\"@id\":\"hdl:21.T11966/JsonSchema\",\"title\":\"JsonSchema\",\"type\":\"object\"},\"21.T11966/b5b58656b1fa5aff0505\":{\"@id\":\"hdl:21.T11966/b5b58656b1fa5aff0505\",\"pattern\":\"^([0-9,A-Z,a-z])+(\\\\.([0-9,A-Z,a-z])+)*\\\\/([!-~])+$\",\"title\":\"FdoService\",\"type\":\"string\"}},\"required\":[\"21.T11966/FdoProfile\",\"21.T11966/JsonSchema\",\"21.T11966/b5b58656b1fa5aff0505\"],\"title\":\"FdoProfileProfile\",\"type\":\"object\"}";

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
        String profileId = HDL.NAMESPACE + "21.T11966/82045bd97a0acce88378";
        String schemaUrl = "https://typeapi.lab.pidconsortium.net/v1/types/schema/21.T11966/82045bd97a0acce88378";
        String jsonResponse = "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"@id\":\"hdl:21.T11966/82045bd97a0acce88378\",\"additionalProperties\":true,\"description\":\"The profile for an FDO that follows configuration type 4.\",\"properties\":{\"21.T11966/1639bb8709dda583d357\":{\"@id\":\"hdl:21.T11966/1639bb8709dda583d357\",\"items\":{\"@id\":\"hdl:21.T11966/06a6c27e3e2ef27779ec\",\"pattern\":\"^([0-9,A-Z,a-z])+(\\\\.([0-9,A-Z,a-z])+)*\\\\/([!-~])+$\",\"type\":\"string\"},\"title\":\"DataRefs\",\"type\":\"array\"},\"21.T11966/FdoProfile\":{\"@id\":\"hdl:21.T11966/FdoProfile\",\"type\":\"string\"},\"21.T11966/b5b58656b1fa5aff0505\":{\"@id\":\"hdl:21.T11966/b5b58656b1fa5aff0505\",\"pattern\":\"^([0-9,A-Z,a-z])+(\\\\.([0-9,A-Z,a-z])+)*\\\\/([!-~])+$\",\"type\":\"string\"},\"21.T11966/d3da8ecbafdc54485a40\":{\"@id\":\"hdl:21.T11966/d3da8ecbafdc54485a40\",\"items\":{\"@id\":\"hdl:21.T11966/68763ca08f0783e44efa\",\"pattern\":\"^([0-9,A-Z,a-z])+(\\\\.([0-9,A-Z,a-z])+)*\\\\/([!-~])+$\",\"type\":\"string\"},\"title\":\"MetadataRefs\",\"type\":\"array\"}},\"required\":[\"21.T11966/FdoProfile\",\"21.T11966/b5b58656b1fa5aff0505\"],\"title\":\"FdoConfigType4Profile\",\"type\":\"object\"}";

        final String artifactCode = "RAojp3TaDSNdSvOMUtf8yzYCdTmIGVbq8XIBdy9RvcvhY";
        TestSuiteEntry entry = NanopubTestSuite.getLatest()
                .getByArtifactCode(artifactCode)
                .getFirst();
        Nanopub nanopub = new NanopubImpl(entry.toFile());

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

    @Test
    void shaclValidationCliSuccessful() throws Exception {
        try (MockedStatic<ShaclValidator> mocked = mockStatic(ShaclValidator.class)) {
            mocked.when(() -> ShaclValidator.validateShacl(any(Nanopub.class), any(Nanopub.class))).thenReturn(new ValidationResult());

            ShaclValidator ro = CliRunner.initJc(new ShaclValidator(), new String[]{
                    "-n", String.valueOf(entry.toFile()),
                    "-s", String.valueOf(entry.toFile()) // just any np will do
            });
            ro.run();
        }
    }

    @Test
    void shaclValidationCliUnsuccessful() throws Exception {

        ValidationResult testResult = new ValidationResult();
        testResult.setShacleValidationException(new RepositoryException("TEST"));
        try (MockedStatic<ShaclValidator> mocked = mockStatic(ShaclValidator.class)) {
            mocked.when(() -> ShaclValidator.validateShacl(any(Nanopub.class), any(Nanopub.class))).thenReturn(testResult);

            ShaclValidator ro = CliRunner.initJc(new ShaclValidator(), new String[]{
                    "-n", String.valueOf(entry.toFile()),
                    "-s", String.valueOf(entry.toFile()) // just any np will do
            });
            ro.run();
        }
    }

}