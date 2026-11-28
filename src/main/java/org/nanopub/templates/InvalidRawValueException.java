package org.nanopub.templates;

/**
 * Thrown where a raw value cannot be an RDF value for a position at all, with the kind of
 * problem it has, so that {@link PlaceholderMatcher#match(RawValue, BindingContext)} can report
 * it as a violation.
 */
final class InvalidRawValueException extends IllegalArgumentException {

    private final Violation.Kind kind;

    InvalidRawValueException(Violation.Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    Violation.Kind getKind() {
        return kind;
    }

}
