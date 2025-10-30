package org.nanopub;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.fdo.rest.rohub.gson.Page;
import org.nanopub.fdo.rest.rohub.gson.RoCrateIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * This Class intentionally does not have the ...IT suffix, since we do not want it to run with maven. It's just
 * ment for scripting in Java, and running in your IDE.
 *
 * Please use carefully, and only if you know what you're doing! Especially when importing huge numbers of new
 * RO_Crates to the Nanopub Networks
 */
public class RoHubImporter {
    final HttpClient client = HttpClient.newHttpClient();
    final static Logger log = LoggerFactory.getLogger(RoHubImporter.class);

    @BeforeAll
    static void makeSureKeysAreAvailable() throws IOException {
        GeneralIntegrationTestsIT.makeSureKeysAreAvailable();
    }

    @Test
    void testRoHubIndexParsing () throws Exception {
        for (int pageNumber = 1; pageNumber <= 10; pageNumber++) {
            RoCrateIndex[] currentPage = readRoHubIndexPage(pageNumber);

            for (int j = 0; j < currentPage.length; j++) {
//                System.out.println();
                prepareRoCrateFromRohubApi(currentPage[j].identifier, String.format("Page %02d - %02d", pageNumber, j));
            }
        }
    }

    void prepareRoCrateFromRohubApi(String roId, String infoString) throws Exception {
        String downloadUrl = "https://api.rohub.org/api/ros/" + roId + "/crate/download/";

        try {
            Nanopub unsignedNp = createUnsignedNpFromRoCrate(downloadUrl, "");
            if (infoString != null) {
                System.out.println(String.format("%04d triple count (%s)", unsignedNp.getTripleCount(), infoString));
            }
        } catch (RDFParseException e) {
            if (infoString != null) {
                System.out.println(String.format("%04d triple count (%s)", 0,
                        infoString + " RDF Exception at id=" + roId + " " + e.getMessage()));
            }
        }
    }

    private RoCrateIndex[] readRoHubIndexPage(int pageNumber) throws URISyntaxException, IOException, InterruptedException {
        final String url = "https://api.rohub.org/api/ros/?page=" + pageNumber;
        HttpRequest req = HttpRequest.newBuilder().GET().uri(new URI(url)).build();
        HttpResponse<InputStream> httpResponse = client.send(req, HttpResponse.BodyHandlers.ofInputStream());

        JsonReader reader = new JsonReader(new InputStreamReader(httpResponse.body()));
        Page p = new Gson().fromJson(reader, Page.class);

        System.out.println(String.format("Page %02d: result size = %d", pageNumber, p.results.length));
        if (log.isDebugEnabled()) {
            for (int i = 0; i < p.results.length; i++) {
                // TODO enableling log output for tests
                System.err.println(String.format("# %02d : %s", i, p.results[i].api_link));
                log.debug(p.results[i].api_link);
            }
        }
        return p.results;
    }

    Nanopub createUnsignedNpFromRoCrate (@NonNull String downloadUrl, @NonNull String metadataFilename) throws Exception {
        InputStream metadata = RoCrateParser.downloadRoCreateMetadataFile(downloadUrl + metadataFilename);
        RoCrateParser parser = new RoCrateParser();
        Nanopub np = parser.parseRoCreate(downloadUrl, metadata);

        return np;
    }

    /**
     * Create a signed Nanopub with the path to a RO-Crate available in the internet. The signature is done with
     * default values (~/nanopub/profile)
     * @param downloadUrl the downloadUrl where the metadata file is published (including trailing "/")
     * @param metadataFilename the ro-create metadata filename, may be empty
     * @return the signed Nanopub
     * @throws Exception any troubles e.g. network or wrong path
     */
    static Nanopub createNpFromRoCrate (@NonNull String downloadUrl, @NonNull String metadataFilename) throws Exception {
        InputStream metadata = RoCrateParser.downloadRoCreateMetadataFile(downloadUrl + metadataFilename);
        RoCrateParser parser = new RoCrateParser();
        Nanopub np = parser.parseRoCreate(downloadUrl, metadata);

        Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
        if (log.isDebugEnabled()) {
            NanopubUtils.writeToStream(signedNp, System.err, RDFFormat.TRIG);
        }
        return signedNp;
    }

}
