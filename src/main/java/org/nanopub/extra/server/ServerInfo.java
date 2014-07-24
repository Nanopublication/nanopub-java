package org.nanopub.extra.server;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.gson.Gson;

public class ServerInfo {

	public static class ServerInfoException extends Exception {

		private static final long serialVersionUID = 3903673740899289181L;

		public ServerInfoException(String serverUrl) {
			super(serverUrl);
		}

	}

	public static ServerInfo load(String serverUrl) throws ServerInfoException {
		return load(serverUrl, ServerInfo.class);
	}

	protected static ServerInfo load(String serverUrl, Class<? extends ServerInfo> serverInfoClass) throws ServerInfoException {
		HttpGet get = new HttpGet(serverUrl);
		get.setHeader("Accept", "application/json");
		ServerInfo si = null;
		try {
		    InputStream in = HttpClientBuilder.create().build().execute(get).getEntity().getContent();
			si = new Gson().fromJson(new InputStreamReader(in), serverInfoClass);
		} catch (Exception ex) {
			throw new ServerInfoException(serverUrl);
		}
		if (!si.getPublicUrl().equals(serverUrl)) {
			throw new ServerInfoException("Server URL does not match its declared public URL");
		}
		return si;
	}

	protected String publicUrl;
	protected String admin;
	protected boolean postNanopubsEnabled;
	protected boolean postPeersEnabled;

	protected int pageSize = -1;
	protected long nextNanopubNo = -1;
	protected long journalId = -1;

	public ServerInfo() {
	}

	public boolean isPostNanopubsEnabled() {
		return postNanopubsEnabled;
	}

	public boolean isPostPeersEnabled() {
		return postPeersEnabled;
	}

	public String getPublicUrl() {
		return publicUrl;
	}

	public String getAdmin() {
		return admin;
	}

	public int getPageSize() {
		return pageSize;
	}

	public long getNextNanopubNo() {
		return nextNanopubNo;
	}

	public long getJournalId() {
		return journalId;
	}

	public String asJson() {
		return new Gson().toJson(this);
	}

}
