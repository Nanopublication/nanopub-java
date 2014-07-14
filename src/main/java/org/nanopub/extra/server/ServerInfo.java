package org.nanopub.extra.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.gson.Gson;

public class ServerInfo {

	public static ServerInfo load(String serverUrl) throws IOException {
		HttpGet get = new HttpGet(serverUrl);
		get.setHeader("Accept", "application/json");
	    InputStream in = HttpClientBuilder.create().build().execute(get).getEntity().getContent();
		return new Gson().fromJson(new InputStreamReader(in), ServerInfo.class);
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
