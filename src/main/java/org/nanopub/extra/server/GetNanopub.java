package org.nanopub.extra.server;

import static org.nanopub.extra.server.NanopubStatus.extractArtifactCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.trusty.TrustyNanopubUtils;

import com.beust.jcommander.ParameterException;

import net.trustyuri.rdf.RdfModule;

public class GetNanopub extends CliRunner {

	@com.beust.jcommander.Parameter(description = "nanopub-uris-or-artifact-codes", required = true)
	private List<String> nanopubIds;

	@com.beust.jcommander.Parameter(names = "-f", description = "Format of the nanopub: trig, nq, trix, trig.gz, ...")
	private String format;

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File outputFile;

	@com.beust.jcommander.Parameter(names = "-e", description = "Write error messages from fetching nanopubs into this file (ignored otherwise)")
	private File errorFile;

	@com.beust.jcommander.Parameter(names = "-i", description = "Retrieve the index for the given index nanopub")
	private boolean getIndex;

	@com.beust.jcommander.Parameter(names = "-c", description = "Retrieve the content of the given index")
	private boolean getIndexContent;

	@com.beust.jcommander.Parameter(names = "--mongodb-host", description = "Directly contact single MongoDB instance instead of the network (e.g. 'localhost')")
	private String mongoDbHost;

	@com.beust.jcommander.Parameter(names = "--mongodb-port", description = "MongoDB port")
	private int mongoDbPort = 27017;

	@com.beust.jcommander.Parameter(names = "--mongodb-dbname", description = "MongoDB database name")
	private String mongoDbName = "nanopub-server";

	@com.beust.jcommander.Parameter(names = "--mongodb-user", description = "MongoDB user name")
	private String mongoDbUsername;

	@com.beust.jcommander.Parameter(names = "--mongodb-pw", description = "MongoDB password")
	private String mongoDbPassword;

	@com.beust.jcommander.Parameter(names = "-r", description = "Show a report in the end")
	private boolean showReport;

	@com.beust.jcommander.Parameter(names = "-l", description = "Use a local server, e.g. http://localhost:7880/")
	private String localServer;

	@com.beust.jcommander.Parameter(names = "--simulate-unreliable-connection",
			description = "Simulate an unreliable connection for testing purposes")
	private boolean simUnrelConn;

