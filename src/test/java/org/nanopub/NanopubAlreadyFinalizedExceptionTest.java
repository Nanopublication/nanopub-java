package org.nanopub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NanopubAlreadyFinalizedExceptionTest {

    @Test
    void constructorWithMessage() {
        NanopubAlreadyFinalizedException ex = new NanopubAlreadyFinalizedException("my message");
        assertEquals("my message", ex.getMessage());
    }

    @Test
    void constructorWithDefaultMessage() {
        NanopubAlreadyFinalizedException ex = new NanopubAlreadyFinalizedException();
        assertEquals("The nanopublication is already finalized.", ex.getMessage());
    }

}
