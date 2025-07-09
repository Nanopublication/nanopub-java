package org.nanopub.extra.server;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;

import org.apache.http.client.methods.HttpGet;
import org.nanopub.NanopubUtils;

import com.google.gson.Gson;

public class RegistryInfo implements Serializable {

	public static class RegistryInfoException extends Exception {

		public RegistryInfoException(String url) {
			super(url);
		}

	}

	public static RegistryInfo load(String registryUrl) throws RegistryInfoException {
		return load(registryUrl, RegistryInfo.class);
	}

	protected static RegistryInfo load(String url, Class<? extends RegistryInfo> serverInfoClass) throws RegistryInfoException {
		HttpGet get = null;
		try {
			get = new HttpGet(url);
		} catch (IllegalArgumentException ex) {
			throw new RegistryInfoException("invalid URL: " + url);
		}
		get.setHeader("Accept", "application/json");
		RegistryInfo r = null;
		try (InputStream in = NanopubUtils.getHttpClient().execute(get).getEntity().getContent()) {
			r = new Gson().fromJson(new InputStreamReader(in, Charset.forName("UTF-8")), serverInfoClass);
			r.url = url;
		} catch (Exception ex) {
			throw new RegistryInfoException(url);
		}
		return r;
	}

	public RegistryInfo() {
	}

	protected String url;
	protected Long setupId;
	protected Long trustStateCounter;
	protected String lastTrustStateUpdate;
	protected String trustStateHash;
	protected String status;
	protected String coverageTypes;
	protected String coverageAgents;
	protected String currentSetting;
	protected String originalSetting;
	protected Long agentCount;
	protected Long accountCount;
	protected Long nanopubCount;
	protected Long loadCounter;

	public String getUrl() {
		return url;
	}

	public String getCollectionUrl() {
		return url + "np/";
	}

	public Long getSetupId() {
		return setupId;
	}

	public Long getTrustStateCounter() {
		return trustStateCounter;
	}

	public String getLastTrustStateUpdate() {
		return lastTrustStateUpdate;
	}

	public String getTrustStateHash() {
		return trustStateHash;
	}

	public String getStatus() {
		return status;
	}

	public String getCoverageTypes() {
		return coverageTypes;
	}

	public String getCoverageAgents() {
		return coverageAgents;
	}

	public String getCurrentSetting() {
		return currentSetting;
	}

	public String getOriginalSetting() {
		return originalSetting;
	}

	public Long getAgentCount() {
		return agentCount;
	}

	public Long getAccountCount() {
		return accountCount;
	}

	public Long getNanopubCount() {
		return nanopubCount;
	}

	public Long getLoadCounter() {
		return loadCounter;
	}

	public String asJson() {
		return new Gson().toJson(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RegistryInfo)) return false;
		return toString().equals(obj.toString());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		return url;
	}

}
