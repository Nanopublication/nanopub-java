package org.nanopub.extra.security;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.CliRunner;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class SignNanopubTest {

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
    void signAndTransform() throws Exception {

        String outPath = "target/test-output/sign-nanopub";
        new File(outPath).mkdirs();
        File outFile = new File(outPath, "signed.trig");

        String keyFile = "src/main/resources/testsuite/transform/signed/rsa-key1/key/id_rsa";
        String inFiles = "src/main/resources/testsuite/transform/plain/";
        String signedFiles = "src/main/resources/testsuite/transform/signed/rsa-key1/";
        for (File testFile : new File(inFiles).listFiles(
                new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".in.trig");
                }
            }))
        {
            // create signed nanopub file
            SignNanopub c = CliRunner.initJc(new SignNanopub(), new String[] {
                    testFile.getPath(),
                    "-k ", keyFile,
                    "-o ", outFile.getPath(),});
            c.run();
            
            // read nanopub from file
            NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);
            String testedArtifactCode = TrustyUriUtils.getArtifactCode(testNano.getUri().toString());

            FileInputStream inputStream = new FileInputStream(new File(signedFiles + testFile.getName().replace("in.trig", "out.code")));
            try {
                String artifactCodeFromSuite = IOUtils.toString(inputStream);
                assertEquals(testedArtifactCode, artifactCodeFromSuite);
                System.out.println("File signed correctly: " + testFile.getName());
            } finally {
                inputStream.close();
            }
            // delete target file if everything was fine
            outFile.delete();
        }
    }

}