package org.nanopub.fdo;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.nanopub.fdo.rest.gson.Data;
import org.nanopub.fdo.rest.gson.ParsedJsonResponse;
import org.nanopub.fdo.rest.gson.Value;
import org.nanopub.vocabulary.HDL;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FdoProfileSchemaLoaderTest {

    @Test
    void findJsonLocation_returnsJsonHrefFromLocationsXml() {
        ParsedJsonResponse r = new ParsedJsonResponse();
        Value v = new Value();
        v.type = "10320/loc";
        v.data = new Data();
        v.data.value = "<locations>\n<location href=\"https://dtr.example.org/objects/21.T1/abc\" weight=\"1\" view=\"json\" />\n<location href=\"https://dtr.example.org/#objects/21.T1/abc\" weight=\"0\" view=\"ui\" />\n</locations>";
        r.values = new Value[]{v};

        assertEquals("https://dtr.example.org/objects/21.T1/abc", FdoProfileSchemaLoader.findJsonLocation(r));
    }

    @Test
    void findJsonLocation_handlesUppercaseView() {
        ParsedJsonResponse r = new ParsedJsonResponse();
        Value v = new Value();
        v.type = "10320/loc";
        v.data = new Data();
        v.data.value = "<locations><location href=\"https://dtr.example.org/x\" id=\"0\" view=\"JSON\" weight=\"1\"/></locations>";
        r.values = new Value[]{v};

        assertEquals("https://dtr.example.org/x", FdoProfileSchemaLoader.findJsonLocation(r));
    }

    @Test
    void parsePropertyMap_dtrStyleProperties() {
        String json = "{\"identifier\":\"21.T11148/profile\",\"name\":\"X\",\"properties\":["
                + "{\"name\":\"fdoProfile\",\"identifier\":\"21.T11148/21e9228a604c7b37dfdf\"},"
                + "{\"name\":\"digitalObjectName\",\"identifier\":\"21.T11148/4f2f5d61b57fb556aad9\"}"
                + "]}";

        Map<String, IRI> map = FdoProfileSchemaLoader.parsePropertyMap(json);

        assertEquals(iri(HDL.NAMESPACE + "21.T11148/21e9228a604c7b37dfdf"), map.get("fdoProfile"));
        assertEquals(iri(HDL.NAMESPACE + "21.T11148/4f2f5d61b57fb556aad9"), map.get("digitalObjectName"));
        assertEquals(2, map.size());
    }

    @Test
    void parsePropertyMap_gwdgStyleSchemaProperties() {
        String json = "{\"Identifier\":\"21.T11966/profile\",\"Schema\":{\"Properties\":["
                + "{\"Name\":\"21.T11966/FdoProfile\",\"Type\":\"21.T11966/FdoProfile\"},"
                + "{\"Name\":\"21.T11966/b5b58656b1fa5aff0505\",\"Type\":\"21.T11966/b5b58656b1fa5aff0505\"}"
                + "]}}";

        Map<String, IRI> map = FdoProfileSchemaLoader.parsePropertyMap(json);

        assertEquals(iri(HDL.NAMESPACE + "21.T11966/FdoProfile"), map.get("21.T11966/FdoProfile"));
        assertEquals(iri(HDL.NAMESPACE + "21.T11966/b5b58656b1fa5aff0505"), map.get("21.T11966/b5b58656b1fa5aff0505"));
        assertEquals(2, map.size());
    }

    @Test
    void parsePropertyMap_jsonSchemaObjectShape() {
        String json = "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"properties\":{"
                + "\"21.T11966/1639bb8709dda583d357\":{\"@id\":\"hdl:21.T11966/1639bb8709dda583d357\",\"title\":\"DataRefs\",\"type\":\"array\"},"
                + "\"21.T11966/FdoProfile\":{\"@id\":\"hdl:21.T11966/FdoProfile\",\"type\":\"string\"}"
                + "}}";

        Map<String, IRI> map = FdoProfileSchemaLoader.parsePropertyMap(json);

        IRI dataRefsIri = iri(HDL.NAMESPACE + "21.T11966/1639bb8709dda583d357");
        assertEquals(dataRefsIri, map.get("21.T11966/1639bb8709dda583d357"));
        assertEquals(dataRefsIri, map.get("DataRefs"));
        assertEquals(iri(HDL.NAMESPACE + "21.T11966/FdoProfile"), map.get("21.T11966/FdoProfile"));
    }

    @Test
    void parsePropertyMap_titlesIndexedAsAlternateKeys() {
        // GWDG-style with Title
        String gwdg = "{\"Schema\":{\"Properties\":["
                + "{\"Name\":\"21.T11966/1639bb8709dda583d357\",\"Title\":\"DataRefs\"}"
                + "]}}";
        Map<String, IRI> m1 = FdoProfileSchemaLoader.parsePropertyMap(gwdg);
        IRI dataRefs = iri(HDL.NAMESPACE + "21.T11966/1639bb8709dda583d357");
        assertEquals(dataRefs, m1.get("21.T11966/1639bb8709dda583d357"));
        assertEquals(dataRefs, m1.get("DataRefs"));

        // DTR array-style without title → only name key
        String dissco = "{\"properties\":[{\"name\":\"fdoProfile\",\"identifier\":\"21.T11148/abc\"}]}";
        Map<String, IRI> m2 = FdoProfileSchemaLoader.parsePropertyMap(dissco);
        assertEquals(1, m2.size());
    }

    @Test
    void parsePropertyMap_emptyOnUnknownShape() {
        assertTrue(FdoProfileSchemaLoader.parsePropertyMap("{\"foo\":\"bar\"}").isEmpty());
        assertTrue(FdoProfileSchemaLoader.parsePropertyMap("[]").isEmpty());
    }

    @Test
    void parsePropertyMap_skipsEntriesWithNonHandleIdentifiers() {
        String json = "{\"properties\":["
                + "{\"name\":\"ok\",\"identifier\":\"21.T11148/valid\"},"
                + "{\"name\":\"bad\",\"identifier\":\"not-a-handle\"}"
                + "]}";

        Map<String, IRI> map = FdoProfileSchemaLoader.parsePropertyMap(json);

        assertTrue(map.containsKey("ok"));
        assertFalse(map.containsKey("bad"));
    }

    @Test
    void loadPropertyMap_returnsEmptyOnResolutionFailure() throws Exception {
        HttpResponse<String> badResponse = mock();
        when(badResponse.body()).thenReturn("not json");

        try (MockedStatic<HttpClient> httpStatic = mockStatic(HttpClient.class)) {
            HttpClient mockClient = mock();
            when(mockClient.send(Mockito.any(), eq(HttpResponse.BodyHandlers.ofString())))
                    .thenReturn(badResponse);
            httpStatic.when(HttpClient::newHttpClient).thenReturn(mockClient);

            Map<String, IRI> map = FdoProfileSchemaLoader.loadPropertyMap(
                    iri("https://hdl.handle.net/21.T11148/profile"));

            assertTrue(map.isEmpty());
        }
    }

    @Test
    void loadPropertyMap_resolvesViaHandleSystemAndDtr() throws Exception {
        String handleResponseBody = "{\"responseCode\":1,\"handle\":\"21.T11148/profile\",\"values\":["
                + "{\"index\":1,\"type\":\"10320/loc\",\"data\":{\"format\":\"string\",\"value\":"
                + "\"<locations><location href=\\\"https://dtr.example.org/objects/21.T11148/profile\\\" view=\\\"json\\\" weight=\\\"1\\\"/></locations>\"}}"
                + "]}";
        String dtrBody = "{\"properties\":[{\"name\":\"fdoProfile\",\"identifier\":\"21.T11148/abc\"}]}";

        HttpResponse<String> handleResp = mock();
        when(handleResp.body()).thenReturn(handleResponseBody);
        HttpResponse<String> dtrResp = mock();
        when(dtrResp.body()).thenReturn(dtrBody);

        try (MockedStatic<HttpClient> httpStatic = mockStatic(HttpClient.class)) {
            HttpClient mockClient = mock();
            when(mockClient.send(Mockito.any(), eq(HttpResponse.BodyHandlers.ofString())))
                    .thenReturn(handleResp)
                    .thenReturn(dtrResp);
            httpStatic.when(HttpClient::newHttpClient).thenReturn(mockClient);

            Map<String, IRI> map = FdoProfileSchemaLoader.loadPropertyMap(
                    iri("https://doi.org/21.T11148/profile"));

            assertEquals(iri(HDL.NAMESPACE + "21.T11148/abc"), map.get("fdoProfile"));
            assertEquals(1, map.size());
        }
    }

}
