package org.nanopub.extra.services;

import org.junit.jupiter.api.*;
import org.nanopub.utils.MockNanopubUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QueryCallTest {

    private MockNanopubUtils mockNanopubUtils;
    private static String overridePropertyOld;

    private static final String DEFAULT_INSTANCES =
            "https://query.knowledgepixels.com/ https://query.petapico.org/ https://query.nanodash.net/";

    @BeforeAll
    static void beforeAll() {
        overridePropertyOld = System.getProperty(QueryCall.QUERY_INSTANCES_PROPERTY);
    }

    @BeforeEach
    void setUp() throws IOException, NoSuchFieldException, IllegalAccessException {
        resetCheckedApiInstances();
        clearEvictedUntil();
        ServiceLookup.clearCache();
        mockNanopubUtils = new MockNanopubUtils();
        System.setProperty(QueryCall.QUERY_INSTANCES_PROPERTY, DEFAULT_INSTANCES);
    }


    @AfterEach
    void tearDown() throws NoSuchFieldException, IllegalAccessException {
        resetCheckedApiInstances();
        clearEvictedUntil();
        mockNanopubUtils.close();
    }

    private static void resetCheckedApiInstances() throws NoSuchFieldException, IllegalAccessException {
        Field field = QueryCall.class.getDeclaredField("checkedApiInstances");
        field.setAccessible(true);
        field.set(null, null);
    }

    @SuppressWarnings("unchecked")
    private static void clearEvictedUntil() throws NoSuchFieldException, IllegalAccessException {
        Field field = QueryCall.class.getDeclaredField("evictedUntil");
        field.setAccessible(true);
        ((java.util.Map<String, Long>) field.get(null)).clear();
    }

    @AfterAll
    static void afterAll() {
        if (overridePropertyOld == null) {
            System.clearProperty(QueryCall.QUERY_INSTANCES_PROPERTY);
        } else {
            System.setProperty(QueryCall.QUERY_INSTANCES_PROPERTY, overridePropertyOld);
        }
    }

    @Test
    void getApiInstancesWithNotAccessibleInstances() {
        assertThrows(NotEnoughAPIInstancesException.class, QueryCall::getApiInstances);
    }

    @Test
    void getApiInstancesAlreadyChecked() throws NotEnoughAPIInstancesException {
        mockNanopubUtils.setHttpResponseStatusCode(200);

        QueryCall.getApiInstances();

        // Call again to check if it uses the cached instances
        assertEquals(List.of(DEFAULT_INSTANCES.split(" ")), QueryCall.getApiInstances());
    }

    @Test
    void getApiInstancesWithOnlyOneInstance() throws NotEnoughAPIInstancesException {
        mockNanopubUtils.setHttpResponseStatusCode(200);
        System.setProperty(QueryCall.QUERY_INSTANCES_PROPERTY, "https://mocked.instance1.com/");
        // Single healthy instance is now accepted (with a warning logged); only zero throws.
        assertEquals(List.of("https://mocked.instance1.com/"), QueryCall.getApiInstances());
    }

    @Test
    void getApiInstancesWithZeroHealthyInstances() {
        // Status 300 => wasSuccessful() returns false for all 3 default instances.
        mockNanopubUtils.setHttpResponseStatusCode(300);
        assertThrows(NotEnoughAPIInstancesException.class, QueryCall::getApiInstances);
    }

    @Test
    void getApiInstancesWithValidInstances() throws NotEnoughAPIInstancesException {
        mockNanopubUtils.setHttpResponseStatusCode(200);
        List<String> apiInstances = QueryCall.getApiInstances();
        assertEquals(apiInstances, List.of(DEFAULT_INSTANCES.split(" ")));
    }

    // --- Status-header gate ---

    @Test
    void getApiInstancesAdmitsReadyStatus() throws NotEnoughAPIInstancesException {
        mockNanopubUtils.setHttpResponseStatusCode(200);
        mockNanopubUtils.setNanopubQueryStatus("READY");
        assertEquals(List.of(DEFAULT_INSTANCES.split(" ")), QueryCall.getApiInstances());
    }

    @Test
    void getApiInstancesAdmitsLoadingUpdatesStatus() throws NotEnoughAPIInstancesException {
        mockNanopubUtils.setHttpResponseStatusCode(200);
        mockNanopubUtils.setNanopubQueryStatus("LOADING_UPDATES");
        assertEquals(List.of(DEFAULT_INSTANCES.split(" ")), QueryCall.getApiInstances());
    }

    @Test
    void getApiInstancesAdmitsMissingStatusHeader() throws NotEnoughAPIInstancesException {
        // Older instances may not set the header at all — must still be admitted.
        mockNanopubUtils.setHttpResponseStatusCode(200);
        mockNanopubUtils.setNanopubQueryStatus(null);
        assertEquals(List.of(DEFAULT_INSTANCES.split(" ")), QueryCall.getApiInstances());
    }

    @Test
    void getApiInstancesRejectsLoadingInitialStatus() {
        mockNanopubUtils.setHttpResponseStatusCode(200);
        mockNanopubUtils.setNanopubQueryStatus("LOADING_INITIAL");
        assertThrows(NotEnoughAPIInstancesException.class, QueryCall::getApiInstances);
    }

    @Test
    void getApiInstancesRejectsResettingStatus() {
        mockNanopubUtils.setHttpResponseStatusCode(200);
        mockNanopubUtils.setNanopubQueryStatus("RESETTING");
        assertThrows(NotEnoughAPIInstancesException.class, QueryCall::getApiInstances);
    }

    @Test
    void isReadyStatusHelper() {
        org.apache.http.HttpResponse resp = org.mockito.Mockito.mock(org.apache.http.HttpResponse.class);
        // No header => ready (backwards compat).
        org.mockito.Mockito.when(resp.getFirstHeader(QueryCall.QUERY_STATUS_HEADER)).thenReturn(null);
        assertEquals(true, QueryCall.isReadyStatus(resp));

        for (String good : List.of("READY", "ready", "Loading_Updates", "LOADING_UPDATES")) {
            org.apache.http.Header h = org.mockito.Mockito.mock(org.apache.http.Header.class);
            org.mockito.Mockito.when(h.getValue()).thenReturn(good);
            org.mockito.Mockito.when(resp.getFirstHeader(QueryCall.QUERY_STATUS_HEADER)).thenReturn(h);
            assertEquals(true, QueryCall.isReadyStatus(resp), good + " should be ready");
        }
        for (String bad : List.of("LAUNCHING", "LOADING_INITIAL", "RESETTING", "unknown")) {
            org.apache.http.Header h = org.mockito.Mockito.mock(org.apache.http.Header.class);
            org.mockito.Mockito.when(h.getValue()).thenReturn(bad);
            org.mockito.Mockito.when(resp.getFirstHeader(QueryCall.QUERY_STATUS_HEADER)).thenReturn(h);
            assertEquals(false, QueryCall.isReadyStatus(resp), bad + " should not be ready");
        }
    }

    // --- Eviction cool-down ---

    @Test
    void getEvictionCooldownMillisDefault() {
        System.clearProperty(QueryCall.EVICTION_COOLDOWN_PROPERTY);
        assertEquals(300_000L, QueryCall.getEvictionCooldownMillis());
    }

    @Test
    void getEvictionCooldownMillisFromProperty() {
        System.setProperty(QueryCall.EVICTION_COOLDOWN_PROPERTY, "60");
        try {
            assertEquals(60_000L, QueryCall.getEvictionCooldownMillis());
        } finally {
            System.clearProperty(QueryCall.EVICTION_COOLDOWN_PROPERTY);
        }
    }

    @Test
    void getApiInstancesEvictsFailedAtStartup() throws Exception {
        // Status 300 means wasSuccessful() returns false for every candidate.
        mockNanopubUtils.setHttpResponseStatusCode(300);
        assertThrows(NotEnoughAPIInstancesException.class, QueryCall::getApiInstances);
        // Every candidate should now be sitting in the eviction map.
        Field f = QueryCall.class.getDeclaredField("evictedUntil");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Long> map = (java.util.Map<String, Long>) f.get(null);
        for (String url : DEFAULT_INSTANCES.split(" ")) {
            assertEquals(true, map.containsKey(url),
                    "Failed-at-startup URL should be tracked in eviction map: " + url);
        }
    }

    @Test
    void getApiInstancesReadmitsAfterCooldownExpires() throws NotEnoughAPIInstancesException {
        // Cool-down 0 means a failed instance is immediately eligible again on
        // the next call — simulating cool-down expiry.
        System.setProperty(QueryCall.EVICTION_COOLDOWN_PROPERTY, "0");
        try {
            // First call: all instances fail liveness.
            mockNanopubUtils.setHttpResponseStatusCode(300);
            assertThrows(NotEnoughAPIInstancesException.class, QueryCall::getApiInstances);

            // Now they become healthy. With a long cool-down they would stay
            // excluded for the JVM; with cool-down 0 the next call re-checks
            // them and admits them.
            mockNanopubUtils.setHttpResponseStatusCode(200);
            assertEquals(List.of(DEFAULT_INSTANCES.split(" ")), QueryCall.getApiInstances());
        } finally {
            System.clearProperty(QueryCall.EVICTION_COOLDOWN_PROPERTY);
        }
    }

    @Test
    void getApiInstancesReadmitsAfterStatusFlipsToReady() throws NotEnoughAPIInstancesException {
        System.setProperty(QueryCall.EVICTION_COOLDOWN_PROPERTY, "0");
        try {
            // Reachable but reports LOADING_INITIAL: rejected at startup.
            mockNanopubUtils.setHttpResponseStatusCode(200);
            mockNanopubUtils.setNanopubQueryStatus("LOADING_INITIAL");
            assertThrows(NotEnoughAPIInstancesException.class, QueryCall::getApiInstances);

            // Flips to READY: next call re-checks (cool-down 0) and admits.
            mockNanopubUtils.setNanopubQueryStatus("READY");
            assertEquals(List.of(DEFAULT_INSTANCES.split(" ")), QueryCall.getApiInstances());
        } finally {
            System.clearProperty(QueryCall.EVICTION_COOLDOWN_PROPERTY);
        }
    }

    @Test
    void getEvictionCooldownMillisIgnoresInvalidValues() {
        System.setProperty(QueryCall.EVICTION_COOLDOWN_PROPERTY, "-5");
        try {
            assertEquals(300_000L, QueryCall.getEvictionCooldownMillis());
        } finally {
            System.clearProperty(QueryCall.EVICTION_COOLDOWN_PROPERTY);
        }
        System.setProperty(QueryCall.EVICTION_COOLDOWN_PROPERTY, "banana");
        try {
            assertEquals(300_000L, QueryCall.getEvictionCooldownMillis());
        } finally {
            System.clearProperty(QueryCall.EVICTION_COOLDOWN_PROPERTY);
        }
    }

    // --- Dispatching an actual call ---
    //
    // Calls are dispatched sequentially on the caller thread, so MockNanopubUtils
    // covers the dispatch too. The mock serves the same response for every
    // instance, so per-instance fallback ordering cannot be distinguished here.

    private static final QueryRef QUERY_REF =
            new QueryRef("RAapc3jbJ3GkDy0ncKx3pok_zEKqwrT6-Z5TkCP1k96II/test-query");

    @Test
    void runReturnsResponseFromHealthyInstance() throws Exception {
        mockNanopubUtils.setHttpResponseStatusCode(200);
        mockNanopubUtils.setNanopubQueryStatus("READY");
        mockNanopubUtils.setResponseBody("a,b\n1,2\n");

        org.apache.http.HttpResponse resp = QueryCall.run(QUERY_REF);
        assertNotNull(resp);
        assertNotNull(resp.getEntity());
    }

    @Test
    void runEvictsInstancesTurningNonReadyAfterAdmission() throws Exception {
        // Admit all instances as READY first...
        mockNanopubUtils.setHttpResponseStatusCode(200);
        mockNanopubUtils.setNanopubQueryStatus("READY");
        List<String> instances = QueryCall.getApiInstances();

        // ...then have them report RESETTING at query time: each attempt evicts
        // its instance, and once every instance is evicted, run() gives up.
        mockNanopubUtils.setNanopubQueryStatus("RESETTING");
        assertThrows(NotEnoughAPIInstancesException.class, () -> QueryCall.run(QUERY_REF));
        for (String url : instances) {
            assertEquals(true, getEvictedUntil().containsKey(url),
                    "Non-ready instance should be evicted: " + url);
        }
    }

    @Test
    void runGivesUpAfterRetriesWhenAllRequestsFail() throws Exception {
        // Admit all instances as healthy first...
        mockNanopubUtils.setHttpResponseStatusCode(200);
        QueryCall.getApiInstances();

        // ...then fail every query request (HTTP 500). Failed requests do not
        // evict, so run() retries and eventually gives up.
        mockNanopubUtils.setHttpResponseStatusCode(500);
        assertThrows(APINotReachableException.class, () -> QueryCall.run(QUERY_REF));
    }

    @Test
    void runRefusesToDispatchWhenEveryInstanceIsEvicted() throws Exception {
        mockNanopubUtils.setHttpResponseStatusCode(200);
        List<String> instances = QueryCall.getApiInstances();
        long farFuture = System.currentTimeMillis() + 600_000L;
        for (String instance : instances) {
            getEvictedUntil().put(instance, farFuture);
        }

        assertThrows(NotEnoughAPIInstancesException.class, () -> QueryCall.run(QUERY_REF));
    }


    @SuppressWarnings("unchecked")
    private static java.util.Map<String, Long> getEvictedUntil() throws NoSuchFieldException, IllegalAccessException {
        Field field = QueryCall.class.getDeclaredField("evictedUntil");
        field.setAccessible(true);
        return (java.util.Map<String, Long>) field.get(null);
    }

}
