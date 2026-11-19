package org.nanopub.templates;

import org.eclipse.rdf4j.model.Value;

import java.util.List;

/**
 * A position the template defines no value for, such as a statement without
 * {@code rdf:subject} (see {@link Template#getStatementErrors()}). Nothing matches it.
 */
final class MissingValueMatcher extends PlaceholderMatcher {

    MissingValueMatcher(Template template) {
        super(template, null);
    }

    @Override
    void check(Value value, BindingContext context, List<Violation> violations) {
        violations.add(blocking(value, Violation.Kind.NO_VALUE_DEFINED, "The template defines no value for this position"));
    }

}
