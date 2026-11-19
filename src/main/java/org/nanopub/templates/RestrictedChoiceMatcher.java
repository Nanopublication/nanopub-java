package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A restricted-choice placeholder, which takes one of the values the template lists for it
 * ({@code nt:possibleValue}, {@code nt:possibleValuesFrom}). A listed value that is itself a
 * placeholder stands for whatever is bound to that placeholder, in any of its repetitions.
 * <p>
 * A value outside a list of fixed values cannot be bound. Where the list refers to other
 * placeholders, the value can be bound and is reported if it is not among the options, as
 * those may be bound only later; {@link BindingContext#getViolations()} checks it again.
 */
final class RestrictedChoiceMatcher extends PlaceholderMatcher {

    private final IRI key;

    RestrictedChoiceMatcher(Template template, IRI iri) {
        super(template, iri);
        this.key = iri;
    }

    @Override
    public boolean isPlaceholder() {
        return true;
    }

    @Override
    void check(Value value, BindingContext context, List<Violation> violations) {
        List<IRI> referredPlaceholders = new ArrayList<>();
        boolean isOption = false;
        for (Value option : template.getPossibleValues(key)) {
            if (option instanceof IRI optionIri && template.isPlaceholder(optionIri)) {
                referredPlaceholders.add(optionIri);
            } else if (option.equals(value)) {
                isOption = true;
            } else if (option instanceof IRI optionIri && value instanceof IRI valueIri
                    && IriUtils.hasArtifactCodeMarker(optionIri.stringValue())
                    && IriUtils.matchesWithArtifactCode(optionIri.stringValue(), valueIri)) {
                isOption = true;
            }
        }
        if (isOption) return;
        // A placeholder among the options never stands for a literal.
        if (referredPlaceholders.isEmpty() || value instanceof Literal) {
            violations.add(blocking(value, Violation.Kind.NOT_A_POSSIBLE_VALUE, describe(value) + " is not among the values to choose from for '" + getFieldLabel() + "'"));
            return;
        }
        for (Map.Entry<IRI, Value> binding : context.getBindings().entrySet()) {
            if (binding.getValue().equals(value) && referredPlaceholders.contains(template.getTemplateIri(binding.getKey()))) return;
        }
        violations.add(reported(value, Violation.Kind.NOT_A_POSSIBLE_VALUE, describe(value) + " is not among the values to choose from for '" + getFieldLabel() + "'"));
    }

    @Override
    public Value toValue(RawValue raw, BindingContext context) {
        String text = raw.text();
        if (text.isEmpty()) return null;
        if (text.startsWith("\"")) return LiteralSerialization.parse(text);
        return toIri(key, text, context);
    }

    @Override
    public RawValue toRaw(Value value, BindingContext context) {
        if (value instanceof Literal literal) return new RawValue(LiteralSerialization.serialize(literal));
        List<String> candidates = new ArrayList<>();
        String full = value.stringValue();
        String prefix = getMintingPrefix(key, context);
        if (!prefix.isEmpty() && full.startsWith(prefix)) candidates.add(full.substring(prefix.length()));
        candidates.add(context.getLocalName(value));
        return shortestRaw(value, context, candidates);
    }

}
