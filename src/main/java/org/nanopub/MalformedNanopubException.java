package org.nanopub;

/**
 * Exception thrown when a nanopublication is malformed.
 *
 * @author Tobias Kuhn
 */
public class MalformedNanopubException extends Exception {

    /**
     * Constructs a new MalformedNanopubException with the specified detail message.
     *
     * @param message the detail message
     */
    public MalformedNanopubException(String message) {
        super(message);
    }

}
