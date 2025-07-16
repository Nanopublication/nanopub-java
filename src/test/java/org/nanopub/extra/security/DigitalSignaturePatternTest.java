package org.nanopub.extra.security;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DigitalSignaturePatternTest {

    @Test
    void getPatternInfoUrl() throws MalformedURLException {
        DigitalSignaturePattern pattern = new DigitalSignaturePattern();
        URL url = pattern.getPatternInfoUrl();
        assertNotNull(url);
    }

}