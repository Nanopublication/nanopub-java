package org.nanopub.extra.services;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import net.trustyuri.TrustyUriUtils;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.server.NanopubServerUtils;
import org.nanopub.extra.setting.NanopubSetting;
import org.nanopub.vocabulary.NPX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves nanopub service URLs of a given type from the active {@link NanopubSetting}.
 * <p>
 * The setting points to a service intro collection (a {@link NanopubIndex}) whose elements
 * are individual service intro nanopubs. Each service intro asserts
 * {@code <service-url> a npx:NanopubService, <service-type-iri>}.
 * <p>
 * Reading the collection means fetching every service intro in it, which takes several seconds.
 * The collection is walked once for all service types, and what it says is kept in a file, so that
 * the command line tools, which start a JVM per call, do not walk it again on every call
 * (issue #208). A collection and its service intros are trusty nanopublications and cannot change,
 * so what was read from them stays valid for as long as the setting names that collection.
 */
public class ServiceLookup {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLookup.class);

    /**
     * System property naming the file that what was read from the service intro collection is kept
     * in, instead of {@code ~/.nanopub/cachedservices}. Set to the empty string, nothing is kept
     * beyond the JVM.
     */
    public static final String CACHE_FILE_PROPERTY = "nanopub.services.cache-file";

    private static final String CACHE_FILE_HEADER = "# services of ";

    /**
     * A service a service intro declares: its URL and the types the intro gives it, other than
     * {@code npx:NanopubService}.
     */
    private record Service(String url, Set<IRI> types) {
    }

    private static final Map<IRI, List<String>> cache = new HashMap<>();

    // What the service intro collection of the setting says, for all service types. Null until read.
    private static List<Service> services;

    private ServiceLookup() {
    }

    /**
     * Returns the URLs of all services of the given type listed in the active setting's
     * service intro collection. Cached for the JVM lifetime, and beyond it in a file when the
     * whole collection could be read.
     *
     * @param typeIri the service type IRI (e.g. {@code NPS.NANOPUB_QUERY_1_1})
     * @return service URLs (may be empty if nothing matched or lookup failed)
     */
    public static synchronized List<String> getServices(IRI typeIri) {
        List<String> cached = cache.get(typeIri);
        if (cached != null) return cached;
        if (services == null) services = lookUpServices();
        List<String> urls = new ArrayList<>();
        for (Service service : services) {
            if (service.types().contains(typeIri)) urls.add(service.url());
        }
        cache.put(typeIri, urls);
        return urls;
    }

    /**
     * Clears the lookup cache held in memory. Intended for tests.
     */
    public static synchronized void clearCache() {
        cache.clear();
        services = null;
    }

    private static List<Service> lookUpServices() {
        List<Service> found = new ArrayList<>();
        try {
            NanopubSetting setting = NanopubSetting.getDefaultSetting();
            IRI collectionIri = setting.getServiceIntroCollection();
            if (collectionIri == null) {
                logger.warn("No service intro collection in setting; cannot look up services");
                return found;
            }
            List<Service> kept = readCacheFile(collectionIri);
            if (kept != null) {
                logger.debug("Using {} service(s) of collection {} kept in {}", kept.size(), collectionIri, cacheFile());
                return kept;
            }
            List<String> bootstrap = NanopubServerUtils.getBootstrapServerList();
            Nanopub collectionNp = GetNanopub.get(collectionIri.stringValue(), bootstrap);
            if (collectionNp == null) {
                logger.error("Could not retrieve service intro collection: {}", collectionIri);
                return found;
            }
            NanopubIndex index = IndexUtils.castToIndex(collectionNp);
            boolean complete = true;
            for (IRI elementIri : index.getElements()) {
                Nanopub introNp;
                try {
                    introNp = GetNanopub.get(elementIri.stringValue(), bootstrap);
                } catch (Exception ex) {
                    logger.warn("Failed to retrieve service intro {}: {}", elementIri, ex.getMessage());
                    logger.debug("Failed to retrieve service intro {}", elementIri, ex);
                    complete = false;
                    continue;
                }
                if (introNp == null) {
                    logger.warn("Could not retrieve service intro nanopub: {}", elementIri);
                    complete = false;
                    continue;
                }
                try {
                    Service service = extractService(introNp);
                    if (service != null) found.add(service);
                } catch (Exception ex) {
                    logger.warn("Failed to process service intro {}: {}", elementIri, ex.getMessage());
                    logger.debug("Failed to process service intro {}", elementIri, ex);
                }
            }
            // An intro that could not be fetched may be there the next time, so only a collection
            // read to the end is worth keeping.
            if (complete) writeCacheFile(collectionIri, found);
        } catch (Exception ex) {
            logger.error("Failed to look up services", ex);
        }
        return found;
    }

    private static File cacheFile() {
        String path = System.getProperty(CACHE_FILE_PROPERTY);
        if (path == null) return new File(System.getProperty("user.home") + "/.nanopub/cachedservices");
        if (path.isEmpty()) return null;
        return new File(path);
    }

    // The file holds a header line naming the collection, then a line per service: its URL, a tab,
    // and its types separated by spaces. Anything else in it, and it is read as if it were not there.
    private static List<Service> readCacheFile(IRI collectionIri) {
        File file = cacheFile();
        if (file == null || !file.isFile() || !isImmutable(collectionIri)) return null;
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            if (lines.isEmpty() || !lines.get(0).equals(CACHE_FILE_HEADER + collectionIri.stringValue())) return null;
            List<Service> kept = new ArrayList<>();
            for (String line : lines.subList(1, lines.size())) {
                if (line.isEmpty()) continue;
                String[] parts = line.split("\t");
                if (parts.length != 2 || parts[0].isEmpty()) return null;
                Set<IRI> types = new LinkedHashSet<>();
                for (String type : parts[1].split(" ")) {
                    types.add(Values.iri(type));
                }
                kept.add(new Service(parts[0], types));
            }
            return kept;
        } catch (Exception ex) {
            logger.debug("Could not read the services kept in {}", file, ex);
            return null;
        }
    }

    private static void writeCacheFile(IRI collectionIri, List<Service> found) {
        File file = cacheFile();
        if (file == null || !isImmutable(collectionIri)) return;
        try {
            StringBuilder content = new StringBuilder(CACHE_FILE_HEADER).append(collectionIri.stringValue()).append("\n");
            for (Service service : found) {
                if (service.types().isEmpty()) continue;
                List<String> types = new ArrayList<>();
                for (IRI type : service.types()) types.add(type.stringValue());
                content.append(service.url()).append("\t").append(String.join(" ", types)).append("\n");
            }
            File dir = file.getAbsoluteFile().getParentFile();
            Files.createDirectories(dir.toPath());
            // written aside and moved into place, so that a call running at the same time never
            // reads half a file
            Path temp = Files.createTempFile(dir.toPath(), file.getName(), ".tmp");
            try {
                Files.writeString(temp, content.toString(), StandardCharsets.UTF_8);
                try {
                    Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (Exception ex) {
            // not being able to keep the answer costs the next call a few seconds, nothing more
            logger.debug("Could not keep the services in {}", file, ex);
        }
    }

    // What a collection lists can only be kept if it cannot change, which holds for a trusty URI.
    private static boolean isImmutable(IRI collectionIri) {
        return TrustyUriUtils.isPotentialTrustyUri(collectionIri.stringValue());
    }

    private static Service extractService(Nanopub introNp) throws MalformedNanopubException {
        IRI subject = null;
        boolean hasNanopubServiceType = false;
        Set<IRI> types = new LinkedHashSet<>();
        for (Statement st : introNp.getAssertion()) {
            if (!st.getPredicate().equals(RDF.TYPE)) continue;
            if (!(st.getSubject() instanceof IRI subj)) continue;
            if (!(st.getObject() instanceof IRI obj)) continue;
            if (subject == null) {
                subject = subj;
            } else if (!subject.equals(subj)) {
                throw new MalformedNanopubException(
                        "Service intro " + introNp.getUri() + " has multiple typed subjects");
            }
            if (obj.equals(NPX.NANOPUB_SERVICE)) {
                hasNanopubServiceType = true;
            } else {
                types.add(obj);
            }
        }
        if (subject != null && hasNanopubServiceType) {
            return new Service(subject.stringValue(), types);
        }
        return null;
    }

}
