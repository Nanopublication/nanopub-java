package org.nanopub.extra.server;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.CliRunner;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.utils.MockFileService;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

}
