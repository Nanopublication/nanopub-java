package org.nanopub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UriSchemesTest {

    // -- getScheme --

    @Test
    void getScheme_httpUri_returnsScheme() {
        assertEquals("https", UriSchemes.getScheme("https://example.org/thing"));
        assertEquals("http", UriSchemes.getScheme("http://example.org/thing"));
    }

    @Test
    void getScheme_opaqueUri_returnsScheme() {
        assertEquals("did", UriSchemes.getScheme("did:plc:ewvi7nxzyoun6zhxrhs64oiz"));
    }

    @Test
    void getScheme_upperCaseScheme_returnsLowerCase() {
        assertEquals("https", UriSchemes.getScheme("HTTPS://example.org/thing"));
    }

    @Test
    void getScheme_schemeWithSpecialCharacters_returnsScheme() {
        assertEquals("view-source", UriSchemes.getScheme("view-source:http://example.org/"));
        assertEquals("a+b.c9", UriSchemes.getScheme("a+b.c9:rest"));
    }

    @Test
    void getScheme_noColon_returnsNull() {
        assertNull(UriSchemes.getScheme("example.org/thing"));
    }

    @Test
    void getScheme_emptyScheme_returnsNull() {
        assertNull(UriSchemes.getScheme(":relative"));
    }

    @Test
    void getScheme_schemeStartingWithDigit_returnsNull() {
        assertNull(UriSchemes.getScheme("1http://example.org/"));
    }

    @Test
    void getScheme_invalidCharacterInScheme_returnsNull() {
        assertNull(UriSchemes.getScheme("ht tp://example.org/"));
        assertNull(UriSchemes.getScheme("http_s://example.org/"));
    }

    @Test
    void getScheme_null_returnsNull() {
        assertNull(UriSchemes.getScheme(null));
    }

    @Test
    void getScheme_emptyString_returnsNull() {
        assertNull(UriSchemes.getScheme(""));
    }

    // -- isAllowedUriScheme --

    @Test
    void isAllowedUriScheme_httpUri_isAllowed() {
        assertTrue(UriSchemes.isAllowedUriScheme("https://example.org/thing"));
        assertTrue(UriSchemes.isAllowedUriScheme("http://example.org/thing"));
    }

    @Test
    void isAllowedUriScheme_decentralizedUri_isAllowed() {
        for (String uri : new String[]{
                "ipfs://bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi",
                "ipns://k51qzi5uqu5dlvj2baxnqndepeb86cbk3ng7n3i46uzyxzyqj2xjonzllnv0v8",
                "did:plc:ewvi7nxzyoun6zhxrhs64oiz",
                "did:web:example.org",
                "did:key:z6MkhaXgBZDvotDkL5257faiztiGiC2QtKLGpbnnEGta2doK",
                "at://did:plc:ewvi7nxzyoun6zhxrhs64oiz/app.bsky.feed.post/3juvq5gxvgt2b",
        }) {
            assertTrue(UriSchemes.isAllowedUriScheme(uri), uri);
        }
    }

    @Test
    void isAllowedUriScheme_schemeIsCaseInsensitive_isAllowed() {
        assertTrue(UriSchemes.isAllowedUriScheme("DID:plc:ewvi7nxzyoun6zhxrhs64oiz"));
    }

    @Test
    void isAllowedUriScheme_unknownScheme_isNotAllowed() {
        assertFalse(UriSchemes.isAllowedUriScheme("ftp://example.org/thing"));
        assertFalse(UriSchemes.isAllowedUriScheme("urn:uuid:1b0e6d5c-1e5b-4a2f-9f4a-3f4e9a5b6c7d"));
    }

    @Test
    void isAllowedUriScheme_schemeLookalike_isNotAllowed() {
        // must not be treated as a "did:" URI by prefix matching
        assertFalse(UriSchemes.isAllowedUriScheme("didsomething://example.org/thing"));
    }

    @Test
    void isAllowedUriScheme_relativeUri_isNotAllowed() {
        assertFalse(UriSchemes.isAllowedUriScheme("example.org/thing"));
    }

    @Test
    void isAllowedUriScheme_null_isNotAllowed() {
        assertFalse(UriSchemes.isAllowedUriScheme(null));
    }

}
