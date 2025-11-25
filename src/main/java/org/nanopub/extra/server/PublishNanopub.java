package org.nanopub.extra.server;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.*;

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

    private static final Log LOG = LogFactory.getLog(PublishNanopub.class);

    @com.beust.jcommander.Parameter(description = "nanopubs", required = true)
    private List<String> nanopubs = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
    private boolean verbose;

    @com.beust.jcommander.Parameter(names = "--dry-run", description = "Simulate (no action)")
    private boolean dryRun;

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
    private String artifactCode;

    /**
     * Default constructor for PublishNanopub.
     * Initializes the command line tool with no parameters.
     */
    public PublishNanopub() {
        super();
        initLogging();
    }



    /**
     * Little hack: We interpret an enabled DEBUG Log like the command line flag "verbose".
     * The other way around, we do interpret the command line concept of setting the verbose flag as
     * activation of debug log.
     */
    // TODO Push-Up! This will be useful with other CliTasks that support the verbose flag.
    public void initLogging () {
        if (LOG.isDebugEnabled()) {
            verbose = true;
        } else if (verbose) {
            logOrSysout(LOG, "Enabling DEBUG log, since VERBOSE cli flag is enabled.");
            if (!LOG.isTraceEnabled()) {
                LogFactory.getFactory().setAttribute(LogFactory.PRIORITY_KEY, "DEBUG");
            }
        }
        logOrSysout(LOG, "Initialized logging for CLI Runner. Verbose mode: " + verbose);
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
                    if (verbose) {
                        logOrSysout(LOG, "Reading file: " + s);
                    }
                    MultiNanopubRdfHandler.process(new File(s), np -> {
                        if (failed) return;
                        processNanopub(np);
                    });
                    if (count == 0) {
                        String msg = "NO NANOPUB FOUND: " + s;
                        LOG.info(msg);
                        System.out.println(msg);
                        break;
                    }
                }
            } catch (RDF4JException ex) {
                logOrSysout(LOG, "RDF ERROR: " + s);
                ex.printStackTrace(System.err);
                break;
            } catch (MalformedNanopubException ex) {
                logOrSysout(LOG, "INVALID NANOPUB: " + s);
                ex.printStackTrace(System.err);
                break;
            }
            if (failed) {
                logOrSysout(LOG, "FAILED TO PUBLISH NANOPUBS");
                break;
            }
        }
        for (String s : usedServers.keySet()) {
            int c = usedServers.get(s);
            logOrSysout(LOG, c + " nanopub" + (c == 1 ? "" : "s") + " published at " + s);
        }
        if (sparqlRepo != null) {
            try {
                sparqlRepo.shutDown();
            } catch (RepositoryException ex) {
                ex.printStackTrace();
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
        if (LOG.isDebugEnabled()) {
            // Little hack: We interpret an enabled DEBUG Log like the command line flag "verbose".
            // The other way around, we do interpret the command line concept of setting the verbose flag as
            // activation of debug log.
            verbose = true;
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
        artifactCode = TrustyUriUtils.getArtifactCode(nanopub.getUri().toString());
        if (verbose) {
            logOrSysout(LOG, "Trying to publish nanopub: " + artifactCode);
        }
        if (NanopubServerUtils.isProtectedNanopub(nanopub)) {
            throw new RuntimeException("Can't publish protected nanopublication: " + artifactCode);
        }
        while (registryInfo != null) {
            String url = registryInfo.getUrl();

            // TODO Check here whether nanopub type is covered at given registry.

            if (verbose) {
                logOrSysout(LOG, "Trying server: " + url);
            }
            try {
                HttpPost post = preparePost(nanopub);
                if (!dryRun) {
                    String nanopubUrl = executePost(post, url);
                    if (nanopubUrl != null) return nanopubUrl;
                }
            } catch (IOException | RDF4JException ex) {
                if (verbose) {
                    logOrSysout(LOG, ex.getClass().getName() + ": " + ex.getMessage());
                }
            }
            registryInfo = serverIterator.next();
        }
        registryInfo = null;
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
            if (verbose) {
                logOrSysout(LOG, "Published: " + nanopubUrl);
            }
            return nanopubUrl;
        } else {
            if (verbose) {
                logOrSysout(LOG, "Response: " + code + " " + response.getStatusLine().getReasonPhrase());
            }
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
