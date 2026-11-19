package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.nanopub.UriSchemes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A value placeholder, which takes an IRI or a literal of any kind. As a raw value, a
 * literal is written as {@link LiteralSerialization} does it, and an IRI in full or as a
 * plain name that is minted within the nanopublication.
 */
final class ValueMatcher extends PlaceholderMatcher {

    ValueMatcher(Template template, IRI iri) {
        super(template, iri);
    }

    @Override
    public boolean isPlaceholder() {
        return true;
    }

    @Override
    void check(Value value, BindingContext context, List<Violation> violations) {
        if (!(value instanceof IRI)) return;
        String full = value.stringValue();
        String localName = context.getLocalName(value);
        if (localName != null && IriUtils.isPlainName(localName)) return;
        if (!IriUtils.isWellFormedUri(full)) {
            violations.add(blocking(value, Violation.Kind.MALFORMED_IRI, "The IRI <" + full + "> of '" + getFieldLabel() + "' is not well-formed"));
        }
        if (!IriUtils.isUriValue(full)) {
            violations.add(blocking(value, Violation.Kind.SCHEME_NOT_ALLOWED, "The IRI <" + full + "> of '" + getFieldLabel()
                    + "' has a scheme that is not allowed here; use one of: "
                    + UriSchemes.ALLOWED_SCHEMES.stream().sorted().collect(Collectors.joining(", "))));
        }
    }

    @Override
    public Value toValue(RawValue raw, BindingContext context) {
        String text = raw.text();
        if (text.isEmpty()) return null;
        if (text.startsWith("\"")) return LiteralSerialization.parse(text);
        if (IriUtils.isPlainName(text)) return context.mint(text);
        return createIri(text);
    }

    @Override
    public RawValue toRaw(Value value, BindingContext context) {
        if (value instanceof Literal literal) return new RawValue(LiteralSerialization.serialize(literal));
        return shortestRaw(value, context, java.util.Collections.singletonList(context.getLocalName(value)));
    }

}
