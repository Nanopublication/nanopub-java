package org.nanopub.templates;

import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.templates.FillRequest.Mode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.templates.TemplateComplianceCheckerTest.QUERY;
import static org.nanopub.templates.TemplateComplianceCheckerTest.RESOURCES;
import static org.nanopub.templates.TemplateComplianceCheckerTest.load;

/**
 * Tests for {@link FillRequest}.
 */
class FillRequestTest {

    private static final String T = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final String PR = "https://w3id.org/np/RAProvProvProvProvProvProvProvProvProvProvPro";
    private static final String PI = "https://w3id.org/np/RAPubinfoPubinfoPubinfoPubinfoPubinfoPubinf0";
    private static final String PI2 = "https://w3id.org/np/RAPubinfoPubinfoPubinfoPubinfoPubinfoPubinf2";
    private static final String OLD = "https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV";
    private static final String ME = "https://orcid.org/0000-0000-0000-0001";

    private static Map<String, String> params(String... keysAndValues) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) params.put(keysAndValues[i], keysAndValues[i + 1]);
        return params;
    }

    private static List<String> problems(Map<String, String> params) {
        return assertThrows(TemplateFillException.class, () -> FillRequest.fromParams(params)).getProblems();
    }

    @Test
    void requestIsReadFromTheParametersOfAFormLink() throws Exception {
        FillRequest request = FillRequest.fromParams(params("template", T, "param_name", "Apple", "param_keyword", "fruit", "param_keyword__1", "red",
                "param_title", "Titel", "param_title__lang", "de",
                "prtemplate", PR, "prparam_agent", ME,
                "pitemplate1", PI, "piparam1_license", "https://creativecommons.org/publicdomain/zero/1.0/", "pitemplate2", PI2,
                "supersede", OLD, "target-namespace", "https://example.org/np/",
                "creator", ME, "creator-name", "Me", "space", "https://example.org/space", "namespace", "https://example.org/ns/"));

        assertEquals(T, request.getTemplate());
        assertEquals(Map.of("name", "Apple", "keyword", "fruit", "keyword__1", "red", "title", "Titel", "title__lang", "de"), request.getValues().toParams());
        assertEquals(new RawValue("Titel", "de"), request.getValues().get("title"));
        assertEquals(PR, request.getProvenanceTemplate());
        assertEquals(Map.of("agent", ME), request.getProvenanceValues().toParams());
        assertEquals(List.of(PI, PI2), request.getPubinfoTemplates());
        assertEquals(Map.of("license", "https://creativecommons.org/publicdomain/zero/1.0/"), request.getPubinfoValues(PI).toParams());
        assertTrue(request.getPubinfoValues(PI2).isEmpty());
        assertNull(request.getPubinfoValues(T));
        assertEquals(Mode.SUPERSEDE, request.getMode());
        assertEquals(OLD, request.getSource());
        assertEquals("https://example.org/np/", request.getTargetNamespace());
        assertEquals(ME, request.getCreator().stringValue());
        assertEquals("Me", request.getCreatorName());
        assertEquals("https://example.org/space", request.getSpace());
        assertEquals("https://example.org/ns/", request.getNamespace());
    }

    @Test
    void requestIsWrittenAsItIsRead() throws Exception {
        Map<String, String> params = params("template", T, "param_name", "Apple", "param_title", "Titel", "param_title__lang", "de", "param_comment", "",
                "prtemplate", PR, "prparam_agent", ME, "pitemplate1", PI, "piparam1_license", "https://example.org/l", "pitemplate2", PI2,
                "override", OLD, "target-namespace", "https://example.org/np/", "creator", ME, "creator-name", "Me",
                "space", "https://example.org/space", "namespace", "https://example.org/ns/");
        FillRequest request = FillRequest.fromParams(params);

        assertEquals(params, request.toParams());
        assertEquals(params, FillRequest.fromJson(request.toJson()).toParams());
        assertEquals(params, FillRequest.fromUrl("https://nanodash.example.org/publish?" + request.toQueryString()).toParams());
        assertEquals(params, FillRequest.parse(request.toJson()).toParams());
        assertEquals(params, FillRequest.parse(request.toQueryString()).toParams());
    }

    @Test
    void modesAreReadWithAndWithoutTheAssertionOnlySuffix() throws Exception {
        assertEquals(Mode.DERIVE, FillRequest.fromParams(params("template", T, "derive", OLD)).getMode());
        assertEquals(Mode.OVERRIDE, FillRequest.fromParams(params("template", T, "override-a", OLD)).getMode());
        assertEquals(Mode.SUPERSEDE, FillRequest.fromParams(params("template", T, "supersede-a", OLD)).getMode());
        assertNull(FillRequest.fromParams(params("template", T)).getMode());
        assertEquals(List.of("Only one of 'supersede', 'override' and 'derive' can be given, not both 'supersede' and 'derive'"),
                problems(params("template", T, "supersede", OLD, "derive", OLD)));
    }

    @Test
    void parametersThatSayNothingAboutTheNanopublicationAreIgnored() throws Exception {
        FillRequest request = FillRequest.fromParams(params("template", T, "link-message", "Please fill in", "postpub-redirect-url", "https://example.org/",
                "context", "https://example.org/space", "locked", "param_name", "template-version", "exact", "prtemplate", ""));
        assertEquals(Map.of("template", T), request.toParams());
    }

    @Test
    void parametersARequestCannotHonourAreRefused() {
        assertEquals(List.of("No template is given (parameter 'template')"), problems(params("param_name", "Apple")));
        List<String> problems = problems(params("template", T, "use", OLD, "fill-query", "x", "template-version", "latest", "parm_name", "Apple",
                "piparam2_license", "x", "creator", "not an iri"));
        assertEquals(List.of(
                "The parameter 'use' is not supported: values are not read from an existing nanopublication; give them as parameters",
                "The parameter 'fill-query' is not supported: values are not read from a query; give them as parameters",
                "The parameter 'template-version=latest' is not supported: a template is used at exactly the version named",
                "Unknown parameter 'parm_name'",
                "The creator is not an IRI: not an iri",
                "There are values for the publication info template 2 (piparam2_...), but no 'pitemplate2' names that template"), problems);
    }

    @Test
    void linkIsDecoded() throws Exception {
        FillRequest request = FillRequest.fromUrl("https://nanodash.example.org/publish?template=" + T.replace(":", "%3A")
                + "&param_name=An+apple+%26+a+pear&param_sparql=select%20*%20where%20%7B%7D&param_empty=#section");
        assertEquals(T, request.getTemplate());
        assertEquals("An apple & a pear", request.getValues().get("name").text());
        assertEquals("select * where {}", request.getValues().get("sparql").text());
        assertTrue(request.getValues().get("empty").isEmpty());

        assertEquals(List.of("The parameter 'param_name' is given twice, with different values"),
                assertThrows(TemplateFillException.class, () -> FillRequest.fromUrl("template=" + T + "&param_name=a&param_name=b")).getProblems());
        assertEquals(List.of("Not a properly encoded parameter: param_name=%zz"),
                assertThrows(TemplateFillException.class, () -> FillRequest.fromUrl("template=" + T + "&param_name=%zz")).getProblems());
    }

    @Test
    void jsonObjectHasTheParametersAsItsMembers() throws Exception {
        FillRequest request = FillRequest.fromJson("{\"template\": \"" + T + "\", \"param_name\": \"Apple\", \"param_count\": 3, \"param_done\": true, \"param_none\": null}");
        assertEquals(Map.of("name", "Apple", "count", "3", "done", "true"), request.getValues().toParams());

        assertEquals(List.of("The JSON has to be an object with the parameters as its members"),
                assertThrows(TemplateFillException.class, () -> FillRequest.fromJson("[]")).getProblems());
        assertEquals(List.of("The value of 'param_keyword' has to be a string; a further repetition has a name of its own, such as 'param_keyword__1'"),
                assertThrows(TemplateFillException.class, () -> FillRequest.fromJson("{\"template\": \"" + T + "\", \"param_keyword\": [\"a\", \"b\"]}")).getProblems());
        assertTrue(assertThrows(TemplateFillException.class, () -> FillRequest.fromJson("{not json")).getMessage().startsWith("Not valid JSON"));
    }

    @Test
    void requestIsBuiltInCode() {
        FillRequest request = new FillRequest(T).set("name", "Apple").setProvenanceTemplate(PR).setSource(Mode.DERIVE, OLD);
        request.addPubinfoTemplate(PI).set("license", "https://example.org/l");
        // The same template again gives the same values to add to.
        assertSame(request.getPubinfoValues(PI), request.addPubinfoTemplate(PI));

        assertEquals(params("template", T, "param_name", "Apple", "prtemplate", PR, "pitemplate1", PI, "piparam1_license", "https://example.org/l", "derive", OLD),
                request.toParams());
        assertThrows(IllegalArgumentException.class, () -> new FillRequest(" "));
        assertThrows(IllegalArgumentException.class, () -> request.setSource(Mode.DERIVE, null));
        assertNull(request.setSource(null, null).getMode());
    }

    @Test
    void requestIsReadBackFromACheckedNanopublication() throws Exception {
        Nanopub np = load(QUERY);
        FillRequest request = FillRequest.of(new TemplateComplianceChecker(RESOURCES).check(np));

        assertEquals("https://w3id.org/np/RAEFAt-QcFK0ZhqfvlsmS10BnzGJA0xwOICZXkO-ai87k", request.getTemplate());
        assertEquals("https://w3id.org/np/RA7lSq6MuK_TIC6JMSHvLtee3lpLoZDOqLJCLXevnrPoU", request.getProvenanceTemplate());
        assertEquals("html-test", request.getValues().get("query").text());
        assertEquals("HTML test", request.getValues().get("title").text());
        assertTrue(request.getValues().get("sparql").text().toLowerCase().contains("select"));
        assertEquals(2, request.getPubinfoTemplates().size());
        assertNotNull(request.getCreator());
        assertEquals(List.of(request.getCreator().stringValue()), List.copyOf(request.getProvenanceValues().toParams().values()));
        assertNull(request.getMode());
    }

}
