package org.nanopub.fdo;

import org.eclipse.rdf4j.repository.RepositoryException;

public class ValidationResult {

    private RepositoryException shacleValidationException;

    public RepositoryException getShacleValidationException() {
        return shacleValidationException;
    }

    public void setShacleValidationException(RepositoryException shacleValidationException) {
        this.shacleValidationException = shacleValidationException;
    }

    public boolean isValid() {
        return shacleValidationException == null;
    }

}
