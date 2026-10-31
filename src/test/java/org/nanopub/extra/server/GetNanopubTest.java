package org.nanopub.extra.server;

import net.trustyuri.ArtifactCode;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.nanopub.CliRunner;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteEntry;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class GetNanopubTest {

    @Test
    public void testGetNanopub() throws Exception {
        String outPath = Objects.requireNonNull(this.getClass().getResource("/")).getPath() + "test-output/get-nanopub/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath + "out.trig");

        ArtifactCode artifactCode = ArtifactCode.of("RAWH0fe1RCpoOgaJE1B2qfTzzdTiBUUK7iIk6l7Zll9mg");
        String nanopubUrl = "https://w3id.org/np/" + artifactCode;
        TestSuiteEntry entry = NanopubTestSuite.getLatest()
                .getByArtifactCode(artifactCode.toString())
                .getFirst();
        Nanopub nanopub = new NanopubImpl(entry.toFile());

        try (MockedStatic<GetNanopub> mockedStatic = mockStatic(GetNanopub.class)) {
            mockedStatic.when(() -> GetNanopub.get(nanopubUrl)).thenReturn(nanopub);

            // download nanopub and create file
            GetNanopub c = CliRunner.initJc(new GetNanopub(), new String[]{
                    nanopubUrl,
                    "-o ", outFile.getPath()});
            c.run();

            // read created nanopub file
            NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);

            assertEquals(nanopub, testNano);
            assertEquals(0, c.exitStatus());
            outFile.delete();
        }
    }

    @Test
    void exitStatus_withNanopubNotFound_isOne(@TempDir File tempDir) throws Exception {
        try (MockedStatic<GetNanopub> ignored = mockStatic(GetNanopub.class)) {
            GetNanopub c = CliRunner.initJc(new GetNanopub(), new String[]{
                    MISSING_NANOPUB, "-o", new File(tempDir, "out.trig").getPath()});

            c.run();

            assertEquals(1, c.exitStatus());
        }
    }

    @Test
    void exitStatus_withIndexNanopubsNotRetrieved_isOne(@TempDir File tempDir) throws Exception {
        try (MockedConstruction<FetchIndex> ignored = mockConstruction(FetchIndex.class,
                (fetchIndex, context) -> when(fetchIndex.getFailedCount()).thenReturn(2))) {
            GetNanopub c = CliRunner.initJc(new GetNanopub(), new String[]{
                    "-c", MISSING_NANOPUB, "-o", new File(tempDir, "out.trig").getPath()});

            c.run();

            assertEquals(1, c.exitStatus());
        }
    }

    @Test
    void exitStatus_withIndexFullyRetrieved_isZero(@TempDir File tempDir) throws Exception {
        try (MockedConstruction<FetchIndex> ignored = mockConstruction(FetchIndex.class)) {
            GetNanopub c = CliRunner.initJc(new GetNanopub(), new String[]{
                    "-c", MISSING_NANOPUB, "-o", new File(tempDir, "out.trig").getPath()});

            c.run();

            assertEquals(0, c.exitStatus());
        }
    }

    private static final String MISSING_NANOPUB = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

}
