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
		HttpResponse resp = null;
		int retryCount = 0;
		while (resp == null && retryCount < maxRetryCount) {
			QueryCall apiCall = new QueryCall(queryId, params);
			apiCall.run();
			while (!apiCall.calls.isEmpty() && apiCall.resp == null) {
				try {
				    Thread.sleep(50);
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
			}
			resp = apiCall.resp;
			retryCount = retryCount + 1;
		}
		return resp;
	}

	// TODO Available services should be retrieved from a setting, not hard-coded:
	public static String[] queryApiInstances = new String[] {
		"https://query.knowledgepixels.com/",
		"https://query.np.kpxl.org/",
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
			new Thread(call).run();
		}
	}

	private void finished(HttpResponse resp, String apiUrl) {
		if (this.resp != null) return; // result already in
		System.err.println("Result in from " + apiUrl);
		this.resp = resp;
	}

	private static boolean wasSuccessful(HttpResponse resp) {
		int c = resp.getStatusLine().getStatusCode();
		return c >= 200 && c < 300;
	}


	private class Call implements Runnable {

		private String apiUrl;

		public Call(String apiUrl) {
			this.apiUrl = apiUrl;
		}

		public void run() {
			HttpGet get = new HttpGet(apiUrl + "api/" + queryId + paramString);
			get.setHeader("Accept", "text/csv");
			try {
				HttpResponse resp = NanopubUtils.getHttpClient().execute(get);
				if (!wasSuccessful(resp)) {
					EntityUtils.consumeQuietly(resp.getEntity());
					throw new IOException(resp.getStatusLine().toString());
				}
				finished(resp, apiUrl);
			} catch (Exception ex) {
//				ex.printStackTrace();
				System.err.println("Request to " + apiUrl + " was not successful");
			}
			calls.remove(this);
		}

	}

}
