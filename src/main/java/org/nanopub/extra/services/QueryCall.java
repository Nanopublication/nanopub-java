package org.nanopub.extra.services;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.nanopub.NanopubUtils;
import org.nanopub.vocabulary.NPS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Second-generation query API call.
 */
public class QueryCall {

    private static final int DEFAULT_PARALLEL_CALL_COUNT = 2;

    /**
     * System property setting how many query API instances to call in parallel.
     * Must be {@code >= 1}; defaults to {@value #DEFAULT_PARALLEL_CALL_COUNT}.
     * Env var {@code NANOPUB_QUERY_PARALLEL_CALL_COUNT} also accepted.
     */
    public static final String PARALLEL_CALL_COUNT_PROPERTY = "nanopub.query.parallel-call-count";

    /**
     * Environment variable equivalent of {@link #PARALLEL_CALL_COUNT_PROPERTY}.
     */
    public static final String PARALLEL_CALL_COUNT_ENV = "NANOPUB_QUERY_PARALLEL_CALL_COUNT";

    private static int maxRetryCount = 3;
    private static final Logger logger = LoggerFactory.getLogger(QueryCall.class);

    /**
     * Returns the number of query API instances to call in parallel, resolved
     * (in order) from {@link #PARALLEL_CALL_COUNT_PROPERTY},
     * {@link #PARALLEL_CALL_COUNT_ENV}, or the default of
     * {@value #DEFAULT_PARALLEL_CALL_COUNT}. Invalid values are ignored.
     *
     * @return the parallel call count (always {@code >= 1})
     */
    public static int getParallelCallCount() {
        String value = System.getProperty(PARALLEL_CALL_COUNT_PROPERTY);
        if (value == null || value.isEmpty()) value = System.getenv(PARALLEL_CALL_COUNT_ENV);
        if (value != null && !value.trim().isEmpty()) {
            try {
                int n = Integer.parseInt(value.trim());
                if (n >= 1) return n;
                logger.warn("Ignoring {}={}: must be >= 1", PARALLEL_CALL_COUNT_PROPERTY, value);
            } catch (NumberFormatException ex) {
                logger.warn("Ignoring {}={}: not an integer", PARALLEL_CALL_COUNT_PROPERTY, value);
            }
        }
        return DEFAULT_PARALLEL_CALL_COUNT;
    }

    /**
     * HTTP response header carrying the query instance's sync state.
     * See nanopub-query's {@code StatusController}.
     */
    public static final String QUERY_STATUS_HEADER = "Nanopub-Query-Status";

    /**
     * System property setting the cool-down (in seconds) before a query instance
     * evicted for non-ready status is re-considered. Default
     * {@value #DEFAULT_EVICTION_COOLDOWN_SECONDS}. Env var
     * {@code NANOPUB_QUERY_EVICTION_COOLDOWN_SECONDS} also accepted.
     */
    public static final String EVICTION_COOLDOWN_PROPERTY = "nanopub.query.eviction-cooldown-seconds";

    /**
     * Environment variable equivalent of {@link #EVICTION_COOLDOWN_PROPERTY}.
     */
    public static final String EVICTION_COOLDOWN_ENV = "NANOPUB_QUERY_EVICTION_COOLDOWN_SECONDS";

    private static final int DEFAULT_EVICTION_COOLDOWN_SECONDS = 300;

    private static final ConcurrentMap<String, Long> evictedUntil = new ConcurrentHashMap<>();

    /**
     * Returns the eviction cool-down in milliseconds, resolved from
     * {@link #EVICTION_COOLDOWN_PROPERTY}, {@link #EVICTION_COOLDOWN_ENV},
     * or the default of {@value #DEFAULT_EVICTION_COOLDOWN_SECONDS} seconds.
     */
    public static long getEvictionCooldownMillis() {
        String value = System.getProperty(EVICTION_COOLDOWN_PROPERTY);
        if (value == null || value.isEmpty()) value = System.getenv(EVICTION_COOLDOWN_ENV);
        if (value != null && !value.trim().isEmpty()) {
            try {
                long n = Long.parseLong(value.trim());
                if (n >= 0) return n * 1000L;
                logger.warn("Ignoring {}={}: must be >= 0", EVICTION_COOLDOWN_PROPERTY, value);
            } catch (NumberFormatException ex) {
                logger.warn("Ignoring {}={}: not a number", EVICTION_COOLDOWN_PROPERTY, value);
            }
        }
        return DEFAULT_EVICTION_COOLDOWN_SECONDS * 1000L;
    }

    /**
     * Returns true if the response's {@link #QUERY_STATUS_HEADER} signals a
     * fully-synced state ({@code READY} or {@code LOADING_UPDATES}). Missing
     * header is treated as ready for backwards compatibility with older
     * query instances.
     */
    static boolean isReadyStatus(HttpResponse resp) {
        Header h = resp.getFirstHeader(QUERY_STATUS_HEADER);
        if (h == null) return true;
        String v = h.getValue();
        if (v == null || v.isEmpty()) return true;
        String upper = v.toUpperCase(Locale.ROOT);
        return upper.equals("READY") || upper.equals("LOADING_UPDATES");
    }

    private static void evict(String apiUrl, String reason) {
        long until = System.currentTimeMillis() + getEvictionCooldownMillis();
        evictedUntil.put(apiUrl, until);
        logger.warn("Evicting Nanopub Query instance {} until {} ({})", apiUrl, new Date(until), reason);
    }

