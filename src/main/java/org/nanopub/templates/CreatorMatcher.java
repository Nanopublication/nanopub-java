package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.List;

/**
 * A position the template fills with {@code nt:CREATOR}, which stands for a creator of the
 * nanopublication: the same one wherever it occurs.
 */
final class CreatorMatcher extends PlaceholderMatcher {

    CreatorMatcher(Template template, IRI iri) {
        super(template, iri);
    }

    @Override
    public boolean isPlaceholder() {
        return true;
    }

    @Override
    void check(Value value, BindingContext context, List<Violation> violations) {
        if (!(value instanceof IRI)) {
            violations.add(blocking(value, Violation.Kind.WRONG_KIND, describe(value) + " is not an IRI"));
        } else if (!context.getCreators().isEmpty() && !context.getCreators().contains(value)) {
            violations.add(blocking(value, Violation.Kind.NOT_A_CREATOR, describe(value) + " is not a creator of the nanopublication"));
        }
    }

    @Override
    public Value toValue(RawValue raw, BindingContext context) {
        if (raw.isEmpty()) return resolve(context);
        return createIri(raw.text());
    }

}
