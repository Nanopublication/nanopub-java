package org.nanopub;

import java.util.ArrayList;
import java.util.List;

import org.nanopub.trusty.TrustyNanopubPattern;

public class NanopubPatterns {

	private static List<NanopubPattern> patterns = new ArrayList<>();

	static {
		addPattern(new TrustyNanopubPattern());
	}

	private NanopubPatterns() {}  // no instances allowed

	public static void addPattern(NanopubPattern pattern) {
		patterns.add(pattern);
	}

	public static List<NanopubPattern> getPatterns() {
		return patterns;
	}

}
