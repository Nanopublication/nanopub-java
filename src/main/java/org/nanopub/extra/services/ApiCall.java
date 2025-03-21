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
 * First-generation query API call. Deprecated and replaced by second-generation query services.
 */
@Deprecated
public class ApiCall {

	public static HttpResponse run(String apiUrl, String operation, Map<String,String> params) {
		ApiCall apiCall = new ApiCall(apiUrl, operation, params);
		apiCall.run();
		while (!apiCall.calls.isEmpty() && apiCall.resp == null) {
			try {
			    Thread.sleep(50);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
		}
		return apiCall.resp;
	}

	// TODO Available services should be retrieved from the network, not hard-coded:
	public static String[] apiInstances = new String[] {
		"http://grlc.nanopubs.lod.labs.vu.nl/api/local/local/",
//		"http://grlc.np.dumontierlab.com/api/local/local/",
//		"https://openphacts.cs.man.ac.uk/nanopub/grlc/api/local/local/",
//		"https://grlc.nanopubs.knows.idlab.ugent.be/api/local/local/",
//		"http://grlc.np.scify.org/api/local/local/",
		// Some are Signed Nanopub Services (because we're only using the 'signed_...' operations here, the difference doesn't matter):
		"http://130.60.24.146:7881/api/local/local/",
		"https://grlc.services.np.trustyuri.net/api/local/local/",
		"https://grlc.nps.knowledgepixels.com/api/local/local/"
	};

	private static List<String> checkedApiInstances;

	public static List<String> getApiInstances() {
		if (checkedApiInstances != null) return checkedApiInstances;
		checkedApiInstances = new ArrayList<String>();
		for (String a : apiInstances) {
			try {
				System.err.println("Checking API instance: " + a);
				HttpResponse resp = NanopubUtils.getHttpClient().execute(new HttpGet(a));
				if (wasSuccessful(resp)) {
					System.err.println("SUCCESS: API instance is accessible: " + a);
					checkedApiInstances.add(a);
				} else {
					System.err.println("FAILURE: API instance isn't accessible: " + a);
				}
			} catch (IOException ex) {
				System.err.println("FAILURE: API instance isn't accessible: " + a);
			}
		}
		System.err.println(checkedApiInstances.size() + " accessible API instances");
		if (checkedApiInstances.size() < 2) {
			checkedApiInstances = null;
			throw new RuntimeException("Not enough healthy API instances available");
		}
		return checkedApiInstances;
	}

	private String apiUrl;
	private String operation;
	private String paramString;
	private int parallelCallCount = 2;
	private List<String> apisToCall = new ArrayList<>();
	private List<Call> calls = new ArrayList<>();

	private HttpResponse resp;

	private ApiCall(String apiUrl, String operation, Map<String,String> params) {
		this.apiUrl = apiUrl;
		this.operation = operation;
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
		System.err.println("Invoking API operation " + operation + " " + paramString);
	}

	private void run() {
		// TODO Make this more general when loading services from setting:
		if (apiUrl == null || apiUrl.equals(ApiAccess.MAIN_GRLC_API_GENERIC_URL)) {
			List<String> apiInstancesToTry = new LinkedList<>(getApiInstances());
			while (!apiInstancesToTry.isEmpty() && apisToCall.size() < parallelCallCount) {
				int randomIndex = (int) ((Math.random() * apiInstancesToTry.size()));
				String apiUrl = apiInstancesToTry.get(randomIndex);
				apisToCall.add(apiUrl);
				System.err.println("Trying API (" + apisToCall.size() + ") " + apiUrl);
				apiInstancesToTry.remove(randomIndex);
			}
		} else {
			apisToCall.add(apiUrl);
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
			HttpGet get = new HttpGet(apiUrl + operation + paramString);
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
