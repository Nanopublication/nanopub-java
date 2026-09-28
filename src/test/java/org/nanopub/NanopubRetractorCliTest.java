package org.nanopub;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.SigningKeyPair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class NanopubRetractorCliTest {

    private static final String SIGNER = "https://orcid.org/0000-0002-4808-1845";

    private static File nanopubToRetract() {
        return NanopubTestSuite.getLatest().getTransformCases("rsa-key2").getFirst().getSignedEntry().toFile();
    }

    private static String privateKeyPath() {
        SigningKeyPair keyPair = NanopubTestSuite.getLatest().getSigningKey("rsa-key2");
        return keyPair.getPrivateKeyFile().getPath();
    }

    private static String captureStandardOutput(ThrowingRunnable action) throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            action.run();
        } finally {
            System.out.flush();
            System.setOut(originalOut);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Test
    void writesTheRetractionToStandardOutput() throws Exception {
        NanopubRetractorCli cli = CliRunner.initJc(new NanopubRetractorCli(), new String[]{
                "-i", nanopubToRetract().getPath(),
                "-k", privateKeyPath(),
                "-s", SIGNER});

        String printed = captureStandardOutput(cli::run);

        assertFalse(printed.isBlank());
        assertTrue(printed.contains("retracts"), printed);
    }

    @Test
    void publishesTheRetractionWhenAsked() throws Exception {
        NanopubRetractorCli cli = CliRunner.initJc(new NanopubRetractorCli(), new String[]{
                "-i", nanopubToRetract().getPath(),
                "-k", privateKeyPath(),
                "-s", SIGNER,
                "-p"});

        try (MockedStatic<PublishNanopub> publishMock = mockStatic(PublishNanopub.class)) {
            publishMock.when(() -> PublishNanopub.publish(any())).thenReturn("published");

            String printed = captureStandardOutput(cli::run);

            assertTrue(printed.contains("Publishing Retraction Nanopub."), printed);
            publishMock.verify(() -> PublishNanopub.publish(any()));
        }
    }

    @Test
    void fetchesTheNanopubToRetractFromItsUrl() throws Exception {
        Nanopub toRetract = new NanopubImpl(nanopubToRetract());
        String url = "https://example.org/np1";
        NanopubRetractorCli cli = CliRunner.initJc(new NanopubRetractorCli(), new String[]{
                "-i", url,
                "-k", privateKeyPath(),
                "-s", SIGNER});

        try (MockedStatic<GetNanopub> getMock = mockStatic(GetNanopub.class)) {
            getMock.when(() -> GetNanopub.get(url)).thenReturn(toRetract);

            String printed = captureStandardOutput(cli::run);

            assertTrue(printed.contains("retracts"), printed);
        }
    }

    /**
     * Stubs the profile file, so that the tests do not depend on whatever is in the developer's
     * {@code ~/.nanopub/profile.yaml}.
     */
    private static MockedConstruction<NanopubProfile> profileWith(String orcidId, String privateKeyPath) {
        return mockConstruction(NanopubProfile.class, (profile, context) -> {
            when(profile.getOrcidId()).thenReturn(orcidId);
            when(profile.getPrivateKeyPath()).thenReturn(privateKeyPath);
        });
    }

    @Test
    void takesTheSignerFromTheProfileWhenNotGiven() throws Exception {
        NanopubRetractorCli cli = CliRunner.initJc(new NanopubRetractorCli(), new String[]{
                "-i", nanopubToRetract().getPath(),
                "-k", privateKeyPath()});

        try (MockedConstruction<NanopubProfile> ignored = profileWith(SIGNER, privateKeyPath())) {
            assertTrue(captureStandardOutput(cli::run).contains("retracts"));
        }
    }

    @Test
    void takesTheKeyFromTheProfileWhenNotGiven() throws Exception {
        NanopubRetractorCli cli = CliRunner.initJc(new NanopubRetractorCli(), new String[]{
                "-i", nanopubToRetract().getPath(),
                "-s", SIGNER});

        try (MockedConstruction<NanopubProfile> ignored = profileWith(SIGNER, privateKeyPath())) {
            assertTrue(captureStandardOutput(cli::run).contains("retracts"));
        }
    }

    @Test
    void fallsBackToTheDefaultKeyPathWhenTheProfileHasNone() {
        NanopubRetractorCli cli = CliRunner.initJc(new NanopubRetractorCli(), new String[]{
                "-i", nanopubToRetract().getPath(),
                "-s", SIGNER});

        try (MockedConstruction<NanopubProfile> ignored = profileWith(SIGNER, null)) {
            // the default key path either holds no key at all, or one that does not match the
            // nanopub being retracted; either way the run cannot succeed
            assertThrows(Exception.class, cli::run);
        }
    }

    @Test
    void leavesTheSignerUnsetWhenNeitherTheOptionNorTheProfileHasOne() {
        NanopubRetractorCli cli = CliRunner.initJc(new NanopubRetractorCli(), new String[]{
                "-i", nanopubToRetract().getPath(),
                "-k", privateKeyPath()});

        try (MockedConstruction<NanopubProfile> ignored = profileWith(null, privateKeyPath())) {
            assertThrows(NullPointerException.class, cli::run);
        }
    }

    @Test
    void reportsAnUnreadableKey() {
        NanopubRetractorCli cli = CliRunner.initJc(new NanopubRetractorCli(), new String[]{
                "-i", nanopubToRetract().getPath(),
                "-k", "/does/not/exist/id_rsa",
                "-s", SIGNER});

        RuntimeException ex = assertThrows(RuntimeException.class, cli::run);
        assertTrue(ex.getMessage().startsWith("Could not load key: "), ex.getMessage());
    }

    @Test
    void picksTheDsaAlgorithmForDsaKeyFiles() {
        NanopubRetractorCli cli = CliRunner.initJc(new NanopubRetractorCli(), new String[]{
                "-i", nanopubToRetract().getPath(),
                "-k", "/does/not/exist/id_dsa",
                "-s", SIGNER});

        // the key cannot be read, but the file name has already selected the DSA algorithm
        RuntimeException ex = assertThrows(RuntimeException.class, cli::run);
        assertTrue(ex.getMessage().startsWith("Could not load key: "), ex.getMessage());
    }

    @Test
    void mainWritesTheRetractionToStandardOutput() throws Exception {
        String printed = captureStandardOutput(() -> NanopubRetractorCli.main(new String[]{
                "-i", nanopubToRetract().getPath(),
                "-k", privateKeyPath(),
                "-s", SIGNER}));

        assertTrue(printed.contains("retracts"), printed);
    }

}
