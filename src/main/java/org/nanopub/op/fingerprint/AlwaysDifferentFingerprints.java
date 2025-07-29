package org.nanopub.op.fingerprint;

import org.nanopub.Nanopub;

import java.util.Random;

/**
 * A FingerprintHandler that always returns a different fingerprint.
 */
public class AlwaysDifferentFingerprints implements FingerprintHandler {

    private final Random random = new Random();

    /**
     * Default constructor.
     */
    public AlwaysDifferentFingerprints() {
    }

    /**
     * {@inheritDoc}
     * <p>
     * Generates a random fingerprint for the given Nanopub.
     */
    @Override
    public String getFingerprint(Nanopub np) {
        return String.valueOf(random.nextLong());
    }

}
