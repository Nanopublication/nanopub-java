package org.nanopub.trusty;

import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteCategory;
import org.nanopub.utils.TestUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class FixTrustyNanopubTest {

    /**
     * A nanopub that carries a trusty URI whose artifact code no longer matches its content — which
     * is exactly what this tool is meant to repair.
     */
    private static final String BROKEN_ARTIFACT_CODE = "RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA";

    private static File brokenTrustyFile() {
        return NanopubTestSuite.getLatest()
                .getByArtifactCode(BROKEN_ARTIFACT_CODE, TestSuiteCategory.INVALID)
                .orElseThrow(() -> new IllegalStateException("broken trusty nanopub not found in the test suite"))
                .toFile();
    }

    private static Nanopub brokenTrustyNanopub() throws Exception {
        return new NanopubImpl(brokenTrustyFile(), RDFFormat.TRIG);
    }

    private static List<Nanopub> readAll(InputStream in) throws Exception {
        List<Nanopub> nanopubs = new ArrayList<>();
        MultiNanopubRdfHandler.process(RDFFormat.TRIG, in, nanopubs::add);
        return nanopubs;
    }

    // ------------------------------------------------------------------ fix

    @Test
    void fixRepairsABrokenTrustyNanopub() throws Exception {
        Nanopub broken = brokenTrustyNanopub();
        assertFalse(TrustyNanopubUtils.isValidTrustyNanopub(broken));

        Nanopub fixed = FixTrustyNanopub.fix(broken);

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(fixed));
        assertNotEquals(broken.getUri(), fixed.getUri());
    }

    @Test
    void fixRefusesNanopubsWithoutATrustyUri() throws Exception {
        Nanopub plain = TestUtils.createNanopub("https://example.org/np1#");

        TrustyUriException ex = assertThrows(TrustyUriException.class, () -> FixTrustyNanopub.fix(plain));
        assertTrue(ex.getMessage().startsWith("Not a (broken) trusty URI: "), ex.getMessage());
    }

    @Test
    void writeAsFixedNanopubWritesTheRepairedNanopub() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Nanopub fixed = FixTrustyNanopub.writeAsFixedNanopub(brokenTrustyNanopub(), RDFFormat.TRIG, out);

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(fixed));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains(fixed.getUri().toString()));
    }

    // -------------------------------------------------- transformMultiNanopub

    @Test
    void transformMultiNanopubFromAStream() throws Exception {
        byte[] input = Files.readAllBytes(brokenTrustyFile().toPath());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        FixTrustyNanopub.transformMultiNanopub(RDFFormat.TRIG, new ByteArrayInputStream(input), out);

        List<Nanopub> fixed = readAll(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, fixed.size());
        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(fixed.getFirst()));
    }

    @Test
    void transformMultiNanopubFromAFile() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        FixTrustyNanopub.transformMultiNanopub(RDFFormat.TRIG, brokenTrustyFile(), out);

        List<Nanopub> fixed = readAll(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, fixed.size());
        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(fixed.getFirst()));
    }

    // ------------------------------------------------------------------ CLI

    private static File copyOfBrokenTrusty(File directory, String name) throws IOException {
        File copy = new File(directory, name);
        Files.copy(brokenTrustyFile().toPath(), copy.toPath());
        return copy;
    }

    @Test
    void reportsNanopubsThatCannotBeFixed(@TempDir File tempDir) throws Exception {
        File input = new File(tempDir, "plain.trig");
        Files.writeString(input.toPath(),
                TestUtils.createNanopub("https://example.org/np1#").writeToString(RDFFormat.TRIG));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // the failure surfaces from inside the per-nanopub callback
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> FixTrustyNanopub.transformMultiNanopub(RDFFormat.TRIG, input, out));
        assertInstanceOf(TrustyUriException.class, ex.getCause());
    }

    @Test
    void mainWritesTheFixedNanopubNextToTheInput(@TempDir File tempDir) throws Exception {
        File input = copyOfBrokenTrusty(tempDir, "input.trig");

        FixTrustyNanopub.main(new String[]{input.getPath()});

        File output = new File(tempDir, "fixed.input.trig");
        assertTrue(output.exists());
        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(new NanopubImpl(output, RDFFormat.TRIG)));
    }

    @Test
    void mainReadsAndWritesGzippedFiles(@TempDir File tempDir) throws Exception {
        File plain = copyOfBrokenTrusty(tempDir, "plain.trig");
        File input = new File(tempDir, "input.trig.gz");
        try (OutputStream out = new GZIPOutputStream(new FileOutputStream(input))) {
            Files.copy(plain.toPath(), out);
        }

        FixTrustyNanopub.main(new String[]{input.getPath()});

        File output = new File(tempDir, "fixed.input.trig.gz");
        assertTrue(output.exists());
        try (InputStream in = new java.util.zip.GZIPInputStream(new FileInputStream(output))) {
            assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(new NanopubImpl(in, RDFFormat.TRIG)));
        }
    }

    @Test
    void mainPrintsTheNanopubUriInVerboseMode(@TempDir File tempDir) throws Exception {
        File input = copyOfBrokenTrusty(tempDir, "input.trig");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            FixTrustyNanopub.main(new String[]{"-v", input.getPath()});
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(captured.toString(StandardCharsets.UTF_8).contains("Nanopub URI: "), captured.toString());
    }

    /**
     * Builds a nanopub with a trusty URI that no longer matches its content, by adding a statement
     * to an already finalized trusty nanopub. Every call yields a different one.
     */
    private static Nanopub brokenTrusty(String label) throws Exception {
        NanopubCreator creator = new NanopubCreator(true);
        creator.addAssertionStatement(TestUtils.anyIri, RDFS.LABEL, TestUtils.vf.createLiteral(label));
        creator.addProvenanceStatement(TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(TestUtils.anyIri, TestUtils.anyIri);
        Nanopub trusty = creator.finalizeTrustyNanopub();

        List<Statement> statements = new ArrayList<>(NanopubUtils.getStatements(trusty));
        statements.add(TestUtils.vf.createStatement(trusty.getUri(), RDFS.COMMENT,
                TestUtils.vf.createLiteral("this breaks the artifact code"), trusty.getPubinfoUri()));
        return new NanopubImpl(statements);
    }

    @Test
    void mainLogsProgressEveryHundredNanopubs(@TempDir File tempDir) throws Exception {
        // the nanopubs have to differ from each other, or the reader sees them as a single one
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 101; i++) {
            many.append(brokenTrusty("nanopub " + i).writeToString(RDFFormat.TRIG));
        }
        File input = new File(tempDir, "many.trig");
        Files.writeString(input.toPath(), many.toString(), StandardCharsets.UTF_8);

        PrintStream originalErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
            FixTrustyNanopub.main(new String[]{input.getPath()});
        } finally {
            System.setErr(originalErr);
        }

        assertTrue(captured.toString(StandardCharsets.UTF_8).contains("100 nanopubs..."), captured.toString());
    }

}
