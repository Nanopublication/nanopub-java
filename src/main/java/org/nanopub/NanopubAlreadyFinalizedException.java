package org.nanopub;

/**
 * Exception thrown when trying to finalize a nanopublication that is already finalized.
 *
 */
public class NanopubAlreadyFinalizedException extends Exception {

    /**
     * Constructs a new NanopubAlreadyFinalizedException with the specified detail message.
     *
     * @param message the detail message
     */
    public NanopubAlreadyFinalizedException(String message) {
        super(message);
    }

    /**
     * Constructs a new NanopubAlreadyFinalizedException with a default message.
     */
    public NanopubAlreadyFinalizedException() {
        super("The nanopublication is already finalized.");
    }

}
