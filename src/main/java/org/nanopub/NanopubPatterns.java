package org.nanopub;

import org.nanopub.extra.aida.AidaPattern;
import org.nanopub.extra.index.NanopubIndexPattern;
import org.nanopub.extra.security.DigitalSignaturePattern;
import org.nanopub.trusty.TrustyNanopubPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a collection of nanopublication patterns.
 */
public class NanopubPatterns {

    private static List<NanopubPattern> patterns = new ArrayList<>();

    static {
        addPattern(new SimpleTimestampPattern());
        addPattern(new SimpleCreatorPattern());
        addPattern(new TrustyNanopubPattern());
        addPattern(new NanopubIndexPattern());
        addPattern(new AidaPattern());
        addPattern(new DigitalSignaturePattern());
    }

    private NanopubPatterns() {}  // no instances allowed

    /**
     * Adds a nanopublication pattern to the collection.
     *
     * @param pattern The nanopublication pattern to add
     */
    public static void addPattern(NanopubPattern pattern) {
        patterns.add(pattern);
    }

    /**
     * Returns a list of nanopublication patterns.
     *
     * @return A list of nanopublication patterns
     */
    public static List<NanopubPattern> getPatterns() {
        return patterns;
    }

}
