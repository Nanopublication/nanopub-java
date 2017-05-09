package org.nanopub.extra.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nanopub.extra.server.ServerInfo.ServerInfoException;

public class ServerIterator implements Iterator<ServerInfo> {

	private static Map<String,ServerInfo> serverInfos = new HashMap<>();
	private static long serverInfoRefreshed = System.currentTimeMillis();
	private static final long serverInfoRefreshInterval = 24 * 60 * 60 * 1000;

	private static Map<String,Boolean> serverBlackList;

	private List<ServerInfo> cachedServers = null;
	private List<String> serversToContact = new ArrayList<>();
	private List<String> serversToGetPeers = new ArrayList<>();
	private Map<String,Boolean> serversContacted = new HashMap<>();
	private Map<String,Boolean> serversPeersGot = new HashMap<>();
	private ServerInfo next = null;

	public ServerIterator() {
		this(false);
		// TODO: Peer URLs should expire so this isn't necessary:
		serverBlackList = new HashMap<>();
		serverBlackList.put("http://s1.semanticscience.org:8082/", true);
		serverBlackList.put("http://nanopub-server.ops.labs.vu.nl/", true);
		serverBlackList.put("http://ristretto.med.yale.edu:8080/nanopub-server/", true);
		serverBlackList.put("http://nanopubs.semanticscience.org:8082/", true);
		serverBlackList.put("http://rdf.disgenet.org/nanopub-server", true);
		serverBlackList.put("http://digitalduchemin.org/np/", true);
		serverBlackList.put("http://nanopub.exynize.com/", true);
		serverBlackList.put("http://digitalduchemin.org/np-mirror/", true);
	}

	public ServerIterator(boolean forceServerReload) {
		if (!forceServerReload) {
			try {
				loadCachedServers();
			} catch (Exception ex) {
			}
		}
		if (cachedServers == null) {
			serversToContact.addAll(NanopubServerUtils.getBootstrapServerList());
		}
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
		if (cachedServers != null) {
			if (cachedServers.isEmpty()) return null;
			return cachedServers.remove(0);
		} else {
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
							if (serverBlackList.containsKey(peerUrl)) continue;
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

	@SuppressWarnings("unchecked")
	private void loadCachedServers() throws Exception {
		File serverListFile = getServerListFile();
		if (serverListFile.exists()) {
			long now = System.currentTimeMillis();
			long fileTime = serverListFile.lastModified();
			if (fileTime > now || now - fileTime > 1000 * 60 * 60 * 24) {
				return;
			}
			FileInputStream fin = new FileInputStream(serverListFile);
			ObjectInputStream oin = new ObjectInputStream(fin);
			cachedServers = (List<ServerInfo>) oin.readObject();
			oin.close();
		}
	}

	public static void writeCachedServers(List<ServerInfo> cachedServers) throws IOException {
		if (cachedServers.size() < 5) return;
		File serverListFile = getServerListFile();
		serverListFile.getParentFile().mkdir();
		FileOutputStream fout = new FileOutputStream(serverListFile);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(cachedServers);
		oos.close();
	}

	private static File getServerListFile() {
		return new File(System.getProperty("user.home") + "/.nanopub/cachedservers");
	}

}
