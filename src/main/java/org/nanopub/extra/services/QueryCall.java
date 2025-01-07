package org.nanopub.extra.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.Charsets;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.nanopub.NanopubUtils;

/**
 * Second-generation query API call.
 */
public class QueryCall {

	private static int parallelCallCount = 2;
	private static int maxRetryCount = 5;

	public static HttpResponse run(String queryId, Map<String,String> params) {
		int retryCount = 0;
		while (retryCount < maxRetryCount) {
			QueryCall apiCall = new QueryCall(queryId, params);
			apiCall.run();
			while (!apiCall.calls.isEmpty() && apiCall.resp == null) {
				try {
				    Thread.sleep(50);
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
			}
			if (apiCall.resp != null) {
				return apiCall.resp;
			}
			retryCount = retryCount + 1;
		}
		throw new RuntimeException("Giving up contacting API: " + queryId);
	}

	// TODO Available services should be retrieved from a setting, not hard-coded:
	public static String[] queryApiInstances = new String[] {
		"https://query.knowledgepixels.com/",
		"https://query.petapico.org/",
		"https://query.np.trustyuri.net/"
	};

	private static List<String> checkedApiInstances;

	public static List<String> getApiInstances() {
		if (checkedApiInstances != null) return checkedApiInstances;
		checkedApiInstances = new ArrayList<String>();
		for (String a : queryApiInstances) {
			try {
				System.err.println("Checking API instance: " + a);
				HttpResponse resp = NanopubUtils.getHttpClient().execute(new HttpGet(a));
				if (wasSuccessful(resp)) {
					System.err.println("SUCCESS: Nanopub Query instance is accessible: " + a);
					checkedApiInstances.add(a);
				} else {
					System.err.println("FAILURE: Nanopub Query instance isn't accessible: " + a);
				}
			} catch (IOException ex) {
				System.err.println("FAILURE: Nanopub Query instance isn't accessible: " + a);
			}
		}
		System.err.println(checkedApiInstances.size() + " accessible Nanopub Query instances");
		if (checkedApiInstances.size() < 2) {
			checkedApiInstances = null;
			throw new RuntimeException("Not enough healthy Nanopub Query instances available");
		}
		return checkedApiInstances;
	}

	private String queryId;
	private String paramString;
	private List<String> apisToCall = new ArrayList<>();
	private List<Call> calls = new ArrayList<>();

	private HttpResponse resp;

	private QueryCall(String queryId, Map<String,String> params) {
		this.queryId = queryId;
		paramString = "";
		if (params != null) {
			paramString = "?";
			for (String k : params.keySet()) {
				if (paramString.length() > 1) paramString += "&";
				try {
					paramString += k + "=";
					paramString += URLEncoder.encode(params.get(k), Charsets.UTF_8.toString());
				} catch (UnsupportedEncodingException ex) {
					ex.printStackTrace();
				}
			}
		}
		System.err.println("Invoking API operation " + queryId + " " + paramString);
	}

	private void run() {
		List<String> apiInstancesToTry = new LinkedList<>(getApiInstances());
		while (!apiInstancesToTry.isEmpty() && apisToCall.size() < parallelCallCount) {
			int randomIndex = (int) ((Math.random() * apiInstancesToTry.size()));
			String apiUrl = apiInstancesToTry.get(randomIndex);
			apisToCall.add(apiUrl);
			System.err.println("Trying API (" + apisToCall.size() + ") " + apiUrl);
			apiInstancesToTry.remove(randomIndex);
		}
		for (String api : apisToCall) {
			Call call = new Call(api);
			calls.add(call);
			new Thread(call).start();
		}
	}

	private synchronized void finished(Call call, HttpResponse resp, String apiUrl) {
		if (this.resp != null) { // result already in
			EntityUtils.consumeQuietly(resp.getEntity());
			return;
		}
		System.err.println("Result in from " + apiUrl + ":");
		System.err.println("- Request: " + queryId + " " + paramString);
		System.err.println("- Response size: " + resp.getEntity().getContentLength());
		this.resp = resp;

		for (Call c : calls) {
			if (c != call) c.abort();
		}
	}

	private static boolean wasSuccessful(HttpResponse resp) {
		if (resp == null || resp.getEntity() == null) return false;
		int c = resp.getStatusLine().getStatusCode();
		if (c < 200 || c >= 300) return false;
		return true;
	}

	private static boolean wasSuccessfulNonempty(HttpResponse resp) {
		if (!wasSuccessful(resp)) return false;
		if (resp.getEntity().getContentLength() < 0) return false;
		return true;
	}


	private class Call implements Runnable {

		private String apiUrl;
		private HttpGet get;

		public Call(String apiUrl) {
			this.apiUrl = apiUrl;
		}

		public void run() {
			get = new HttpGet(apiUrl + "api/" + queryId + paramString);
			get.setHeader("Accept", "text/csv");
			HttpResponse resp = null;
			try {
				resp = NanopubUtils.getHttpClient().execute(get);
				if (!wasSuccessfulNonempty(resp)) {
					throw new IOException(resp.getStatusLine().toString());
				}
				finished(this, resp, apiUrl);
			} catch (Exception ex) {
				if (resp != null) EntityUtils.consumeQuietly(resp.getEntity());
				System.err.println("Request to " + apiUrl + " was not successful: " + ex.getMessage());
			}
			calls.remove(this);
		}

		private void abort() {
			if (get == null) return;
			if (get.isAborted()) return;
			get.abort();
		}

	}

}
