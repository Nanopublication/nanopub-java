package org.nanopub.extra.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;

public class NanopubServerUtils {

	// Version numbers have the form MAJOR.MINOR (for example, 0.12 is a newer version than 0.9!)
	public static final String requiredProtocolVersion = "0.2";
	public static final int requiredProtocolVersionValue = getVersionValue(requiredProtocolVersion);

	private static HttpClient httpClient;

	protected NanopubServerUtils() {
		throw new RuntimeException("no instances allowed");
	}

	public static List<String> loadPeerList(String serverUrl) throws IOException {
		return loadList(serverUrl + "peers");
	}

	public static List<String> loadPeerList(ServerInfo si) throws IOException {
		return loadPeerList(si.getPublicUrl());
	}

	public static List<String> loadNanopubUriList(String serverUrl, int page) throws IOException {
		return loadList(serverUrl + "nanopubs?page=" + page);
	}

	public static List<String> loadNanopubUriList(ServerInfo si, int page) throws IOException {
		return loadNanopubUriList(si.getPublicUrl(), page);
	}

	public static List<String> loadList(String url) throws IOException {
		List<String> list = new ArrayList<String>();
		HttpGet get = new HttpGet(url);
		get.setHeader("Content-Type", "text/plain");
		BufferedReader r = null;
		try {
			if (httpClient == null) {
				RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(2000)
						.setConnectionRequestTimeout(100).setSocketTimeout(2000)
						.setCookieSpec(CookieSpecs.STANDARD).build();
				PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
				connManager.setDefaultMaxPerRoute(10);
				connManager.setMaxTotal(1000);
				httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig)
						.setConnectionManager(connManager).build();
			}
			HttpResponse resp = httpClient.execute(get);
			int code = resp.getStatusLine().getStatusCode();
			if (code < 200 || code > 299) {
				EntityUtils.consumeQuietly(resp.getEntity());
				throw new IOException("HTTP error: " + code + " " + resp.getStatusLine().getReasonPhrase());
			}
			InputStream in = resp.getEntity().getContent();
			r = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
			String line = null;
			while ((line = r.readLine()) != null) {
				list.add(line.trim());
			}
		} finally {
			if (r != null) r.close();
		}
		return list;
	}

	private static final List<String> bootstrapServerList = new ArrayList<>();

	static {
		// Hard-coded server instances:
		bootstrapServerList.add("http://server.nanopubs.lod.labs.vu.nl/");
		bootstrapServerList.add("http://130.60.24.146:7880/");
		bootstrapServerList.add("https://server.nanopubs.knows.idlab.ugent.be/");
		bootstrapServerList.add("https://openphacts.cs.man.ac.uk/nanopub/server/");
		bootstrapServerList.add("http://server.np.scify.org/");
		bootstrapServerList.add("http://app.tkuhn.eculture.labs.vu.nl/nanopub-server-1/");
		bootstrapServerList.add("http://app.tkuhn.eculture.labs.vu.nl/nanopub-server-2/");
		bootstrapServerList.add("http://app.tkuhn.eculture.labs.vu.nl/nanopub-server-3/");
		bootstrapServerList.add("http://app.tkuhn.eculture.labs.vu.nl/nanopub-server-4/");
	}

	public static List<String> getBootstrapServerList() {
		return bootstrapServerList;
	}

	public static int getVersionValue(String versionString) {
		try {
			int major = Integer.parseInt(versionString.split("\\.")[0]);
			int minor = Integer.parseInt(versionString.split("\\.")[1]);
			return (major * 1000) + minor;
		} catch (Exception ex) {
			return 0;
		}
	}

	public static final IRI PROTECTED_NANOPUB = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/ProtectedNanopub");

	public static boolean isProtectedNanopub(Nanopub np) {
		for (Statement st : np.getPubinfo()) {
			if (!st.getSubject().equals(np.getUri())) continue;
			if (!st.getPredicate().equals(RDF.TYPE)) continue;
			if (st.getObject().equals(PROTECTED_NANOPUB)) return true;
		}
		return false;
	}

}
