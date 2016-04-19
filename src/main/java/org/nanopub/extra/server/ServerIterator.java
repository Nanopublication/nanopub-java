package org.nanopub.extra.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nanopub.extra.server.ServerInfo.ServerInfoException;

public class ServerIterator implements Iterator<ServerInfo> {

	private static Map<String,ServerInfo> serverInfos = new HashMap<>();
	private static long serverInfoRefreshed = System.currentTimeMillis();
	private static final long serverInfoRefreshInterval = 60 * 60 * 1000;

	private List<String> serversToContact = new ArrayList<>();
	private List<String> serversToGetPeers = new ArrayList<>();
	private Map<String,Boolean> serversContacted = new HashMap<>();
	private Map<String,Boolean> serversPeersGot = new HashMap<>();
	private ServerInfo next = null;

	public ServerIterator() {
		serversToContact.addAll(NanopubServerUtils.getBootstrapServerList());
	}

	public ServerIterator(String... bootstrapServers) {
		for (String s : bootstrapServers) {
			serversToContact.add(s);
		}
	}

	public ServerIterator(List<String> bootstrapServers) {
		serversToContact.addAll(bootstrapServers);
	}

	@Override
	public boolean hasNext() {
		if (next == null) {
			next = getNextServer();
		}
		return next != null;
	}

	@Override
	public ServerInfo next() {
		ServerInfo n = next;
		next = null;
		if (n == null) {
			n = getNextServer();
		}
		return n;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	private ServerInfo getNextServer() {
		while (!serversToContact.isEmpty() || !serversToGetPeers.isEmpty()) {
			if (!serversToContact.isEmpty()) {
				String url = serversToContact.remove(0);
				if (serversContacted.containsKey(url)) continue;
				serversContacted.put(url, true);
				ServerInfo info = getServerInfo(url);
				if (info == null) continue;
				if (!info.getPublicUrl().equals(url)) continue;
				serversToGetPeers.add(url);
				return info;
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

	private ServerInfo getServerInfo(String url) {
		if (System.currentTimeMillis() - serverInfoRefreshed > serverInfoRefreshInterval) {
			serverInfos.clear();
			serverInfoRefreshed = System.currentTimeMillis();
		}
		if (!serverInfos.containsKey(url)) {
			try {
				serverInfos.put(url, ServerInfo.load(url));
			} catch (ServerInfoException ex) {
				// ignore
			}
		}
		return serverInfos.get(url);
	}

}
