package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import java.util.List;

/**
 * A date placeholder, which takes a point in time at one of the precisions it accepts (see
 * {@link Template#getDatePrecisions(IRI)}), as a literal typed by that precision.
 */
final class DateMatcher extends PlaceholderMatcher {

    private final IRI key;

    DateMatcher(Template template, IRI iri) {
        super(template, iri);
        this.key = iri;
    }

    @Override
    public boolean isPlaceholder() {
        return true;
    }

    @Override
    void check(Value value, BindingContext context, List<Violation> violations) {
        if (!(value instanceof Literal literal)) {
            violations.add(blocking(value, Violation.Kind.WRONG_KIND, "The value " + describe(value) + " of '" + getFieldLabel() + "' is not a literal"));
            return;
        }
        List<DatePrecision> accepted = template.getDatePrecisions(key);
        DatePrecision precision = DatePrecision.of(literal.getDatatype());
        DateValue dateValue = DateValue.parse(literal.getLabel());
        if (precision == null || !accepted.contains(precision) || dateValue == null || dateValue.getPrecision() != precision) {
            violations.add(blocking(value, Violation.Kind.DATE_PRECISION, describe(value) + " is not " + DatePrecision.describe(accepted) + " for '" + getFieldLabel() + "'"));
        }
        checkRegex(key, literal.getLabel(), value, violations);
    }

    @Override
    public Value toValue(RawValue raw, BindingContext context) {
        if (raw.isEmpty()) return null;
        DateValue dateValue = DateValue.parse(raw.text());
        if (dateValue == null) {
            throw new IllegalArgumentException("'" + raw.text() + "' is not " + DatePrecision.describe(DatePrecision.ALL) + " for '" + getFieldLabel() + "'");
        }
        return vf.createLiteral(dateValue.toLexical(), dateValue.getPrecision().getDatatype());
    }

}
