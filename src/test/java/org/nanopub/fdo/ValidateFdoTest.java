package org.nanopub.fdo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidateFdoTest {

    @Test
    void validateValidFdo() throws Exception {
        String id = "21.T11966/82045bd97a0acce88378";
        FdoRecord record = RetrieveFdo.resolveId(id);
        assertTrue(ValidateFdo.validate(record).isValid());
    }

    @Test
    void validateInvalidFdo() throws Exception {
        String id = "21.T11967/39b0ec87d17a4856c5f7";
        FdoRecord record = RetrieveFdo.resolveId(id);
        assertFalse(ValidateFdo.validate(record).isValid());
    }

}