    private static List<String> filterEvicted(List<String> instances) {
        long now = System.currentTimeMillis();
        List<String> result = new ArrayList<>(instances.size());
        for (String url : instances) {
            Long until = evictedUntil.get(url);
            if (until == null || until <= now) result.add(url);
        }
        return result;
    }

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
        throw new APINotReachableException("Giving up contacting API: " + queryRef.getQueryId());
    }

    /**
     * System property naming a whitespace-separated list of query API instance URLs.
     * When set, this overrides discovery via the nanopub setting (env var
     * {@code NANOPUB_QUERY_INSTANCES} also accepted).
     */
    public static final String QUERY_INSTANCES_PROPERTY = "nanopub.query.instances";

    /**
     * Environment variable equivalent of {@link #QUERY_INSTANCES_PROPERTY}.
     */
    public static final String QUERY_INSTANCES_ENV = "NANOPUB_QUERY_INSTANCES";

    private static List<String> checkedApiInstances;

    /**
     * Returns the list of available query API instances that are currently accessible.
     * <p>
     * Sources, in order of priority:
     * <ol>
     *   <li>{@code nanopub.query.instances} system property / {@code NANOPUB_QUERY_INSTANCES} env var
     *       (whitespace-separated URLs).</li>
     *   <li>The active {@link org.nanopub.extra.setting.NanopubSetting}'s service intro collection,
     *       filtered to services of type {@link NPS#NANOPUB_QUERY_1_1}.</li>
     * </ol>
     * Each candidate is liveness-checked via an HTTP GET to its root URL.
     *
     * @return a list of accessible query API instances
     */
    public static synchronized List<String> getApiInstances() throws NotEnoughAPIInstancesException {
        List<String> candidates = resolveCandidateInstances();
        if (candidates.isEmpty()) {
            throw new NotEnoughAPIInstancesException("No query API instances configured or discoverable");
        }
        if (checkedApiInstances == null) checkedApiInstances = new ArrayList<>();
        long now = System.currentTimeMillis();
        boolean anyNewAdmitted = false;
        for (String a : candidates) {
            if (checkedApiInstances.contains(a)) continue;
            Long until = evictedUntil.get(a);
            if (until != null && until > now) continue;
            try {
                logger.info("Checking API instance: {}", a);
                HttpResponse resp = NanopubUtils.getHttpClient().execute(new HttpGet(a));
                if (!wasSuccessful(resp)) {
                    EntityUtils.consumeQuietly(resp.getEntity());
                    logger.error("FAILURE: Nanopub Query instance isn't accessible: {}", a);
                    evict(a, "not accessible");
                } else if (!isReadyStatus(resp)) {
                    Header h = resp.getFirstHeader(QUERY_STATUS_HEADER);
                    String status = h == null ? "missing" : h.getValue();
                    EntityUtils.consumeQuietly(resp.getEntity());
                    logger.error("FAILURE: Nanopub Query instance not ready (status={}): {}", status, a);
                    evict(a, "status " + status);
                } else {
                    EntityUtils.consumeQuietly(resp.getEntity());
                    logger.info("SUCCESS: Nanopub Query instance is accessible: {}", a);
                    checkedApiInstances.add(a);
                    anyNewAdmitted = true;
                }
            } catch (IOException ex) {
                logger.error("FAILURE: Nanopub Query instance isn't accessible: {}", a);
                evict(a, "not accessible");
            }
        }
        if (anyNewAdmitted) {
            logger.info("{} accessible Nanopub Query instances", checkedApiInstances.size());
        }
        if (checkedApiInstances.isEmpty()) {
            throw new NotEnoughAPIInstancesException("No healthy Nanopub Query instances available");
        }
        if (anyNewAdmitted && checkedApiInstances.size() == 1) {
            logger.warn("Only one healthy Nanopub Query instance available; no failover.");
        }
        return checkedApiInstances;
    }

    private static List<String> resolveCandidateInstances() {
        String override = System.getProperty(QUERY_INSTANCES_PROPERTY);
        if (override == null || override.isEmpty()) override = System.getenv(QUERY_INSTANCES_ENV);
        if (override != null && !override.trim().isEmpty()) {
            List<String> list = new ArrayList<>();
            for (String url : override.trim().split("\\s+")) list.add(url);
            logger.info("Using {} query API instance(s) from override", list.size());
            return list;
        }
        List<String> fromSetting = ServiceLookup.getServices(NPS.NANOPUB_QUERY_1_1);
        logger.info("Discovered {} query API instance(s) from setting", fromSetting.size());
        return new ArrayList<>(fromSetting);
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
        List<String> candidates = filterEvicted(getApiInstances());
        if (candidates.isEmpty()) {
            throw new NotEnoughAPIInstancesException(
                    "All Nanopub Query instances are currently evicted (loading/resetting); try again later");
        }
        List<String> apiInstancesToTry = new LinkedList<>(candidates);
        int parallelCallCount = getParallelCallCount();
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
            get.setHeader("Accept", "text/csv, text/turtle;q=0.9, application/ld+json;q=0.8");
            HttpResponse resp = null;
            try {
                resp = NanopubUtils.getHttpClient().execute(get);
                if (!wasSuccessfulNonempty(resp)) {
                    throw new IOException(resp.getStatusLine().toString());
                }
                if (!isReadyStatus(resp)) {
                    Header h = resp.getFirstHeader(QUERY_STATUS_HEADER);
                    String status = h == null ? "missing" : h.getValue();
                    evict(apiUrl, "status " + status);
                    EntityUtils.consumeQuietly(resp.getEntity());
                } else {
                    finished(this, resp, apiUrl);
                }
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
