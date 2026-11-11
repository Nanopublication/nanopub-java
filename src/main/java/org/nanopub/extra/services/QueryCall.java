package org.nanopub.extra.services;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.util.EntityUtils;
import org.nanopub.NanopubUtils;
import org.nanopub.vocabulary.NPS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Second-generation query API call.
 */
public class QueryCall {

    /**
     * How many times an instance that cannot be reached is tried during one call.
     */
    private static final int MAX_ATTEMPTS = 3;

    /**
     * HTTP status 429 (Too Many Requests), which {@link HttpStatus} lacks.
     */
    private static final int SC_TOO_MANY_REQUESTS = 429;

    private static final Logger logger = LoggerFactory.getLogger(QueryCall.class);

    private QueryCall() {
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
        if (value == null || value.isEmpty()) {
            value = System.getenv(EVICTION_COOLDOWN_ENV);
        }
        if (value != null && !value.trim().isEmpty()) {
            try {
                long n = Long.parseLong(value.trim());
                if (n >= 0) {
                    return n * 1000L;
                }
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
        if (h == null) {
            return true;
        }
        String v = h.getValue();
        if (v == null || v.isEmpty()) {
            return true;
        }
        String upper = v.toUpperCase(Locale.ROOT);
        return upper.equals("READY") || upper.equals("LOADING_UPDATES");
    }

    private static void evict(String apiUrl, String reason) {
        long until = System.currentTimeMillis() + getEvictionCooldownMillis();
        evictedUntil.put(apiUrl, until);
        logger.warn("Evicting Nanopub Query instance {} for {}s (reason: {}); re-eligible at {}", apiUrl, getEvictionCooldownMillis() / 1000, reason, new Date(until));
    }

    private static List<String> filterEvicted(List<String> instances) {
        long now = System.currentTimeMillis();
        List<String> result = new ArrayList<>(instances.size());
        for (String url : instances) {
            Long until = evictedUntil.get(url);
            if (until == null || until <= now) {
                result.add(url);
            }
        }
        return result;
    }

    /**
     * Run a query call with the given query ID and parameters.
     * <p>
     * The available query API instances are tried sequentially in random order,
     * returning the first successful response. Instances reporting a non-ready
     * status are evicted for a cool-down period (see
     * {@link #EVICTION_COOLDOWN_PROPERTY}).
     * <p>
     * An instance is health-checked the first time it is about to be asked, not before:
     * checking all of them up front costs a connection to each, which a call that the
     * first instance answers never needs (issue #208). An instance that fails its check is
     * evicted and the next one is tried.
     * <p>
     * Only instances that could not be reached at all (connection refused, connect
     * timeout, unknown host) are tried again, up to {@value #MAX_ATTEMPTS} times in
     * all. An instance that answered with an error status, or timed out while the
     * query was running, is asked once: it would most likely fail again, and the
     * server may still be evaluating the query after the client has given up.
     * <p>
     * An instance that refuses the query with {@code 503} or {@code 429} and a
     * {@code Retry-After} header is not asked again either. If some instances refused
     * and the others could not be reached or were evicted, a {@link QueryRefusedException}
     * gives the earliest time one of them will accept the query again.
     *
     * @param queryRef the reference to the query to run
     * @return the HTTP response from the query API
     * @throws QueryRefusedException          if the query was refused and no other instance could take it
     * @throws APINotReachableException       if no instance returned a response
     * @throws NotEnoughAPIInstancesException if there are not enough API instances available
     */
    public static HttpResponse run(QueryRef queryRef) throws APINotReachableException, NotEnoughAPIInstancesException {
        logger.debug("Preparing query call: {}", queryRef);
        List<String> configured = resolveCandidateInstances();
        if (configured.isEmpty()) {
            throw new NotEnoughAPIInstancesException("No query API instances configured or discoverable");
        }
        List<String> candidates = filterEvicted(configured);
        if (candidates.isEmpty()) {
            logger.warn("All {} Nanopub Query instance(s) currently evicted; cannot dispatch call for {}", configured.size(), queryRef.getQueryId());
            throw new NotEnoughAPIInstancesException(
                    "All Nanopub Query instances are currently evicted (loading/resetting); try again later");
        }
        Collections.shuffle(candidates);
        List<String> asked = new ArrayList<>(candidates);
        Instant retryAfter = null;
        boolean failed = false;
        boolean anyAdmitted = false;
        for (int round = 1; round <= MAX_ATTEMPTS && !candidates.isEmpty(); round++) {
            List<String> unreachable = new ArrayList<>();
            for (String apiUrl : candidates) {
                if (!admit(apiUrl)) {
                    continue;
                }
                anyAdmitted = true;
                Attempt a = tryInstance(apiUrl, queryRef);
                switch (a.outcome()) {
                    case ANSWERED -> {
                        return a.response();
                    }
                    case REFUSED -> {
                        if (retryAfter == null || a.retryAfter().isBefore(retryAfter)) {
                            retryAfter = a.retryAfter();
                        }
                    }
                    case UNREACHABLE -> unreachable.add(apiUrl);
                    case FAILED -> failed = true;
                    case EVICTED -> {
                    }
                }
            }
            candidates = filterEvicted(unreachable);
        }
        if (!anyAdmitted) {
            throw new NotEnoughAPIInstancesException("No healthy Nanopub Query instances available");
        }
        if (retryAfter != null && !failed) {
            throw new QueryRefusedException("Query refused by Nanopub Query instance(s): " + queryRef.getQueryId()
                                            + "; try again after " + retryAfter, retryAfter);
        }
        if (filterEvicted(asked).isEmpty()) {
            throw new NotEnoughAPIInstancesException(
                    "All Nanopub Query instances are currently evicted (loading/resetting); try again later");
        }
        throw new APINotReachableException("Giving up contacting API: " + queryRef.getQueryId());
    }

    /**
     * How a request to one instance ended.
     */
    private enum Outcome {
        /** A successful response, to be returned. */
        ANSWERED,
        /** Refused with a {@code Retry-After}; not asked again in this call. */
        REFUSED,
        /** Not reached at all; asked again in the next round. */
        UNREACHABLE,
        /** Answered with an error, or timed out while the query ran; not asked again in this call. */
        FAILED,
        /** Reported a non-ready status and was evicted. */
        EVICTED
    }

    private record Attempt(Outcome outcome, HttpResponse response, Instant retryAfter) {

        static Attempt of(Outcome outcome) {
            return new Attempt(outcome, null, null);
        }

    }

    private static Attempt tryInstance(String apiUrl, QueryRef queryRef) {
        HttpGet get = new HttpGet(apiUrl + "api/" + queryRef.getAsUrlString());
        get.setHeader("Accept", "text/csv, text/turtle;q=0.9, application/ld+json;q=0.8");
        HttpResponse resp = null;
        try {
            resp = NanopubUtils.getHttpClient().execute(get);
            Instant retryAfter = getRefusalRetryAfter(resp, Instant.now());
            if (retryAfter != null) {
                EntityUtils.consumeQuietly(resp.getEntity());
                logger.warn("Request to {} refused for query {} — {}; try again after {}", apiUrl, queryRef, resp.getStatusLine(), retryAfter);
                return new Attempt(Outcome.REFUSED, null, retryAfter);
            }
            if (!wasSuccessfulNonempty(resp)) {
                throw new IOException(resp.getStatusLine().toString());
            }
            if (!isReadyStatus(resp)) {
                Header h = resp.getFirstHeader(QUERY_STATUS_HEADER);
                String status = h == null ? "missing" : h.getValue();
                evict(apiUrl, "status " + status);
                EntityUtils.consumeQuietly(resp.getEntity());
                return Attempt.of(Outcome.EVICTED);
            }
            long len = resp.getEntity().getContentLength();
            String sizeStr = len >= 0 ? len + " bytes" : "unknown (chunked/no Content-Length)";
            logger.debug("Response received from {} for query {} ({})", apiUrl, queryRef, sizeStr);
            return new Attempt(Outcome.ANSWERED, resp, null);
        } catch (Exception ex) {
            if (resp != null) {
                EntityUtils.consumeQuietly(resp.getEntity());
            }
            logger.warn("Request to {} failed for query {} — {}: {}", apiUrl, queryRef, ex.getClass().getSimpleName(), ex.getMessage());
            logger.debug("Request to {} failed for query {}", apiUrl, queryRef, ex);
            return Attempt.of(isUnreachable(ex) ? Outcome.UNREACHABLE : Outcome.FAILED);
        }
    }

    /**
     * Returns true if a request failed without reaching the instance, so that asking
     * again cannot add to its load: the connection was refused or timed out (including a
     * timeout waiting for a pooled connection), or the host could not be resolved or routed to.
     * A read timeout ({@link java.net.SocketTimeoutException}) does not count: the query
     * was running.
     */
    static boolean isUnreachable(Exception ex) {
        return ex instanceof ConnectException
               || ex instanceof ConnectTimeoutException
               || ex instanceof UnknownHostException
               || ex instanceof NoRouteToHostException;
    }

    /**
     * Returns when to try again if the response refuses the request: status {@code 503}
     * or {@code 429} with a valid {@code Retry-After} header. Returns null otherwise.
     */
    static Instant getRefusalRetryAfter(HttpResponse resp, Instant now) {
        int code = resp.getStatusLine().getStatusCode();
        if (code != HttpStatus.SC_SERVICE_UNAVAILABLE && code != SC_TOO_MANY_REQUESTS) {
            return null;
        }
        Header h = resp.getFirstHeader(HttpHeaders.RETRY_AFTER);
        return h == null ? null : parseRetryAfter(h.getValue(), now);
    }

    /**
     * Parses a {@code Retry-After} value, given as a number of seconds or as an HTTP date.
     * Returns null if it is neither.
     */
    static Instant parseRetryAfter(String value, Instant now) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.chars().allMatch(c -> c >= '0' && c <= '9')) {
            try {
                return now.plusSeconds(Long.parseLong(v));
            } catch (ArithmeticException | DateTimeException | NumberFormatException ex) {
                return null;
            }
        }
        Date date = DateUtils.parseDate(v);
        return date == null ? null : date.toInstant();
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
        if (checkedApiInstances == null) {
            checkedApiInstances = new ArrayList<>();
        }
        boolean anyNewAdmitted = false;
        for (String a : candidates) {
            if (checkedApiInstances.contains(a)) {
                continue;
            }
            if (admit(a)) {
                anyNewAdmitted = true;
            }
        }
        if (anyNewAdmitted) {
            logger.debug("{} accessible Nanopub Query instance(s) in pool", checkedApiInstances.size());
        }
        if (checkedApiInstances.isEmpty()) {
            throw new NotEnoughAPIInstancesException("No healthy Nanopub Query instances available");
        }
        if (anyNewAdmitted && checkedApiInstances.size() == 1) {
            logger.warn("Only one healthy Nanopub Query instance available; no failover.");
        }
        return checkedApiInstances;
    }

