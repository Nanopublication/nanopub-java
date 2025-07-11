package org.nanopub;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;

public class RoCrateIT {

    Nanopub createNpFromRoCrate (String url, String metadata) throws Exception {
        RoCrateParser parser = new RoCrateParser();
        Nanopub np = parser.parseRoCreate(url, metadata);

        Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
        NanopubUtils.writeToStream(signedNp, System.err, RDFFormat.TRIG);
        return signedNp;
    }

    @Test
    void examples () throws Exception {
        String url0 = "https://rawcdn.githack.com/biocompute-objects/bco-ro-example-chipseq/76cb84c8d6a17a3fd7ae3102f68de3f780458601/data/";
        String metadata0 = "ro-crate-metadata.json";
        createNpFromRoCrate(url0, metadata0);

        String url1 = "https://www.researchobject.org/ro-crate/specification/1.1/";
        String metadata1 = "ro-crate-metadata.jsonld";
        createNpFromRoCrate(url1, metadata1);

        String url2 = "https://zenodo.org/records/3541888/files/";
        String metadata2 = "ro-crate-metadata.jsonld";
        createNpFromRoCrate(url2, metadata2);

        // The following examples do not work, since there are spaces in @id elements, which should convert to urls

        //        String url3 = "http://mod.paradisec.org.au/repository/72/b3/dc/14/01/c8/ff/06/aa/cb/a0/99/0a/12/8f/c1/13/cf/9a/d5/27/5f/49/4b/05/c1/14/21/77/35/65/61/bd/7f/4c/0e/88/00/ba/de/2c/bb/be/d7/5f/6d/9d/01/98/94/73/5a/d7/e4/07/62/68/4d/24/3a/44/2d/65/8a/v1/content/";
        //        String metadata3 = "ro-crate-metadata.json";
        //        createNpFromRoCrate(url3, metadata3);

        //        String url4 = "https://workflowhub.eu/ga4gh/trs/v2/tools/26/versions/1/PLAIN_CWL/descriptor/";
        //        String metadata4 = "ro-crate-metadata.json";
        //        createNpFromRoCrate(url4, metadata4);
    }

//    @Test
    void moreExamples() throws Exception {
        String p0 = "https://mod.paradisec.org.au/repository/46/0d/05/87/db/45/55/64/09/01/35/fa/21/da/c5/43/10/48/b3/28/84/2e/1e/c9/c4/9e/f2/81/f2/92/cd/d1/de/9d/8a/e8/f1/37/e8/5f/66/d1/06/c8/e7/0d/82/71/d9/c0/f1/36/97/71/9a/24/7a/7e/d1/ab/75/fa/46/4a/v1/content/";
        String p1 = "https://mod.paradisec.org.au/repository/00/8a/19/78/29/09/bf/18/98/da/3c/03/88/99/f1/9d/f9/32/35/97/4d/86/be/6c/84/99/39/31/32/d9/3a/b3/0a/c2/5e/93/4a/12/65/57/ef/aa/a0/9b/da/bb/0b/ad/3b/fa/5d/3e/da/67/77/d0/66/c7/6d/d9/1d/4c/07/b0/v1/content/ro-crate-metadata.json";
        String m0 = "ro-crate-metadata.json";
        Nanopub np = createNpFromRoCrate(p0, m0);
//        PublishNanopub.publish(np);
    }

    //

}