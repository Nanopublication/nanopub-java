package org.nanopub;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteEntry;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Nanopub2HtmlTest {

    private Nanopub readFile() throws MalformedNanopubException, IOException {
        String npUri = "http://purl.org/nanopub/temp/155322900/";
        TestSuiteEntry entry = NanopubTestSuite.getLatest()
                .getByNanopubUri(npUri).getFirst();
        return new NanopubImpl(entry.toFile());
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

    @Test
    void createsAFragmentWhenNotStandalone() throws Exception {
        String html = Nanopub2Html.createHtmlString(readFile(), false);

        assertFalse(html.contains("<html"), html);
        assertTrue(html.contains("nanopub-assertion"), html);
    }

    @Test
    void createsAStandaloneDocument() throws Exception {
        String html = Nanopub2Html.createHtmlString(readFile(), true);

        assertTrue(html.contains("<!DOCTYPE html>"), html);
        assertTrue(html.contains("<html lang=\"en\">"), html);
    }

    @Test
    void createsHtmlWithoutContextIndentation() throws Exception {
        Nanopub np = readFile();

        String indented = Nanopub2Html.createHtmlString(np, false, true);
        String flat = Nanopub2Html.createHtmlString(np, false, false);

        assertNotEquals(indented, flat);
        assertTrue(flat.contains(np.getUri().toString()), flat);
    }

    @Test
    void createsHtmlForACollectionWithoutContextIndentation() throws Exception {
        Nanopub np = readFile();

        String html = Nanopub2Html.createHtmlString(List.of(np), false, false);

        assertTrue(html.contains(np.getUri().toString()), html);
    }

    @Test
    void writesHtmlForASingleNanopubToAStream() throws Exception {
        Nanopub np = readFile();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Nanopub2Html.createHtml(np, out, false);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains(np.getUri().toString()));
    }

    @Test
    void writesHtmlForASingleNanopubToAStreamWithoutIndentation() throws Exception {
        Nanopub np = readFile();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Nanopub2Html.createHtml(np, out, false, false);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains(np.getUri().toString()));
    }

    @Test
    void writesHtmlForACollectionToAStream() throws Exception {
        Nanopub np = readFile();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Nanopub2Html.createHtml(List.of(np), out, false);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains(np.getUri().toString()));
    }

    @Test
    void writesHtmlForANanopubWithoutNamespaceSupport() throws Exception {
        Nanopub withNs = readFile();
        Nanopub plain = mock(Nanopub.class);
        when(plain.getUri()).thenReturn(withNs.getUri());
        when(plain.getHead()).thenReturn(withNs.getHead());
        when(plain.getAssertion()).thenReturn(withNs.getAssertion());
        when(plain.getProvenance()).thenReturn(withNs.getProvenance());
        when(plain.getPubinfo()).thenReturn(withNs.getPubinfo());

        String html = Nanopub2Html.createHtmlString(plain, false);

        assertTrue(html.contains(withNs.getUri().toString()), html);
    }

    @Test
    void mainWritesToTheGivenOutputFile(@TempDir File tempDir) throws Exception {
        Nanopub np = readFile();
        File input = new File(tempDir, "input.trig");
        Files.writeString(input.toPath(), np.writeToString(RDFFormat.TRIG), StandardCharsets.UTF_8);
        File output = new File(tempDir, "output.html");

        Nanopub2Html.main(new String[]{"-s", "-i", "-o", output.getAbsolutePath(), input.getAbsolutePath()});

        String html = Files.readString(output.toPath(), StandardCharsets.UTF_8);
        assertTrue(html.contains("<html lang=\"en\">"), html);
        assertTrue(html.contains(np.getUri().toString()), html);
    }

    @Test
    void mainReportsUnreadableInput(@TempDir File tempDir) throws Exception {
        File input = new File(tempDir, "broken.trig");
        Files.writeString(input.toPath(), "this is not TriG at all", StandardCharsets.UTF_8);
        File output = new File(tempDir, "output.html");

        // the parse error is reported, but the tool still finishes and writes its (empty) output
        assertDoesNotThrow(() -> Nanopub2Html.main(
                new String[]{"-o", output.getAbsolutePath(), input.getAbsolutePath()}));
        assertTrue(output.exists());
    }

}