    /**
     * Returns true if the instance may be asked: it passed its health check earlier, or passes it
     * now. An instance that is evicted is not checked, and one that fails its check is evicted.
     * The check is an HTTP GET to the instance's root URL.
     */
    private static synchronized boolean admit(String a) {
        if (checkedApiInstances == null) {
            checkedApiInstances = new ArrayList<>();
        }
        if (checkedApiInstances.contains(a)) {
            return true;
        }
        Long until = evictedUntil.get(a);
        if (until != null && until > System.currentTimeMillis()) {
            return false;
        }
        try {
            logger.debug("Checking API instance: {}", a);
            HttpResponse resp = NanopubUtils.getHttpClient().execute(new HttpGet(a));
            if (!wasSuccessful(resp)) {
                EntityUtils.consumeQuietly(resp.getEntity());
                logger.warn("Nanopub Query instance not accessible (HTTP {}): {}", resp.getStatusLine().getStatusCode(), a);
                evict(a, "not accessible");
                return false;
            }
            if (!isReadyStatus(resp)) {
                Header h = resp.getFirstHeader(QUERY_STATUS_HEADER);
                String status = h == null ? "missing" : h.getValue();
                EntityUtils.consumeQuietly(resp.getEntity());
                logger.warn("Nanopub Query instance not ready (status={}): {}; evicting", status, a);
                evict(a, "status " + status);
                return false;
            }
            EntityUtils.consumeQuietly(resp.getEntity());
            logger.debug("Nanopub Query instance admitted: {}", a);
            checkedApiInstances.add(a);
            return true;
        } catch (IOException ex) {
            logger.warn("Nanopub Query instance not accessible ({}): {}", ex.getMessage(), a);
            logger.debug("Health check request to {} failed", a, ex);
            evict(a, "not accessible");
            return false;
        }
    }

