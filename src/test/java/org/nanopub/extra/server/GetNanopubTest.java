package org.nanopub.extra.server;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Test;
import org.nanopub.CliRunner;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;

import java.io.File;
import java.util.HashSet;

import static org.junit.Assert.*;

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
                "-o ", outFile.getPath()});
        c.run();

        // read created nanopub file
        NanopubImpl testNano = new NanopubImpl(outFile, RDFFormat.TRIG);
        IRI resultUri = testNano.getUri();

        assertEquals(resultUri.stringValue(), nanopubUrl);

        outFile.delete();
    }

    @Test
    public void testGetIndex() throws Exception {
        String outPath = "target/test-output/get-nanopub/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath + "out.trig");

        String nanopubUrl = "https://w3id.org/fair/fip/np/index/RALMoJU3xZoCyWlVAzxtbwct7W22AU-LLcE4U8QrnmUVM";

        // download index nanopub itself and create file
        GetNanopub cli1 = CliRunner.initJc(new GetNanopub(), new String[] {
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

        assertEquals(89, indexedNanopubs.size());

        // now download the indexed nanopubs into a file
        File indexContentFile = new File(outPath + "content.trig");
        GetNanopub cli2 = CliRunner.initJc(new GetNanopub(), new String[] {
                nanopubUrl,
                "-c ",
                "-o ", indexContentFile.getPath()});
        cli2.run();

        // read created multi-nanopub file
        HashSet<String> containedNanopubs = new HashSet<>();
        MultiNanopubRdfHandler.process(indexContentFile, new MultiNanopubRdfHandler.NanopubHandler() {
            @Override
            public void handleNanopub(Nanopub np) {
                containedNanopubs.add(np.getUri().toString());
                assertTrue(indexedNanopubs.remove(np.getUri().toString()));
            }
        });
        outFile.delete();
        indexContentFile.delete();

    }
}
