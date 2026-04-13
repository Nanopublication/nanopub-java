package org.nanopub;

import com.beust.jcommander.ParameterException;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.jspecify.annotations.NonNull;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryAccess;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.fdo.rest.rohub.gson.Page;
import org.nanopub.vocabulary.NPX;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Optional;

/**
 * Update the nanopublications from the RO-Hub.
 */
public class RoHubUpdater extends CliRunner {

    @com.beust.jcommander.Parameter(names = "-l", description = "write to log, no publishing")
    private boolean createLocally;

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    static final IRI CREATOR = vf.createIRI("http://purl.org/dc/terms/creator");
    static final IRI RO_CRATE_BOT = vf.createIRI("https://w3id.org/kpxl/gen/terms/RoCrateBot");

    static final String QUERY_ID = "RAFGm6dE-H9M5WgOViQJQrvKrwQofZxViNF2wq2CbAZTA/get-rocrate-nanopubs";

    /**
     * Main method to run the RoHubUpdater.
     *
     * @param args command line arguments, expects the RoCrate metadata URL
     */
    public static void main(String[] args) {
        try {
            RoHubUpdater obj = CliRunner.initJc(new RoHubUpdater(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    final HttpClient client = HttpClient.newHttpClient();

    public void run() throws Exception {
        importNewRoCrates();
    }

    void importNewRoCrates() throws Exception {

        QueryRef query = new QueryRef(QUERY_ID);
        ApiResponse response = QueryAccess.get(query);

        for (int pageNumber = 1; ; pageNumber++) {
            Page currentPage = readRoHubIndexPage(pageNumber);

            for (int j = 0; j < currentPage.results.length; j++) {

                String roCrateIdentifier = currentPage.results[j].identifier;
                // check if the RO-Crate is already in the Nanopub network and
                // modification not newer as Nanopub publication
                String npToReplace = null;

                Optional<ApiResponseEntry> entry = response.getData().stream().filter(
                        e -> e.get("rocrate").equals("https://w3id.org/ro-id/" + roCrateIdentifier)).findFirst();
                if (entry.isPresent()) {
                    // check if entry on RoHub is updated after entry from Nanopub
                    Instant roHubUpdateTime = Instant.parse(currentPage.results[j].modified);
                    Instant npUpdateTime = Instant.parse(entry.get().get("date"));
                    if (npUpdateTime.isAfter(roHubUpdateTime)) {
                        // do not process this result from RO-Hub
                        continue;
                    } else {
                        npToReplace = entry.get().get("np");
                    }
                }

                Nanopub roCrate = prepareRoCrateFromRohubApi(roCrateIdentifier, npToReplace);

                if (roCrate != null) {
                    try {
                        Nanopub signedNp = SignNanopub.signAndTransform(roCrate, TransformContext.makeDefault());

                        if (createLocally) {
                            System.out.println("Dry run, not publishing " + roCrateIdentifier);
                            // NanopubUtils.writeToStream(signedNp, System.out, RDFFormat.TRIG);
                        } else {
                            System.out.println("Publishing " + signedNp.getUri());
                            PublishNanopub.publish(signedNp);
                        }
                    } catch (Exception e) {
                        System.out.println("Not publishing " + roCrateIdentifier + ", since there was an Exception: " +
                                           e.getMessage());
                    }
                }
            }
            if (currentPage.next == null) {
                System.out.println("All RO-Crates from RO-Hub processed.");
                break;
            }
        }
    }

    Page readRoHubIndexPage(int pageNumber)
            throws URISyntaxException, IOException, InterruptedException {
        final String url = "https://api.rohub.org/api/ros/?page=" + pageNumber;
        System.out.println("Read page: " + url);
        HttpRequest req = HttpRequest.newBuilder().GET().uri(new URI(url)).build();
        HttpResponse<InputStream> httpResponse = client.send(req, HttpResponse.BodyHandlers.ofInputStream());

        JsonReader reader = new JsonReader(new InputStreamReader(httpResponse.body()));
        Page p = new Gson().fromJson(reader, Page.class);

        return p;
    }

    /**
     * @return the unsigned Nanopub, or null if a not-severe exception occurred
     * @throws Exception if a severe exception occurred, e.g. IOException
     */
    Nanopub prepareRoCrateFromRohubApi(String roId, String noToReplace) throws Exception {
        String downloadUrl = "https://api.rohub.org/api/ros/" + roId + "/crate/download/";
        try {
            return createUnsignedNpFromRoCrate(downloadUrl, noToReplace);
        } catch (RDFParseException | MalformedNanopubException e) {
            System.out.println("Stop processing " + roId + ", since there was an Exception: " +
                               e.getMessage());
            return null;
        }
    }

    private Nanopub createUnsignedNpFromRoCrate(@NonNull String downloadUrl, String npToReplace)
            throws Exception {
        InputStream metadata = RoCrateParser.downloadRoCreateMetadataFile(downloadUrl);
        RoCrateParser parser = new RoCrateParser();
        NanopubCreator npCreator = parser.parseRoCreate(downloadUrl, metadata);
        npCreator.addPubinfoStatement(CREATOR, RO_CRATE_BOT);
        if (npToReplace != null) {
            npCreator.addPubinfoStatement(NPX.SUPERSEDES, vf.createIRI(npToReplace));
        }

        return npCreator.finalizeNanopub(true);
    }

}