    private static List<String> resolveCandidateInstances() {
        String override = System.getProperty(QUERY_INSTANCES_PROPERTY);
        if (override == null || override.isEmpty()) {
            override = System.getenv(QUERY_INSTANCES_ENV);
        }
        if (override != null && !override.trim().isEmpty()) {
            List<String> list = new ArrayList<>();
            for (String url : override.trim().split("\\s+")) list.add(url);
            logger.debug("Using {} query API instance(s) from override", list.size());
            return list;
        }
        List<String> fromSetting = ServiceLookup.getServices(NPS.NANOPUB_QUERY_1_1);
        logger.debug("Discovered {} query API instance(s) from setting", fromSetting.size());
        return new ArrayList<>(fromSetting);
    }

    private static boolean wasSuccessful(HttpResponse resp) {
        if (resp == null || resp.getEntity() == null) {
            return false;
        }
        int c = resp.getStatusLine().getStatusCode();
        return c >= 200 && c < 300;
    }

    private static boolean wasSuccessfulNonempty(HttpResponse resp) {
        if (!wasSuccessful(resp)) {
            return false;
        }
        // TODO Make sure we always return proper error codes, and then this shouldn't be necessary:
        if (resp.getHeaders("Content-Length").length > 0 && resp.getEntity().getContentLength() < 0) {
            return false;
        }
        return true;
    }

}
