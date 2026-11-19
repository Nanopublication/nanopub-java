package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;

/**
 * A position the template fills with an IRI that is not a placeholder. That IRI stands for
 * itself, with these exceptions:
 * <ul>
 *   <li>{@code nt:NANOPUB} and {@code nt:ASSERTION} stand for the nanopublication and its
 *       assertion graph;</li>
 *   <li>a local resource stands for the IRI of the same name in the namespace of the
 *       nanopublication;</li>
 *   <li>an IRI with the artifact-code marker stands for that IRI with the artifact code of
 *       the nanopublication, which it gets when the nanopublication is signed.</li>
 * </ul>
 */
final class FixedIriMatcher extends PlaceholderMatcher {

    private final IRI iri;

    FixedIriMatcher(Template template, IRI iri) {
        super(template, iri);
        this.iri = iri;
    }

    private boolean isLocal() {
        return template.isLocalResource(iri);
    }

    // The name of a local resource: what follows the URI of the template nanopublication.
    private String getLocalName() {
        String s = iri.stringValue();
        String templateNpUri = template.getNanopub().getUri().stringValue();
        if (s.startsWith(templateNpUri) && s.length() > templateNpUri.length()) {
            String name = s.substring(templateNpUri.length());
            if (name.startsWith("/") || name.startsWith("#")) name = name.substring(1);
            if (!name.isEmpty()) return name;
        }
        return IriUtils.getPostfix(s);
    }

    // The IRI this position stands for in a nanopublication that is made now.
    private IRI getExpected(BindingContext context) {
        if (iri.equals(NTEMPLATE.ASSERTION_PLACEHOLDER)) return context.getAssertionUri();
        if (iri.equals(NTEMPLATE.NANOPUB_PLACEHOLDER)) return context.getNanopubUri();
        if (isLocal()) return context.mint(getLocalName());
        if (IriUtils.hasArtifactCodeMarker(iri.stringValue())) return vf.createIRI(IriUtils.toTargetMarker(iri.stringValue()));
        return iri;
    }

    @Override
    void check(Value value, BindingContext context, List<Violation> violations) {
        if (!(value instanceof IRI valueIri)) {
            violations.add(blocking(value, Violation.Kind.WRONG_KIND, describe(value) + " is not an IRI"));
            return;
        }
        IRI expected = getExpected(context);
        if (expected.equals(value)) return;
        if (isLocal()) {
            // An introduced resource keeps its IRI when the nanopublication is superseded, so
            // it may sit in the namespace of an earlier version.
            if (template.isIntroducedResource(iri) && getLocalName().equals(IriUtils.getPostfix(value.stringValue()))) return;
        } else if (IriUtils.hasArtifactCodeMarker(iri.stringValue()) && IriUtils.matchesWithArtifactCode(iri.stringValue(), valueIri)) {
            return;
        }
        violations.add(blocking(value, Violation.Kind.DIFFERENT_VALUE, describe(value) + " is not " + describe(expected)));
    }

    @Override
    public Value resolve(BindingContext context) {
        Value bound = context.get(iri);
        return bound != null ? bound : getExpected(context);
    }

}
