package org.nanopub.jelly;

import org.nanopub.Nanopub;

/**
 * Simple wrapper around the Nanopub class that can also hold an exception.
 * <p>
 * This is basically the same as Try<Nanopub> in Scala.
 */
public class MaybeNanopub {
    private final boolean success;
    private final Nanopub nanopub;
    private final Exception exception;

    public MaybeNanopub(Exception ex) {
        success = false;
        nanopub = null;
        exception = ex;
    }

    public MaybeNanopub(Nanopub np) {
        success = true;
        nanopub = np;
        exception = null;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public Nanopub getNanopub() {
        return nanopub;
    }

    public Exception getException() {
        return exception;
    }
}
