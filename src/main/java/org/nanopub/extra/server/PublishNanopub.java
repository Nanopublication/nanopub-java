package org.nanopub.extra.server;

import com.beust.jcommander.ParameterException;
import net.trustyuri.ArtifactCode;
import net.trustyuri.TrustyUriUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command line tool to publish nanopublications to a nanopub server.
 */
public class PublishNanopub extends CliRunner {

    private static final Logger logger = LoggerFactory.getLogger(PublishNanopub.class);

    @com.beust.jcommander.Parameter(description = "nanopubs", required = true)
    private List<String> nanopubs = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
    private boolean verbose;

    @com.beust.jcommander.Parameter(names = "--dry-run", description = "Simulate (no action)")
    private boolean dryRun;

    @com.beust.jcommander.Parameter(names = "--strict", description = "Only publish if validation shows no issues")
    private boolean strict;

    @com.beust.jcommander.Parameter(names = "-u", description = "Use the given nanopub server URLs")
    private List<String> serverUrls;

    @com.beust.jcommander.Parameter(names = "-s", description = "Get nanopubs to be published from given SPARQL endpoint")
    private String sparqlEndpointUrl;

    /**
     * Main method to run the PublishNanopub command line tool.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            PublishNanopub obj = CliRunner.initJc(new PublishNanopub(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Publish a nanopublication to the default nanopub server.
     *
     * @param nanopub the nanopublication to publish
     * @return the URL of the published nanopublication
     * @throws java.io.IOException if an error occurs during publishing
     */
    public static String publish(Nanopub nanopub) throws IOException {
        return new PublishNanopub().publishNanopub(nanopub);
    }

    /**
     * Publish a nanopublication to a specified nanopub server.
     *
     * @param nanopub   the nanopublication to publish
     * @param serverUrl the URL of the nanopub server
     * @return the URL of the published nanopublication
     * @throws java.io.IOException if an error occurs during publishing
     */
    public static String publish(Nanopub nanopub, String serverUrl) throws IOException {
        return new PublishNanopub().publishNanopub(nanopub, serverUrl);
    }

    /**
     * Test server URL for publishing nanopublications.
     */
    // TODO Make this dynamic/configureable:
    public static final String TEST_SERVER_URL = "https://test.registry.knowledgepixels.com/";

    /**
     * Publish a nanopublication to the test server.
     *
     * @param nanopub the nanopublication to publish
     * @return the URL of the published nanopublication
     * @throws java.io.IOException if an error occurs during publishing
     */
    public static String publishToTestServer(Nanopub nanopub) throws IOException {
        return new PublishNanopub().publishNanopub(nanopub, TEST_SERVER_URL);
    }

    private ServerIterator serverIterator = null;
    private RegistryInfo registryInfo = null;
    private Map<String, Integer> usedServers = new HashMap<>();
    private int count;
    private boolean failed;
    private SPARQLRepository sparqlRepo;
    private ArtifactCode artifactCode;

    /**
     * Default constructor for PublishNanopub.
     * Initializes the command line tool with no parameters.
     */
    public PublishNanopub() {
        super();
    }

    /**
     * No-op retained for backwards compatibility.
     *
     * @deprecated This used to reconfigure the JVM-wide logging backend, which is not something a
     * library may do to its host application. Progress detail is now emitted through SLF4J at DEBUG
     * level, and additionally to standard error when the {@code -v} command line flag is given.
     * Configure your logging backend instead of calling this method.
     */
    @Deprecated(forRemoval = true)
    public void initLogging() {
        // Intentionally empty: see deprecation notice.
    }

    /**
     * Reports progress detail that is useful when running interactively but must stay out of an
     * embedding application's output. On the command line ({@code -v}) it goes to standard error, so
     * that standard output remains usable for piping; otherwise it is logged at DEBUG level.
     */
    private void reportProgress(String message) {
        if (verbose) {
            System.err.println(message);
        } else {
            logger.debug(message);
        }
    }


