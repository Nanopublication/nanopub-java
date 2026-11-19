package org.nanopub.templates;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import java.util.List;

/**
 * A position the template fills with a literal: only that literal matches.
 */
final class FixedLiteralMatcher extends PlaceholderMatcher {

    private final Literal literal;

    FixedLiteralMatcher(Template template, Literal literal) {
        super(template, literal);
        this.literal = literal;
    }

    @Override
    void check(Value value, BindingContext context, List<Violation> violations) {
        if (!(value instanceof Literal)) {
            violations.add(blocking(value, Violation.Kind.WRONG_KIND, describe(value) + " is not a literal"));
        } else if (!literal.equals(value)) {
            violations.add(blocking(value, Violation.Kind.DIFFERENT_VALUE, describe(value) + " is not the literal " + describe(literal)));
        }
    }

    @Override
    public Value resolve(BindingContext context) {
        return literal;
    }

}
