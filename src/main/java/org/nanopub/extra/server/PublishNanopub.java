package org.nanopub.extra.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.server.ServerInfo.ServerInfoException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import net.trustyuri.TrustyUriUtils;

public class PublishNanopub {

	@com.beust.jcommander.Parameter(description = "nanopubs", required = true)
	private List<String> nanopubs = new ArrayList<String>();

	@com.beust.jcommander.Parameter(names = "-v", description = "Verbose")
	private boolean verbose = false;

	@com.beust.jcommander.Parameter(names = "-u", description = "Use the given nanopub server URLs")
	private List<String> serverUrls;

	@com.beust.jcommander.Parameter(names = "-s", description = "Get nanopubs to be published from given SPARQL endpoint")
	private String sparqlEndpointUrl;

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		PublishNanopub obj = new PublishNanopub();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		try {
			obj.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public static void publish(Nanopub nanopub) throws IOException {
		new PublishNanopub().publishNanopub(nanopub);
	}

	private ServerIterator serverIterator = null;
	private ServerInfo serverInfo = null;
	private Map<String,Integer> usedServers = new HashMap<>();
	private int count;
	private boolean failed;
	private SPARQLRepository sparqlRepo;
	private String artifactCode;

	public PublishNanopub() {
	}

	private void run() throws IOException {
		failed = false;
		for (String s : nanopubs) {
			count = 0;
			try {
				if (sparqlEndpointUrl != null) {
					if (sparqlRepo == null) {
						sparqlRepo = new SPARQLRepository(sparqlEndpointUrl);
						sparqlRepo.initialize();
					}
					processNanopub(new NanopubImpl(sparqlRepo, SimpleValueFactory.getInstance().createIRI(s)));
				} else {
					if (verbose) {
						System.out.println("Reading file: " + s);
					}
					MultiNanopubRdfHandler.process(new File(s), new NanopubHandler() {
						@Override
						public void handleNanopub(Nanopub np) {
							if (failed) return;
							processNanopub(np);
						}
					});
					if (count == 0) {
						System.out.println("NO NANOPUB FOUND: " + s);
						break;
					}
				}
			} catch (RDF4JException ex) {
				System.out.println("RDF ERROR: " + s);
				ex.printStackTrace(System.err);
				break;
			} catch (MalformedNanopubException ex) {
				System.out.println("INVALID NANOPUB: " + s);
				ex.printStackTrace(System.err);
				break;
			}
			if (failed) {
				System.out.println("FAILED TO PUBLISH NANOPUBS");
				break;
			}
		}
		for (String s : usedServers.keySet()) {
			int c = usedServers.get(s);
			System.out.println(c + " nanopub" + (c==1?"":"s") + " published at " + s);
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
			System.err.print(count + " nanopubs...\r");
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

	public void publishNanopub(Nanopub nanopub) throws IOException {
		if (serverInfo == null) {
			if (serverUrls == null || serverUrls.isEmpty()) {
				serverIterator = new ServerIterator();
			} else {
				serverIterator = new ServerIterator(serverUrls);
			}
			serverInfo = serverIterator.next();
		}
		artifactCode = TrustyUriUtils.getArtifactCode(nanopub.getUri().toString());
		if (verbose) {
			System.out.println("---");
			System.out.println("Trying to publish nanopub: " + artifactCode);
		}
		if (NanopubServerUtils.isProtectedNanopub(nanopub)) {
			throw new RuntimeException("Can't publish protected nanopublication: " + artifactCode);
		}
		while (serverInfo != null) {
			String serverUrl = serverInfo.getPublicUrl();
			try {
				if (!ServerInfo.load(serverUrl).isPostNanopubsEnabled()) {
					serverInfo = serverIterator.next();
					continue;
				}
			} catch (ServerInfoException ex) {
				serverInfo = serverIterator.next();
				continue;
			}
			if (!serverInfo.getNanopubSurfacePattern().matchesUri(nanopub.getUri().stringValue())) {
				continue;
			}
			if (verbose) {
				System.out.println("Trying server: " + serverUrl);
			}
			try {
				HttpPost post = new HttpPost(serverUrl);
				String nanopubString = NanopubUtils.writeToString(nanopub, RDFFormat.TRIG);
				post.setEntity(new StringEntity(nanopubString, "UTF-8"));
				post.setHeader("Content-Type", RDFFormat.TRIG.getDefaultMIMEType());
				RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
				HttpResponse response = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build().execute(post);
				int code = response.getStatusLine().getStatusCode();
				if (code >= 200 && code < 300) {
					if (usedServers.containsKey(serverUrl)) {
						usedServers.put(serverUrl, usedServers.get(serverUrl) + 1);
					} else {
						usedServers.put(serverUrl, 1);
					}
					if (verbose) {
						System.out.println("Published: " + artifactCode);
					}
					return;
				} else {
					if (verbose) {
						System.out.println("Response: " + code + " " + response.getStatusLine().getReasonPhrase());
					}
				}
			} catch (IOException ex) {
				if (verbose) {
					System.out.println(ex.getClass().getName() + ": " + ex.getMessage());
				}
			} catch (RDF4JException ex) {
				if (verbose) {
					System.out.println(ex.getClass().getName() + ": " + ex.getMessage());
				}
			}
			serverInfo = serverIterator.next();
		}
		serverInfo = null;
		throw new RuntimeException("Failed to publish the nanopub");
	}

	public ServerInfo getUsedServer() {
		return serverInfo;
	}

	public String getPublishedNanopubUrl() {
		if (serverInfo == null || artifactCode == null) {
			return null;
		}
		return serverInfo.getPublicUrl() + artifactCode;
	}

}
