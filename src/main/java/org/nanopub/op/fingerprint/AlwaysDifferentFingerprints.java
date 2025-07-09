package org.nanopub.op.fingerprint;

import java.util.Random;

import org.nanopub.Nanopub;

public class AlwaysDifferentFingerprints implements FingerprintHandler {

	private final Random random = new Random();

	public AlwaysDifferentFingerprints() {
	}

	@Override
	public String getFingerprint(Nanopub np) {
		return random.nextLong() + "";
	}

}
