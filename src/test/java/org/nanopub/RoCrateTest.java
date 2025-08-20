package org.nanopub;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.nanopub.vocabulary.NPX;

import java.io.FileInputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the RoCrateImporter and RoCrateParser against a local json-ld ro-crate-metadata file.
 */
public class RoCrateTest {

    final String roCrateUrl = "https://w3id.org/ro-id/7ad44bec-6784-437f-b5f3-2199b43a5303/";
    final String roCrateMetadataPath = Objects.requireNonNull(this.getClass().getResource("/")).getPath() + "7ad44bec-6784-437f-b5f3-2199b43a5303.jsonld";

    @Test
    void testParseRoCrateMetadata() throws Exception {
        Nanopub np = new RoCrateParser().parseRoCreate(roCrateUrl, new FileInputStream(roCrateMetadataPath));
        assertEquals(310, np.getTripleCount());
        List<Statement> typePred = np.getPubinfo().stream().filter(st -> st.getPredicate().equals(RDF.TYPE))
                .toList();
        assertEquals(1, typePred.size());
        assertEquals(NPX.RO_CRATE_NANOPUB, typePred.getFirst().getObject());
    }

    @Test
    void testCommandLineWithExplicitLocalFile () throws Exception {
        RoCrateImporter ro = CliRunner.initJc(new RoCrateImporter(), new String[] {
                "-l",
                "-f", roCrateMetadataPath,
                roCrateUrl
        });
        ro.run();
    }

    @Test
    void testCommandLineWithMockedMetadataDownload () throws Exception {
        String mockedUrl = roCrateUrl + "ro-crate-metadata.json";
        try (MockedStatic<RoCrateParser> staticMock = Mockito.mockStatic(RoCrateParser.class)) {
            staticMock.when(() -> RoCrateParser.downloadRoCreateMetadataFile(mockedUrl)).thenReturn(new FileInputStream(roCrateMetadataPath));

            RoCrateImporter ro = CliRunner.initJc(new RoCrateImporter(), new String[]{
                    "-l",
                    mockedUrl
            });
            ro.run();
        }
    }

}
