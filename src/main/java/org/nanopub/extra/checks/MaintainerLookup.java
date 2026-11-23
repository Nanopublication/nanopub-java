package org.nanopub.extra.checks;

import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryAccess;
import org.nanopub.extra.services.QueryRef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Says which spaces maintain a resource. A space maintains a resource once one of its admins
 * has declared so; only then does it govern the definitions that are versions of that
 * resource.
 */
@FunctionalInterface
public interface MaintainerLookup {

    /**
     * The query that lists the maintained resources the query services have validated, each
     * with the space that maintains it.
     */
    String GET_MAINTAINED_RESOURCES = "RAEdX-wzz7vfswC5Y6sbL7XeUyJrL2BKT-luDHPZZpmRY/get-maintained-resources";

    /**
     * Asks the query services of the nanopublication network. The answer is kept for a few
     * minutes, so that checking many nanopublications asks once.
     */
    MaintainerLookup NETWORK = new MaintainerLookup() {

        private static final long MAX_AGE_MS = 5 * 60 * 1000;

        private Map<String, Set<String>> spacesByResource;
        private long fetchedAt;

        @Override
        public synchronized Set<String> getMaintainingSpaces(String resource) throws Exception {
            long now = System.currentTimeMillis();
            if (spacesByResource == null || now - fetchedAt > MAX_AGE_MS) {
                ApiResponse response = QueryAccess.get(new QueryRef(GET_MAINTAINED_RESOURCES));
                Map<String, Set<String>> fetched = new HashMap<>();
                for (ApiResponseEntry row : response.getData()) {
                    String maintained = row.get("resource");
                    String space = row.get("space");
                    if (maintained == null || space == null) continue;
                    fetched.computeIfAbsent(maintained, k -> new HashSet<>()).add(space);
                }
                spacesByResource = fetched;
                fetchedAt = now;
            }
            return spacesByResource.getOrDefault(resource, Set.of());
        }

    };

    /**
     * Returns the spaces that maintain a resource.
     *
     * @param resource the IRI of the resource
     * @return the IRIs of the spaces that maintain it, empty if none does
     * @throws Exception if this cannot be found out, as when no service answers
     */
    Set<String> getMaintainingSpaces(String resource) throws Exception;

}
