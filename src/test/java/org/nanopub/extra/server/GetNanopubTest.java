package org.nanopub.extra.server;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Test;
import org.nanopub.CliRunner;
import org.nanopub.NanopubImpl;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class GetNanopubTest {

    @Test
    public void testGetNanopub() throws Exception {
        String outPath = "target/test-output/get-nanopub/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath + "out.trig");

        String nanopubUrl = "https://w3id.org/np/RAWH0fe1RCpoOgaJE1B2qfTzzdTiBUUK7iIk6l7Zll9mg";

        // download nanopub and create file
        GetNanopub c = CliRunner.initJc(new GetNanopub(), new String[] {
                nanopubUrl,
                "-o ", outFile.getPath(),});
        c.run();

        // read created nanopub file
        NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);
        IRI resultUri = testNano.getUri();

        assertEquals(resultUri.stringValue(), nanopubUrl);

        outFile.delete();
    }
}
