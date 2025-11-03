package org.nanopub;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.apache.commons.lang.time.StopWatch;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.fdo.rest.rohub.gson.Page;
import org.nanopub.fdo.rest.rohub.gson.RoCrateIndex;
import org.nanopub.vocabulary.SCHEMA;
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
    static final Logger log = LoggerFactory.getLogger(RoHubImporter.class);

    // TODO debug output and log and timer are ideally different things
    final boolean enabledTimer = false; // tODO false
    final StopWatch webRequestTimer = new StopWatch();
    final StopWatch creationTimer = new StopWatch();

    @BeforeAll
    static void makeSureKeysAreAvailable() throws IOException {
        GeneralIntegrationTestsIT.makeSureKeysAreAvailable();
    }

    @Test
    synchronized void testRoHubIndexParsing () throws Exception {
        for (int pageNumber = 10; pageNumber <= 10; pageNumber++) {
            RoCrateIndex[] currentPage = readRoHubIndexPage(pageNumber);

            for (int j = 0; j < currentPage.length; j++) {
                prepareRoCrateFromRohubApi(currentPage[j].identifier, String.format("Page %02d - %02d", pageNumber, j));
            }
        }
    }

    synchronized void prepareRoCrateFromRohubApi(String roId, String infoString) throws Exception {
        String downloadUrl = "https://api.rohub.org/api/ros/" + roId + "/crate/download/";

        try {
            restartTimerIfEnabled();
            Nanopub unsignedNp = createUnsignedNpFromRoCrate(downloadUrl, "");
            int containingFilesInRoCrate = countHasPartRelationsInRoCrateNanopub(unsignedNp);
            if (infoString != null) {
                System.out.println(String.format("%04d, triple count - hasPart=, %03d, (%s)", unsignedNp.getTripleCount(), containingFilesInRoCrate, infoString));
            }
        } catch (RDFParseException | MalformedNanopubException e) {
            if (infoString != null) {
                System.out.println(String.format("%04d triple count (%s)", 0,
                        infoString + " RDF Exception at id=" + roId + " " + e.getMessage()));
            }
            if (enabledTimer) {
                creationTimer.suspend();
            }
        } finally {
            if (enabledTimer) {
                System.err.println("Web-Requests total Time: " + webRequestTimer.getTime());
                System.err.println("NanoCreations total Time: " + creationTimer.getTime());
            }
        }
    }

    /**
     * Count the files in a nanopub created from an RO-Crate.
     * @param unsignedNp it can be signed, too
     * @return the count +1
     */
    private int countHasPartRelationsInRoCrateNanopub(Nanopub unsignedNp) {
        int count = 1; // we start at 1 for the ro-crate-metadata.jsonld - file itself
        for (Statement st: unsignedNp.getAssertion()) {
            if (st.getObject().equals(SCHEMA.RO_CRATE_HAS_PART) ||
            st.getPredicate().equals(SCHEMA.RO_CRATE_HAS_PART) ||
            st.getSubject().equals(SCHEMA.RO_CRATE_HAS_PART) ||
            st.getContext().equals(SCHEMA.RO_CRATE_HAS_PART)
            ) {
                count++;
            }
        }
        return count;
    }

    /**
     * Also initializes the timer if not yet done
     */
    private synchronized void restartTimerIfEnabled() {
        if (enabledTimer) {
            try {
                webRequestTimer.start();
            } catch (IllegalStateException e) {
                System.out.println("Web request timer " + webRequestTimer.toString());
                webRequestTimer.resume();
            } finally {
                // Try suspension anyway
                webRequestTimer.suspend();
            }
            try {
                creationTimer.start();
            } catch (IllegalStateException e) {
                System.out.println("NanoCreation timer " + creationTimer.toString());
                creationTimer.resume();
            } finally {
                // Try suspension
                creationTimer.suspend();
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

    synchronized Nanopub createUnsignedNpFromRoCrate (@NonNull String downloadUrl, @NonNull String metadataFilename) throws Exception {
        resumeRoHubTimerIfEnabled(downloadUrl);
        InputStream metadata = RoCrateParser.downloadRoCreateMetadataFile(downloadUrl + metadataFilename);
        RoCrateParser parser = new RoCrateParser();
        resumeOurNanopublicationTimerIfEnabled();
        Nanopub np = parser.parseRoCreate(downloadUrl, metadata);
        suspendOurNanopubCreationTimerIfEnabled();
        return np;
    }

    private synchronized void suspendOurNanopubCreationTimerIfEnabled() {
        if (enabledTimer) {
            creationTimer.suspend();
        }
    }

    private synchronized void resumeOurNanopublicationTimerIfEnabled() {
        if (enabledTimer) {
            webRequestTimer.suspend();
            creationTimer.resume();
        }
    }

    private synchronized void resumeRoHubTimerIfEnabled(String downloadUrl) {
        if (enabledTimer) {
            webRequestTimer.resume();
            System.out.println("Downoading: " + downloadUrl);
        }
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
