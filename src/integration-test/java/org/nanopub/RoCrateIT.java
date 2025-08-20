package org.nanopub;

import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Objects;

public class RoCrateIT {

    @BeforeAll
    static void makeSureKeysAreAvailable() throws IOException {
        GeneralIntegrationTestsIT.makeSureKeysAreAvailable();
    }

    @Test
    void importFromFile() throws IOException, MalformedNanopubException, TrustyUriException, SignatureException, InvalidKeyException {
        String url = "https://w3id.org/ro-id/588ada8d-a185-402e-8b60-3c17435110ee/";
        String filename = Objects.requireNonNull(RoCrateTest.class.getResource("/588ada8d-a185-402e-8b60-3c17435110ee.jsonld").getPath());

        FileInputStream metadata = new FileInputStream(filename);
        RoCrateParser parser = new RoCrateParser();
        Nanopub np = parser.parseRoCreate(url, metadata);

        Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
        NanopubUtils.writeToStream(signedNp, System.err, RDFFormat.TRIG);

//        PublishNanopub.publish(signedNp);
    }

    Nanopub createNpFromRoCrate (String url, String metadataFilename) throws Exception {
        InputStream metadata = RoCrateParser.downloadRoCreateMetadataFile(url + metadataFilename);
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
        String metadataFilename = "ro-crate-metadata.json";

        String[] extracted = {
                // https://mod.paradisec.org.au/advanced-search
                "https://mod.paradisec.org.au/repository/7f/a0/21/11/ec/0e/d7/07/2f/4a/fd/84/d0/2d/16/96/b2/6c/93/2c/0d/73/38/9d/6f/3d/54/ce/1d/2f/8d/f6/6c/a9/90/65/b4/1e/6d/22/ad/f5/d7/97/bc/61/0c/2c/de/7b/5b/0f/15/07/18/85/da/d0/25/16/39/6a/aa/d5/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/9f/2b/3b/53/0a/69/1d/a3/01/8d/91/18/e3/c7/d3/3b/ff/c8/16/c3/67/fb/10/5a/60/09/5c/8a/c1/99/6d/28/e2/f9/99/db/99/36/0d/b0/55/94/66/13/4d/79/42/15/23/39/63/18/4a/e7/e3/f8/e0/a1/15/26/22/48/bc/a9/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/bb/dc/ce/4a/9d/75/64/2a/8a/cd/14/84/a4/70/ea/e8/de/28/2c/1c/95/3b/08/a2/02/cb/b9/08/e0/e3/8a/b8/04/c7/d2/66/a4/77/c0/00/b2/5f/95/40/0b/12/22/af/5a/e9/91/39/65/9e/eb/8b/67/5e/56/dd/fd/08/b5/e4/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/fa/6c/53/25/31/a8/28/e2/3d/bb/ee/f4/1d/ee/7a/a1/56/bb/f3/6a/07/45/9e/8b/e8/b1/38/fc/b4/ab/fd/0c/fd/df/75/7f/2c/b8/d6/6e/1e/12/e1/2c/6a/ab/5d/a6/5a/6f/3b/83/ce/2e/02/e6/34/11/a6/b5/e4/b6/ac/36/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/bb/04/16/fd/ce/12/8d/78/10/8e/59/f7/97/32/c4/e4/e4/8d/29/e8/20/86/e6/58/54/f5/da/b3/48/e8/8c/c1/aa/2b/5c/08/18/cb/b3/1f/e9/8e/de/f1/7e/57/f7/be/8c/0c/14/32/05/dc/ad/15/77/bf/be/cc/38/64/96/08/v1/content/ro-crate-metadata.json",
                // search page 2
                "https://mod.paradisec.org.au/repository/fa/41/67/ba/4d/df/f4/7a/3b/ed/a4/07/f0/4e/fd/eb/89/11/c6/ca/c2/cc/94/f2/5b/9f/10/27/5b/25/a5/2a/25/c8/16/d9/18/7c/5a/b1/62/fa/7b/66/5b/e1/0d/d8/bc/8d/fa/c5/cd/c4/9e/dd/d0/88/80/cc/bf/0d/70/00/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/00/85/b7/4c/97/ca/9f/68/60/ef/2a/01/c7/b3/84/da/96/94/03/68/26/d5/b7/62/9a/30/6c/af/60/25/9d/16/30/48/af/2e/f2/de/93/4d/03/39/3e/a3/51/c6/f5/bc/45/01/28/6c/5c/34/2c/fd/37/6c/c7/cd/0d/db/5d/23/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/24/9d/6b/1d/7a/d2/f9/bf/aa/c2/7b/96/7d/4c/86/13/96/73/ec/ae/19/ff/6d/37/3d/b9/6e/5a/a4/77/0b/2c/9f/bd/1e/c0/fe/af/43/31/15/8f/bd/5f/df/43/17/2a/78/f2/c6/34/3a/83/92/60/ec/55/d0/54/69/0a/d5/00/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/80/21/c2/b2/6a/70/a0/a9/f9/73/3c/50/eb/18/e6/e2/67/da/9a/f8/eb/03/6f/c8/28/d3/ec/d9/a2/7a/09/d9/31/de/c9/db/47/ff/6f/81/dd/c8/0c/b2/b3/6d/d5/78/33/16/5b/ec/4a/d5/18/6c/94/90/83/f1/86/94/75/b2/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/d8/3f/ed/b5/dd/e6/d8/de/33/ca/ad/4a/18/dd/97/29/93/bd/86/43/14/c5/91/43/60/10/7f/6b/fb/36/da/8b/87/c6/66/4e/ed/b0/1c/f0/cd/3d/63/f7/f8/3e/de/17/80/93/69/08/a9/21/cc/00/af/31/23/2c/f2/97/ea/da/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/9f/23/c8/32/ea/65/b4/bb/1d/db/4a/75/00/9b/8c/99/38/74/da/ab/39/66/83/3e/22/95/15/25/5e/a9/00/69/07/03/2b/89/58/70/22/84/9d/64/43/82/30/16/ab/6a/7d/10/ce/7d/67/ba/fc/8b/05/28/9e/ff/0e/2d/4c/3a/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/62/8f/11/41/8f/74/8f/51/ae/e0/f3/06/60/1e/11/b7/6b/51/93/2b/6f/f5/57/f5/4f/71/16/0e/6e/08/dd/fa/01/0e/67/b2/65/c7/5c/96/99/c9/9f/15/dc/1d/15/68/d9/96/2b/be/50/21/0c/98/0a/5c/c0/02/97/95/d2/e4/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/81/29/37/18/99/7b/52/c6/e7/bc/85/50/d7/92/11/b7/99/61/6d/5e/e3/cd/0f/55/6c/41/6a/f5/84/b7/91/da/c0/25/9d/e2/7f/21/93/fc/a9/de/33/03/10/b3/23/65/74/ad/70/d6/83/84/74/d4/49/85/83/27/f3/a8/25/03/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/45/41/cd/1f/d9/e2/36/ea/b5/14/c1/b9/84/c4/87/4f/78/7d/38/05/26/a7/af/b5/f2/fb/fd/2a/9e/e4/6b/3a/6f/ef/1f/0b/78/ee/44/f2/41/48/06/6e/7a/3a/6f/62/15/8e/4b/14/78/58/25/55/b2/ca/c5/28/fd/c9/f4/29/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/24/68/b4/ef/3e/4d/8a/03/13/d3/c2/59/33/46/74/87/3d/5e/bd/2c/99/1c/86/5d/07/db/f1/a0/b0/4a/d0/bb/e5/4a/c9/f7/d8/45/b7/4c/a9/a8/8f/45/b4/49/03/af/29/c4/f9/f2/a1/d2/b5/56/6b/31/2c/33/e1/f8/12/77/v1/content/ro-crate-metadata.json",
                // search page 3
                "https://mod.paradisec.org.au/repository/45/75/92/90/0d/bc/cd/6f/68/45/f0/f2/cf/3b/0e/f3/02/23/57/da/ed/28/51/c0/de/65/86/8f/42/3b/35/69/16/b1/cf/e6/d7/63/fd/f7/ef/7b/c0/17/6b/60/84/4d/bf/3d/7f/c8/ff/48/fe/f5/f5/50/2e/0a/cf/06/86/2c/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/d9/9f/93/1c/1a/23/d7/29/44/b0/89/e8/b5/8c/76/29/75/04/b9/3c/64/c7/88/17/58/34/ae/4d/6b/18/08/b8/55/ba/e7/db/16/25/89/0c/21/ad/eb/5d/be/05/73/47/ae/3e/a6/e9/cc/9d/a6/a6/1a/ec/12/6a/68/96/89/c7/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/a0/52/2e/ce/3d/50/f9/3c/ae/9c/3e/2d/6e/c4/89/64/76/9e/44/d2/34/6d/08/28/0e/f3/be/5f/27/51/46/13/48/2a/8b/09/ea/ab/b4/98/9a/39/f5/04/ff/03/c0/80/8a/6f/19/59/28/51/a9/e3/48/43/5a/5e/4c/4b/41/a6/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/bc/5d/85/8d/f7/10/c4/70/1f/ad/01/46/cc/1b/c0/f0/45/2a/c2/53/03/56/2a/0d/17/fd/c3/8a/ac/4d/c9/a0/61/ba/a4/63/df/65/f8/a3/0c/02/23/6b/9a/a9/a3/b8/ce/16/72/f9/6b/26/5a/21/f0/d9/3e/23/1a/08/33/e4/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/62/74/31/17/a8/30/0c/b7/2b/27/a6/1b/5c/f2/72/6b/24/5e/dd/52/12/67/77/8c/da/80/bc/7e/25/d6/0d/ee/9b/67/a1/5f/84/42/9c/a3/30/90/bb/2a/b2/5c/4d/9a/49/a6/a7/0c/75/74/0e/95/7b/02/d2/1d/80/1d/4a/83/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/00/84/a0/04/c1/78/de/2f/45/50/a7/9f/82/c3/44/88/d0/1a/b0/0e/fb/6b/b7/f7/1c/73/79/b3/f6/12/73/dc/63/7c/1c/df/35/56/28/bb/5d/c5/3d/d8/06/c1/35/45/6c/47/6f/2d/78/b9/4c/c2/61/f1/44/cd/98/18/7f/fa/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/fb/9e/34/95/0a/77/ac/1f/ab/13/5b/af/83/af/29/57/fc/19/e6/b8/b4/54/bf/b8/ee/28/3d/b6/69/cb/d9/ae/27/86/71/b5/42/04/a8/53/8f/5a/4a/1f/e6/e2/d5/34/27/4d/55/f5/0d/2b/e0/9c/c0/8e/96/b6/d2/c0/d1/db/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/bc/2d/4c/6b/7c/6a/c8/4a/19/5f/74/2d/cd/7b/88/26/38/6b/32/5c/24/38/0a/2d/55/69/71/af/68/db/47/c3/90/c1/49/00/77/c1/6a/d5/01/d9/30/dd/5f/d9/65/e5/2b/cd/2a/07/ed/c6/fb/3b/41/7f/89/73/59/13/f5/39/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/62/4c/cb/5d/aa/bb/16/60/27/e5/d5/f9/f9/70/78/1d/17/2c/8b/0c/9b/76/a6/f7/7f/5f/18/aa/9e/d2/55/83/1c/b4/c0/ab/aa/1f/9d/51/88/57/8e/b5/67/a4/b2/72/8d/36/1c/c7/06/eb/48/97/c0/b5/ec/a7/83/8a/f8/56/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/83/01/0a/59/b9/a6/58/f3/6f/ba/5c/12/43/9f/5f/62/9a/35/5d/2f/be/fb/6f/cc/ac/6e/86/0f/d6/01/91/e2/39/0f/d1/c7/89/96/94/5c/09/73/57/0c/dc/ba/6e/15/f8/ed/9b/6e/8a/36/17/43/6b/7c/84/ed/8f/31/f0/2b/v1/content/ro-crate-metadata.json",
                // search page 4
                "https://mod.paradisec.org.au/repository/fb/6b/23/c6/75/c0/16/b4/41/ae/22/27/f5/ba/de/d0/9c/f4/51/ab/aa/af/a9/cf/64/b6/a7/5c/6d/f7/29/47/e4/4d/a7/20/4f/ad/f7/e7/c9/d5/9b/a3/9a/f2/0f/d6/85/cb/70/99/18/e6/44/b7/4c/4f/be/20/7b/d2/98/5a/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/a1/58/6a/bf/eb/a8/aa/e2/35/d5/7d/53/19/22/8b/4a/45/13/87/d4/45/4f/9e/a3/d8/3c/1b/52/58/34/c5/7e/0f/a0/f2/14/c0/96/9a/11/01/81/39/03/c6/b9/35/fd/97/4c/c0/54/d2/16/2a/72/00/6e/29/9a/dd/b1/36/30/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/00/6e/a6/78/44/c6/dc/bd/9e/e8/03/a5/6b/3f/fd/2e/0e/3b/61/8a/48/5a/12/88/24/88/d8/46/c9/b4/56/93/a0/d3/4b/14/e3/52/3b/72/b2/bf/72/0e/ea/62/0e/26/dd/41/95/b6/77/62/e7/56/97/55/cc/9c/05/ea/eb/a2/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/00/10/7a/8a/49/e1/a8/91/0d/c3/9d/6e/3f/90/e8/53/79/b9/3d/64/e9/c3/31/11/ca/d4/69/78/80/e5/d3/28/a6/81/ed/d1/e0/49/dd/bb/c2/f7/8a/40/5f/81/3d/54/06/de/62/f0/3e/4d/7a/56/1c/2d/08/31/d8/ce/19/00/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/46/0d/05/87/db/45/55/64/09/01/35/fa/21/da/c5/43/10/48/b3/28/84/2e/1e/c9/c4/9e/f2/81/f2/92/cd/d1/de/9d/8a/e8/f1/37/e8/5f/66/d1/06/c8/e7/0d/82/71/d9/c0/f1/36/97/71/9a/24/7a/7e/d1/ab/75/fa/46/4a/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/d9/7d/36/95/8b/6c/17/44/72/69/a1/59/f2/71/7c/9d/0e/3a/21/dd/93/83/3c/6b/cd/bb/36/3f/ec/ff/2f/13/5a/6a/f1/ae/de/17/ba/04/b7/cf/01/4b/8e/24/28/cf/f4/74/05/94/9f/5a/7d/f9/ec/6b/e6/74/1b/d6/47/ce/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/24/15/53/57/c9/74/1b/aa/26/7d/a5/fc/07/14/5b/0b/14/dc/01/70/45/ae/12/b8/2a/93/30/ca/18/84/70/0c/6a/ae/ca/89/b0/32/e6/39/af/60/4a/dd/4f/98/c3/be/fc/97/5c/78/f4/86/b6/b8/48/d2/0a/86/46/a2/02/67/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/84/3f/56/da/48/e9/e8/bb/25/11/78/ad/96/03/85/fd/62/54/3d/46/e6/06/22/45/e7/57/bf/72/88/48/e5/6b/de/e6/4f/e3/fa/65/e7/d7/12/ab/be/ee/01/bf/7b/33/49/6a/82/ea/d1/37/dc/06/a6/bb/9f/83/8c/76/61/a8/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/fb/43/d2/f0/ff/24/ed/2a/27/9f/0f/95/55/03/72/61/fb/a0/af/71/43/8c/19/8c/01/2c/ab/99/4d/97/37/c6/7a/b4/f3/c4/bd/bf/0a/b4/e2/8c/90/bf/5d/ea/bf/23/f7/b3/d9/f4/3b/05/82/d8/bd/5b/dc/d4/46/aa/9f/d5/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/a2/fe/31/00/ba/3f/ff/44/de/23/1a/4b/c9/08/78/be/6c/5b/5e/f1/70/fe/34/fd/f5/cb/e1/ac/d2/58/b2/2d/2c/84/2e/42/0b/cd/a6/3d/bb/78/40/cc/d4/24/96/16/f1/c5/a8/12/9b/26/6f/0c/b5/b1/54/de/08/49/f3/c4/v1/content/ro-crate-metadata.json",
                // search page 5
                "https://mod.paradisec.org.au/repository/bd/e5/55/d4/6e/ac/b7/4c/a0/1e/30/11/dd/c9/ed/9d/93/4d/41/31/5e/ca/5f/69/e8/69/92/5e/85/0e/8d/36/d8/9d/6f/0a/37/5b/63/27/03/f9/c1/e7/cf/a0/f2/7b/be/f5/1b/0a/64/eb/00/1c/73/84/7b/51/1d/4d/91/9b/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/25/d9/b4/fe/80/f1/d1/07/46/2d/aa/71/1e/1b/f1/e2/85/2f/92/9e/7d/cc/40/a2/7e/a1/6a/e9/c5/88/e6/6f/da/4f/73/9b/ed/67/f9/88/07/c4/2b/87/7e/71/c0/e6/31/31/32/d4/18/ba/aa/b4/ef/b0/ec/bf/8c/7d/fb/90/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/a2/f2/4c/46/5f/4e/2c/f3/9e/b0/70/34/46/ea/6a/95/9b/06/69/af/e1/8d/48/aa/5c/60/bd/f0/cb/c2/11/2f/ad/a6/19/fa/39/5f/f0/47/69/7e/ff/f4/b6/a1/b8/70/70/99/11/88/5e/d8/11/d6/06/50/21/e2/06/35/4d/75/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/47/fe/84/ad/35/a3/3a/08/1a/35/5f/41/2d/50/8a/8c/6c/be/2a/21/fd/9e/e7/41/d7/6d/91/6e/a4/5c/3d/ec/3e/dd/ea/c6/46/e3/6f/40/a7/2f/c8/9c/53/d0/9a/8f/ac/88/3b/18/d9/8d/8f/59/0e/4c/ff/20/cf/22/7f/99/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/62/1f/5a/29/fa/d3/9f/fa/7a/a7/a4/d7/72/10/6c/40/be/ec/7f/32/c5/29/fa/d0/8d/5e/9f/ad/b6/32/b6/15/51/f3/fd/d1/0f/fa/c6/7d/fa/be/25/2d/60/e7/ab/b7/8d/5c/7b/5e/61/b8/17/55/7c/62/c1/67/1c/72/1d/7b/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/d9/71/2a/9f/de/8b/06/6c/ec/79/2d/f5/ce/e8/18/d2/d9/97/29/43/a6/18/27/fd/7b/7d/9f/a1/12/ad/8f/69/dc/89/c0/9c/29/95/08/c4/dc/09/ce/a3/9c/fa/c5/35/58/cc/ab/02/1a/df/76/94/f0/3f/8a/8d/a1/65/c5/78/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/bd/aa/d4/3c/a3/4d/f0/ea/ed/e2/97/ee/50/dc/86/03/41/64/94/76/e8/2a/a2/88/21/aa/7a/82/38/8c/f2/02/37/33/8d/26/d9/6d/73/d3/9f/85/d7/7b/fe/59/04/a2/25/09/b3/21/e1/09/06/4e/99/f1/75/50/b4/d5/a4/bf/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/01/c4/16/77/d8/e2/9c/e7/11/71/ed/db/21/0e/76/76/f2/88/b3/e6/a6/95/1a/c9/4f/f9/81/b0/6d/e2/31/86/e9/47/fa/20/ac/0d/32/2e/b0/41/40/58/14/d4/a7/3f/a9/ed/66/5b/1d/5e/75/84/a9/53/7c/dd/0c/c1/d3/97/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/fc/0e/48/2b/dd/31/ca/fb/8e/a4/3e/b6/12/ad/77/34/39/23/90/6f/4f/40/63/84/08/9f/f9/a4/b4/fe/bc/b5/67/94/ef/78/8b/22/a5/72/df/7b/c3/c8/9b/f0/0c/30/60/61/93/d1/4a/f5/90/f0/a1/e5/33/3c/2e/67/ce/74/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/fc/0e/48/2b/dd/31/ca/fb/8e/a4/3e/b6/12/ad/77/34/39/23/90/6f/4f/40/63/84/08/9f/f9/a4/b4/fe/bc/b5/67/94/ef/78/8b/22/a5/72/df/7b/c3/c8/9b/f0/0c/30/60/61/93/d1/4a/f5/90/f0/a1/e5/33/3c/2e/67/ce/74/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/47/d8/6f/ff/a4/40/3d/45/3d/13/25/37/eb/1a/b8/46/fb/7f/57/c7/d9/84/b8/7e/81/c7/fd/7c/4e/d1/96/e5/3e/ba/9b/4a/4a/50/64/b1/c3/c0/b3/3a/ea/73/a3/11/0a/cc/5f/e7/78/e1/42/d9/45/c1/4c/bc/03/d6/6a/b1/v1/content/ro-crate-metadata.json",
                // search page 6
                "https://mod.paradisec.org.au/repository/63/c6/ad/cf/f4/e1/99/ba/d2/59/81/f6/9b/83/67/cd/0a/91/dd/cd/dc/fb/a4/51/da/69/23/7f/5c/f9/c9/48/c3/38/4a/43/19/71/07/0c/48/85/33/8c/e7/2a/ba/64/32/a9/b4/98/92/a8/a2/c3/7d/43/61/3f/8c/81/65/4e/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/a2/c6/a0/d8/8f/1a/43/4e/63/f0/6f/38/7f/43/50/f9/d5/44/48/83/f8/71/94/ff/cb/d0/12/10/c1/d9/d0/77/88/25/ae/70/55/a5/e5/85/29/e6/c7/19/24/7d/65/3b/77/ce/1c/1b/9a/7c/5f/ca/10/4f/c6/be/ae/92/a2/9e/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/25/91/82/0a/90/74/6c/47/51/0c/7e/7a/4f/db/4c/d1/0f/09/eb/b0/d9/ad/25/80/a0/a9/eb/37/69/a2/76/6b/dc/8d/71/18/67/7d/96/52/83/11/ee/d1/84/a2/8a/71/73/f2/e1/99/dc/a5/05/10/d4/05/c0/09/9f/82/97/f9/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/da/ae/80/62/a2/7f/1b/41/9e/5e/5d/db/d5/a2/33/0f/22/08/b4/5c/a9/83/cd/eb/b3/86/33/d0/8a/54/14/49/08/49/d2/46/eb/cc/31/61/e2/58/2e/ee/6f/81/93/7c/77/5d/5b/f1/52/ec/0b/cc/36/7b/6d/e7/aa/14/4f/0e/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/84/29/58/61/3a/c1/89/a2/6a/d0/6c/47/b3/e9/30/c4/bd/5a/2a/e3/2a/77/e2/96/b3/2f/51/bc/21/f1/c0/99/47/9a/26/99/bd/8b/64/fe/3a/e5/5e/78/77/28/6d/63/78/28/af/10/59/0d/af/70/1e/70/c4/16/96/f9/c9/8b/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/db/f6/66/62/9b/75/df/d9/91/bc/ea/1b/f1/c5/11/f9/ae/33/2c/2b/58/25/ac/b8/41/48/fa/33/9a/45/c1/0f/b5/0b/1a/e9/d1/c4/b8/a0/23/27/ff/76/88/88/3b/1c/56/5f/4e/05/07/db/04/17/6e/5b/ed/5a/a1/ea/bc/95/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/be/2e/d9/0c/1b/99/73/7b/e5/6b/dd/57/77/80/6b/a0/78/a7/31/1d/c4/ce/15/c4/60/af/d4/80/1e/e9/4d/32/e1/25/e1/65/55/67/8d/d4/26/c2/14/10/bf/94/47/03/fd/cd/35/59/76/5e/02/c0/19/4d/97/e1/89/e3/40/e0/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/85/ab/bc/f5/36/2f/45/8e/ee/ac/59/db/62/28/9a/fc/31/f7/9d/1b/c7/e9/f9/07/56/dd/9f/a7/7d/34/6a/1b/2b/4e/a6/d6/be/d3/5f/ae/cc/42/c8/5c/ce/e8/1b/50/c4/49/f2/2e/b4/de/ee/47/d5/68/87/9e/d4/5e/94/53/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/a3/c4/95/fb/4d/2c/59/db/00/87/e9/7e/49/1f/3f/74/c1/fa/35/97/6a/e4/c6/6e/ab/b4/8d/61/2d/80/5f/41/14/36/47/2b/90/c4/0d/63/2b/2e/00/c7/64/4f/39/fd/c2/a2/d7/fe/ce/e9/f9/df/c6/27/70/57/2f/c6/60/5f/v1/content/ro-crate-metadata.json",
                "https://mod.paradisec.org.au/repository/25/33/1d/af/b4/e4/91/91/9d/f5/55/ff/eb/64/8b/14/51/94/71/d6/ae/28/71/a3/1f/55/aa/28/fa/73/2a/59/1a/cb/de/d3/99/f0/49/71/af/03/d4/65/47/75/f5/b2/2d/83/3f/15/91/93/ad/6d/33/6d/2a/da/15/09/f3/63/v1/content/ro-crate-metadata.json",
        };
        int i = 0;
        for (String extractedUrl : extracted) {
            System.out.println(i++);
            String path = extractedUrl.replace(metadataFilename, "");

            Nanopub np = createNpFromRoCrate(path, metadataFilename);
            PublishNanopub.publish(np);
        }
    }

}