package org.nanopub.extra.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.gson.Gson;

public class ServerInfo implements Serializable {

	private static final long serialVersionUID = 5893051633759794791L;

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
		InputStream in = null;
		try {
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000)
					.setConnectionRequestTimeout(100).setSocketTimeout(10000).build();
			HttpClient c = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		    in = c.execute(get).getEntity().getContent();
			si = new Gson().fromJson(new InputStreamReader(in, Charset.forName("UTF-8")), serverInfoClass);
		} catch (Exception ex) {
			throw new ServerInfoException(serverUrl);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
					throw new ServerInfoException("Could not close stream: " + ex.getMessage());
				}
			}
		}
		if (!si.getPublicUrl().equals(serverUrl)) {
			throw new ServerInfoException("Server URL does not match its declared public URL");
		}
		if (si.getProtocolVersionValue() < NanopubServerUtils.requiredProtocolVersionValue) {
			throw new ServerInfoException("Protocol version of server is too old: " + si.getProtocolVersion());
		}
		return si;
	}

	protected String publicUrl;
	protected String admin;
	protected String protocolVersion = "0.0";
	protected String description;
	protected boolean postNanopubsEnabled;
	protected boolean postPeersEnabled;

	protected int pageSize = -1;
	protected long nextNanopubNo = -1;
	protected long journalId = -1;

	protected Integer maxNanopubTriples;
	protected Long maxNanopubBytes;
	protected Long maxNanopubs;

	// TODO Use these fields when getting nanopubs:
	protected String uriPattern;
	protected String hashPattern;

	private transient NanopubSurfacePattern pattern;

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

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public int getProtocolVersionValue() {
		return NanopubServerUtils.getVersionValue(protocolVersion);
	}

	public String getDescription() {
		return description;
	}

	public Integer getMaxNanopubTriples() {
		return maxNanopubTriples;
	}

	public Long getMaxNanopubBytes() {
		return maxNanopubBytes;
	}

	public Long getMaxNanopubs() {
		return maxNanopubs;
	}

	public String getUriPattern() {
		return uriPattern;
	}

	public String getHashPattern() {
		return hashPattern;
	}

	public NanopubSurfacePattern getNanopubSurfacePattern() {
		if (pattern == null) {
			pattern = new NanopubSurfacePattern(this);
		}
		return pattern;
	}

	public String asJson() {
		return new Gson().toJson(this);
	}

}
