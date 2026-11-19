package org.nanopub.templates;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.Nanopub;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nanopublications read from files, to be found by their ID before anything else is asked
 * for them. This is how a command uses a template that is not published yet.
 */
final class LocalNanopubs {

    private static final Pattern ARTIFACT_CODE = Pattern.compile("RA[A-Za-z0-9_-]{43}");

    private LocalNanopubs() {
    }

    /**
     * Returns a resolver that finds the nanopublications of the given files, and asks the
     * given resolver for all others.
     *
     * @param files    files with one or more nanopublications each
     * @param fallback the resolver for nanopublications that are not in the files
     * @return the resolver
     * @throws IOException if a file cannot be read or holds no valid nanopublications
     */
    static NanopubResolver resolver(List<String> files, NanopubResolver fallback) throws IOException {
        Map<String, Nanopub> local = new HashMap<>();
        for (String file : files) {
            try {
                MultiNanopubRdfHandler.process(new File(file), np -> local.put(np.getUri().stringValue(), np));
            } catch (RDF4JException | MalformedNanopubException ex) {
                throw new IOException("Cannot read nanopublication file " + file + ": " + ex.getMessage(), ex);
            }
        }
        return id -> {
            Nanopub found = find(local, id);
            return found != null ? found : fallback.getNanopub(id);
        };
    }

    // Finds a nanopublication among the given ones: by its URI, by an IRI within it (a
    // template can be referred to by its node), or by its artifact code.
    private static Nanopub find(Map<String, Nanopub> local, String id) {
        Nanopub exact = local.get(id);
        if (exact != null) return exact;
        String artifactCode = getArtifactCode(id);
        for (Map.Entry<String, Nanopub> e : local.entrySet()) {
            String uri = e.getKey();
            if (artifactCode != null && artifactCode.equals(getArtifactCode(uri))) return e.getValue();
            if (!id.startsWith(uri)) continue;
            if (uri.endsWith("/") || uri.endsWith("#") || "/#".indexOf(id.charAt(uri.length())) >= 0) return e.getValue();
        }
        return null;
    }

    private static String getArtifactCode(String iri) {
        Matcher m = ARTIFACT_CODE.matcher(iri);
        return m.find() ? m.group() : null;
    }

}
