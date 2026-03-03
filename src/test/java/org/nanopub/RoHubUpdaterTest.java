package org.nanopub;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RoHubUpdaterTest {

    @Test
    void testDateConversion() throws Exception {
        String nanopubDate = "2025-11-11T16:08:23.025Z";
        String roCrateDate = "2025-03-05T01:21:25.016018Z";

        Instant np = Instant.parse(nanopubDate);
        Instant ro = Instant.parse(roCrateDate);
        assertTrue(ro.isBefore(np));
    }

}