	public static void main(String[] args) {
		try {
			GetNanopub obj = CliRunner.initJc(new GetNanopub(), args);
			simulateUnreliableConnection = obj.simUnrelConn;
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private static boolean simulateUnreliableConnection = false;

	public static Nanopub get(String uriOrArtifactCode) {
		return get(uriOrArtifactCode, NanopubUtils.getHttpClient());
	}

	public static Nanopub get(String uriOrArtifactCode, HttpClient httpClient) {
		ServerIterator serverIterator = new ServerIterator();
		String ac = getArtifactCode(uriOrArtifactCode);
		if (!ac.startsWith(RdfModule.MODULE_ID)) {
			throw new IllegalArgumentException("Not a trusty URI of type RA");
		}
		while (serverIterator.hasNext()) {
			RegistryInfo registryInfo = serverIterator.next();
			try {
				Nanopub np = get(ac, registryInfo, httpClient);
				if (np != null) {
					return np;
				}
			} catch (IOException ex) {
				// ignore
			} catch (RDF4JException ex) {
				// ignore
			} catch (MalformedNanopubException ex) {
				// ignore
			}
		}
		return null;
	}

	public static Nanopub get(String uriOrArtifactCode, NanopubDb db) {
		String ac = getArtifactCode(uriOrArtifactCode);
		if (!ac.startsWith(RdfModule.MODULE_ID)) {
			throw new IllegalArgumentException("Not a trusty URI of type RA");
		}
		return db.getNanopub(ac);
	}

	public static Nanopub get(String artifactCode, RegistryInfo registryInfo)
			throws IOException, RDF4JException, MalformedNanopubException {
		return get(artifactCode, registryInfo, NanopubUtils.getHttpClient());
	}

	public static Nanopub get(String artifactCode, RegistryInfo registryInfo, HttpClient httpClient)
			throws IOException, RDF4JException, MalformedNanopubException {
		HttpGet get = null;
		try {
			get = new HttpGet(registryInfo.getCollectionUrl() + artifactCode);
		} catch (IllegalArgumentException ex) {
			throw new IOException("invalid URL: " + registryInfo.getCollectionUrl() + artifactCode);
		}
		get.setHeader("Accept", "application/trig");
		InputStream in = null;
		try {
			HttpResponse resp = httpClient.execute(get);
			if (!wasSuccessful(resp)) {
				EntityUtils.consumeQuietly(resp.getEntity());
				throw new IOException(resp.getStatusLine().toString());
			}
			in = resp.getEntity().getContent();
			if (simulateUnreliableConnection) {
				in = new UnreliableInputStream(in);
			}
			Nanopub nanopub = new NanopubImpl(in, RDFFormat.TRIG);
			if (!TrustyNanopubUtils.isValidTrustyNanopub(nanopub)) {
				throw new MalformedNanopubException("Nanopub is not trusty");
			}
			return nanopub;
		} finally {
			if (in != null) in.close();
		}
	}

	public static String getArtifactCode(String uriOrArtifactCode) {
		return extractArtifactCode(uriOrArtifactCode);
	}

	private OutputStream outputStream = System.out;
	private PrintStream errorStream = null;
	private int count;
	private List<Exception> exceptions;
	private NanopubDb db = null;

	private RDFFormat rdfFormat;

	public GetNanopub() {}

	protected void run() throws IOException, RDFHandlerException, MalformedNanopubException {
		if (showReport) {
			exceptions = new ArrayList<>();
		}
		if (outputFile == null) {
			if (format == null) {
				format = "trig";
			}
			rdfFormat = Rio.getParserFormatForFileName("file." + format).orElse(RDFFormat.TRIG);
		} else {
			rdfFormat = Rio.getParserFormatForFileName(outputFile.getName()).orElse(RDFFormat.TRIG);
			if (outputFile.getName().endsWith(".gz")) {
				outputStream = new GZIPOutputStream(new FileOutputStream(outputFile));
			} else {
				outputStream = new FileOutputStream(outputFile);
			}
		}
		if (errorFile != null) {
			errorStream = new PrintStream(errorFile);
		}
		if (mongoDbHost != null) {
			db = new NanopubDb(mongoDbHost, mongoDbPort, mongoDbName, mongoDbUsername, mongoDbPassword);
		}
		FetchIndex fetchIndex = null;
		try {
			for (String nanopubId : nanopubIds) {
				if (getIndex || getIndexContent) {
					if (db == null) {
						fetchIndex = new FetchIndex(nanopubId, outputStream, rdfFormat, getIndex, getIndexContent, localServer);
					} else {
						fetchIndex = new FetchIndexFromDb(nanopubId, db, outputStream, rdfFormat, getIndex, getIndexContent);
					}
					fetchIndex.setProgressListener(new FetchIndex.Listener() {
	
						@Override
						public void progress(int count) {
							System.err.print(count + " nanopubs...\r");
						}
	
						@Override
						public void exceptionHappened(Exception ex, RegistryInfo r, String artifactCode) {
							if (showReport) {
								exceptions.add(ex);
							}
							if (errorStream != null) {
								String exString = ex.toString().replaceAll("\\n", "\\\\n");
								errorStream.println(r + " " + artifactCode + " " + exString);
							}
						}
	
					});
					fetchIndex.run();
					count = fetchIndex.getNanopubCount();
				} else {
					Nanopub np;
					if (db == null) {
						np = get(nanopubId);
					} else {
						np = get(nanopubId, db);
					}
					outputNanopub(nanopubId, np);
				}
			}
			if (outputStream != null && outputStream != System.out) {
				System.err.println(count + " nanopubs retrieved and saved in " + outputFile);
			}
		} finally {
			if (outputStream != System.out) outputStream.close();
			if (errorStream != null) errorStream.close();
		}
		if (showReport && fetchIndex != null) {
			System.err.println("Number of retries: " + exceptions.size());
			System.err.println("Used servers:");
			List<RegistryInfo> usedServers = fetchIndex.getRegistries();
			final FetchIndex fi = fetchIndex;
			Collections.sort(usedServers, new Comparator<RegistryInfo>() {
				@Override
				public int compare(RegistryInfo o1, RegistryInfo o2) {
					return fi.getServerUsage(o2) - fi.getServerUsage(o1);
				}
			});
			int usedServerCount = 0;
			for (RegistryInfo si : usedServers) {
				if (fetchIndex.getServerUsage(si) > 0) usedServerCount++;
				System.err.format("%8d %s%n", fetchIndex.getServerUsage(si), si.getUrl());
			}
			System.err.format("Number of servers used: " + usedServerCount);
		}
	}

	private void outputNanopub(String nanopubId, Nanopub np) throws IOException, RDFHandlerException {
		if (np == null) {
			System.err.println("NOT FOUND: " + nanopubId);
			return;
		}
		count++;
		if (outputStream == System.out) {
			NanopubUtils.writeToStream(np, System.out, rdfFormat);
			System.out.print("\n\n");
		} else {
			NanopubUtils.writeToStream(np, outputStream, rdfFormat);
			if (count % 100 == 0) {
				System.err.print(count + " nanopubs...\r");
			}
		}
	}

	private static boolean wasSuccessful(HttpResponse resp) {
		int c = resp.getStatusLine().getStatusCode();
		return c >= 200 && c < 300;
	}

}
