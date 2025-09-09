package org.nanopub.extra.index;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MakeIndexTest {

    @Test
    void testMakeIndex() throws IOException, MalformedNanopubException {
        List<String> nanopubFiles = getNanopubFiles();
        File outFile = getOutputFileForIndex();

        List<String> prepareArgs = new ArrayList<>(nanopubFiles);
        prepareArgs.add("-o");
        prepareArgs.add(outFile.getAbsolutePath());
        String[] args = prepareArgs.toArray(new String[0]);

        // create the index
        MakeIndex.main(args);

        // read index
        NanopubIndex index = new NanopubIndexImpl(new NanopubImpl(outFile));

        // compare index with expected IRIs
        Set<IRI> nanopubIris = new HashSet<>();
        for (String nanopubFile : nanopubFiles) {
            nanopubIris.add(new NanopubImpl(new File(nanopubFile)).getUri());
        }
        assertEquals(nanopubIris, index.getElements());
    }

    private File getOutputFileForIndex() {
        String outPath = Objects.requireNonNull(this.getClass().getResource("/")).getPath() + "test-output/index/";
        new File(outPath).mkdirs();
        File outFile = new File(outPath + "index.trig");
        return outFile;
    }

    private List<String> getNanopubFiles () {
        File[] files =  new File(this.getClass().getResource("/testsuite/valid/signed/").getPath()).listFiles();
        return Arrays.asList(files).stream()
                .limit(100) // make it more stable when testsuite changes
                .map(File::getPath)
                .filter(s -> s.endsWith("1024.trig")) // filter for one keysize, to get rid of duplicates
                .toList();
    }

}