package org.nanopub.extra.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FailedApiCallExceptionTest {

    @Test
    void constructorSetsCauseCorrectly() {
        Exception cause = new Exception("API failure");
        FailedApiCallException exception = new FailedApiCallException(cause);
        assertEquals(cause, exception.getCause());
    }

    @Test
    void constructorWithNullCauseDoesNotThrow() {
        FailedApiCallException exception = new FailedApiCallException(null);
        assertNull(exception.getCause());
    }

}