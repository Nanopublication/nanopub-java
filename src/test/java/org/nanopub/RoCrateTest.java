package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the RoCrateImporter and RoCrateParser against a local json-ld ro-crate-metadata file.
 */
public class RoCrateTest {

    final String roCrateUrl = "https://w3id.org/ro-id/7ad44bec-6784-437f-b5f3-2199b43a5303/";
    final String roCrateMetadataPath = Objects.requireNonNull(this.getClass().getResource("/")).getPath() + "7ad44bec-6784-437f-b5f3-2199b43a5303.jsonld";
    static final Logger log = LoggerFactory.getLogger(RoCrateTest.class);

    @Test
    void testCommandLineWithExplicitLocalFile() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bout);
        System.setOut(ps);

        try {
            RoCrateImporter ro = CliRunner.initJc(new RoCrateImporter(), new String[]{
                    "-u",
                    "-f", roCrateMetadataPath,
                    roCrateUrl
            });
            ro.run();
        } finally {
            System.out.flush();
            System.setOut(originalOut);
        }
        String out = bout.toString(StandardCharsets.UTF_8);
        log.debug("Captured CLI output: {}", out);
        assertFalse(out.isBlank(), "Expected CLI to write to stdout");
    }

    @Test
    void testCommandLineWithMockedMetadataDownload() {
        String mockedUrl = roCrateUrl + "ro-crate-metadata.json";
        IRI res = RoCrateParser.constructRoCrateUrl(mockedUrl, null);
        try (MockedStatic<RoCrateParser> staticMock = Mockito.mockStatic(RoCrateParser.class)) {
            staticMock.when(() -> RoCrateParser.downloadRoCreateMetadataFile(mockedUrl))
                    .thenReturn(new FileInputStream(roCrateMetadataPath));
            staticMock.when(() -> RoCrateParser.constructRoCrateUrl(Mockito.any(), Mockito.any()))
                    .thenReturn(res);
            RoCrateImporter ro = CliRunner.initJc(new RoCrateImporter(), new String[]{
                    "-u",
                    mockedUrl
            });
            ro.run();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertTrue(true);
    }

}
