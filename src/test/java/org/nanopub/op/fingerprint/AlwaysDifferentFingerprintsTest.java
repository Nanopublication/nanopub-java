package org.nanopub.op.fingerprint;

import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.utils.TestUtils;

import static org.junit.jupiter.api.Assertions.*;

class AlwaysDifferentFingerprintsTest {

    @Test
    void getFingerprintGeneratesNonEmptyString() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        AlwaysDifferentFingerprints handler = new AlwaysDifferentFingerprints();
        Nanopub nanopub = TestUtils.createNanopub(TestUtils.NANOPUB_URI);
        String fingerprint = handler.getFingerprint(nanopub);
        assertNotNull(fingerprint);
        assertFalse(fingerprint.isEmpty());
    }

    @Test
    void getFingerprintGeneratesUniqueFingerprintsForDifferentCalls() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        AlwaysDifferentFingerprints handler = new AlwaysDifferentFingerprints();
        Nanopub nanopub = TestUtils.createNanopub(TestUtils.NANOPUB_URI);
        String fingerprint1 = handler.getFingerprint(nanopub);
        String fingerprint2 = handler.getFingerprint(nanopub);
        assertNotEquals(fingerprint1, fingerprint2);
    }

    @Test
    void getFingerprintHandlesNullNanopub() {
        AlwaysDifferentFingerprints handler = new AlwaysDifferentFingerprints();
        String fingerprint = handler.getFingerprint(null);
        assertNotNull(fingerprint);
        assertFalse(fingerprint.isEmpty());
    }

}