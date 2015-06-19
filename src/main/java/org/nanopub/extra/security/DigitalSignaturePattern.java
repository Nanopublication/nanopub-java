package org.nanopub.extra.security;

import java.net.MalformedURLException;
import java.net.URL;

import org.nanopub.Nanopub;
import org.nanopub.NanopubPattern;

public class DigitalSignaturePattern implements NanopubPattern {

	private static final long serialVersionUID = 669651544354988407L;

	@Override
	public String getName() {
		return "Digitally signed nanopublication";
	}

	@Override
	public boolean appliesTo(Nanopub nanopub) {
		return CheckSignature.hasSignature(nanopub);
	}

	@Override
	public boolean isCorrectlyUsedBy(Nanopub nanopub) {
		return CheckSignature.hasValidSignatures(nanopub);
	}

	@Override
	public String getDescriptionFor(Nanopub nanopub) {
		if (isCorrectlyUsedBy(nanopub)) {
			return "Valid digital signature";
		} else {
			return "Digital signature is not valid";
		}
	}

	@Override
	public URL getPatternInfoUrl() throws MalformedURLException {
		return new URL("https://github.com/Nanopublication/nanopub-java/blob/master/src/main/java/org/nanopub/extra/security/CheckSignature.java");
	}

}
