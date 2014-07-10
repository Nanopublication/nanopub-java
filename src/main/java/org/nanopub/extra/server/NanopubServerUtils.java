package org.nanopub.extra.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

public class NanopubServerUtils {

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
		InputStream in = HttpClientBuilder.create().build().execute(get).getEntity().getContent();
	    BufferedReader r = new BufferedReader(new InputStreamReader(in));
	    String line = null;
	    while ((line = r.readLine()) != null) {
	    	list.add(line.trim());
	    }
	    r.close();
		return list;
	}

	private static final List<String> bootstrapServerList = new ArrayList<>();

	static {
		// Hard-coded server instances:
		bootstrapServerList.add("http://np.inn.ac/");
		// more to come...
	}

	public static List<String> getBootstrapServerList() {
		return bootstrapServerList;
	}

}
