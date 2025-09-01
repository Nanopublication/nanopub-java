package org.nanopub.extra.services;

/**
 * Exception thrown when the API is not reachable.
 */
public class APINotReachableException extends Exception {

    /**
     * Constructs a new APINotReachableException with the specified message.
     *
     * @param message the detail message
     */
    public APINotReachableException(String message) {
        super(message);
    }

}