    private void run() throws IOException {
        failed = false;
        for (String s : nanopubs) {
            count = 0;
            try {
                if (sparqlEndpointUrl != null) {
                    if (sparqlRepo == null) {
                        sparqlRepo = new SPARQLRepository(sparqlEndpointUrl);
                        sparqlRepo.init();
                    }
                    processNanopub(new NanopubImpl(sparqlRepo, SimpleValueFactory.getInstance().createIRI(s)));
                } else {
                    reportProgress("Reading file: " + s);
                    MultiNanopubRdfHandler.process(new File(s), np -> {
                        if (failed) {
                            return;
                        }
                        processNanopub(np);
                    });
                    if (count == 0) {
                        logger.warn("No nanopub found in {}", s);
                        System.out.println("NO NANOPUB FOUND: " + s);
                        break;
                    }
                }
            } catch (RDF4JException ex) {
                logger.error("Could not read RDF from {}", s, ex);
                System.err.println("RDF ERROR: " + s);
                break;
            } catch (MalformedNanopubException ex) {
                logger.error("Malformed nanopub in {}", s, ex);
                System.err.println("INVALID NANOPUB: " + s);
                break;
            }
            if (failed) {
                logger.error("Failed to publish nanopubs from {}", s);
                System.err.println("FAILED TO PUBLISH NANOPUBS");
                break;
            }
        }
        for (String s : usedServers.keySet()) {
            int c = usedServers.get(s);
            System.out.println(c + " nanopub" + (c == 1 ? "" : "s") + " published at " + s);
        }
        if (sparqlRepo != null) {
            try {
                sparqlRepo.shutDown();
            } catch (RepositoryException ex) {
                logger.warn("Failed to shut down SPARQL repository {}", sparqlEndpointUrl, ex);
            }
        }
    }

