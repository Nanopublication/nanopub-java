package org.nanopub;

import org.junit.jupiter.api.Test;
import org.nanopub.UriSchemes.Position;

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
    void isAllowedUriScheme_httpUri_allowedEverywhere() {
        for (Position position : Position.values()) {
            assertTrue(UriSchemes.isAllowedUriScheme("https://example.org/thing", position), position.name());
            assertTrue(UriSchemes.isAllowedUriScheme("http://example.org/thing", position), position.name());
        }
    }

    @Test
    void isAllowedUriScheme_decentralizedUri_allowedForSubjectAndObject() {
        for (String uri : new String[]{
                "ipfs://bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi",
                "ipns://k51qzi5uqu5dlvj2baxnqndepeb86cbk3ng7n3i46uzyxzyqj2xjonzllnv0v8",
                "did:plc:ewvi7nxzyoun6zhxrhs64oiz",
                "did:web:example.org",
                "did:key:z6MkhaXgBZDvotDkL5257faiztiGiC2QtKLGpbnnEGta2doK",
                "at://did:plc:ewvi7nxzyoun6zhxrhs64oiz/app.bsky.feed.post/3juvq5gxvgt2b",
        }) {
            assertTrue(UriSchemes.isAllowedUriScheme(uri, Position.SUBJECT), uri);
            assertTrue(UriSchemes.isAllowedUriScheme(uri, Position.OBJECT), uri);
            assertFalse(UriSchemes.isAllowedUriScheme(uri, Position.PREDICATE), uri);
            assertFalse(UriSchemes.isAllowedUriScheme(uri, Position.NANOPUB_URI), uri);
        }
    }

    @Test
    void isAllowedUriScheme_unknownScheme_notAllowedAnywhere() {
        for (Position position : Position.values()) {
            assertFalse(UriSchemes.isAllowedUriScheme("ftp://example.org/thing", position), position.name());
            assertFalse(UriSchemes.isAllowedUriScheme("urn:uuid:1b0e6d5c-1e5b-4a2f-9f4a-3f4e9a5b6c7d", position), position.name());
        }
    }

    @Test
    void isAllowedUriScheme_relativeUri_notAllowedAnywhere() {
        for (Position position : Position.values()) {
            assertFalse(UriSchemes.isAllowedUriScheme("example.org/thing", position), position.name());
        }
    }

    @Test
    void isAllowedUriScheme_null_notAllowedAnywhere() {
        for (Position position : Position.values()) {
            assertFalse(UriSchemes.isAllowedUriScheme(null, position), position.name());
        }
    }

    // -- getAllowedSchemes --

    @Test
    void getAllowedSchemes_predicateAndNanopubUri_areHttpOnly() {
        assertEquals(UriSchemes.HTTP_SCHEMES, UriSchemes.getAllowedSchemes(Position.PREDICATE));
        assertEquals(UriSchemes.HTTP_SCHEMES, UriSchemes.getAllowedSchemes(Position.NANOPUB_URI));
    }

    @Test
    void getAllowedSchemes_subjectAndObject_areTheWiderSet() {
        assertEquals(UriSchemes.RESOURCE_SCHEMES, UriSchemes.getAllowedSchemes(Position.SUBJECT));
        assertEquals(UriSchemes.RESOURCE_SCHEMES, UriSchemes.getAllowedSchemes(Position.OBJECT));
        assertTrue(UriSchemes.RESOURCE_SCHEMES.containsAll(UriSchemes.HTTP_SCHEMES));
    }

}
