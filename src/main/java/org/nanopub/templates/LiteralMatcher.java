package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.nanopub.extra.services.SparqlSyntax;

import java.util.List;

/**
 * A placeholder that takes a literal: a literal, long literal or language-tagged literal
 * placeholder, with whatever datatype or language tag the template declares for it.
 * <p>
 * Any literal that matches the regular expression of the placeholder can be bound, and for a
 * placeholder that leaves the language open, any literal with one of the language tags it
 * allows. A datatype or language tag other than the declared one, and a lexical form that is
 * not valid for the datatype, are reported: such a value can be read, but filling in the
 * template would not have produced it.
 */
final class LiteralMatcher extends PlaceholderMatcher {

    private static final String LANGUAGE_TAG_PATTERN = "[a-zA-Z]{2,8}(-[0-9a-zA-Z]{1,8})*";

    private final IRI key;

    LiteralMatcher(Template template, IRI iri) {
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
        String text = literal.getLabel();
        checkRegex(key, text, value, violations);
        String tag = literal.getLanguage().map(Literals::normalizeLanguageTag).orElse(null);
        IRI declaredDatatype = template.getDatatype(key);
        if (template.isLanguageTagSelectable(key)) {
            List<String> possibleTags = template.getPossibleLanguageTags(key);
            if (tag == null) {
                violations.add(blocking(value, Violation.Kind.LANGUAGE_TAG, "The value of '" + getFieldLabel() + "' has no language tag"));
            } else if (possibleTags != null && !possibleTags.contains(tag)) {
                violations.add(blocking(value, Violation.Kind.LANGUAGE_TAG, "Language '" + tag + "' is not among the allowed languages for '" + getFieldLabel() + "'"));
            }
        } else if (template.getLanguageTag(key) != null) {
            String declaredTag = template.getLanguageTag(key);
            if (!declaredTag.equals(tag)) {
                violations.add(reported(value, Violation.Kind.LANGUAGE_TAG, "The value of '" + getFieldLabel() + "' has "
                        + (tag == null ? "no language tag" : "the language tag '" + tag + "'") + " instead of '" + declaredTag + "'"));
            }
        } else if (tag != null) {
            violations.add(reported(value, Violation.Kind.LANGUAGE_TAG, "The value of '" + getFieldLabel() + "' has the language tag '" + tag + "', but none is declared for it"));
        } else {
            IRI expectedDatatype = declaredDatatype != null ? declaredDatatype : XSD.STRING;
            if (!expectedDatatype.equals(literal.getDatatype())) {
                violations.add(reported(value, Violation.Kind.DATATYPE, "The value of '" + getFieldLabel() + "' is of type "
                        + getDatatypeLabel(literal.getDatatype()) + " instead of " + getDatatypeLabel(expectedDatatype)));
            }
        }
        IRI datatype = (declaredDatatype != null && tag == null) ? declaredDatatype : literal.getDatatype();
        if (XMLDatatypeUtil.isBuiltInDatatype(datatype) && !XMLDatatypeUtil.isValidValue(text, datatype)) {
            violations.add(reported(value, Violation.Kind.ILL_TYPED, "Value '" + text + "' of '" + getFieldLabel() + "' is not a valid " + getDatatypeLabel(datatype)));
        }
        if (template.isSparqlPlaceholder(key)) {
            String problem = SparqlSyntax.getSyntaxError(text);
            if (problem != null) {
                violations.add(reported(value, Violation.Kind.INVALID_SPARQL, "The value of '" + getFieldLabel() + "' is not valid SPARQL: " + problem));
            }
        }
    }

    @Override
    public Value toValue(RawValue raw, BindingContext context) {
        String text = raw.text();
        if (text.isEmpty()) return null;
        String declaredTag = template.getLanguageTag(key);
        if (template.isLanguageTagSelectable(key)) {
            // The declared tag of such a placeholder is the one taken if none is chosen.
            String tag = raw.languageTag() != null ? raw.languageTag() : declaredTag;
            if (tag == null) throw new InvalidRawValueException(Violation.Kind.LANGUAGE_TAG, "'" + getFieldLabel() + "' needs a language tag");
            if (!tag.matches(LANGUAGE_TAG_PATTERN)) {
                throw new InvalidRawValueException(Violation.Kind.LANGUAGE_TAG, "'" + tag + "' is not a valid language tag for '" + getFieldLabel() + "'");
            }
            return vf.createLiteral(text, Literals.normalizeLanguageTag(tag));
        }
        if (raw.languageTag() != null && !Literals.normalizeLanguageTag(raw.languageTag()).equals(declaredTag)) {
            throw new InvalidRawValueException(Violation.Kind.LANGUAGE_TAG, "No language can be chosen for '" + getFieldLabel() + "'");
        }
        if (declaredTag != null) return vf.createLiteral(text, declaredTag);
        IRI datatype = template.getDatatype(key);
        if (datatype != null) return vf.createLiteral(text, datatype);
        return vf.createLiteral(text);
    }

    @Override
    public RawValue toRaw(Value value, BindingContext context) {
        if (!(value instanceof Literal literal)) return new RawValue(value.stringValue());
        String tag = null;
        if (template.isLanguageTagSelectable(key)) tag = literal.getLanguage().map(Literals::normalizeLanguageTag).orElse(null);
        return new RawValue(literal.getLabel(), tag);
    }

}
