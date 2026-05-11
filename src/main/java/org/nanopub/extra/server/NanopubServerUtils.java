package org.nanopub.extra.server;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ServiceLookup;
import org.nanopub.extra.setting.NanopubSetting;
import org.nanopub.vocabulary.NPS;
import org.nanopub.vocabulary.NPX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility class for handling nanopub server-related operations.
 */
public class NanopubServerUtils {

    private static final Logger logger = LoggerFactory.getLogger(NanopubServerUtils.class);

    /**
     * System property naming a whitespace-separated list of registry server URLs.
     * When set, this overrides discovery via the nanopub setting (env var
     * {@code NANOPUB_REGISTRY_INSTANCES} also accepted).
     */
    public static final String REGISTRY_INSTANCES_PROPERTY = "nanopub.registry.instances";

    /**
     * Environment variable equivalent of {@link #REGISTRY_INSTANCES_PROPERTY}.
     */
    public static final String REGISTRY_INSTANCES_ENV = "NANOPUB_REGISTRY_INSTANCES";

    /**
     * Singleton, no instances.
     */
    private NanopubServerUtils() {
    }

    private static final List<String> bootstrapServerList = new ArrayList<>();
    private static List<String> registryServerList;

    /**
     * Returns a list of bootstrap servers.
     *
     * @return a list of bootstrap server URIs
     */
    public static List<String> getBootstrapServerList() {
        if (bootstrapServerList.isEmpty()) {
            try {
                for (IRI iri : NanopubSetting.getDefaultSetting().getBootstrapServices()) {
                    bootstrapServerList.add(iri.stringValue());
                }
            } catch (RDF4JException | MalformedNanopubException | IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return bootstrapServerList;
    }

    /**
     * Returns the list of registry server URLs to seed a {@link ServerIterator}.
     * <p>
     * Sources, in order of priority:
     * <ol>
     *   <li>{@code nanopub.registry.instances} system property /
     *       {@code NANOPUB_REGISTRY_INSTANCES} env var (whitespace-separated URLs).
     *       Replaces both bootstrap and discovery when set.</li>
     *   <li>Otherwise: the union of the setting's bootstrap services and
     *       {@link ServiceLookup#getServices(IRI)} for {@link NPS#NANOPUB_REGISTRY_1_0}.
     *       Bootstrap servers come first; discovered servers are appended without duplicates.</li>
     * </ol>
     * Cached for the JVM lifetime.
     *
     * @return a list of registry server URLs
     */
    public static synchronized List<String> getRegistryServerList() {
        if (registryServerList != null) return registryServerList;
        String override = System.getProperty(REGISTRY_INSTANCES_PROPERTY);
        if (override == null || override.isEmpty()) override = System.getenv(REGISTRY_INSTANCES_ENV);
        if (override != null && !override.trim().isEmpty()) {
            List<String> list = new ArrayList<>();
            for (String url : override.trim().split("\\s+")) list.add(url);
            logger.info("Using {} registry instance(s) from override", list.size());
            registryServerList = list;
            return registryServerList;
        }
        List<String> bootstrap = getBootstrapServerList();
        List<String> discovered = ServiceLookup.getServices(NPS.NANOPUB_REGISTRY_1_0);
        LinkedHashSet<String> union = new LinkedHashSet<>(bootstrap);
        union.addAll(discovered);
        logger.info("Using {} registry instance(s): {} bootstrap + {} discovered",
                union.size(), bootstrap.size(), discovered.size());
        registryServerList = new ArrayList<>(union);
        return registryServerList;
    }

    /**
     * HTTP response header carrying the registry instance's sync state.
     * See nanopub-registry's {@code Page} / {@code RegistryInfo.status}.
     */
    public static final String REGISTRY_STATUS_HEADER = "Nanopub-Registry-Status";

    /**
     * System property setting the cool-down (in seconds) before a registry
     * instance evicted for non-ready status is re-considered. Default
     * {@value #DEFAULT_REGISTRY_EVICTION_COOLDOWN_SECONDS}. Env var
     * {@code NANOPUB_REGISTRY_EVICTION_COOLDOWN_SECONDS} also accepted.
     */
    public static final String REGISTRY_EVICTION_COOLDOWN_PROPERTY = "nanopub.registry.eviction-cooldown-seconds";

    /**
     * Environment variable equivalent of {@link #REGISTRY_EVICTION_COOLDOWN_PROPERTY}.
     */
    public static final String REGISTRY_EVICTION_COOLDOWN_ENV = "NANOPUB_REGISTRY_EVICTION_COOLDOWN_SECONDS";

    private static final int DEFAULT_REGISTRY_EVICTION_COOLDOWN_SECONDS = 300;

    private static final ConcurrentMap<String, Long> evictedRegistriesUntil = new ConcurrentHashMap<>();

    /**
     * Returns the registry eviction cool-down in milliseconds, resolved from
     * {@link #REGISTRY_EVICTION_COOLDOWN_PROPERTY}, {@link #REGISTRY_EVICTION_COOLDOWN_ENV},
     * or the default of {@value #DEFAULT_REGISTRY_EVICTION_COOLDOWN_SECONDS} seconds.
     */
    public static long getRegistryEvictionCooldownMillis() {
        String value = System.getProperty(REGISTRY_EVICTION_COOLDOWN_PROPERTY);
        if (value == null || value.isEmpty()) value = System.getenv(REGISTRY_EVICTION_COOLDOWN_ENV);
        if (value != null && !value.trim().isEmpty()) {
            try {
                long n = Long.parseLong(value.trim());
                if (n >= 0) return n * 1000L;
                logger.warn("Ignoring {}={}: must be >= 0", REGISTRY_EVICTION_COOLDOWN_PROPERTY, value);
            } catch (NumberFormatException ex) {
                logger.warn("Ignoring {}={}: not a number", REGISTRY_EVICTION_COOLDOWN_PROPERTY, value);
            }
        }
        return DEFAULT_REGISTRY_EVICTION_COOLDOWN_SECONDS * 1000L;
    }

    /**
     * Returns true if the given registry status signals a fully-loaded state
     * usable for fetching nanopubs ({@code ready} or {@code updating};
     * case-insensitive). {@code updating} is the transient state entered from
     * {@code ready} during the registry's periodic re-sync, so the corpus is
     * still complete. {@code coreReady} is rejected: at that stage only core
     * nanopubs are loaded and the rest are still being fetched — matching the
     * registry's own peer-sync checks in {@code RegistryPeerConnector} and
     * {@code NanopubLoader}. Null/empty is treated as ready for backwards
     * compatibility with older registry instances that do not report a status.
     */
    public static boolean isReadyRegistryStatus(String status) {
        if (status == null || status.isEmpty()) return true;
        String lower = status.toLowerCase(Locale.ROOT);
        return lower.equals("ready") || lower.equals("updating");
    }

    /**
     * Returns true if the response's {@link #REGISTRY_STATUS_HEADER} signals a
     * fully-loaded state. Missing header is treated as ready (older instances).
     */
    public static boolean isReadyRegistryStatus(HttpResponse resp) {
        Header h = resp.getFirstHeader(REGISTRY_STATUS_HEADER);
        return isReadyRegistryStatus(h == null ? null : h.getValue());
    }

    /**
     * Marks the given registry URL as evicted for {@link #getRegistryEvictionCooldownMillis()}.
     */
    public static void evictRegistry(String registryUrl, String reason) {
        long until = System.currentTimeMillis() + getRegistryEvictionCooldownMillis();
        evictedRegistriesUntil.put(registryUrl, until);
        logger.warn("Evicting Nanopub Registry {} until {} ({})", registryUrl, new Date(until), reason);
    }

    /**
     * Returns true if the given registry URL is currently evicted (cool-down active).
     */
    public static boolean isRegistryEvicted(String registryUrl) {
        Long until = evictedRegistriesUntil.get(registryUrl);
        return until != null && until > System.currentTimeMillis();
    }

    /**
     * Checks if the given nanopub is a protected nanopub.
     *
     * @param np the nanopub to check
     * @return true if the nanopub is protected, false otherwise
     */
    public static boolean isProtectedNanopub(Nanopub np) {
        for (Statement st : np.getPubinfo()) {
            if (!st.getSubject().equals(np.getUri())) continue;
            if (!st.getPredicate().equals(RDF.TYPE)) continue;
            if (st.getObject().equals(NPX.PROTECTED_NANOPUB)) return true;
        }
        return false;
    }

}
