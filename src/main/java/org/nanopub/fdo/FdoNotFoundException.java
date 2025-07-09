package org.nanopub.fdo;

public class FdoNotFoundException extends Exception {

    public FdoNotFoundException(String message) {
        super(message);
    }

    public FdoNotFoundException(Exception cause) {
        super(cause);
    }

}
