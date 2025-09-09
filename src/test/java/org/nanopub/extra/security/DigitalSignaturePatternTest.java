package org.nanopub.extra.security;

import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class DigitalSignaturePatternTest {

    private DigitalSignaturePattern pattern = new DigitalSignaturePattern();

    @Test
    void getPatternInfoUrl() throws MalformedURLException, URISyntaxException {
        URL url = pattern.getPatternInfoUrl();
        assertNotNull(url);
    }

    @Test
    void testValidSignature() throws MalformedNanopubException, IOException {
        Nanopub np = readValidNanopub();

        assertTrue(pattern.isCorrectlyUsedBy(np));
        assertTrue(pattern.appliesTo(np));
        assertEquals("Valid digital signature", pattern.getDescriptionFor(np));
    }

    private Nanopub readValidNanopub() throws MalformedNanopubException, IOException {
        File file = new File(this.getClass().getResource("/testsuite/valid/signed/example6.trig").getFile());
        return new NanopubImpl(file);
    }

    @Test
    void testInvalidSignature() throws MalformedNanopubException, IOException {
        Nanopub np = readInvalidRsaNanopub();

        assertTrue(pattern.appliesTo(np));
        assertFalse(pattern.isCorrectlyUsedBy(np));
        assertEquals("Digital signature is not valid", pattern.getDescriptionFor(np));
    }

    private Nanopub readInvalidRsaNanopub() throws MalformedNanopubException, IOException {
        File file = new File(this.getClass().getResource("/testsuite/invalid/signed/simple1-invalid-rsa.trig").getFile());
        return new NanopubImpl(file);
    }

    @Test
    void testInvalidDsaSignature() throws MalformedNanopubException, IOException {
        Nanopub np = readInvalidDsaNanopub();

        assertTrue(pattern.appliesTo(np));
        assertFalse(pattern.isCorrectlyUsedBy(np));
        assertEquals("Digital signature is not valid", pattern.getDescriptionFor(np));
    }

    private Nanopub readInvalidDsaNanopub() throws MalformedNanopubException, IOException {
        File file = new File(this.getClass().getResource("/testsuite/invalid/signed/simple1-invalid-dsa.trig").getFile());
        return new NanopubImpl(file);
    }

    @Test
    void testTrustyNanopub() throws MalformedNanopubException, IOException {
        Nanopub np = readTrustyNanopub();

        assertFalse(pattern.appliesTo(np));
        assertFalse(pattern.isCorrectlyUsedBy(np));
        assertEquals("Digital signature is not valid", pattern.getDescriptionFor(np));
    }

    private Nanopub readTrustyNanopub() throws MalformedNanopubException, IOException {
        File file = new File(this.getClass().getResource("/testsuite/invalid/trusty/trusty1.trig").getFile());
        return new NanopubImpl(file);
    }

}