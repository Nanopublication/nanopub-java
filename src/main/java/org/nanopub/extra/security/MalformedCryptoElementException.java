package org.nanopub.extra.security;

/**
 * Exception thrown when a cryptographic element (like a signature or key) is malformed.
 */
public class MalformedCryptoElementException extends Exception {

    /**
     * Constructs a new MalformedCryptoElementException with the specified detail message.
     *
     * @param message the detail message, which is saved for later retrieval by the {@link #getMessage()} method.
     */
    public MalformedCryptoElementException(String message) {
        super(message);
    }

    /**
     * Constructs a new MalformedCryptoElementException with the specified cause.
     *
     * @param cause the cause of the exception, which is saved for later retrieval by the {@link #getCause()} method.
     */
    public MalformedCryptoElementException(Throwable cause) {
        super(cause);
    }

}
