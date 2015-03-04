package org.nanopub.extra.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

	@com.beust.jcommander.Parameter(names = "-i", description = "Retrieve the index for the given index nanopub")
	private boolean getIndex;

	@com.beust.jcommander.Parameter(names = "-c", description = "Retrieve the content of the given index")
	private boolean getIndexContent;

	public static void main(String[] args) {
		GetNanopub obj = new GetNanopub();
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
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5 * 1000).build();
		HttpClient c = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		HttpGet get = new HttpGet(serverUrl + artifactCode);
		get.setHeader("Accept", "application/trig");
		HttpResponse resp = c.execute(get);
		if (!wasSuccessful(resp)) return null;
		Nanopub nanopub = new NanopubImpl(resp.getEntity().getContent(), RDFFormat.TRIG);
		if (TrustyNanopubUtils.isValidTrustyNanopub(nanopub)) {
			return nanopub;
		}
		return null;
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

	private OutputStream outputStream = null;
	private int count;

	private RDFFormat rdfFormat;

	public GetNanopub() {
	}

	private void run() throws IOException, RDFHandlerException, MalformedNanopubException {
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
		for (String nanopubId : nanopubIds) {
			if (getIndex || getIndexContent) {
				FetchIndex fetchIndex = new FetchIndex(nanopubId, outputStream, rdfFormat, getIndex, getIndexContent);
				fetchIndex.run();
				count = fetchIndex.getNanopubCount();
			} else {
				outputNanopub(nanopubId, get(nanopubId));
			}
		}
		if (outputStream != null) {
			outputStream.close();
			System.out.println(count + " nanopubs retrieved and saved in " + outputFile);
		}
	}

	private void outputNanopub(String nanopubId, Nanopub np) throws IOException, RDFHandlerException {
		count++;
		if (np == null) {
			System.err.println("NOT FOUND: " + nanopubId);
		} else if (outputStream == null) {
			NanopubUtils.writeToStream(np, System.out, rdfFormat);
			System.out.print("\n\n");
		} else {
			NanopubUtils.writeToStream(np, outputStream, rdfFormat);
			if (count % 100 == 0) {
				System.out.print(count + " nanopubs...\r");
			}
		}
	}

	private static boolean wasSuccessful(HttpResponse resp) {
		int c = resp.getStatusLine().getStatusCode();
		return c >= 200 && c < 300;
	}

}
