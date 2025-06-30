package org.nanopub.extra.server;

import org.nanopub.extra.server.RegistryInfo.RegistryInfoException;

import java.io.*;
import java.util.*;

public class ServerIterator implements Iterator<RegistryInfo> {

	private static Map<String,RegistryInfo> serverInfos = new HashMap<>();
	private static long serverInfoRefreshed = System.currentTimeMillis();
	private static final long serverInfoRefreshInterval = 24 * 60 * 60 * 1000;

	private List<RegistryInfo> cachedServers = null;
	private List<String> serversToContact = new ArrayList<>();
	private Map<String,Boolean> serversContacted = new HashMap<>();
	private RegistryInfo next = null;

	public ServerIterator() {
		this(false);
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
        serversToContact.addAll(Arrays.asList(bootstrapServers));
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
	public RegistryInfo next() {
		RegistryInfo n = next;
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

	private RegistryInfo getNextServer() {
		if (cachedServers != null) {
			if (cachedServers.isEmpty()) return null;
			return cachedServers.remove(0);
		} else {
			while (!serversToContact.isEmpty()) {
				if (!serversToContact.isEmpty()) {
					String url = serversToContact.remove(0);
					if (serversContacted.containsKey(url)) continue;
					serversContacted.put(url, true);
					RegistryInfo info = getServerInfo(url);
					if (info == null) continue;
					return info;
				}
			}
		}
		return null;
	}

	private RegistryInfo getServerInfo(String url) {
		if (System.currentTimeMillis() - serverInfoRefreshed > serverInfoRefreshInterval) {
			serverInfos.clear();
			serverInfoRefreshed = System.currentTimeMillis();
		}
		if (!serverInfos.containsKey(url)) {
			try {
				serverInfos.put(url, RegistryInfo.load(url));
			} catch (RegistryInfoException ex) {
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
			try (ObjectInputStream oin = new ObjectInputStream(new FileInputStream(serverListFile))) {
				cachedServers = (List<RegistryInfo>) oin.readObject();
			}
		}
	}

	public static void writeCachedServers(List<RegistryInfo> cachedServers) throws IOException {
		if (cachedServers.size() < 5) return;
		File serverListFile = getServerListFile();
		serverListFile.getParentFile().mkdir();
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serverListFile))) {
			oos.writeObject(cachedServers);
		}
	}

	private static File getServerListFile() {
		return new File(System.getProperty("user.home") + "/.nanopub/cachedservers");
	}

}
