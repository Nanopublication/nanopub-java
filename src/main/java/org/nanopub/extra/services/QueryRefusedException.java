package org.nanopub.extra.services;

import java.time.Instant;

/**
 * Exception thrown when query instances refused to start a query and said when to try again
 * (a {@code 503} or {@code 429} response with a {@code Retry-After} header), and no other
 * instance could take it.
 * <p>
 * Nanopub Query refuses a request this way while an identical one may still be running
 * (knowledgepixels/nanopub-query#247). Callers should not try the query again before
 * {@link #getRetryAfter()}. Being an {@link APINotReachableException}, it is caught by existing
 * handlers for that exception.
 */
public class QueryRefusedException extends APINotReachableException {

    private final Instant retryAfter;

    /**
     * Constructs a new QueryRefusedException.
     *
     * @param message    the detail message
     * @param retryAfter the earliest time at which one of the instances said it would accept the query
     */
    public QueryRefusedException(String message, Instant retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    /**
     * Returns the earliest time at which one of the instances said it would accept the query.
     *
     * @return the earliest retry time
     */
    public Instant getRetryAfter() {
        return retryAfter;
    }

}
