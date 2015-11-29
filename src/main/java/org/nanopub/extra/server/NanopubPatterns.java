package org.nanopub.extra.server;

import net.trustyuri.TrustyUriUtils;

// TODO Make pattern processing more efficient
public class NanopubPatterns {

	private final String[] uriPattern;
	private final String[] hashPattern;

	public NanopubPatterns(String uriPattern, String hashPattern) {
		if (uriPattern.isEmpty()) {
			this.uriPattern = null;
		} else {
			this.uriPattern = uriPattern.split(" ");
		}
		if (hashPattern.isEmpty()) {
			this.hashPattern = null;
		} else {
			this.hashPattern = hashPattern.split(" ");
		}
	}

	public NanopubPatterns(ServerInfo serverInfo) {
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

}
