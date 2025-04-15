package org.nanopub;

import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.security.SignNanopub;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimestampUpdaterTest {

    @Test
    void initWithoutArgs() throws IOException {
        assertThrowsExactly(ParameterException.class, () -> CliRunner.initJc(new SignNanopub(), new String[0]));
    }

    @Test
    void initWithValidArgs() throws Exception {
        String path = "src/main/resources/testsuite/valid/plain/aida1.trig";
        String[] args = new String[] {"-v", path};

        CliRunner.initJc(new SignNanopub(), args);
    }

    @Test
    void upgradeTimestamp() throws Exception {

        String outPath = "target/test-output/timestamp/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath, "signed.trig");

        String inFiles = "src/main/resources/testsuite/valid/plain/";
        for (File testFile : new File(inFiles).listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".trig");
                    }
                }))
        {
            Calendar before = Calendar.getInstance();

            // create signed nanopub file
            TimestampUpdater c = CliRunner.initJc(new TimestampUpdater(), new String[] {
                    "-o", outFile.getPath(),
                    testFile.getPath()});
            c.run();

            // read created nanopub from file
            NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);

            assertTrue(testNano.getCreationTime().after(before));
            System.out.println("Successfully updated timestamp: " + testFile.getName());

            // delete target file if everything was fine
            outFile.delete();
        }
    }

}
