package org.nanopub.extra.server;

import com.google.gson.Gson;
import org.apache.http.client.methods.HttpGet;
import org.nanopub.NanopubUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * RegistryInfo class represents the information about a nanopub registry server.
 */
public class RegistryInfo implements Serializable {

    /**
     * Exception class for handling errors related to loading registry information.
     */
    public static class RegistryInfoException extends Exception {

        /**
         * Constructs a RegistryInfoException with a message indicating an invalid URL.
         *
         * @param url the URL that caused the exception
         */
        public RegistryInfoException(String url) {
            super(url);
        }

    }

    /**
     * Loads the registry information from the specified URL.
     *
     * @param registryUrl the URL of the registry server
     * @return a RegistryInfo object containing the server information
     * @throws org.nanopub.extra.server.RegistryInfo.RegistryInfoException if there is an error loading the registry information
     */
    public static RegistryInfo load(String registryUrl) throws RegistryInfoException {
        return load(registryUrl, RegistryInfo.class);
    }

    /**
     * Loads the registry information from the specified URL using a specific class for deserialization.
     *
     * @param url             the URL of the registry server
     * @param serverInfoClass the class to use for deserializing the registry information
     * @return a RegistryInfo object containing the server information
     * @throws org.nanopub.extra.server.RegistryInfo.RegistryInfoException if there is an error loading the registry information
     */
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
            r = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), serverInfoClass);
            r.url = url;
        } catch (Exception ex) {
            throw new RegistryInfoException(url);
        }
        return r;
    }

    /**
     * Default constructor for RegistryInfo.
     * Initializes an empty RegistryInfo object.
     */
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

    /**
     * Returns the URL of the nanopub registry server.
     *
     * @return the URL of the registry server
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the URL for accessing the nanopublications collection of the registry server.
     *
     * @return the URL for the nanopublications collection
     */
    public String getCollectionUrl() {
        return url + "np/";
    }

    /**
     * Returns the setup ID of the registry server.
     *
     * @return the setup ID
     */
    public Long getSetupId() {
        return setupId;
    }

    /**
     * Returns the trust state counter of the registry server.
     *
     * @return the trust state counter
     */
    public Long getTrustStateCounter() {
        return trustStateCounter;
    }

    /**
     * Returns the last trust state update timestamp of the registry server.
     *
     * @return a string representing the last trust state update
     */
    public String getLastTrustStateUpdate() {
        return lastTrustStateUpdate;
    }

    /**
     * Returns the hash of the current trust state of the registry server.
     *
     * @return a string representing the trust state hash
     */
    public String getTrustStateHash() {
        return trustStateHash;
    }

    /**
     * Returns the status of the nanopub registry server.
     *
     * @return a string representing the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Returns the types of coverage provided by the registry server.
     *
     * @return a string representing the coverage types
     */
    public String getCoverageTypes() {
        return coverageTypes;
    }

    /**
     * Returns the agents covered by the registry server.
     *
     * @return a string representing the coverage agents
     */
    public String getCoverageAgents() {
        return coverageAgents;
    }

    /**
     * Returns the current setting of the registry server.
     *
     * @return a string representing the current setting
     */
    public String getCurrentSetting() {
        return currentSetting;
    }

    /**
     * Returns the original setting of the registry server.
     *
     * @return a string representing the original setting
     */
    public String getOriginalSetting() {
        return originalSetting;
    }

    /**
     * Returns the count of agents registered on the server.
     *
     * @return the number of agents
     */
    public Long getAgentCount() {
        return agentCount;
    }

    /**
     * Returns the count of accounts registered on the server.
     *
     * @return the number of accounts
     */
    public Long getAccountCount() {
        return accountCount;
    }

    /**
     * Returns the count of nanopublications stored on the server.
     *
     * @return the number of nanopublications
     */
    public Long getNanopubCount() {
        return nanopubCount;
    }

    /**
     * Returns the load counter for the registry server.
     *
     * @return the load counter
     */
    public Long getLoadCounter() {
        return loadCounter;
    }

    /**
     * Returns the JSON representation of this RegistryInfo object.
     *
     * @return a JSON string representing the RegistryInfo
     */
    public String asJson() {
        return new Gson().toJson(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the URL of the registry server as a string.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RegistryInfo)) {
            return false;
        }
        return toString().equals(obj.toString());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the hash code of the registry server.
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the string representation of the registry server URL.
     */
    @Override
    public String toString() {
        return url;
    }

}
