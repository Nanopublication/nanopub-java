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
    private final long counter;

    public MaybeNanopub(Exception ex) {
        success = false;
        nanopub = null;
        exception = ex;
        counter = -1;
    }

    public MaybeNanopub(Nanopub np) {
        success = true;
        nanopub = np;
        exception = null;
        counter = -1;
    }

    public MaybeNanopub(Nanopub np, long counter) {
        success = true;
        nanopub = np;
        exception = null;
        this.counter = counter;
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

    /**
     * Counter may be attached to Jelly responses from the Registry.
     * It can be used to track the progress of a stream of Nanopubs.
     * Will return a negative value if the counter is not present.
     * @return counter
     */
    public long getCounter() {
        return counter;
    }

    public Exception getException() {
        return exception;
    }
}
