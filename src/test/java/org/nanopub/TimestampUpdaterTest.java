package org.nanopub;

import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class TimestampUpdaterTest {

    @Test
    void initWithoutArgs() {
        assertThrowsExactly(ParameterException.class, () -> CliRunner.initJc(new TimestampUpdater(), new String[0]));
    }

    @Test
    void initWithValidArgs() {
        String path = this.getClass().getResource("/testsuite/valid/plain/aida1.trig").getPath();
        String[] args = new String[]{"-v", path};

        CliRunner.initJc(new TimestampUpdater(), args);
    }

    @Test
    void upgradeTimestamp() throws Exception {
        String outPath = this.getClass().getResource("/").getPath() + "test-output/timestamp/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath, "updated.trig");

        String inFiles = this.getClass().getResource("/testsuite/valid/plain/").getPath();
        for (File testFile : new File(inFiles).listFiles((dir, name) -> name.endsWith(".trig"))) {
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

}
