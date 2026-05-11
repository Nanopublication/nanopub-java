package org.nanopub.extra.services;

import org.junit.jupiter.api.*;
import org.nanopub.utils.MockNanopubUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        ServiceLookup.clearCache();
        mockNanopubUtils = new MockNanopubUtils();
        System.setProperty(QueryCall.QUERY_INSTANCES_PROPERTY, DEFAULT_INSTANCES);
    }


    @AfterEach
    void tearDown() throws NoSuchFieldException, IllegalAccessException {
        resetCheckedApiInstances();
        mockNanopubUtils.close();
    }

    private static void resetCheckedApiInstances() throws NoSuchFieldException, IllegalAccessException {
        Field field = QueryCall.class.getDeclaredField("checkedApiInstances");
        field.setAccessible(true);
        field.set(null, null);
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

    @Test
    void getParallelCallCountDefault() {
        System.clearProperty(QueryCall.PARALLEL_CALL_COUNT_PROPERTY);
        assertEquals(2, QueryCall.getParallelCallCount());
    }

    @Test
    void getParallelCallCountFromProperty() {
        System.setProperty(QueryCall.PARALLEL_CALL_COUNT_PROPERTY, "1");
        try {
            assertEquals(1, QueryCall.getParallelCallCount());
        } finally {
            System.clearProperty(QueryCall.PARALLEL_CALL_COUNT_PROPERTY);
        }
    }

    @Test
    void getParallelCallCountIgnoresInvalidValues() {
        System.setProperty(QueryCall.PARALLEL_CALL_COUNT_PROPERTY, "0");
        try {
            assertEquals(2, QueryCall.getParallelCallCount());
        } finally {
            System.clearProperty(QueryCall.PARALLEL_CALL_COUNT_PROPERTY);
        }
        System.setProperty(QueryCall.PARALLEL_CALL_COUNT_PROPERTY, "not-a-number");
        try {
            assertEquals(2, QueryCall.getParallelCallCount());
        } finally {
            System.clearProperty(QueryCall.PARALLEL_CALL_COUNT_PROPERTY);
        }
    }

}
