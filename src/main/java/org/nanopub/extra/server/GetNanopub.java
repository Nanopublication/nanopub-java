package org.nanopub.extra.server;

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

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfModule;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.trusty.TrustyNanopubUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class GetNanopub {

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

	@com.beust.jcommander.Parameter(names = "-r", description = "Show a report in the end")
	private boolean showReport;

	@com.beust.jcommander.Parameter(names = "--simulate-unreliable-connection",
			description = "Simulate an unreliable connection for testing purposes")
	private boolean simUnrelConn;

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		GetNanopub obj = new GetNanopub();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		simulateUnreliableConnection = obj.simUnrelConn;
		try {
			obj.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private static boolean simulateUnreliableConnection = false;

	public static Nanopub get(String uriOrArtifactCode) {
		ServerIterator serverIterator = new ServerIterator();
		String ac = getArtifactCode(uriOrArtifactCode);
		if (!ac.startsWith(RdfModule.MODULE_ID)) {
			throw new IllegalArgumentException("Not a trusty URI of type RA");
		}
		while (serverIterator.hasNext()) {
			ServerInfo serverInfo = serverIterator.next();
			try {
				Nanopub np = get(ac, serverInfo);
				if (np != null) {
					return np;
				}
			} catch (IOException ex) {
				// ignore
			} catch (OpenRDFException ex) {
				// ignore
			} catch (MalformedNanopubException ex) {
				// ignore
			}
		}
		return null;
	}

	public static Nanopub get(String artifactCode, ServerInfo serverInfo)
			throws IOException, OpenRDFException, MalformedNanopubException {
		return get(artifactCode, serverInfo.getPublicUrl());
	}

	public static Nanopub get(String artifactCode, String serverUrl)
			throws IOException, OpenRDFException, MalformedNanopubException {
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000)
				.setConnectionRequestTimeout(100).setSocketTimeout(10000).build();
		HttpClient c = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		return get(artifactCode, serverUrl, c);
	}

	public static Nanopub get(String artifactCode, String serverUrl, HttpClient httpClient)
			throws IOException, OpenRDFException, MalformedNanopubException {
		HttpGet get = new HttpGet(serverUrl + artifactCode);
		get.setHeader("Accept", "application/trig");
		InputStream in = null;
		try {
			HttpResponse resp = httpClient.execute(get);
			if (!wasSuccessful(resp)) {
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
		if (uriOrArtifactCode.indexOf(":") > 0) {
			URI uri = new URIImpl(uriOrArtifactCode);
			if (!TrustyUriUtils.isPotentialTrustyUri(uri)) {
				throw new IllegalArgumentException("Not a well-formed trusty URI");
			}
			return TrustyUriUtils.getArtifactCode(uri.toString());
		} else {
			if (!TrustyUriUtils.isPotentialArtifactCode(uriOrArtifactCode)) {
				throw new IllegalArgumentException("Not a well-formed artifact code");
			}
			return uriOrArtifactCode;
		}
	}

	private OutputStream outputStream = System.out;
	private PrintStream errorStream = null;
	private int count;
	private List<Exception> exceptions;

	private RDFFormat rdfFormat;

	public GetNanopub() {
	}

	private void run() throws IOException, RDFHandlerException, MalformedNanopubException {
		if (showReport) {
			exceptions = new ArrayList<>();
		}
		if (outputFile == null) {
			if (format == null) {
				format = "trig";
			}
			rdfFormat = Rio.getParserFormatForFileName("file." + format);
		} else {
			rdfFormat = Rio.getParserFormatForFileName(outputFile.getName());
			if (outputFile.getName().endsWith(".gz")) {
				outputStream = new GZIPOutputStream(new FileOutputStream(outputFile));
			} else {
				outputStream = new FileOutputStream(outputFile);
			}
		}
		if (errorFile != null) {
			errorStream = new PrintStream(errorFile);
		}
		FetchIndex fetchIndex = null;
		for (String nanopubId : nanopubIds) {
			if (getIndex || getIndexContent) {
				fetchIndex = new FetchIndex(nanopubId, outputStream, rdfFormat, getIndex, getIndexContent);
				fetchIndex.setProgressListener(new FetchIndex.Listener() {

					@Override
					public void progress(int count) {
						System.err.print(count + " nanopubs...\r");
					}

					@Override
					public void exceptionHappened(Exception ex, String serverUrl, String artifactCode) {
						if (showReport) {
							exceptions.add(ex);
						}
						if (errorStream != null) {
							String exString = ex.toString().replaceAll("\\n", "\\\\n");
							errorStream.println(serverUrl + " " + artifactCode + " " + exString);
						}
					}

				});
				fetchIndex.run();
				count = fetchIndex.getNanopubCount();
			} else {
				outputNanopub(nanopubId, get(nanopubId));
			}
		}
		if (outputStream != System.out) {
			outputStream.close();
			System.err.println(count + " nanopubs retrieved and saved in " + outputFile);
		}
		if (errorStream != null) {
			errorStream.close();
		}
		if (showReport && fetchIndex != null) {
			System.err.println("Number of retries: " + exceptions.size());
			System.err.println("Used servers:");
			List<ServerInfo> usedServers = fetchIndex.getServers();
			final FetchIndex fi = fetchIndex;
			Collections.sort(usedServers, new Comparator<ServerInfo>() {
				@Override
				public int compare(ServerInfo o1, ServerInfo o2) {
					return fi.getServerUsage(o2) - fi.getServerUsage(o1);
				}
			});
			int usedServerCount = 0;
			for (ServerInfo si : usedServers) {
				if (fetchIndex.getServerUsage(si) > 0) usedServerCount++;
				System.err.format("%8d %s%n", fetchIndex.getServerUsage(si), si.getPublicUrl());
			}
			System.err.format("Number of servers used: " + usedServerCount);
		}
	}

	private void outputNanopub(String nanopubId, Nanopub np) throws IOException, RDFHandlerException {
		count++;
		if (np == null) {
			System.err.println("NOT FOUND: " + nanopubId);
		} else if (outputStream == System.out) {
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
