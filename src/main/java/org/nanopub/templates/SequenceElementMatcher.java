package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A sequence-element placeholder, which stands for the membership property of its position
 * in the sequence: {@code rdf:_1} for the first repetition of its statement, {@code rdf:_2}
 * for the second, and so on. The repetition is read off the {@code __N} suffix of the
 * placeholder IRI, where a missing suffix stands for the first one.
 */
final class SequenceElementMatcher extends PlaceholderMatcher {

    private static final Pattern REPETITION_SUFFIX = Pattern.compile("^.*__([0-9]+)$");

    private final IRI element;

    SequenceElementMatcher(Template template, IRI iri) {
        super(template, iri);
        Matcher m = REPETITION_SUFFIX.matcher(iri.stringValue());
        int number = m.matches() ? Integer.parseInt(m.group(1)) + 1 : 1;
        element = vf.createIRI(RDF.NAMESPACE + "_" + number);
    }

    @Override
    public boolean isPlaceholder() {
        return true;
    }

    @Override
    void check(Value value, BindingContext context, List<Violation> violations) {
        if (!(value instanceof IRI)) {
            violations.add(blocking(value, Violation.Kind.WRONG_KIND, describe(value) + " is not an IRI"));
        } else if (!element.equals(value)) {
            violations.add(blocking(value, Violation.Kind.DIFFERENT_VALUE, describe(value) + " is not " + describe(element)));
        }
    }

    @Override
    public Value resolve(BindingContext context) {
        return element;
    }

}
