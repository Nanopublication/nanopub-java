package org.nanopub.extra.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class APINotReachableExceptionTest {

    @Test
    void constructorSetsMessageCorrectly() {
        String message = "API not reachable";
        APINotReachableException exception = new APINotReachableException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void constructorWithNullMessageDoesNotThrow() {
        APINotReachableException exception = new APINotReachableException(null);
        assertNull(exception.getMessage());
    }

}