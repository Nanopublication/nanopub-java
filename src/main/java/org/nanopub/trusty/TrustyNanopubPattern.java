package org.nanopub.trusty;

import java.net.MalformedURLException;
import java.net.URL;

import net.trustyuri.TrustyUriUtils;

import org.nanopub.Nanopub;
import org.nanopub.NanopubPattern;

public class TrustyNanopubPattern implements NanopubPattern {

	public TrustyNanopubPattern() {
	}

	@Override
	public String getName() {
		return "Trusty nanopublication";
	}

	@Override
	public boolean appliesTo(Nanopub nanopub) {
		return TrustyUriUtils.isPotentialTrustyUri(nanopub.getUri());
	}

	@Override
	public boolean isCorrectlyUsedBy(Nanopub nanopub) {
		return TrustyNanopubUtils.isValidTrustyNanopub(nanopub);
	}

	@Override
	public String getDescriptionFor(Nanopub nanopub) {
		if (TrustyNanopubUtils.isValidTrustyNanopub(nanopub)) {
			return "This nanopublication has a valid Trusty URI.";
		} else {
			return "The Trusty URI of this nanopublication is not valid.";
		}
	}

	@Override
	public URL getPatternInfoUrl() throws MalformedURLException {
		return new URL("http://trustyuri.net/");
	}

}
