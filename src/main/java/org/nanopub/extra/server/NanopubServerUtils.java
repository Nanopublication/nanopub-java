package org.nanopub.extra.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

public class NanopubServerUtils {

	public static final String protocolVersion = "0.2";
	public static final float protocolVersionValue = getVersionValue(protocolVersion);
	public static final String requiredProtocolVersion = "0.2";
	public static final float requiredProtocolVersionValue = getVersionValue(requiredProtocolVersion);

	private NanopubServerUtils() {}  // no instances allowed

	public static List<String> loadPeerList(String serverUrl) throws IOException {
		return loadList(serverUrl + "peers");
	}

	public static List<String> loadPeerList(ServerInfo si) throws IOException {
		return loadPeerList(si.getPublicUrl());
	}

	public static List<String> loadNanopubUriList(String serverUrl, int page) throws IOException {
		return loadList(serverUrl +"nanopubs?page=" + page);
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
			CloseableHttpResponse resp = HttpClientBuilder.create().build().execute(get);
			int code = resp.getStatusLine().getStatusCode();
			if (code < 200 || code > 299) {
				throw new IOException("HTTP error: " + code + " " + resp.getStatusLine().getReasonPhrase());
			}
			InputStream in = resp.getEntity().getContent();
			r = new BufferedReader(new InputStreamReader(in));
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
		bootstrapServerList.add("http://np.inn.ac/");
		bootstrapServerList.add("http://s1.semanticscience.org:8080/nanopub-server/");
		bootstrapServerList.add("http://ristretto.med.yale.edu:8080/nanopub-server/");
	}

	public static List<String> getBootstrapServerList() {
		return bootstrapServerList;
	}

	public static float getVersionValue(String versionString) {
		try {
			int major = Integer.parseInt(versionString.split("\\.")[0]);
			int minor = Integer.parseInt(versionString.split("\\.")[1]);
			return (float) (major + 0.001*minor);
		} catch (Exception ex) {
			return 0.0f;
		}
	}

}
