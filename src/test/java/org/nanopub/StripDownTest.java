package org.nanopub;

import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.security.NanopubSignatureElement;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

class StripDownTest {

    @Test
    void stripDown() throws Exception {
        String outPath = "target/test-output/strip/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath, "updated.trig");
        String inFiles = "src/test/resources/testsuite/valid/signed";

        for (File testFile : new File(inFiles).listFiles()) {
            // create signed nanopub file
            StripDown c = CliRunner.initJc(new StripDown(), new String[]{
                    "-o", outFile.getPath(),
                    testFile.getPath()});
            c.run();

            // read created nanopub from file
            NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);
            assertFalse(TrustyUriUtils.isPotentialTrustyUri(testNano.getUri()));
            for (Statement statement : NanopubUtils.getStatements(testNano)) {
                assertThat(statement.getPredicate()).isNotEqualTo(NanopubSignatureElement.HAS_SIGNATURE_ELEMENT);
            }

            System.out.println("Successfully removed sig: " + testFile.getName());

            // delete target file if everything was fine
            outFile.delete();
        }
    }
}