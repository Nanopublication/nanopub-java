package org.nanopub.extra.services;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves nanopub service URLs of a given type from the active {@link NanopubSetting}.
 * <p>
 * The setting points to a service intro collection (a {@link NanopubIndex}) whose elements
 * are individual service intro nanopubs. Each service intro asserts
 * {@code <service-url> a npx:NanopubService, <service-type-iri>}.
 */
public class ServiceLookup {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLookup.class);

    private static final Map<IRI, List<String>> cache = new HashMap<>();

    private ServiceLookup() {
    }

    /**
     * Returns the URLs of all services of the given type listed in the active setting's
     * service intro collection. Cached for the JVM lifetime.
     *
     * @param typeIri the service type IRI (e.g. {@code NPS.NANOPUB_QUERY_1_1})
     * @return service URLs (may be empty if nothing matched or lookup failed)
     */
    public static synchronized List<String> getServices(IRI typeIri) {
        List<String> cached = cache.get(typeIri);
        if (cached != null) return cached;
        List<String> urls = new ArrayList<>();
        try {
            NanopubSetting setting = NanopubSetting.getDefaultSetting();
            IRI collectionIri = setting.getServiceIntroCollection();
            if (collectionIri == null) {
                logger.warn("No service intro collection in setting; cannot look up services of type {}", typeIri);
                cache.put(typeIri, urls);
                return urls;
            }
            List<String> bootstrap = NanopubServerUtils.getBootstrapServerList();
            Nanopub collectionNp = GetNanopub.get(collectionIri.stringValue(), bootstrap);
            if (collectionNp == null) {
                logger.error("Could not retrieve service intro collection: {}", collectionIri);
                cache.put(typeIri, urls);
                return urls;
            }
            NanopubIndex index = IndexUtils.castToIndex(collectionNp);
            for (IRI elementIri : index.getElements()) {
                try {
                    Nanopub introNp = GetNanopub.get(elementIri.stringValue(), bootstrap);
                    if (introNp == null) {
                        logger.warn("Could not retrieve service intro nanopub: {}", elementIri);
                        continue;
                    }
                    String url = extractServiceUrl(introNp, typeIri);
                    if (url != null) urls.add(url);
                } catch (Exception ex) {
                    logger.warn("Failed to process service intro {}: {}", elementIri, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            logger.error("Failed to look up services of type {}", typeIri, ex);
        }
        cache.put(typeIri, urls);
        return urls;
    }

    /**
     * Clears the lookup cache. Intended for tests.
     */
    public static synchronized void clearCache() {
        cache.clear();
    }

    private static String extractServiceUrl(Nanopub introNp, IRI typeIri) throws MalformedNanopubException {
        IRI subject = null;
        boolean hasNanopubServiceType = false;
        boolean hasRequestedType = false;
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
            if (obj.equals(NPX.NANOPUB_SERVICE)) hasNanopubServiceType = true;
            if (obj.equals(typeIri)) hasRequestedType = true;
        }
        if (subject != null && hasNanopubServiceType && hasRequestedType) {
            return subject.stringValue();
        }
        return null;
    }

}
