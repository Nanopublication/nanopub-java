package org.nanopub.extra.security;

import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteEntry;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class DigitalSignaturePatternTest {

    private final DigitalSignaturePattern pattern = new DigitalSignaturePattern();

    @Test
    void getPatternInfoUrl() throws MalformedURLException, URISyntaxException {
        URL url = pattern.getPatternInfoUrl();
        assertNotNull(url);
    }

    @Test
    void testValidSignature() throws MalformedNanopubException, IOException {
        String npUri = "https://w3id.org/np/RAl53C75tDbAoDF0RZzKu1DUVtJbWnV2w9UdHXw-oBmOw";
        Nanopub np = fetchNanopub(npUri);

        assertTrue(pattern.isCorrectlyUsedBy(np));
        assertTrue(pattern.appliesTo(np));
        assertEquals("Valid digital signature", pattern.getDescriptionFor(np));
    }

    @Test
    void testInvalidSignature() throws MalformedNanopubException, IOException {
        Nanopub np = readInvalidRsaNanopub();

        assertTrue(pattern.appliesTo(np));
        assertFalse(pattern.isCorrectlyUsedBy(np));
        assertEquals("Digital signature is not valid", pattern.getDescriptionFor(np));
    }

    private Nanopub readInvalidRsaNanopub() throws MalformedNanopubException, IOException {
        String npUri = "http://example.org/nanopub-validator-example/RAeUPiCKlke8Pw9wYbqIESyBqFJM5UDSkx4uF9kkRfCh0";
        TestSuiteEntry entry = NanopubTestSuite.getLatest()
                .getByNanopubUri(npUri).getFirst();
        return new NanopubImpl(entry.toFile());
    }

    @Test
    void testInvalidDsaSignature() throws MalformedNanopubException, IOException {
        String npUri = "http://example.org/nanopub-validator-example/RAuryDo2ZM4ezVvol4IfLUE6nYlLIjojxjZhooRcy-eUY";
        Nanopub np = fetchNanopub(npUri);

        assertTrue(pattern.appliesTo(np));
        assertFalse(pattern.isCorrectlyUsedBy(np));
        assertEquals("Digital signature is not valid", pattern.getDescriptionFor(np));
    }

    @Test
    void testTrustyNanopub() throws MalformedNanopubException, IOException {
        String npUri = "http://example.org/nanopub-validator-example/RAPpJU5UOB4pavfWyk7FE3WQiam5yBpmIlviAQWtBSC4M";
        Nanopub np = fetchNanopub(npUri);

        assertFalse(pattern.appliesTo(np));
        assertFalse(pattern.isCorrectlyUsedBy(np));
        assertEquals("Digital signature is not valid", pattern.getDescriptionFor(np));
    }

    private Nanopub fetchNanopub(String npUri) throws MalformedNanopubException, IOException {
        TestSuiteEntry entry = NanopubTestSuite.getLatest()
                .getByNanopubUri(npUri)
                .getFirst();
        return new NanopubImpl(entry.toFile());
    }

}