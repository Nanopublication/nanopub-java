package org.nanopub;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nanopub.utils.TestUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.utils.TestUtils.anyIri;

class MultiNanopubRdfHandlerTest {

    private static String twoNanopubs() throws Exception {
        return TestUtils.createNanopub("https://example.org/np1#").writeToString(RDFFormat.TRIG)
                + TestUtils.createNanopub("https://example.org/np2#").writeToString(RDFFormat.TRIG);
    }

    private static List<Nanopub> collect(String trig, RDFFormat format) throws Exception {
        List<Nanopub> collected = new ArrayList<>();
        MultiNanopubRdfHandler.process(format,
                new ByteArrayInputStream(trig.getBytes(StandardCharsets.UTF_8)), collected::add);
        return collected;
    }

    private static File write(File directory, String name, String content) throws IOException {
        File file = new File(directory, name);
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }

    private static File writeGzipped(File directory, String name, String content) throws IOException {
        File file = new File(directory, name);
        try (OutputStream out = new GZIPOutputStream(new FileOutputStream(file))) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    @Test
    void processesAStreamOfSeveralNanopubs() throws Exception {
        List<Nanopub> collected = collect(twoNanopubs(), RDFFormat.TRIG);

        assertEquals(2, collected.size());
        assertEquals("https://example.org/np1#", collected.get(0).getUri().stringValue());
        assertEquals("https://example.org/np2#", collected.get(1).getUri().stringValue());
    }

    @Test
    void processesAnEmptyStream() throws Exception {
        // a stream of zero nanopubs is a valid nanopub stream
        assertTrue(collect("", RDFFormat.TRIG).isEmpty());
    }

    @Test
    void reportsMalformedNanopubsInTheStream() {
        String notANanopub = """
                @prefix ex: <https://example.org/> .
                ex:graph { ex:subject ex:predicate ex:object . }
                """;

        assertThrows(MalformedNanopubException.class, () -> collect(notANanopub, RDFFormat.TRIG));
    }

    @Test
    void propagatesParseErrors() {
        String brokenTrig = "this is not TriG at all";

        assertThrows(RDFParseException.class, () -> collect(brokenTrig, RDFFormat.TRIG));
    }

    @Test
    void propagatesRuntimeExceptionsFromTheHandler() throws Exception {
        // the message matches the one used internally to smuggle a MalformedNanopubException out of
        // the parser, but the cause does not, so the exception has to be passed on unchanged
        RuntimeException thrownByHandler = new RuntimeException("wrapped MalformedNanopubException",
                new IllegalStateException("something else"));
        String trig = twoNanopubs();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> MultiNanopubRdfHandler.process(RDFFormat.TRIG,
                        new ByteArrayInputStream(trig.getBytes(StandardCharsets.UTF_8)),
                        np -> {
                            throw thrownByHandler;
                        }));
        assertSame(thrownByHandler, ex);
    }

    @Test
    void wrapsAlreadyFinalizedExceptionsFromTheHandler() throws Exception {
        String trig = twoNanopubs();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> MultiNanopubRdfHandler.process(RDFFormat.TRIG,
                        new ByteArrayInputStream(trig.getBytes(StandardCharsets.UTF_8)),
                        np -> {
                            throw new NanopubAlreadyFinalizedException();
                        }));
        assertInstanceOf(NanopubAlreadyFinalizedException.class, ex.getCause());
    }

    @Test
    void processesAFileWithAnExplicitFormat(@TempDir File tempDir) throws Exception {
        File file = write(tempDir, "nanopubs.trig", twoNanopubs());

        List<Nanopub> collected = new ArrayList<>();
        MultiNanopubRdfHandler.process(RDFFormat.TRIG, file, collected::add);

        assertEquals(2, collected.size());
    }

    @Test
    void processesAGzippedFile(@TempDir File tempDir) throws Exception {
        File file = writeGzipped(tempDir, "nanopubs.trig.gz", twoNanopubs());

        List<Nanopub> collected = new ArrayList<>();
        MultiNanopubRdfHandler.process(RDFFormat.TRIG, file, collected::add);

        assertEquals(2, collected.size());
    }

    @Test
    void guessesTheFormatFromTheFileName(@TempDir File tempDir) throws Exception {
        File file = write(tempDir, "nanopubs.trig", twoNanopubs());

        List<Nanopub> collected = new ArrayList<>();
        MultiNanopubRdfHandler.process(file, collected::add);

        assertEquals(2, collected.size());
    }

    @Test
    void fallsBackToTrigForUnknownFileNames(@TempDir File tempDir) throws Exception {
        File file = write(tempDir, "nanopubs.unknown", twoNanopubs());

        List<Nanopub> collected = new ArrayList<>();
        MultiNanopubRdfHandler.process(file, collected::add);

        assertEquals(2, collected.size());
    }

    @Test
    void keepsTheNamespacesOfEachNanopub() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator("https://example.org/np1#");
        creator.addAssertionStatement(anyIri, anyIri, anyIri);
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        creator.addNamespace("ex", "https://example.org/");

        List<Nanopub> collected = collect(creator.finalizeNanopub().writeToString(RDFFormat.TRIG), RDFFormat.TRIG);

        assertEquals(1, collected.size());
        assertEquals("https://example.org/", ((NanopubWithNs) collected.getFirst()).getNamespace("ex"));
    }

}
