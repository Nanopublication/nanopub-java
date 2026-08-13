package org.nanopub.jelly;

import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.utils.TestUtils;

import static org.junit.jupiter.api.Assertions.*;

class MaybeNanopubTest {

    @Test
    void holdsANanopubWithoutACounter() throws Exception {
        Nanopub nanopub = TestUtils.createNanopub();

        MaybeNanopub maybe = new MaybeNanopub(nanopub);

        assertTrue(maybe.isSuccess());
        assertFalse(maybe.isFailure());
        assertEquals(nanopub, maybe.getNanopub());
        assertEquals(-1, maybe.getCounter());
        assertNull(maybe.getException());
    }

    @Test
    void holdsANanopubWithACounter() throws Exception {
        Nanopub nanopub = TestUtils.createNanopub();

        MaybeNanopub maybe = new MaybeNanopub(nanopub, 42);

        assertTrue(maybe.isSuccess());
        assertFalse(maybe.isFailure());
        assertEquals(nanopub, maybe.getNanopub());
        assertEquals(42, maybe.getCounter());
        assertNull(maybe.getException());
    }

    @Test
    void holdsTheFailure() {
        MalformedNanopubException ex = new MalformedNanopubException("not a nanopub");

        MaybeNanopub maybe = new MaybeNanopub(ex);

        assertFalse(maybe.isSuccess());
        assertTrue(maybe.isFailure());
        assertNull(maybe.getNanopub());
        assertEquals(-1, maybe.getCounter());
        assertEquals(ex, maybe.getException());
    }

}
