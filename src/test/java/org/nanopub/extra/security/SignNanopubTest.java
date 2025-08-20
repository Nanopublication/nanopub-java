package org.nanopub.extra.security;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.CliRunner;
import org.nanopub.NanopubImpl;
import org.nanopub.utils.TestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class SignNanopubTest {

    @Test
    void initWithoutArgs() {
        assertThrowsExactly(ParameterException.class, () -> CliRunner.initJc(new SignNanopub(), new String[0]));
    }

    @Test
    void initWithValidArgs() {
        String path = this.getClass().getResource("/testsuite/valid/plain/aida1.trig").getPath();
        String[] args = new String[]{"-v", path};

        CliRunner.initJc(new SignNanopub(), args);
    }

    @Test
    void signAndTransform1024RSA() throws Exception {
        String outPath = this.getClass().getResource("/").getPath() + "test-output/sign-nanopub/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath, "signed.trig");

        String keyFile = this.getClass().getResource("/testsuite/transform/signed/rsa-key1/key/id_rsa").getPath();
        String signerOrcid = TestUtils.ORCID;
        String inFiles = this.getClass().getResource("/testsuite/transform/plain/").getPath();
        String signedFiles = this.getClass().getResource("/testsuite/transform/signed/rsa-key1/").getPath();
        for (File testFile : new File(inFiles).listFiles(
                (dir, name) -> name.endsWith(".in.trig"))) {
            // create signed nanopub file
            SignNanopub c = CliRunner.initJc(new SignNanopub(), new String[]{
                    testFile.getPath(),
                    "-k ", keyFile,
                    "-s ", signerOrcid,
                    "-o ", outFile.getPath(),});
            c.run();

            // read nanopub from file
            NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);
            String testedArtifactCode = TrustyUriUtils.getArtifactCode(testNano.getUri().toString());

            FileInputStream inputStream = new FileInputStream(signedFiles + testFile.getName().replace("in.trig", "out.code"));
            try {
                String artifactCodeFromSuite = IOUtils.toString(inputStream, Charset.defaultCharset());
                assertEquals(testedArtifactCode, artifactCodeFromSuite, "Problem with file: " + testFile.getName());
                System.out.println("File signed correctly: " + testFile.getName());
            } finally {
                inputStream.close();
            }
            // delete target file if everything was fine
            outFile.delete();
        }
    }

    @Test
    void signAndTransform2048RSA() throws Exception {
        String outPath = this.getClass().getResource("/").getPath() + "test-output/sign-nanopub/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath, "signed.trig");

        String profileFile = this.getClass().getResource("/testsuite/transform/profile.yaml").getPath();
        String inFiles = this.getClass().getResource("/testsuite/transform/plain/").getPath();
        String signedFiles = this.getClass().getResource("/testsuite/transform/signed/rsa-key2/").getPath();
        for (File testFile : new File(inFiles).listFiles(
                (dir, name) -> name.endsWith("in.trig"))) {
            // create signed nanopub file
            SignNanopub c = CliRunner.initJc(new SignNanopub(), new String[]{
                    testFile.getPath(),
                    "--profile ", profileFile,
                    "-o ", outFile.getPath(),});
            c.run();

            // read nanopub from file
            NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);
            String testedArtifactCode = TrustyUriUtils.getArtifactCode(testNano.getUri().toString());

            FileInputStream inputStream = new FileInputStream(signedFiles + testFile.getName().replace("in.trig", "out.code"));
            try {
                String artifactCodeFromSuite = IOUtils.toString(inputStream, Charset.defaultCharset());
                assertEquals(testedArtifactCode, artifactCodeFromSuite, "Problem with file: " + testFile.getName());
                System.out.println("File signed correctly: " + testFile.getName());
            } finally {
                inputStream.close();
            }
            // delete target file if everything was fine
            outFile.delete();
        }
    }

}