package org.nanopub.extra.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ServerIterator implements Iterator<String> {

	private List<String> serversToContact = new ArrayList<>();
	private List<String> serversToGetPeers = new ArrayList<>();
	private Map<String,Boolean> serversContacted = new HashMap<>();
	private Map<String,Boolean> serversPeersGot = new HashMap<>();
	private String next = null;

	public ServerIterator() {
		serversToContact.addAll(NanopubServerUtils.getBootstrapServerList());
		serversToGetPeers.addAll(NanopubServerUtils.getBootstrapServerList());
	}

	public ServerIterator(String... bootstrapServers) {
		for (String s : bootstrapServers) {
			serversToContact.add(s);
			serversToGetPeers.add(s);
		}
	}

	public ServerIterator(List<String> bootstrapServers) {
		serversToContact.addAll(bootstrapServers);
		serversToGetPeers.addAll(bootstrapServers);
	}

	@Override
	public boolean hasNext() {
		if (next == null) {
			next = getNextServerUrl();
		}
		return next != null;
	}

	@Override
	public String next() {
		String r = next;
		next = null;
		if (r == null) {
			r = getNextServerUrl();
		}
		return r;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	private String getNextServerUrl() {
		while (!serversToContact.isEmpty() || !serversToGetPeers.isEmpty()) {
			if (!serversToContact.isEmpty()) {
				String url = serversToContact.remove(0);
				if (serversContacted.containsKey(url)) continue;
				serversContacted.put(url, true);
				return url;
			}
			if (!serversToGetPeers.isEmpty()) {
				String url = serversToGetPeers.remove(0);
				if (serversPeersGot.containsKey(url)) continue;
				serversPeersGot.put(url, true);
				try {
					for (String peerUrl : NanopubServerUtils.loadPeerList(url)) {
						if (!serversContacted.containsKey(peerUrl)) {
							serversToContact.add(peerUrl);
						}
						if (!serversPeersGot.containsKey(peerUrl)) {
							serversToGetPeers.add(peerUrl);
						}
					}
				} catch (IOException ex) {
					// ignore
				}
			}
		}
		return null;
	}

}
