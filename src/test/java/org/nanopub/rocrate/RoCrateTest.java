package org.nanopub.rocrate;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.RoCrateParser;

public class RoCrateTest {

    @Test
    void foo () throws Exception {
        String exampleUrl = "https://rawcdn.githack.com/biocompute-objects/bco-ro-example-chipseq/76cb84c8d6a17a3fd7ae3102f68de3f780458601/data/";
        RoCrateParser parser = new RoCrateParser();
        Nanopub np = parser.parseRoCreate(exampleUrl);

        NanopubUtils.writeToStream(np, System.err, RDFFormat.TRIG);
    }

}
