package org.nanopub;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the RoCrateImporter against a local json-ld ro-crate-metadata file.
 */
public class RoCrateImporterTest {

    static final String BASE_ROHUB_URL = "https://w3id.org/ro-id/";
    static final String RO_CRATE_ID = "7ad44bec-6784-437f-b5f3-2199b43a5303";
    static final String RO_CRATE_URL = BASE_ROHUB_URL + RO_CRATE_ID + "/";
    static final String RO_CRATE_METADATA_PATH = Objects.requireNonNull(RoCrateImporterTest.class.getResource("/")).getPath() + RO_CRATE_ID + ".jsonld";
    static final Logger logger = LoggerFactory.getLogger(RoCrateImporterTest.class);

    @Test
    void testCommandLineWithInvalidCommands() throws IOException {
        String mockedUrl = RO_CRATE_URL + "ro-crate-metadata.json";
        IRI res = RoCrateParser.constructRoCrateUrl(mockedUrl, null);
        try (MockedStatic<RoCrateParser> staticMock = Mockito.mockStatic(RoCrateParser.class)) {
            staticMock.when(() -> RoCrateParser.downloadRoCreateMetadataFile(mockedUrl))
                    .thenReturn(new FileInputStream(RO_CRATE_METADATA_PATH));
            staticMock.when(() -> RoCrateParser.constructRoCrateUrl(Mockito.any(), Mockito.any()))
                    .thenReturn(res);
            RoCrateImporter ro = CliRunner.initJc(new RoCrateImporter(), new String[]{
                    "-u", "-p",
                    mockedUrl
            });
            assertThrows(ParameterException.class, ro::run);
        }
    }

    @Test
    void testCommandLineWithExplicitLocalFile() throws TrustyUriException, NanopubAlreadyFinalizedException, MalformedNanopubException, IOException, URISyntaxException, SignatureException, InvalidKeyException, InterruptedException {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bout);
        System.setOut(ps);

        try {
            RoCrateImporter ro = CliRunner.initJc(new RoCrateImporter(), new String[]{
                    "-u",
                    "-f", RO_CRATE_METADATA_PATH,
                    RO_CRATE_URL
            });
            ro.run();
        } finally {
            System.out.flush();
            System.setOut(originalOut);
        }
        String out = bout.toString(StandardCharsets.UTF_8);
        logger.debug("Captured CLI output: {}", out);
        assertFalse(out.isBlank(), "Expected CLI to write to stdout");
    }

    @Test
    void testCommandLineWithMockedMetadataDownload() throws TrustyUriException, NanopubAlreadyFinalizedException, MalformedNanopubException, IOException, URISyntaxException, SignatureException, InvalidKeyException, InterruptedException {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bout);
        System.setOut(ps);

        String mockedUrl = RO_CRATE_URL + "ro-crate-metadata.json";
        IRI res = RoCrateParser.constructRoCrateUrl(mockedUrl, null);
        try (MockedStatic<RoCrateParser> staticMock = Mockito.mockStatic(RoCrateParser.class)) {
            staticMock.when(() -> RoCrateParser.downloadRoCreateMetadataFile(mockedUrl))
                    .thenReturn(new FileInputStream(RO_CRATE_METADATA_PATH));
            staticMock.when(() -> RoCrateParser.constructRoCrateUrl(Mockito.any(), Mockito.any()))
                    .thenReturn(res);
            RoCrateImporter ro = CliRunner.initJc(new RoCrateImporter(), new String[]{
                    "-u",
                    mockedUrl
            });
            ro.run();
        } finally {
            System.out.flush();
            System.setOut(originalOut);
        }
        String out = bout.toString(StandardCharsets.UTF_8);
        logger.debug("Captured CLI output: {}", out);
        assertFalse(out.isBlank(), "Expected CLI to write to stdout");

        Nanopub createdNanopub = new NanopubImpl(out, RDFFormat.TRIG);
        Nanopub expectedNanopub = new NanopubImpl(new File(Objects.requireNonNull(RoCrateImporterTest.class.getResource("/" + RO_CRATE_ID + ".trig")).getPath()));
        assertTrue(NanopubEquality.unsignedNanopubsAreEqual(createdNanopub, expectedNanopub));
    }

    /**
     * Runs the given action with {@code System.out} captured, and returns what was printed.
     */
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
    void mainWritesTheUnsignedNanopubToStandardOutput() throws Exception {
        String out = captureStandardOutput(() -> RoCrateImporter.main(new String[]{
                "-u", "-f", RO_CRATE_METADATA_PATH, RO_CRATE_URL}));

        assertFalse(out.isBlank());
        assertNotNull(new NanopubImpl(out, RDFFormat.TRIG).getUri());
    }

    @Test
    void writesTheSignedNanopubToStandardOutputByDefault() throws Exception {
        Nanopub signed = new NanopubImpl(new File(Objects.requireNonNull(
                RoCrateImporterTest.class.getResource("/" + RO_CRATE_ID + ".trig")).getPath()));

        try (MockedStatic<TransformContext> contextMock = Mockito.mockStatic(TransformContext.class);
             MockedStatic<SignNanopub> signMock = Mockito.mockStatic(SignNanopub.class)) {
            contextMock.when(TransformContext::makeDefault).thenReturn(Mockito.mock(TransformContext.class));
            signMock.when(() -> SignNanopub.signAndTransform(Mockito.any(), Mockito.any())).thenReturn(signed);

            String out = captureStandardOutput(() -> CliRunner.initJc(new RoCrateImporter(), new String[]{
                    "-f", RO_CRATE_METADATA_PATH, RO_CRATE_URL}).run());

            assertTrue(out.contains(signed.getUri().toString()), out);
        }
    }

    @Test
    void publishesTheSignedNanopubWhenAsked() throws Exception {
        Nanopub signed = new NanopubImpl(new File(Objects.requireNonNull(
                RoCrateImporterTest.class.getResource("/" + RO_CRATE_ID + ".trig")).getPath()));

        try (MockedStatic<TransformContext> contextMock = Mockito.mockStatic(TransformContext.class);
             MockedStatic<SignNanopub> signMock = Mockito.mockStatic(SignNanopub.class);
             MockedStatic<PublishNanopub> publishMock = Mockito.mockStatic(PublishNanopub.class)) {
            contextMock.when(TransformContext::makeDefault).thenReturn(Mockito.mock(TransformContext.class));
            signMock.when(() -> SignNanopub.signAndTransform(Mockito.any(), Mockito.any())).thenReturn(signed);
            publishMock.when(() -> PublishNanopub.publish(signed)).thenReturn("published");

            CliRunner.initJc(new RoCrateImporter(), new String[]{
                    "-p", "-f", RO_CRATE_METADATA_PATH, RO_CRATE_URL}).run();

            publishMock.verify(() -> PublishNanopub.publish(signed));
        }
    }

}
