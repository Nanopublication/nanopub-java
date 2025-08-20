package org.nanopub;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NanopubPatternsTest {

    @Test
    void addPatternAddsPatternToCollection() {
        NanopubPattern pattern = new NanopubPattern() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public boolean appliesTo(Nanopub nanopub) {
                return false;
            }

            @Override
            public boolean isCorrectlyUsedBy(Nanopub nanopub) {
                return false;
            }

            @Override
            public String getDescriptionFor(Nanopub nanopub) {
                return null;
            }

            @Override
            public URL getPatternInfoUrl() {
                return null;
            }
        };
        assertFalse(NanopubPatterns.getPatterns().contains(pattern));
        NanopubPatterns.addPattern(pattern);
        assertTrue(NanopubPatterns.getPatterns().contains(pattern));
    }

    @Test
    void getPatterns() {
        List<NanopubPattern> patterns = NanopubPatterns.getPatterns();
        assertFalse(patterns.isEmpty());
    }

}