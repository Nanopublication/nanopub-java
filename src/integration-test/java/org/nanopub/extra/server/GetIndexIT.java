package org.nanopub.extra.server;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nanopub.CliRunner;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;
import org.nanopub.utils.MockFileService;
import org.nanopub.utils.MockFileServiceExtension;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test which uses GetNanopub, GetIndex and the Nanopub network.
 */
@ExtendWith(MockFileServiceExtension.class)
public class GetIndexIT {

    @Test
    public void testGetIndex() throws Exception {
        String outPath = Objects.requireNonNull(this.getClass().getResource("/")).getPath() + "target/test-output/get-nanopub/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath + "out.trig");

        int expectedNanopubs = 102; // number of nanopubs in the index
        String artifactCode = "RApww43dy8UvCoEc8QKOaXhojCTgao3ZXX_d6V_jVBo6s";
        String nanopubUrl = "https://w3id.org/fair/fip/np/index/" + artifactCode;
        Nanopub npFromFilesystem = new NanopubImpl(new File(MockFileService.getValidAndSignedNanopubFromId(artifactCode)));

        // download index nanopub itself and create file
        GetNanopub cli1 = CliRunner.initJc(new GetNanopub(), new String[]{nanopubUrl, "-i ", "-o ", outFile.getPath()});
        cli1.run();

        // read created nanopub file and test validity
        NanopubImpl testIndex = new NanopubImpl(outFile, RDFFormat.TRIG);
        assertTrue(IndexUtils.isIndex(testIndex));
        NanopubIndex indexNano = IndexUtils.castToIndex(testIndex);
        assertFalse(indexNano.isIncomplete());
        assertEquals(npFromFilesystem, testIndex);

        HashSet<String> indexedNanopubs = new HashSet<>();
        for (Statement st : indexNano.getAssertion()) {
            indexedNanopubs.add(st.getObject().stringValue());
        }

        assertEquals(expectedNanopubs, indexedNanopubs.size());

        // now download the indexed nanopubs into a file
        File indexContentFile = new File(outPath + "content.trig");
        GetNanopub cli2 = CliRunner.initJc(new GetNanopub(), new String[]{nanopubUrl, "-c ", "-o ", indexContentFile.getPath()});
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
