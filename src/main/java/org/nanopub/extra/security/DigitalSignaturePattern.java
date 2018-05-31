package org.nanopub.extra.security;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;

import org.nanopub.Nanopub;
import org.nanopub.NanopubPattern;

// TODO Update to new signature scheme

public class DigitalSignaturePattern implements NanopubPattern {

	private static final long serialVersionUID = 669651544354988407L;

	@Override
	public String getName() {
		return "Digitally signed nanopublication";
	}

	@Override
	public boolean appliesTo(Nanopub nanopub) {
		try {
			return LegacySignatureUtils.getSignatureElement(nanopub) != null;
		} catch (MalformedSignatureException ex) {
			return true;
		}
	}

	@Override
	public boolean isCorrectlyUsedBy(Nanopub nanopub) {
		NanopubSignatureElement se;
		try {
			se = LegacySignatureUtils.getSignatureElement(nanopub);
		} catch (MalformedSignatureException ex) {
			return false;
		}
		if (se == null) {
			return false;
		} else {
			try {
				return LegacySignatureUtils.hasValidSignature(se);
			} catch (GeneralSecurityException ex) {
				return false;
			}
		}
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
