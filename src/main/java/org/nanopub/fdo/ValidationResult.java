package org.nanopub.fdo;

import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * ValidationResult is used to encapsulate the result of a SHACL validation process.
 */
public class ValidationResult {

    private RepositoryException shacleValidationException;

    /**
     * Returns the exception that occurred during SHACL validation, if any.
     *
     * @return the exception, or null if validation was successful.
     */
    public RepositoryException getShacleValidationException() {
        return shacleValidationException;
    }

    /**
     * Sets the exception that occurred during SHACL validation.
     *
     * @param shacleValidationException the exception to set, or null if validation was successful.
     */
    public void setShacleValidationException(RepositoryException shacleValidationException) {
        this.shacleValidationException = shacleValidationException;
    }

    /**
     * Checks if the validation was successful.
     *
     * @return true if there were no validation exceptions, false otherwise.
     */
    public boolean isValid() {
        return shacleValidationException == null;
    }

}
