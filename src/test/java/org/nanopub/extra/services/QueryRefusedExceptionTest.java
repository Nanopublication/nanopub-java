package org.nanopub.extra.services;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class QueryRefusedExceptionTest {

    @Test
    void carriesMessageAndRetryAfter() {
        Instant retryAfter = Instant.parse("2026-11-05T09:01:00Z");
        QueryRefusedException exception = new QueryRefusedException("refused", retryAfter);
        assertEquals("refused", exception.getMessage());
        assertEquals(retryAfter, exception.getRetryAfter());
    }

    @Test
    void isCaughtAsApiNotReachable() {
        assertInstanceOf(APINotReachableException.class, new QueryRefusedException("refused", Instant.now()));
    }

}
