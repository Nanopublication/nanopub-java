package org.nanopub;

import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteEntry;
import org.nanopub.testsuite.TestSuiteSubfolder;
import org.nanopub.trusty.TempUriReplacer;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPX;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class StripDownTest {

    @Test
    void stripDown() throws Exception {
        String outPath = this.getClass().getResource("/").getPath() + "test-output/strip/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath, "updated.trig");

        for (TestSuiteEntry entry : NanopubTestSuite.getLatest().getValid(TestSuiteSubfolder.SIGNED)) {
            File testFile = entry.toFile();
            StripDown c = CliRunner.initJc(new StripDown(), new String[]{
                    "-o", outFile.getPath(),
                    testFile.getPath()});
            c.run();

            // read created nanopub from file
            NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);
            assertFalse(TrustyUriUtils.isPotentialTrustyUri(testNano.getUri()));
            for (Statement statement : NanopubUtils.getStatements(testNano)) {
                assertNotEquals(NPX.HAS_SIGNATURE_ELEMENT, statement.getPredicate());
            }

            System.out.println("Successfully removed sig: " + testFile.getName());

            // check if public key from assertion is not removed
            NanopubImpl sigNano = new NanopubImpl(testFile, RDFFormat.TRIG);
            for (Statement st : sigNano.getAssertion()) {
                if (st.getPredicate().equals(NPX.HAS_PUBLIC_KEY)) {
                    assertTrue(testNano.getAssertion().stream().anyMatch(
                            s -> s.getPredicate().equals(NPX.HAS_PUBLIC_KEY)));
                }
            }

            // delete target file if everything was fine
            outFile.delete();
        }
    }

    @Test
    void transformWithValidResource() {
        Resource resource = SimpleValueFactory.getInstance().createIRI("http://purl.org/np/RAYskLSM5x29icArnWvo9nVrIVEN2mfPoDq3TQSgm-9kk#Head");
        String artifact = "RAYskLSM5x29icArnWvo9nVrIVEN2mfPoDq3TQSgm-9kk";
        String replacement = TempUriReplacer.tempUri + Math.abs(new Random().nextInt()) + "/";

        IRI result = new StripDown().transform(resource, artifact, replacement);

        assertTrue(result.stringValue().startsWith(TempUriReplacer.tempUri));
        assertTrue(result.stringValue().endsWith("/Head"));
    }

    @Test
    void transformWithNullResource() {
        Resource resource = null;
        String artifact = "RAdf9taM_Gyq2-WavUq3CxaVIvsHockMXzonj3W_igNhM";
        String replacement = TempUriReplacer.tempUri + Math.abs(new Random().nextInt()) + "/";
        IRI result = new StripDown().transform(resource, artifact, replacement);

        assertNull(result);
    }

    @Test
    void transformWithBlankNodeThrowsException() {
        Resource resource = SimpleValueFactory.getInstance().createBNode();
        String artifact = "RAdf9taM_Gyq2-WavUq3CxaVIvsHockMXzonj3W_igNhM";
        String replacement = TempUriReplacer.tempUri + Math.abs(new Random().nextInt()) + "/";

        assertThrows(RuntimeException.class, () -> new StripDown().transform(resource, artifact, replacement));
    }

    @Test
    void transformWithNoArtifactMatch() {
        Resource resource = SimpleValueFactory.getInstance().createIRI("http://purl.org/np/RAYskLSM5x29icArnWvo9nVrIVEN2mfPoDq3TQSgm-9kk#Head");
        String artifact = "artifact123"; // No match for this artifact
        String replacement = TempUriReplacer.tempUri + Math.abs(new Random().nextInt()) + "/";

        IRI result = new StripDown().transform(resource, artifact, replacement);
        assertEquals(resource.toString(), result.stringValue());
    }

    private static File copyOfFirstSignedNanopub(File directory, String name) throws IOException {
        File source = NanopubTestSuite.getLatest().getValid(TestSuiteSubfolder.SIGNED).getFirst().toFile();
        File copy = new File(directory, name);
        Files.copy(source.toPath(), copy.toPath());
        return copy;
    }

    @Test
    void writesNextToTheInputWhenNoOutputFileIsGiven(@TempDir File tempDir) throws Exception {
        File input = copyOfFirstSignedNanopub(tempDir, "input.trig");

        CliRunner.initJc(new StripDown(), new String[]{input.getPath()}).run();

        File output = new File(tempDir, "plain.input.trig");
        assertTrue(output.exists());
        assertFalse(TrustyUriUtils.isPotentialTrustyUri(new NanopubImpl(output, RDFFormat.TRIG).getUri()));
    }

    @Test
    void writesAGzippedSingleOutputFile(@TempDir File tempDir) throws Exception {
        File input = copyOfFirstSignedNanopub(tempDir, "input.trig");
        File output = new File(tempDir, "out.trig.gz");

        CliRunner.initJc(new StripDown(), new String[]{"-o", output.getPath(), input.getPath()}).run();

        assertTrue(output.exists());
        try (InputStream in = new GZIPInputStream(new FileInputStream(output))) {
            assertFalse(TrustyUriUtils.isPotentialTrustyUri(new NanopubImpl(in, RDFFormat.TRIG).getUri()));
        }
    }

    @Test
    void readsAndWritesGzippedFiles(@TempDir File tempDir) throws Exception {
        File signed = copyOfFirstSignedNanopub(tempDir, "signed.trig");
        File input = new File(tempDir, "input.trig.gz");
        try (OutputStream out = new GZIPOutputStream(new FileOutputStream(input))) {
            Files.copy(signed.toPath(), out);
        }

        CliRunner.initJc(new StripDown(), new String[]{input.getPath()}).run();

        File output = new File(tempDir, "plain.input.trig.gz");
        assertTrue(output.exists());
        try (InputStream in = new GZIPInputStream(new FileInputStream(output))) {
            assertFalse(TrustyUriUtils.isPotentialTrustyUri(new NanopubImpl(in, RDFFormat.TRIG).getUri()));
        }
    }

    @Test
    void mainStripsTheSignature(@TempDir File tempDir) throws Exception {
        File input = copyOfFirstSignedNanopub(tempDir, "input.trig");
        File output = new File(tempDir, "out.trig");

        StripDown.main(new String[]{"-o", output.getPath(), input.getPath()});

        assertTrue(output.exists());
        assertFalse(TrustyUriUtils.isPotentialTrustyUri(new NanopubImpl(output, RDFFormat.TRIG).getUri()));
    }

    @Test
    void refusesNanopubsWithoutAnArtifactCode(@TempDir File tempDir) throws Exception {
        File input = new File(tempDir, "plain-np.trig");
        Files.writeString(input.toPath(),
                TestUtils.createNanopub("https://example.org/np1#").writeToString(RDFFormat.TRIG));

        StripDown stripDown = CliRunner.initJc(new StripDown(),
                new String[]{"-o", new File(tempDir, "out.trig").getPath(), input.getPath()});

        RuntimeException ex = assertThrows(RuntimeException.class, stripDown::run);
        assertTrue(ex.getMessage().startsWith("No artifact code found for "), ex.getMessage());
    }

}