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

    /**
     * Create a MaybeNanopub.
     *
     * @param ex Exception that caused the failure
     */
    public MaybeNanopub(Exception ex) {
        success = false;
        nanopub = null;
        exception = ex;
        counter = -1;
    }

    /**
     * Create a MaybeNanopub with a Nanopub.
     *
     * @param np Nanopub that was successfully created
     */
    public MaybeNanopub(Nanopub np) {
        success = true;
        nanopub = np;
        exception = null;
        counter = -1;
    }

    /**
     * Create a MaybeNanopub with a Nanopub and a counter.
     *
     * @param np      Nanopub that was successfully created
     * @param counter Counter value, may be negative if not present
     */
    public MaybeNanopub(Nanopub np, long counter) {
        success = true;
        nanopub = np;
        exception = null;
        this.counter = counter;
    }

    /**
     * Check if the MaybeNanopub is successful.
     *
     * @return true if the MaybeNanopub contains a Nanopub, false if it contains an exception.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Check if the MaybeNanopub is a failure.
     *
     * @return true if the MaybeNanopub contains an exception, false if it contains a Nanopub.
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * Get the Nanopub if the MaybeNanopub is successful.
     *
     * @return Nanopub object if successful, null if it contains an exception.
     */
    public Nanopub getNanopub() {
        return nanopub;
    }

    /**
     * Counter may be attached to Jelly responses from the Registry.
     * It can be used to track the progress of a stream of Nanopubs.
     * Will return a negative value if the counter is not present.
     *
     * @return counter
     */
    public long getCounter() {
        return counter;
    }

    /**
     * Get the exception if the MaybeNanopub is a failure.
     *
     * @return Exception object if it contains an exception, null if it contains a Nanopub.
     */
    public Exception getException() {
        return exception;
    }

}
