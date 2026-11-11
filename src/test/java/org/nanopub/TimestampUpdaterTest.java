package org.nanopub;

import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteEntry;
import org.nanopub.testsuite.TestSuiteSubfolder;
import org.nanopub.utils.TestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class TimestampUpdaterTest {

    @Test
    void initWithoutArgs() {
        assertThrowsExactly(ParameterException.class, () -> CliRunner.initJc(new TimestampUpdater(), new String[0]));
    }

    @Test
    void initWithValidArgs() {
        String path = NanopubTestSuite.getLatest().getValid(TestSuiteSubfolder.PLAIN).getFirst().toFile().getPath();
        String[] args = new String[]{"-v", path};

        CliRunner.initJc(new TimestampUpdater(), args);
    }

    @Test
    void upgradeTimestamp() throws Exception {
        String outPath = this.getClass().getResource("/").getPath() + "test-output/timestamp/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath, "updated.trig");

        for (TestSuiteEntry testSuiteEntry : NanopubTestSuite.getLatest().getValid(TestSuiteSubfolder.PLAIN)) {
            File testFile = testSuiteEntry.toFile();
            Calendar before = Calendar.getInstance();

            // create signed nanopub file
            TimestampUpdater c = CliRunner.initJc(new TimestampUpdater(), new String[]{"-o", outFile.getPath(), testFile.getPath()});
            c.run();

            // read created nanopub from file
            NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);
            assertFalse(before.after(testNano.getCreationTime()));
            System.out.println("Successfully updated timestamp: " + testFile.getName());

            // delete target file if everything was fine
            outFile.delete();
        }
    }

    private static File copyOfFirstPlainNanopub(File directory, String name) throws IOException {
        File source = NanopubTestSuite.getLatest().getValid(TestSuiteSubfolder.PLAIN).getFirst().toFile();
        File copy = new File(directory, name);
        Files.copy(source.toPath(), copy.toPath());
        return copy;
    }

    @Test
    void writesNextToTheInputWhenNoOutputFileIsGiven(@TempDir File tempDir) throws Exception {
        File input = copyOfFirstPlainNanopub(tempDir, "input.trig");

        CliRunner.initJc(new TimestampUpdater(), new String[]{input.getPath()}).run();

        File output = new File(tempDir, "updated.input.trig");
        assertTrue(output.exists());
        assertNotNull(new NanopubImpl(output, RDFFormat.TRIG).getCreationTime());
    }

    @Test
    void writesAGzippedSingleOutputFile(@TempDir File tempDir) throws Exception {
        File input = copyOfFirstPlainNanopub(tempDir, "input.trig");
        File output = new File(tempDir, "out.trig.gz");

        CliRunner.initJc(new TimestampUpdater(), new String[]{"-o", output.getPath(), input.getPath()}).run();

        assertTrue(output.exists());
        try (InputStream in = new GZIPInputStream(new FileInputStream(output))) {
            assertNotNull(new NanopubImpl(in, RDFFormat.TRIG).getCreationTime());
        }
    }

    @Test
    void readsAndWritesGzippedFiles(@TempDir File tempDir) throws Exception {
        File plain = copyOfFirstPlainNanopub(tempDir, "plain.trig");
        File input = new File(tempDir, "input.trig.gz");
        try (OutputStream out = new GZIPOutputStream(new FileOutputStream(input))) {
            Files.copy(plain.toPath(), out);
        }

        CliRunner.initJc(new TimestampUpdater(), new String[]{input.getPath()}).run();

        File output = new File(tempDir, "updated.input.trig.gz");
        assertTrue(output.exists());
        try (InputStream in = new GZIPInputStream(new FileInputStream(output))) {
            assertNotNull(new NanopubImpl(in, RDFFormat.TRIG).getCreationTime());
        }
    }

    @Test
    void mainUpdatesTheTimestamp(@TempDir File tempDir) throws Exception {
        File input = copyOfFirstPlainNanopub(tempDir, "input.trig");
        File output = new File(tempDir, "out.trig");

        TimestampUpdater.main(new String[]{"-v", "-o", output.getPath(), input.getPath()});

        assertTrue(output.exists());
        assertNotNull(new NanopubImpl(output, RDFFormat.TRIG).getCreationTime());
    }

    @Test
    void replacesAnExistingCreationTime(@TempDir File tempDir) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator("https://example.org/np1#");
        creator.addAssertionStatement(TestUtils.anyIri, TestUtils.anyIri, TestUtils.anyIri);
        creator.addProvenanceStatement(TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(DCTERMS.CREATED,
                TestUtils.vf.createLiteral("2000-01-02T03:04:05Z", XSD.DATETIME));
        File input = new File(tempDir, "input.trig");
        Files.writeString(input.toPath(), creator.finalizeNanopub().writeToString(RDFFormat.TRIG));
        File output = new File(tempDir, "out.trig");

        CliRunner.initJc(new TimestampUpdater(), new String[]{"-o", output.getPath(), input.getPath()}).run();

        Nanopub updated = new NanopubImpl(output, RDFFormat.TRIG);
        assertEquals(1, updated.getPubinfo().stream()
                .filter(st -> st.getPredicate().equals(DCTERMS.CREATED)).count());
        assertTrue(updated.getCreationTime().get(Calendar.YEAR) > 2000);
    }

    @Test
    void updatesTheInputFileInPlace(@TempDir File tempDir) throws Exception {
        File file = copyOfFirstPlainNanopub(tempDir, "input.trig");
        Calendar before = Calendar.getInstance();
        before.set(Calendar.MILLISECOND, 0);

        CliRunner.initJc(new TimestampUpdater(), new String[]{"-o", file.getPath(), file.getPath()}).run();

        Nanopub updated = new NanopubImpl(file, RDFFormat.TRIG);
        assertFalse(before.after(updated.getCreationTime()));
        assertEquals(List.of("input.trig"), List.of(tempDir.list()));
    }

    @Test
    void updatesAFileWithSeveralNanopubsInPlace(@TempDir File tempDir) throws Exception {
        List<TestSuiteEntry> valid = NanopubTestSuite.getLatest().getValid(TestSuiteSubfolder.PLAIN);
        File file = new File(tempDir, "several.trig");
        Files.writeString(file.toPath(), Files.readString(valid.get(0).toFile().toPath()) + "\n"
                + Files.readString(valid.get(1).toFile().toPath()));

        CliRunner.initJc(new TimestampUpdater(), new String[]{"-o", file.getPath(), file.getPath()}).run();

        assertEquals(2, countNanopubs(file));
    }

    @Test
    void writesSeveralInputsIntoOneOutputFile(@TempDir File tempDir) throws Exception {
        List<TestSuiteEntry> valid = NanopubTestSuite.getLatest().getValid(TestSuiteSubfolder.PLAIN);
        File output = new File(tempDir, "out.trig");

        CliRunner.initJc(new TimestampUpdater(), new String[]{"-o", output.getPath(),
                valid.get(0).toFile().getPath(), valid.get(1).toFile().getPath()}).run();

        assertEquals(2, countNanopubs(output));
    }

    @Test
    void leavesAnExistingOutputUntouchedWhenTheRunFails(@TempDir File tempDir) throws Exception {
        File input = new File(tempDir, "broken.trig");
        Files.writeString(input.toPath(), "this is not TriG");
        File output = new File(tempDir, "out.trig");
        Files.writeString(output.toPath(), "previous content");

        TimestampUpdater c = CliRunner.initJc(new TimestampUpdater(), new String[]{"-o", output.getPath(), input.getPath()});
        assertThrows(Exception.class, c::run);

        assertEquals("previous content", Files.readString(output.toPath()));
        assertEquals(2, tempDir.list().length, "no temporary file may be left behind");
    }

    private static int countNanopubs(File file) throws Exception {
        List<Nanopub> nanopubs = new ArrayList<>();
        MultiNanopubRdfHandler.process(RDFFormat.TRIG, file, nanopubs::add);
        return nanopubs.size();
    }

}
