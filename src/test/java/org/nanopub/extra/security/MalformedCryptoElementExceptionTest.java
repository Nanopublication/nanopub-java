package org.nanopub.extra.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MalformedCryptoElementExceptionTest {

    @Test
    void exceptionMessageIsCorrectlyRetrieved() {
        MalformedCryptoElementException exception = new MalformedCryptoElementException("Malformed element");
        assertEquals("Malformed element", exception.getMessage());
    }

    @Test
    void exceptionCauseIsCorrectlyRetrieved() {
        Throwable cause = new RuntimeException("Cause of the issue");
        MalformedCryptoElementException exception = new MalformedCryptoElementException(cause);
        assertEquals(cause, exception.getCause());
    }

}