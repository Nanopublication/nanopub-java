package org.nanopub.extra.services;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.nanopub.NanopubUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Second-generation query API call.
 */
public class QueryCall {

    private static int parallelCallCount = 2;
    private static int maxRetryCount = 3;
    private static final Logger logger = LoggerFactory.getLogger(QueryCall.class);

    /**
     * Run a query call with the given query ID and parameters.
     *
     * @param queryRef the reference to the query to run
     * @return the HTTP response from the query API
     * @throws APINotReachableException       if the API is not reachable after retries
     * @throws NotEnoughAPIInstancesException if there are not enough API instances available
     */
    public static HttpResponse run(QueryRef queryRef) throws APINotReachableException, NotEnoughAPIInstancesException {
        int retryCount = 0;
        while (retryCount < maxRetryCount) {
            QueryCall apiCall = new QueryCall(queryRef);
            apiCall.run();
            while (!apiCall.calls.isEmpty() && apiCall.resp == null) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            if (apiCall.resp != null) {
                return apiCall.resp;
            }
            retryCount = retryCount + 1;
        }
        throw new APINotReachableException("Giving up contacting API: " + queryRef.getName());
    }

    /**
     * List of available query API instances.
     */
    // TODO Available services should be retrieved from a setting, not hard-coded:
    public static String[] queryApiInstances = new String[]{
            "https://query.knowledgepixels.com/",
            "https://query.petapico.org/",
            "https://query.nanodash.net/"
    };

    private static List<String> checkedApiInstances;

    /**
     * Returns the list of available query API instances that are currently accessible.
     *
     * @return a list of accessible query API instance
     */
    public static List<String> getApiInstances() throws NotEnoughAPIInstancesException {
        if (checkedApiInstances != null) return checkedApiInstances;
        checkedApiInstances = new ArrayList<>();
        for (String a : queryApiInstances) {
            try {
                logger.info("Checking API instance: {}", a);
                HttpResponse resp = NanopubUtils.getHttpClient().execute(new HttpGet(a));
                if (wasSuccessful(resp)) {
                    logger.info("SUCCESS: Nanopub Query instance is accessible: {}", a);
                    checkedApiInstances.add(a);
                } else {
                    logger.error("FAILURE: Nanopub Query instance isn't accessible: {}", a);
                }
            } catch (IOException ex) {
                logger.error("FAILURE: Nanopub Query instance isn't accessible: {}", a);
            }
        }
        logger.info("{} accessible Nanopub Query instances", checkedApiInstances.size());
        if (checkedApiInstances.size() < 2) {
            checkedApiInstances = null;
            throw new NotEnoughAPIInstancesException("Not enough healthy Nanopub Query instances available");
        }
        return checkedApiInstances;
    }

    private QueryRef queryRef;
    private List<String> apisToCall = new ArrayList<>();
    private List<Call> calls = new ArrayList<>();

    private HttpResponse resp;

    private QueryCall(QueryRef queryRef) {
        this.queryRef = queryRef;
        logger.info("Invoking API operation {}", queryRef);
    }

    private void run() throws NotEnoughAPIInstancesException {
        List<String> apiInstancesToTry = new LinkedList<>(getApiInstances());
        while (!apiInstancesToTry.isEmpty() && apisToCall.size() < parallelCallCount) {
            int randomIndex = (int) ((Math.random() * apiInstancesToTry.size()));
            String apiUrl = apiInstancesToTry.get(randomIndex);
            apisToCall.add(apiUrl);
            logger.info("Trying API ({}) {}", apisToCall.size(), apiUrl);
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
        logger.info("Result in from {}:", apiUrl);
        logger.info("- Request: {}", queryRef);
        logger.info("- Response size: {}", resp.getEntity().getContentLength());
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
        // TODO Make sure we always return proper error codes, and then this shouldn't be necessary:
        if (resp.getHeaders("Content-Length").length > 0 && resp.getEntity().getContentLength() < 0) return false;
        return true;
    }


    private class Call implements Runnable {

        private String apiUrl;
        private HttpGet get;

        public Call(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public void run() {
            get = new HttpGet(apiUrl + "api/" + queryRef.getAsUrlString());
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
                logger.error("Request to {} was not successful: {}", apiUrl, ex.getMessage());
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
