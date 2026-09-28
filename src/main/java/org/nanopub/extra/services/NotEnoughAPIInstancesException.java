package org.nanopub.extra.services;

/**
 * Exception thrown when there are not enough API instances available.
 */
public class NotEnoughAPIInstancesException extends Exception {

    /**
     * Constructs a new NotEnoughAPIInstancesException with the specified message.
     *
     * @param message the detail message
     */
    public NotEnoughAPIInstancesException(String message) {
        super(message);
    }

}
