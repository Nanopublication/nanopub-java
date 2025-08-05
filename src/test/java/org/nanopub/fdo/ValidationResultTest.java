package org.nanopub.fdo;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationResultTest {

    @Test
    void setShacleValidationException() {
        ValidationResult validationResult = new ValidationResult();
        RepositoryException exception = new RepositoryException("Test exception");
        validationResult.setShacleValidationException(exception);
        assertEquals(exception, validationResult.getShacleValidationException());
    }

    @Test
    void getShacleValidationException() {
        ValidationResult validationResult = new ValidationResult();
        assertNull(validationResult.getShacleValidationException());

        RepositoryException exception = new RepositoryException("Test exception");
        validationResult.setShacleValidationException(exception);
        assertEquals(exception, validationResult.getShacleValidationException());
    }

    @Test
    void isValid() {
        ValidationResult validationResult = new ValidationResult();
        assertTrue(validationResult.isValid());

        RepositoryException exception = new RepositoryException("Test exception");
        validationResult.setShacleValidationException(exception);
        assertFalse(validationResult.isValid());
    }

}