package org.nanopub;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.trusty.TrustyNanopubUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class NanopubImplTest {

    @Test
    void equalsTextBlockWithSameLineSeparator() throws IOException, MalformedNanopubException {
        NanopubImpl nanopub1 = new NanopubImpl(new URL("https://w3id.org/np/RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA"));
        NanopubImpl nanopub2 = new NanopubImpl(new File("src/test/resources/RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA.trig"), RDFFormat.TRIG);

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(nanopub1));
        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(nanopub2));
        assertEquals(nanopub1, nanopub2);
    }

    @Test
    void equalsTextBlockWithDifferentLineSeparator() throws IOException, MalformedNanopubException {
        NanopubImpl nanopub1 = new NanopubImpl(new URL("https://w3id.org/np/RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA"));
        NanopubImpl nanopub2 = new NanopubImpl(new File("src/test/resources/RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA-all-LF.trig"), RDFFormat.TRIG);

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(nanopub1));
        assertFalse(TrustyNanopubUtils.isValidTrustyNanopub(nanopub2));
        assertNotEquals(nanopub1, nanopub2);
    }

}