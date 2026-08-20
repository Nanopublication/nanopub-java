package org.nanopub.extra.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Comparator;

import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.nanopub.CliRunner;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubProfile;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.SigningKeyPair;
import org.nanopub.testsuite.TestSuiteEntry;
import org.nanopub.testsuite.TestSuiteSubfolder;
import org.nanopub.testsuite.TransformTestCase;
import org.nanopub.utils.TestUtils;
import static org.nanopub.utils.TestUtils.anyIri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

import net.trustyuri.TrustyUriUtils;

class SignNanopubTest {

    private final Logger logger = LoggerFactory.getLogger(SignNanopubTest.class);

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
        Path tempDir = Files.createTempDirectory("test-output-sign-nanopub");
        File outFile = new File(tempDir.toFile(), "signed.trig");
        outFile.deleteOnExit();

        SigningKeyPair signingKeyPair = NanopubTestSuite.getLatest().getSigningKey("rsa-key1");
        String signerOrcid = "https://orcid.org/0000-0000-0000-0000";
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

            assertNotNull(SignatureUtils.getSignatureElement(testNano), "No signature element found in signed nanopub: " + testFile.getName());
            assertFalse(SignatureUtils.getSignatureElement(testNano).getSigners().isEmpty(), "No signers found in signed nanopub: " + testFile.getName());
            assertTrue(SignatureUtils.getSignatureElement(testNano).getSigners().contains(Values.iri(signerOrcid)), "Expected signer not found in signed nanopub: " + testFile.getName());
            logger.info("File signed correctly: {}", testFile.getName());
        }
    }

    @Test
    void signAndTransform2048RSA() throws Exception {
        Path tempDir = Files.createTempDirectory("test-output-sign-nanopub");
        File outFile = new File(tempDir.toFile(), "signed.trig");
        outFile.deleteOnExit();

        final String keyName = "rsa-key2";
        NanopubTestSuite suite = NanopubTestSuite.getLatest();
        SigningKeyPair keySource = suite.getSigningKey(keyName);
        String profileFile = NanopubTestSuite.getLatest().getTransformProfile().getPath();
        NanopubProfile profile = new NanopubProfile(profileFile);

        Path keyPath = Path.of(profile.getPrivateKeyPath());
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

            assertNotNull(SignatureUtils.getSignatureElement(testNano), "No signature element found in signed nanopub: " + testFile.getName());
            assertFalse(SignatureUtils.getSignatureElement(testNano).getSigners().isEmpty(), "No signers found in signed nanopub: " + testFile.getName());
            assertTrue(SignatureUtils.getSignatureElement(testNano).getSigners().contains(Values.iri(profile.getOrcidId())), "Expected signer not found in signed nanopub: " + testFile.getName());
            logger.info("File signed correctly: {}", testFile.getName());
        }

        if (Files.exists(keyPath.getParent())) {
            Files.walk(keyPath.getParent())
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

    @Test
    void loadKeyFromPemFiles() throws Exception {
        Path tempDir = Files.createTempDirectory("test-load-key-pem");
        SigningKeyPair keySource = NanopubTestSuite.getLatest().getSigningKey("rsa-key1");
        Path keyPath = tempDir.resolve("id_rsa");
        Files.writeString(keyPath, toPem(keySource.getPrivateKeyFile(), "PRIVATE KEY"));
        Files.writeString(tempDir.resolve("id_rsa.pub"), toPem(keySource.getPublicKeyFile(), "PUBLIC KEY"));

        KeyPair fromPem = SignNanopub.loadKey(keyPath.toString(), SignatureAlgorithm.RSA);
        KeyPair fromPlain = SignNanopub.loadKey(keySource.getPrivateKeyFile().getPath(), SignatureAlgorithm.RSA);

        assertArrayEquals(fromPlain.getPrivate().getEncoded(), fromPem.getPrivate().getEncoded());
        assertArrayEquals(fromPlain.getPublic().getEncoded(), fromPem.getPublic().getEncoded());
    }

    @Test
    void loadKeyWithUnsupportedFormatReportsHelpfulMessage() throws Exception {
        Path tempDir = Files.createTempDirectory("test-load-key-pkcs1");
        SigningKeyPair keySource = NanopubTestSuite.getLatest().getSigningKey("rsa-key1");
        Path keyPath = tempDir.resolve("id_rsa");
        // a key that cannot be read as PKCS#8, declaring itself to be in PKCS#1 format
        Files.writeString(keyPath, "-----BEGIN RSA PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString("not a PKCS#8 key".getBytes(StandardCharsets.UTF_8))
                + "\n-----END RSA PRIVATE KEY-----\n");
        Files.writeString(tempDir.resolve("id_rsa.pub"), toPem(keySource.getPublicKeyFile(), "PUBLIC KEY"));

        InvalidKeySpecException e = assertThrows(InvalidKeySpecException.class,
                () -> SignNanopub.loadKey(keyPath.toString(), SignatureAlgorithm.RSA));
        assertTrue(e.getMessage().contains(keyPath.toString()), "Error message should name the key file: " + e.getMessage());
        assertTrue(e.getMessage().contains("PKCS#1"), "Error message should mention the detected format: " + e.getMessage());
    }

    private String toPem(File keyFile, String label) throws IOException {
        String base64 = Files.readString(keyFile.toPath()).trim();
        return "-----BEGIN " + label + "-----\n"
                + String.join("\n", base64.split("(?<=\\G.{64})"))
                + "\n-----END " + label + "-----\n";

    }

    @Test
    void refusesToSignNanopubWithIllTypedLiteral() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, anyIri, TestUtils.vf.createLiteral("two", XSD.INTEGER));
        creator.addProvenanceStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        Nanopub np = creator.finalizeNanopub();

        SigningKeyPair signingKeyPair = NanopubTestSuite.getLatest().getSigningKey("rsa-key1");
        KeyPair key = SignNanopub.loadKey(signingKeyPair.getPrivateKeyFile().getPath(), SignatureAlgorithm.RSA);
        TransformContext context = new TransformContext(SignatureAlgorithm.RSA, key,
                Values.iri("https://orcid.org/0000-0000-0000-0000"), false, false, false);

        SignatureException ex = assertThrows(SignatureException.class, () -> SignNanopub.signAndTransform(np, context));
        assertTrue(ex.getMessage().contains("ill-typed literal(s) and cannot be signed"));
    }

    // --------------------------------------------------------------- helpers
    private static TransformContext transformContext(boolean ignoreSigned) throws Exception {
        SigningKeyPair keyPair = NanopubTestSuite.getLatest().getSigningKey("rsa-key1");
        KeyPair key = SignNanopub.loadKey(keyPair.getPrivateKeyFile().getPath(), SignatureAlgorithm.RSA);
        return new TransformContext(SignatureAlgorithm.RSA, key,
                Values.iri("https://orcid.org/0000-0000-0000-0000"), false, false, ignoreSigned);
    }

    private static Nanopub preNanopub() throws Exception {
        NanopubCreator creator = new NanopubCreator(true);
        creator.addAssertionStatement(anyIri, org.eclipse.rdf4j.model.vocabulary.RDFS.LABEL,
                TestUtils.vf.createLiteral("an assertion"));
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        return creator.finalizeNanopub();
    }

    private static File writeToFile(File directory, String name, Nanopub... nanopubs) throws Exception {
        StringBuilder trig = new StringBuilder();
        for (Nanopub np : nanopubs) {
            trig.append(np.writeToString(RDFFormat.TRIG));
        }
        File file = new File(directory, name);
        Files.writeString(file.toPath(), trig.toString());
        return file;
    }

    private static String privateKeyPath() {
        return NanopubTestSuite.getLatest().getSigningKey("rsa-key1").getPrivateKeyFile().getPath();
    }

    // ------------------------------------------------------- signAndTransform
    @Test
    void signAndTransformProducesAVerifiableSignature() throws Exception {
        Nanopub signed = SignNanopub.signAndTransform(preNanopub(), transformContext(false));

        assertTrue(TrustyUriUtils.isPotentialTrustyUri(signed.getUri()));
        assertTrue(SignatureUtils.hasValidSignature(SignatureUtils.getSignatureElement(signed)));
    }

    @Test
    void signAndTransformRefusesAnAlreadySignedNanopub() throws Exception {
        Nanopub signed = SignNanopub.signAndTransform(preNanopub(), transformContext(false));

        SignatureException ex = assertThrows(SignatureException.class,
                () -> SignNanopub.signAndTransform(signed, transformContext(false)));
        assertTrue(ex.getMessage().startsWith("Seems to have signature before signing: "), ex.getMessage());
    }

    @Test
    void signAndTransformPassesAnAlreadySignedNanopubThroughWhenIgnoringSigned() throws Exception {
        Nanopub signed = SignNanopub.signAndTransform(preNanopub(), transformContext(false));

        assertSame(signed, SignNanopub.signAndTransform(signed, transformContext(true)));
    }

    // ------------------------------------------- signAndTransformMultiNanopub
    @Test
    void signAndTransformMultiNanopubFromAStream() throws Exception {
        String trig = preNanopub().writeToString(RDFFormat.TRIG);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SignNanopub.signAndTransformMultiNanopub(RDFFormat.TRIG,
                new ByteArrayInputStream(trig.getBytes(StandardCharsets.UTF_8)), transformContext(false), out);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("hasSignature"));
    }

    @Test
    void signAndTransformMultiNanopubFromAFile() throws Exception {
        Path tempDir = Files.createTempDirectory("test-sign-multi");
        File input = writeToFile(tempDir.toFile(), "input.trig", preNanopub());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SignNanopub.signAndTransformMultiNanopub(RDFFormat.TRIG, input, transformContext(false), out);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("hasSignature"));
    }

    // ------------------------------------------------------------------- CLI
    @Test
    void writesNextToTheInputWhenNoOutputFileIsGiven() throws Exception {
        Path tempDir = Files.createTempDirectory("test-sign-default-output");
        File input = writeToFile(tempDir.toFile(), "input.trig", preNanopub());

        CliRunner.initJc(new SignNanopub(), new String[]{
            "-k", privateKeyPath(), "-s", "https://orcid.org/0000-0000-0000-0000", input.getPath()}).run();

        File output = new File(tempDir.toFile(), "signed.input.trig");
        assertTrue(output.exists());
        assertTrue(TrustyUriUtils.isPotentialTrustyUri(new NanopubImpl(output, RDFFormat.TRIG).getUri()));
    }

    @Test
    void writesAGzippedSingleOutputFile() throws Exception {
        Path tempDir = Files.createTempDirectory("test-sign-gz-output");
        File input = writeToFile(tempDir.toFile(), "input.trig", preNanopub());
        File output = new File(tempDir.toFile(), "out.trig.gz");

        CliRunner.initJc(new SignNanopub(), new String[]{
            "-k", privateKeyPath(), "-s", "https://orcid.org/0000-0000-0000-0000",
            "-o", output.getPath(), input.getPath()}).run();

        assertTrue(output.exists());
        try (java.io.InputStream in = new java.util.zip.GZIPInputStream(new java.io.FileInputStream(output))) {
            assertTrue(TrustyUriUtils.isPotentialTrustyUri(new NanopubImpl(in, RDFFormat.TRIG).getUri()));
        }
    }

    @Test
    void readsAndWritesGzippedFiles() throws Exception {
        Path tempDir = Files.createTempDirectory("test-sign-gz-input");
        File plain = writeToFile(tempDir.toFile(), "plain.trig", preNanopub());
        File input = new File(tempDir.toFile(), "input.trig.gz");
        try (java.io.OutputStream out = new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream(input))) {
            Files.copy(plain.toPath(), out);
        }

        CliRunner.initJc(new SignNanopub(), new String[]{
            "-k", privateKeyPath(), "-s", "https://orcid.org/0000-0000-0000-0000", input.getPath()}).run();

        File output = new File(tempDir.toFile(), "signed.input.trig.gz");
        assertTrue(output.exists());
        try (java.io.InputStream in = new java.util.zip.GZIPInputStream(new java.io.FileInputStream(output))) {
            assertTrue(TrustyUriUtils.isPotentialTrustyUri(new NanopubImpl(in, RDFFormat.TRIG).getUri()));
        }
    }

    @Test
    void refusesToRunWithoutASigner() throws Exception {
        Path tempDir = Files.createTempDirectory("test-sign-no-signer");
        File input = writeToFile(tempDir.toFile(), "input.trig", preNanopub());
        File emptyProfile = new File(tempDir.toFile(), "profile.yaml");
        Files.writeString(emptyProfile.toPath(), "private_key: " + privateKeyPath() + "\n");

        SignNanopub signer = CliRunner.initJc(new SignNanopub(),
                new String[]{"--profile", emptyProfile.getPath(), input.getPath()});

        Exception ex = assertThrows(Exception.class, signer::run);
        assertTrue(ex.getMessage().contains("No valid signer specified"), ex.getMessage());
    }

    @Test
    void picksTheDsaAlgorithmForDsaKeyFiles() throws Exception {
        Path tempDir = Files.createTempDirectory("test-sign-dsa");
        File input = writeToFile(tempDir.toFile(), "input.trig", preNanopub());

        SignNanopub signer = CliRunner.initJc(new SignNanopub(), new String[]{
            "-k", tempDir + "/missing_dsa", "-s", "https://orcid.org/0000-0000-0000-0000", input.getPath()});

        // the key cannot be read, but the file name has already selected the DSA algorithm
        assertThrows(Exception.class, signer::run);
    }

    @Test
    void mainSignsTheNanopub() throws Exception {
        Path tempDir = Files.createTempDirectory("test-sign-main");
        File input = writeToFile(tempDir.toFile(), "input.trig", preNanopub());
        File output = new File(tempDir.toFile(), "out.trig");

        SignNanopub.main(new String[]{"-v", "-k", privateKeyPath(),
            "-s", "https://orcid.org/0000-0000-0000-0000", "-o", output.getPath(), input.getPath()});

        assertTrue(output.exists());
        assertTrue(TrustyUriUtils.isPotentialTrustyUri(new NanopubImpl(output, RDFFormat.TRIG).getUri()));
    }

}
