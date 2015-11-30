package org.nanopub.extra.server;

import net.trustyuri.TrustyUriUtils;

// TODO Make pattern processing more efficient
public class NanopubSurfacePattern {

	private final String[] uriPattern;
	private final String[] hashPattern;

	public NanopubSurfacePattern(String uriPattern, String hashPattern) {
		if (uriPattern == null || uriPattern.isEmpty()) {
			this.uriPattern = null;
		} else {
			this.uriPattern = uriPattern.split(" ");
		}
		if (hashPattern == null || hashPattern.isEmpty()) {
			this.hashPattern = null;
		} else {
			this.hashPattern = hashPattern.split(" ");
		}
	}

	public NanopubSurfacePattern(ServerInfo serverInfo) {
		this(serverInfo.getUriPattern(), serverInfo.getHashPattern());
	}

	public boolean matchesUri(String uri) {
		if (!matchesHash(TrustyUriUtils.getArtifactCode(uri))) {
			return false;
		}
		if (uriPattern == null) return true;
		boolean match = false;
		for (String p : uriPattern) {
			if (uri.startsWith(p)) {
				match = true;
				break;
			}
		}
		return match;
	}

	public boolean matchesHash(String artifactCode) {
		if (hashPattern == null) return true;
		boolean match = false;
		String hash = TrustyUriUtils.getDataPart(artifactCode);
		for (String p : hashPattern) {
			if (hash.startsWith(p)) {
				match = true;
				break;
			}
		}
		return match;
	}

	public boolean overlapsWith(NanopubSurfacePattern other) {
		// TODO This is inefficient: improve!
		if (uriPattern != null && other.uriPattern != null) {
			boolean overlapEncountered = false;
			for (String p1 : uriPattern) {
				for (String p2 : other.uriPattern) {
					if (p1.startsWith(p2) || p2.startsWith(p1)) {
						overlapEncountered = true;
						break;
					}
				}
				if (overlapEncountered) break;
			}
			if (!overlapEncountered) return false;
		}
		if (hashPattern != null && other.hashPattern != null) {
			boolean overlapEncountered = false;
			for (String p1 : hashPattern) {
				for (String p2 : other.hashPattern) {
					if (p1.startsWith(p2) || p2.startsWith(p1)) {
						overlapEncountered = true;
						break;
					}
				}
				if (overlapEncountered) break;
			}
			if (!overlapEncountered) return false;
		}
		return true;
	}

	public static boolean matchesUri(String uri, String uriPattern, String hashPattern) {
		return new NanopubSurfacePattern(uriPattern, hashPattern).matchesUri(uri);
	}

	public static boolean matchesUri(String uri, ServerInfo serverInfo) {
		return new NanopubSurfacePattern(serverInfo).matchesUri(uri);
	}

	public static boolean matchesHash(String artifactCode, String uriPattern, String hashPattern) {
		return new NanopubSurfacePattern(uriPattern, hashPattern).matchesHash(artifactCode);
	}

	public static boolean matchesHash(String artifactCode, ServerInfo serverInfo) {
		return new NanopubSurfacePattern(serverInfo).matchesHash(artifactCode);
	}

}
