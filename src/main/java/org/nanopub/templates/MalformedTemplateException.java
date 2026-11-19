package org.nanopub.templates;

/**
 * Thrown when a nanopublication cannot be read as a template.
 */
public class MalformedTemplateException extends Exception {

    /**
     * Creates a new exception with the given message.
     *
     * @param message what is wrong with the template
     */
    public MalformedTemplateException(String message) {
        super(message);
    }

}
