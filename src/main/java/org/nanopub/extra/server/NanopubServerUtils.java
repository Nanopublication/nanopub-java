package org.nanopub.extra.server;

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
import java.util.LinkedHashSet;
import java.util.List;

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
