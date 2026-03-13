package org.nanopub.extra.security;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.CliRunner;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubProfile;
import org.nanopub.testsuite.*;
import org.nanopub.utils.TestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class SignNanopubTest {

    @Test
    void initWithoutArgs() {
        assertThrowsExactly(ParameterException.class, () -> CliRunner.initJc(new SignNanopub(), new String[0]));
    }

    @Test
    void initWithValidArgs() {
        TestSuiteEntry entry = NanopubTestSuite.getLatest().getValid(TestSuiteSubfolder.PLAIN).getFirst();
        String path = entry.toFile().getPath();
        String[] args = new String[]{"-v", path};

        CliRunner.initJc(new SignNanopub(), args);
    }

    @Test
    void signAndTransform1024RSA() throws Exception {
        String outPath = this.getClass().getResource("/").getPath() + "test-output/sign-nanopub/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath, "signed.trig");

        SigningKeyPair signingKeyPair = NanopubTestSuite.getLatest().getSigningKey("rsa-key1");
        String signerOrcid = TestUtils.ORCID;
        for (TransformTestCase transformTestCase : NanopubTestSuite.getLatest().getTransformCases("rsa-key1")) {
            File testFile = transformTestCase.getPlainEntry().toFile();

            // create signed nanopub file
            SignNanopub c = CliRunner.initJc(new SignNanopub(), new String[]{
                    testFile.getPath(),
                    "-k ", signingKeyPair.getPrivateKeyFile().getPath(),
                    "-s ", signerOrcid,
                    "-o ", outFile.getPath(),});
            c.run();

            // read nanopub from file
            NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);
            String testedArtifactCode = TrustyUriUtils.getArtifactCode(testNano.getUri().toString());

            assertEquals(testedArtifactCode, transformTestCase.getSignedEntry().getArtifactCode(), "Problem with file: " + testFile.getName());
            System.out.println("File signed correctly: " + testFile.getName());

            // delete target file if everything was fine
            outFile.delete();
        }
    }

    @Test
    void signAndTransform2048RSA() throws Exception {
        String outPath = this.getClass().getResource("/").getPath() + "test-output/sign-nanopub/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath, "signed.trig");

        final String keyName = "rsa-key2";
        NanopubTestSuite suite = NanopubTestSuite.getLatest();
        SigningKeyPair keySource = suite.getSigningKey(keyName);
        String profileFile = NanopubTestSuite.getLatest().getTransformProfile().getPath();

        Path keyPath = Path.of(new NanopubProfile(profileFile).getPrivateKeyPath());
        Files.createDirectories(keyPath.getParent());
        Files.copy(keySource.getPrivateKeyFile().toPath(), keyPath, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(keySource.getPublicKeyFile().toPath(), Path.of(keyPath + ".pub"), StandardCopyOption.REPLACE_EXISTING);

        for (TransformTestCase transformTestCase : NanopubTestSuite.getLatest().getTransformCases(keyName)) {
            File testFile = transformTestCase.getPlainEntry().toFile();

            // create signed nanopub file
            SignNanopub c = CliRunner.initJc(new SignNanopub(), new String[]{
                    testFile.getPath(),
                    "--profile ", profileFile,
                    "-o ", outFile.getPath(),});
            c.run();

            // read nanopub from file
            NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);
            String testedArtifactCode = TrustyUriUtils.getArtifactCode(testNano.getUri().toString());

            assertEquals(testedArtifactCode, transformTestCase.getSignedEntry().getArtifactCode(), "Problem with file: " + testFile.getName());
            System.out.println("File signed correctly: " + testFile.getName());

            // delete target file if everything was fine
            outFile.delete();
        }

        if (Files.exists(keyPath)) {
            Files.walk(keyPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

}