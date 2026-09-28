package org.nanopub;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class NanopubRdfHandlerTest {

    private static final String NANOPUB_TRIG = """
            @prefix this: <https://example.org/np1#> .
            @prefix np: <http://www.nanopub.org/nschema#> .
            @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
            this:Head {
                this: a np:Nanopublication ;
                    np:hasAssertion this:assertion ;
                    np:hasProvenance this:provenance ;
                    np:hasPublicationInfo this:pubinfo .
            }
            this:assertion { this:assertion rdfs:label "assertion" . }
            this:provenance { this:assertion rdfs:label "provenance" . }
            this:pubinfo { this: rdfs:label "pubinfo" . }
            """;

    @Test
    void collectsStatementsAndNamespaces() throws Exception {
        NanopubRdfHandler handler = new NanopubRdfHandler();
        RDFParser parser = NanopubUtils.getParser(RDFFormat.TRIG);
        parser.setRDFHandler(handler);
        parser.parse(new StringReader(NANOPUB_TRIG));

        Nanopub nanopub = handler.getNanopub();

        assertEquals("https://example.org/np1#", nanopub.getUri().stringValue());
        assertEquals(7, nanopub.getTripleCount());
        assertEquals("http://www.nanopub.org/nschema#", ((NanopubWithNs) nanopub).getNamespace("np"));
    }

    @Test
    void refusesToBuildANanopubBeforeTheDocumentIsFinished() {
        NanopubRdfHandler handler = new NanopubRdfHandler();
        handler.startRDF();

        RuntimeException ex = assertThrows(RuntimeException.class, handler::getNanopub);
        assertEquals("No complete RDF document received", ex.getMessage());
    }

}
