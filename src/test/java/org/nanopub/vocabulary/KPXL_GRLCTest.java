package org.nanopub.vocabulary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KPXL_GRLCTest {

    @Test
    void namespaceAndPrefix() {
        assertEquals("https://w3id.org/kpxl/grlc/", KPXL_GRLC.NAMESPACE);
        assertEquals("kpxl_grlc", KPXL_GRLC.PREFIX);
        assertEquals(KPXL_GRLC.NAMESPACE, KPXL_GRLC.NS.getName());
        assertEquals(KPXL_GRLC.PREFIX, KPXL_GRLC.NS.getPrefix());
    }

    @Test
    void iriConstants() {
        assertEquals("https://w3id.org/kpxl/grlc/grlc-query", KPXL_GRLC.GRLC_QUERY.stringValue());
        assertEquals("https://w3id.org/kpxl/grlc/endpoint", KPXL_GRLC.ENDPOINT.stringValue());
        assertEquals("https://w3id.org/kpxl/grlc/sparql", KPXL_GRLC.SPARQL.stringValue());
    }

}
