package org.nanopub.extra.index;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.utils.TestUtils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class NanopubIndexPatternTest {

    @Test
    void getNameDoesNotReturnNull() {
        NanopubIndexPattern pattern = new NanopubIndexPattern();
        assertNotNull(pattern.getName());
    }

    @Test
    void appliesToReturnsTrueForValidIndexNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        try (var mockedIndexUtils = Mockito.mockStatic(IndexUtils.class)) {
            mockedIndexUtils.when(() -> IndexUtils.isIndex(nanopub)).thenReturn(true);
            NanopubIndexPattern pattern = new NanopubIndexPattern();
            assertTrue(pattern.appliesTo(nanopub));
        }
    }

    @Test
    void appliesToReturnsFalseForNonIndexNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        try (var mockedIndexUtils = Mockito.mockStatic(IndexUtils.class)) {
            mockedIndexUtils.when(() -> IndexUtils.isIndex(nanopub)).thenReturn(false);
            NanopubIndexPattern pattern = new NanopubIndexPattern();
            assertFalse(pattern.appliesTo(nanopub));
        }
    }

    @Test
    void getPatternInfoUrlReturnsValidUrl() throws MalformedURLException, URISyntaxException {
        NanopubIndexPattern pattern = new NanopubIndexPattern();
        URL url = pattern.getPatternInfoUrl();
        assertNotNull(url);
    }

}