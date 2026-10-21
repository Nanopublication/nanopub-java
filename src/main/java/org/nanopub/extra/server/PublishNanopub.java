package org.nanopub.extra.server;

import com.beust.jcommander.ParameterException;
import net.trustyuri.ArtifactCode;
import net.trustyuri.TrustyUriUtils;
import org.apache.http.Header;
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
import org.nanopub.extra.security.ProfileKeyCheck;
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

    @com.beust.jcommander.Parameter(names = "--strict", description = "Only publish if verification and the signing-key check show no issues")
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
            if (obj.failed) {
                // A batch that stopped early has to be distinguishable from one that completed:
                // the nanopubs after the stopping point were not published, and a caller seeing
                // only the exit code would otherwise take the run for a success.
                System.exit(1);
            }
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
    private String currentInput;
    private String stoppedAt;
    private String failureReason;
    private String lastRegistryFailure;
    private final List<String> registriesTried = new ArrayList<>();

    /**
     * How often one registry is asked before the next one is tried. A rate limit or a short
     * outage passes within seconds, so a handful of attempts covers it; a registry that is down
     * for longer is better served by moving on.
     */
    private static final int MAX_ATTEMPTS_PER_REGISTRY = 4;

    /**
     * Upper bound on a single wait, including one a {@code Retry-After} header asks for, so that
     * a misconfigured or hostile header cannot stall a batch indefinitely.
     */
    private static final long MAX_RETRY_DELAY_MILLIS = 30000;

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
            currentInput = s;
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
                        stopBatch(s, "no nanopub found");
                        break;
                    }
                }
            } catch (RDF4JException ex) {
                logger.error("Could not read RDF from {}", s, ex);
                System.err.println("RDF ERROR: " + s);
                stopBatch(s, "could not be read as RDF: " + ex.getMessage());
                break;
            } catch (MalformedNanopubException ex) {
                logger.error("Malformed nanopub in {}", s, ex);
                System.err.println("INVALID NANOPUB: " + s);
                stopBatch(s, "malformed nanopub: " + ex.getMessage());
                break;
            }
            if (failed) {
                logger.error("Failed to publish nanopubs from {}", s);
                System.err.println("FAILED TO PUBLISH NANOPUBS");
                break;
            }
        }
        int publishedTotal = 0;
        for (String s : usedServers.keySet()) {
            int c = usedServers.get(s);
            publishedTotal += c;
            System.out.println(c + " nanopub" + (c == 1 ? "" : "s") + " published at " + s);
        }
        if (failed) {
            // The counts above are per registry and are printed on a successful run too, so say
            // explicitly that this run did not get through its input, and where it stopped. Input
            // after that point was not published at all.
            System.out.println("BATCH INCOMPLETE: stopped at " + stoppedAt
                               + (failureReason == null ? "" : " (" + failureReason + ")")
                               + "; " + publishedTotal + " nanopub" + (publishedTotal == 1 ? "" : "s")
                               + " published before that, the rest was not");
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
        } catch (IOException | RuntimeException ex) {
            // publishNanopub reports "could not publish this one" with an unchecked exception as
            // well — a registry rejecting the nanopub outright, or every registry having been
            // tried — and letting that escape would end the run with a stack trace instead of
            // the summary and the exit code that say what happened.
            if (ex instanceof NanopubRejectedException) {
                // Its message says what happened and why; a stack trace would only bury it.
                logger.error("Failed to publish nanopub {}: {}", nanopub.getUri(), ex.getMessage());
            } else {
                logger.error("Failed to publish nanopub {}", nanopub.getUri(), ex);
            }
            System.err.println(ex.getMessage());
            if (verbose) {
                System.err.println(ex.getClass().getName());
                System.err.println("---");
            }
            stopBatch(currentInput == null ? String.valueOf(nanopub.getUri())
                    : currentInput + ", at nanopub " + nanopub.getUri(), null);
        }
    }

    /**
     * Records that the batch is not going to finish, so that the summary, the message and the
     * exit code can say so. The first stopping point wins: it is the one the user has to resume
     * from.
     */
    private void stopBatch(String where, String reason) {
        failed = true;
        if (stoppedAt == null) {
            stoppedAt = where;
            failureReason = reason;
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

        // A signature made with a key no introduction declares is valid, so the verifier above has
        // nothing to say about it, but the registry has nothing tying the key to its signer and the
        // nanopub shows up under an unapproved agent. Publishing cannot be undone, so it is worth
        // saying so first (issue #150).
        ProfileKeyCheck.Result keyCheck = ProfileKeyCheck.check(nanopub);
        if (!keyCheck.isAcceptable()) {
            logger.warn("Signing key of nanopub {}: {}", nanopub.getUri(), keyCheck.message());
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
        // Per nanopublication: the instance is reused for a whole batch, and the failure report
        // has to describe this nanopublication's attempts, not every earlier one's.
        registriesTried.clear();
        lastRegistryFailure = null;
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
                String nanopubUrl = publishToRegistry(nanopub, url);
                if (nanopubUrl != null) {
                    return nanopubUrl;
                }
            } catch (IOException | RDF4JException ex) {
                lastRegistryFailure = url + ": " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
                logger.warn("Publishing {} to {} failed, trying next registry — {}: {}", artifactCode, url, ex.getClass().getSimpleName(), ex.getMessage());
                logger.debug("Publishing {} to {} failed", artifactCode, url, ex);
                reportProgress(ex.getClass().getName() + ": " + ex.getMessage());
            }
            registriesTried.add(url);
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
            // Name the registries that were actually contacted and the last thing one of them
            // said. The message used to report the serverUrl parameter, which is null on the
            // command-line path, so it never identified anything.
            throw new RuntimeException("Failed to publish nanopublication " + nanopub.getUri()
                                       + " to any registry. Registries tried: "
                                       + (registriesTried.isEmpty() ? "none" : String.join(", ", registriesTried))
                                       + (lastRegistryFailure == null ? "" : ". Last failure: " + lastRegistryFailure));
        }
    }

    /**
     * Posts a nanopublication to one registry, retrying while the registry says "not now" rather
     * than "not ever".
     * <p>
     * A rate limit (429) or a brief unavailability (408 and the 5xx statuses) is the expected
     * answer during a bulk publish — the public registries allow 10 requests per second per IP —
     * and moving to the next registry instead of waiting a moment uses up the registry list in
     * seconds, after which the whole batch fails. Any other 4xx is the registry refusing this
     * nanopublication on its content, which every registry would do, so it is reported as a
     * rejection straight away rather than tried elsewhere.
     *
     * @return the URL of the published nanopublication, or null if this registry did not take it
     * and the next one should be tried
     */
    private String publishToRegistry(Nanopub nanopub, String url) throws IOException {
        for (int attempt = 1; ; attempt++) {
            HttpPost post = preparePost(nanopub);
            if (dryRun) {
                return null;
            }
            PostResponse response = executePost(post, url);
            if (response.isSuccess()) {
                return getPublishedNanopubUrl();
            }
            if (isNanopubRejection(response.statusCode())) {
                throw new NanopubRejectedException(nanopub.getUri(), url, response.statusCode(), response.reasonPhrase());
            }
            lastRegistryFailure = url + ": HTTP " + response.statusCode()
                                  + (response.reasonPhrase() == null ? "" : " " + response.reasonPhrase());
            if (!isWorthRetrying(response.statusCode())) {
                // Not about this nanopublication and not something that passes by itself (an
                // unexpected status, a wrong endpoint, a client that this registry will not
                // accept): try the next registry, as before.
                logger.warn("Registry {} answered HTTP {} for {}; trying the next registry",
                        url, response.statusCode(), artifactCode);
                return null;
            }
            if (attempt >= MAX_ATTEMPTS_PER_REGISTRY) {
                logger.warn("Registry {} answered HTTP {} for {} on all {} attempts; trying the next registry",
                        url, response.statusCode(), artifactCode, MAX_ATTEMPTS_PER_REGISTRY);
                return null;
            }
            long waitMillis = retryDelayMillis(attempt, response.retryAfterSeconds());
            logger.info("Registry {} answered HTTP {} for {}; retrying in {} ms (attempt {} of {})",
                    url, response.statusCode(), artifactCode, waitMillis, attempt, MAX_ATTEMPTS_PER_REGISTRY);
            reportProgress("HTTP " + response.statusCode() + " from " + url + "; waiting " + waitMillis + " ms");
            waitBeforeRetry(waitMillis);
        }
    }

    /**
     * Waits before the next attempt at the same registry. Package-private so that tests can
     * override it and not spend the backoff.
     *
     * @param millis how long to wait
     */
    void waitBeforeRetry(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to retry a registry", ex);
        }
    }

    /**
     * Whether a status means the registry is judging the nanopublication itself, so that no other
     * registry would take it either. Deliberately narrow: 401, 403, 404 and 405 are about the
     * endpoint or the client rather than the content, and those keep falling through to the next
     * registry the way they always have.
     */
    private static boolean isNanopubRejection(int statusCode) {
        return statusCode == 400 || statusCode == 409 || statusCode == 413
               || statusCode == 415 || statusCode == 422;
    }

    /**
     * Whether a status says "not now": a rate limit, a request timeout, or a server-side problem
     * that a moment's wait may clear.
     */
    private static boolean isWorthRetrying(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private static long retryDelayMillis(int attempt, Integer retryAfterSeconds) {
        if (retryAfterSeconds != null) {
            return Math.min(retryAfterSeconds * 1000L, MAX_RETRY_DELAY_MILLIS);
        }
        return Math.min((1L << (attempt - 1)) * 1000L, MAX_RETRY_DELAY_MILLIS);
    }

    /**
     * What a registry answered to a POST: enough to tell a rate limit from a rejection, and to
     * honour a {@code Retry-After} header.
     */
    private record PostResponse(int statusCode, String reasonPhrase, Integer retryAfterSeconds) {

        boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }

    }

    private HttpPost preparePost(Nanopub nanopub) throws IOException {
        HttpPost post = new HttpPost(registryInfo.getUrl());
        String nanopubString = NanopubUtils.writeToString(nanopub, RDFFormat.TRIG);
        post.setEntity(new StringEntity(nanopubString, "UTF-8"));
        post.setHeader("Content-Type", RDFFormat.TRIG.getDefaultMIMEType());
        return post;
    }

    private PostResponse executePost(HttpPost post, String url) throws IOException {
        HttpResponse response = NanopubUtils.getHttpClient().execute(post);
        int code = response.getStatusLine().getStatusCode();
        String reasonPhrase = response.getStatusLine().getReasonPhrase();
        if (code >= 200 && code < 300) {
            if (usedServers.containsKey(url)) {
                usedServers.put(url, usedServers.get(url) + 1);
            } else {
                usedServers.put(url, 1);
            }
            String nanopubUrl = registryInfo.getCollectionUrl() + artifactCode;
            logger.debug("Published {} at {}", artifactCode, nanopubUrl);
            reportProgress("Published: " + nanopubUrl);
        } else {
            logger.warn("Registry {} rejected {} with HTTP {} {}", url, artifactCode, code, reasonPhrase);
            reportProgress("Response: " + code + " " + reasonPhrase);
        }
        return new PostResponse(code, reasonPhrase, retryAfterSeconds(response));
    }

    /**
     * Reads a {@code Retry-After} header in its delay-seconds form. The HTTP-date form is left to
     * the backoff schedule rather than parsed, as registries send the seconds form.
     */
    private static Integer retryAfterSeconds(HttpResponse response) {
        Header header = response.getFirstHeader("Retry-After");
        if (header == null || header.getValue() == null) {
            return null;
        }
        try {
            int seconds = Integer.parseInt(header.getValue().trim());
            return seconds >= 0 ? seconds : null;
        } catch (NumberFormatException ex) {
            return null;
        }
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
