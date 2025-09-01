package org.nanopub.extra.services;

import org.junit.jupiter.api.*;
import org.nanopub.utils.MockNanopubUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QueryCallTest {

    private MockNanopubUtils mockNanopubUtils;
    private static String[] queryApiInstancesOld;

    @BeforeAll
    static void beforeAll() {
        // Store the original queryApiInstances for later restoration
        queryApiInstancesOld = QueryCall.queryApiInstances;
    }

    @BeforeEach
    void setUp() throws IOException {
        mockNanopubUtils = new MockNanopubUtils();
    }


    @AfterEach
    void tearDown() throws NoSuchFieldException, IllegalAccessException {
        // Reset the static field 'checkedApiInstances' to null after each test using reflection
        Field field = QueryCall.class.getDeclaredField("checkedApiInstances");
        field.setAccessible(true);
        field.set(null, null);

        mockNanopubUtils.close();
    }

    @AfterAll
    static void afterAll() {
        // Restore the original queryApiInstances
        QueryCall.queryApiInstances = queryApiInstancesOld;
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
        assertEquals(Arrays.stream(QueryCall.queryApiInstances).toList(), QueryCall.getApiInstances());
    }

    @Test
    void getApiInstancesWithOnlyOneInstance() {
        mockNanopubUtils.setHttpResponseStatusCode(200);
        QueryCall.queryApiInstances = new String[]{"https://mocked.instance1.com/"};
        assertThrows(NotEnoughAPIInstancesException.class, QueryCall::getApiInstances);
    }

    @Test
    void getApiInstancesWithValidInstances() throws NotEnoughAPIInstancesException {
        mockNanopubUtils.setHttpResponseStatusCode(200);
        List<String> apiInstances = QueryCall.getApiInstances();
        assertEquals(apiInstances, List.of(QueryCall.queryApiInstances));
    }

}