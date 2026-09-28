package org.nanopub.extra.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotEnoughAPIInstancesExceptionTest {

    @Test
    void constructorSetsMessageCorrectly() {
        String message = "Not enough API instances available";
        NotEnoughAPIInstancesException exception = new NotEnoughAPIInstancesException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void constructorWithNullMessageDoesNotThrow() {
        NotEnoughAPIInstancesException exception = new NotEnoughAPIInstancesException(null);
        assertNull(exception.getMessage());
    }

}