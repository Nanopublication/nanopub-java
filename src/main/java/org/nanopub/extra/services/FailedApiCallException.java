package org.nanopub.extra.services;

/**
 * Exception thrown when an API call fails.
 */
public class FailedApiCallException extends Exception {

    /**
     * Constructs a new FailedApiCallException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public FailedApiCallException(Exception cause) {
        super(cause);
    }

}
