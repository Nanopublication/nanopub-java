package org.nanopub.extra.server;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.CliRunner;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;
import org.nanopub.utils.MockFileService;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

public class GetNanopubTest {

    @Test
    public void testGetNanopub() throws Exception {
        String outPath = Objects.requireNonNull(this.getClass().getResource("/")).getPath() + "test-output/get-nanopub/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath + "out.trig");

        String artifactCode = "RAWH0fe1RCpoOgaJE1B2qfTzzdTiBUUK7iIk6l7Zll9mg";
        String nanopubUrl = "https://w3id.org/np/" + artifactCode;
        new MockFileService();
        Nanopub nanopub = new NanopubImpl(new File(MockFileService.getValidAndSignedNanopubFromId(artifactCode)));

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
            outFile.delete();
        }
    }

    // TODO need to be refined since not all the network calls are mocked yet
    @Test
    public void testGetIndex() throws Exception {
        String outPath = Objects.requireNonNull(this.getClass().getResource("/")).getPath() + "test-output/get-nanopub/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath + "out.trig");

        int expectedNanopubs = 102; // number of nanopubs in the index
        String artifactCode = "RApww43dy8UvCoEc8QKOaXhojCTgao3ZXX_d6V_jVBo6s";
        String nanopubUrl = "https://w3id.org/fair/fip/np/index/" + artifactCode;
        new MockFileService();
        Nanopub nanopub = new NanopubImpl(new File(MockFileService.getValidAndSignedNanopubFromId(artifactCode)));

        try (MockedStatic<GetNanopub> mockedStatic = mockStatic(GetNanopub.class)) {
            mockedStatic.when(() -> GetNanopub.get(nanopubUrl)).thenReturn(nanopub);

            // download index nanopub itself and create file
            GetNanopub cli1 = CliRunner.initJc(new GetNanopub(), new String[]{
                    nanopubUrl,
                    "-i ",
                    "-o ", outFile.getPath()});
            cli1.run();

            // read created nanopub file and test validity
            NanopubImpl testIndex = new NanopubImpl(outFile, RDFFormat.TRIG);
            assertTrue(IndexUtils.isIndex(testIndex));
            NanopubIndex indexNano = IndexUtils.castToIndex(testIndex);
            assertFalse(indexNano.isIncomplete());

            HashSet<String> indexedNanopubs = new HashSet<>();
            for (Statement st : indexNano.getAssertion()) {
                indexedNanopubs.add(st.getObject().stringValue());
            }

            assertEquals(expectedNanopubs, indexedNanopubs.size());

            // now download the indexed nanopubs into a file
            File indexContentFile = new File(outPath + "content.trig");
            GetNanopub cli2 = CliRunner.initJc(new GetNanopub(), new String[]{
                    nanopubUrl,
                    "-c ",
                    "-o ", indexContentFile.getPath()});
            cli2.run();

            // read created multi-nanopub file
            HashSet<String> containedNanopubs = new HashSet<>();
            MultiNanopubRdfHandler.process(indexContentFile, np -> {
                containedNanopubs.add(np.getUri().toString());
                assertTrue(indexedNanopubs.remove(np.getUri().toString()));
            });
            assertEquals(expectedNanopubs, containedNanopubs.size());
            outFile.delete();
            indexContentFile.delete();
        }
    }

}
