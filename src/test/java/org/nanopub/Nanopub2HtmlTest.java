package org.nanopub;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Nanopub2HtmlTest {

    private Nanopub readFile() throws MalformedNanopubException, IOException {
        File file = new File(this.getClass().getResource("/testsuite/valid/plain/workflow-1.trig").getFile());
        return new NanopubImpl(file);
    }

    @Test
    void testSimpleTrigFile() throws MalformedNanopubException, IOException {
        Nanopub np = readFile();
        String html = Nanopub2Html.createHtmlString(np, true);

        assertHtmlContainsSomeNpContent(html, np);
    }

    private static void assertHtmlContainsSomeNpContent(String html, Nanopub np) {
        assertTrue(html.contains(np.getUri().toString()));
        assertTrue(html.contains("dcterms:description"));

        assertTrue(html.contains("This is a test workflow"));
        assertTrue(html.contains("<a href=\"http://www.w3.org/ns/prov#generatedAtTime\">prov:generatedAtTime</a>"));

        assertTrue(html.contains("2020-10-27T10:46:36.512175"));
    }

    @Test
    void testPseudoCollectionOfTrigFile() throws MalformedNanopubException, IOException {
        Nanopub np = readFile();
        String html = Nanopub2Html.createHtmlString(List.of(np), true);
        assertHtmlContainsSomeNpContent(html, np);
    }

}
