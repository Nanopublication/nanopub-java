package org.nanopub.fdo;

/**
 * Exception thrown when a requested FDO is not found.
 */
public class FdoNotFoundException extends Exception {

    /**
     * Constructs a new FdoNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public FdoNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new FdoNotFoundException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public FdoNotFoundException(Exception cause) {
        super(cause);
    }

}
