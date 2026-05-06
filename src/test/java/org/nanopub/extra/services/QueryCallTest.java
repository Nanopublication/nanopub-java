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
    void getApiInstancesWithOnlyOneInstance() {
        mockNanopubUtils.setHttpResponseStatusCode(200);
        System.setProperty(QueryCall.QUERY_INSTANCES_PROPERTY, "https://mocked.instance1.com/");
        assertThrows(NotEnoughAPIInstancesException.class, QueryCall::getApiInstances);
    }

    @Test
    void getApiInstancesWithValidInstances() throws NotEnoughAPIInstancesException {
        mockNanopubUtils.setHttpResponseStatusCode(200);
        List<String> apiInstances = QueryCall.getApiInstances();
        assertEquals(apiInstances, List.of(DEFAULT_INSTANCES.split(" ")));
    }

}
