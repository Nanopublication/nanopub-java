package org.nanopub.fdo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FdoNotFoundExceptionTest {

    @Test
    void exceptionWithMessage() {
        FdoNotFoundException exception = new FdoNotFoundException("FDO not found");
        assertEquals("FDO not found", exception.getMessage());
    }

    @Test
    void exceptionWithCause() {
        Exception cause = new Exception("Root cause");
        FdoNotFoundException exception = new FdoNotFoundException(cause);
        assertEquals(cause, exception.getCause());
    }

    @Test
    void exceptionWithNullMessage() {
        FdoNotFoundException exception = new FdoNotFoundException((String) null);
        assertNull(exception.getMessage());
    }

    @Test
    void exceptionWithNullCause() {
        FdoNotFoundException exception = new FdoNotFoundException((Exception) null);
        assertNull(exception.getCause());
    }

}