    private void processNanopub(Nanopub nanopub) {
        count++;
        if (count % 100 == 0) {
            System.err.print(count + " nanopubs...\r"); // TODO handle System.err with logging in a similar way to System.out with logOrSysout --> logAndSysERR ??
        }
        try {
            publishNanopub(nanopub);
        } catch (IOException ex) {
            logger.error("Failed to publish nanopub {}", nanopub.getUri(), ex);
            if (verbose) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                System.err.println("---");
            }
            failed = true;
        }
    }

    /**
     * Publish a nanopublication to the default nanopub server.
     *
     * @param nanopub the nanopublication to publish
     * @return the URL of the published nanopublication
     * @throws java.io.IOException if an error occurs during publishing
     */
    public String publishNanopub(Nanopub nanopub) throws IOException {
        return publishNanopub(nanopub, null);
    }

    /**
     * Publish a nanopublication to a specified nanopub server.
     *
     * @param nanopub   the nanopublication to publish
     * @param serverUrl the URL of the nanopub server
     * @return the URL of the published nanopublication, iff not --dry-run (then it's np.getUri() which may be null)
     * @throws java.io.IOException if an error occurs during publishing
     */
    public String publishNanopub(Nanopub nanopub, String serverUrl) throws IOException {
        NanopubVerifier verifier = new NanopubVerifier(nanopub);
        if (verifier.verify()) {
            logger.debug("Verification of nanopub {} done, no issues", nanopub.getUri());
        } else {
            logger.warn("Verification of nanopub {} shows some issues: {}", nanopub.getUri(), verifier.getIssues());
            if (strict) {
                logger.warn("Strict mode: nanopub {} is not published", nanopub.getUri());
                return null;
            }
        }

        List<Statement> invalidSparql = NanopubUtils.getInvalidSparqlStatements(nanopub);
        if (!invalidSparql.isEmpty()) {
            // A nanopub cannot be edited after the fact, so a query published with broken SPARQL can
            // never run: publishing it is refused rather than warned about. This happens before any
            // server is contacted.
            throw new RuntimeException("Can't publish nanopublication with invalid SPARQL: " + nanopub.getUri() +
                    ". " + NanopubUtils.describeInvalidSparql(invalidSparql.getFirst()));
        }

        if (registryInfo == null) {
            if (serverUrl != null) {
                serverIterator = new ServerIterator(serverUrl);
            } else if (serverUrls == null || serverUrls.isEmpty()) {
                serverIterator = new ServerIterator();
            } else {
                serverIterator = new ServerIterator(serverUrls);
            }
            registryInfo = serverIterator.next();
        }
        artifactCode = ArtifactCode.of(TrustyUriUtils.getArtifactCode(nanopub.getUri().toString()));
        reportProgress("Trying to publish nanopub: " + artifactCode);
        boolean isProtected = NanopubServerUtils.isProtectedNanopub(nanopub);
        boolean foundEligibleRegistry = false;
        while (registryInfo != null) {
            String url = registryInfo.getUrl();

            // TODO Check here whether nanopub type is covered at given registry.

            if (isProtected && !registryInfo.isLocalInstance()) {
                // Only local instances accept protected nanopubs; public registries would reject this.
                logger.debug("Skipping {} for protected nanopub {}: not a local instance", url, artifactCode);
                reportProgress("Skipping server (not a local instance): " + url);
                registryInfo = serverIterator.next();
                continue;
            }
            foundEligibleRegistry = true;

            reportProgress("Trying server: " + url);
            try {
                HttpPost post = preparePost(nanopub);
                if (!dryRun) {
                    String nanopubUrl = executePost(post, url);
                    if (nanopubUrl != null) {
                        return nanopubUrl;
                    }
                }
            } catch (IOException | RDF4JException ex) {
                logger.warn("Publishing {} to {} failed, trying next registry — {}: {}", artifactCode, url, ex.getClass().getSimpleName(), ex.getMessage());
                logger.debug("Publishing {} to {} failed", artifactCode, url, ex);
                reportProgress(ex.getClass().getName() + ": " + ex.getMessage());
            }
            registryInfo = serverIterator.next();
        }
        registryInfo = null;
        if (isProtected && !foundEligibleRegistry) {
            throw new RuntimeException("Can't publish protected nanopublication: " + artifactCode +
                                       ". None of the available registries is a local instance, " +
                                       "and only local instances accept protected nanopublications.");
        }
        if (dryRun) {
            System.out.println("Nanopub NOT published: --dry-run, np-uri=" + nanopub.getUri());
            return null;
        } else {
            throw new RuntimeException(String.format("Failed to publish the nanopub. " +
                                                     "Details: Probably the HTTP Response Codes from Servers where not between 200 and 300\n" +
                                                     "Server URL = '%s'", serverUrl));
        }
    }

    private HttpPost preparePost(Nanopub nanopub) throws IOException {
        HttpPost post = new HttpPost(registryInfo.getUrl());
        String nanopubString = NanopubUtils.writeToString(nanopub, RDFFormat.TRIG);
        post.setEntity(new StringEntity(nanopubString, "UTF-8"));
        post.setHeader("Content-Type", RDFFormat.TRIG.getDefaultMIMEType());
        return post;
    }

    private String executePost(HttpPost post, String url) throws IOException {
        HttpResponse response = NanopubUtils.getHttpClient().execute(post);
        int code = response.getStatusLine().getStatusCode();
        if (code >= 200 && code < 300) {
            if (usedServers.containsKey(url)) {
                usedServers.put(url, usedServers.get(url) + 1);
            } else {
                usedServers.put(url, 1);
            }
            String nanopubUrl = registryInfo.getCollectionUrl() + artifactCode;
            logger.debug("Published {} at {}", artifactCode, nanopubUrl);
            reportProgress("Published: " + nanopubUrl);
            return nanopubUrl;
        } else {
            logger.warn("Registry {} rejected {} with HTTP {} {}", url, artifactCode, code, response.getStatusLine().getReasonPhrase());
            reportProgress("Response: " + code + " " + response.getStatusLine().getReasonPhrase());
        }
        return null; // post failed
    }

    /**
     * Get the registry information of the server used for publishing.
     *
     * @return the registry information
     */
    public RegistryInfo getUsedServer() {
        return registryInfo;
    }

    /**
     * Get the URL of the published nanopublication.
     *
     * @return the URL of the published nanopublication, or null if not available
     */
    public String getPublishedNanopubUrl() {
        if (registryInfo == null || artifactCode == null) {
            return null;
        }
        return registryInfo.getCollectionUrl() + artifactCode;
    }

}
