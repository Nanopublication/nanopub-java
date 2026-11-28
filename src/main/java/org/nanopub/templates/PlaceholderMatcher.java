package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.nanopub.UriSchemes;
import org.nanopub.vocabulary.NTEMPLATE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Says what a value means for one position of a template statement: the subject, predicate
 * or object, which the template fills with a placeholder or with a fixed IRI or literal.
 * <p>
 * A matcher works in both directions, with the same rules:
 * <ul>
 *   <li>{@link #match(Value, BindingContext)} checks an RDF value found in a
 *       nanopublication against the position;</li>
 *   <li>{@link #toValue(RawValue, BindingContext)} turns a value as a person or a program
 *       gives it into the RDF value for the position, and
 *       {@link #toRaw(Value, BindingContext)} goes back.</li>
 * </ul>
 * A check has two answers. A <em>blocking</em> violation means the value cannot stand in the
 * position at all, which is what decides whether a triple belongs to a statement. Any other
 * violation means the value can stand there but is not what filling in the template would
 * have produced, such as a literal with the wrong datatype.
 * <p>
 * Use {@link #of(Template, Value)} to get the matcher for a position.
 */
public abstract class PlaceholderMatcher implements Serializable {

    static final ValueFactory vf = SimpleValueFactory.getInstance();

    final Template template;
    final Value templateValue;

    PlaceholderMatcher(Template template, Value templateValue) {
        this.template = Objects.requireNonNull(template, "template");
        this.templateValue = templateValue;
    }

    /**
     * Returns the matcher for a position of a template statement.
     *
     * @param template      the template
     * @param templateValue the value the template has in that position, as
     *                      {@link Template#getSubject(IRI)}, {@link Template#getPredicate(IRI)}
     *                      and {@link Template#getObject(IRI)} return it, with a {@code __N}
     *                      suffix if the position is in a repetition; null if the template
     *                      defines none
     * @return the matcher for the kind of value the position takes
     */
    public static PlaceholderMatcher of(Template template, Value templateValue) {
        Objects.requireNonNull(template, "template");
        if (templateValue instanceof Literal literal) return new FixedLiteralMatcher(template, literal);
        if (!(templateValue instanceof IRI iri)) return new MissingValueMatcher(template);
        if (template.isSequenceElementPlaceholder(iri)) return new SequenceElementMatcher(template, iri);
        if (iri.equals(NTEMPLATE.CREATOR_PLACEHOLDER)) return new CreatorMatcher(template, iri);
        if (template.isRootNanopubPlaceholder(iri)) return new IriMatcher(template, iri);
        if (template.isRestrictedChoicePlaceholder(iri)) return new RestrictedChoiceMatcher(template, iri);
        if (template.isUriPlaceholder(iri)) return new IriMatcher(template, iri);
        if (template.isDatePlaceholder(iri)) return new DateMatcher(template, iri);
        if (template.isLiteralPlaceholder(iri)) return new LiteralMatcher(template, iri);
        if (template.isPlaceholder(iri)) return new ValueMatcher(template, iri);
        return new FixedIriMatcher(template, iri);
    }

    /**
     * @return the value the template has in this position; null if it defines none
     */
    public Value getTemplateValue() {
        return templateValue;
    }

    /**
     * Returns the key under which a {@link BindingContext} holds the value of this position.
     *
     * @return the value the template has in this position if it is an IRI, otherwise null
     */
    public IRI getKey() {
        return templateValue instanceof IRI iri ? iri : null;
    }

    /**
     * Whether this position takes a value that is given when the template is filled in,
     * rather than one the template fixes.
     *
     * @return true if the position holds a placeholder
     */
    public boolean isPlaceholder() {
        return false;
    }

    /**
     * Checks a value against this position. The context is not changed; use
     * {@link BindingContext#bind(Value, Value)} to also bind the value.
     *
     * @param value   the RDF value, as found in a nanopublication
     * @param context the nanopublication the value is from and the values bound so far
     * @return the violations of the value, among them one that is blocking if the position
     * is bound to a different value already
     */
    public final MatchResult match(Value value, BindingContext context) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(context, "context");
        List<Violation> violations = new ArrayList<>();
        IRI key = getKey();
        Value bound = key == null ? null : context.get(key);
        if (bound != null && !bound.equals(value)) {
            violations.add(blocking(value, Violation.Kind.ALREADY_BOUND,
                    "'" + getFieldLabel() + "' already has the value " + describe(bound) + ", which differs from " + describe(value)));
        }
        check(value, context, violations);
        return new MatchResult(violations);
    }

    /**
     * Checks a raw value against this position, as a value given when the template is filled
     * in: the RDF value {@link #toValue(RawValue, BindingContext)} makes of it is checked as
     * {@link #match(Value, BindingContext)} checks it. A text that cannot be an RDF value for
     * this position at all, such as a name with a colon in it, gives a blocking violation whose
     * value is null. The context is not changed.
     *
     * @param raw     the raw value
     * @param context the nanopublication the value is for and the values bound so far
     * @return the violations of the value; none for an empty raw value of a placeholder
     */
    public final MatchResult match(RawValue raw, BindingContext context) {
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(context, "context");
        Value value;
        try {
            value = toValue(raw, context);
        } catch (InvalidRawValueException ex) {
            return new MatchResult(List.of(blocking(null, ex.getKind(), ex.getMessage())));
        }
        if (value == null) return new MatchResult(List.of());
        return match(value, context);
    }

    abstract void check(Value value, BindingContext context, List<Violation> violations);

    /**
     * Returns the RDF value this position stands for under the bindings of the context.
     *
     * @param context the nanopublication and the values bound so far
     * @return the value, or null if the position holds a placeholder nothing is bound to
     */
    public Value resolve(BindingContext context) {
        IRI key = getKey();
        return key == null ? null : context.get(key);
    }

    /**
     * Turns a raw value into the RDF value for this position. What the text of the raw value
     * means depends on the kind of position:
     * <ul>
     *   <li>for a placeholder that takes an IRI, the IRI in full, or the part after the
     *       prefix of the placeholder ({@code nt:hasPrefix}), or a plain name that is minted
     *       within the nanopublication; for an auto-escape placeholder, the text that is
     *       escaped to form the IRI;</li>
     *   <li>for a placeholder that takes a literal, the text of the literal, with the
     *       language tag of the raw value if the placeholder leaves the language open;</li>
     *   <li>for a date placeholder, the lexical form of the point in time, such as
     *       {@code 2026} or {@code 2026-11-19};</li>
     *   <li>for a value placeholder, an IRI or name as above, or a literal as
     *       {@link LiteralSerialization} writes it.</li>
     * </ul>
     * The value returned is not checked against the constraints of the position; pass it to
     * {@link #match(Value, BindingContext)} for that. For a position with a fixed value, the
     * raw value is ignored.
     *
     * @param raw     the raw value
     * @param context the nanopublication the value is for
     * @return the RDF value, or null if the raw value is empty
     * @throws IllegalArgumentException if the text cannot be an RDF value for this kind of
     *                                  position at all
     */
    public Value toValue(RawValue raw, BindingContext context) {
        return resolve(context);
    }

    /**
     * Turns an RDF value into the raw value that stands for it in this position: the short
     * form where {@link #toValue(RawValue, BindingContext)} gives the value back from it,
     * and otherwise the value in full.
     *
     * @param value   the RDF value
     * @param context the nanopublication the value is from
     * @return the raw value
     */
    public RawValue toRaw(Value value, BindingContext context) {
        return new RawValue(value.stringValue());
    }

    /**
     * Returns the default value of this position ({@code nt:hasDefaultValue}) as it stands in
     * the nanopublication of the context: {@code nt:CREATOR} stands for the creator bound in
     * the context, and an IRI within the template for the IRI of the same name within the
     * nanopublication. {@link #toRaw(Value, BindingContext)} gives the value as a person would
     * enter it.
     *
     * @param context the nanopublication the value is for and the values bound so far
     * @return the default value, or null if the position has none, or none that can stand
     * there
     */
    public Value getDefault(BindingContext context) {
        Objects.requireNonNull(context, "context");
        IRI key = getKey();
        if (key == null) return null;
        Value value = template.getDefault(key);
        if (value == null) return null;
        if (value.equals(NTEMPLATE.CREATOR_PLACEHOLDER)) {
            value = context.get(NTEMPLATE.CREATOR_PLACEHOLDER);
        } else if (value instanceof IRI iri) {
            String templateUri = template.getNanopub().getUri().stringValue();
            if (iri.stringValue().startsWith(templateUri) && !iri.stringValue().equals(templateUri)) {
                try {
                    value = toValue(new RawValue(IriUtils.getPostfix(iri.stringValue())), context);
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }
        }
        return value != null && match(value, context).isBindable() ? value : null;
    }

    Violation blocking(Value value, Violation.Kind kind, String message) {
        return new Violation(templateValue, value, kind, true, message);
    }

    Violation reported(Value value, Violation.Kind kind, String message) {
        return new Violation(templateValue, value, kind, false, message);
    }

    /**
     * The name of this position in messages: the label the template gives it, or else the
     * last part of its IRI.
     */
    String getFieldLabel() {
        if (!(templateValue instanceof IRI iri)) return templateValue == null ? "(undefined)" : describe(templateValue);
        String label = template.getLabel(iri);
        if (label != null) return label;
        return IriUtils.getPostfix(template.getTemplateIri(iri).stringValue());
    }

    static String describe(Value value) {
        if (value instanceof Literal literal) return LiteralSerialization.serialize(literal);
        return "<" + value.stringValue() + ">";
    }

    static String getDatatypeLabel(IRI datatype) {
        return datatype.stringValue().replace(XSD.NAMESPACE, "xsd:").replace(RDF.NAMESPACE, "rdf:");
    }

    /**
     * Adds a blocking violation if the template has a regular expression for the given
     * placeholder that the text does not match.
     */
    void checkRegex(IRI placeholder, String text, Value value, List<Violation> violations) {
        String regex = template.getRegex(placeholder);
        if (regex == null) return;
        boolean matches;
        try {
            matches = text.matches(regex);
        } catch (PatternSyntaxException ex) {
            // A pattern that is not one constrains nothing.
            return;
        }
        if (!matches) {
            violations.add(blocking(value, Violation.Kind.REGEX,
                    "Value '" + text + "' of '" + getFieldLabel() + "' doesn't match the pattern '" + regex + "'"));
        }
    }

    /**
     * The prefix put in front of a name given for the placeholder: the namespace of the
     * nanopublication for a local resource, otherwise the prefix the template declares, or
     * nothing.
     */
    String getMintingPrefix(IRI placeholder, BindingContext context) {
        if (template.isLocalResource(placeholder) && !context.hasDynamicPrefix(placeholder)) return context.getNamespace();
        String prefix = context.getPrefix(placeholder);
        return prefix == null ? "" : prefix;
    }

    /**
     * Forms the IRI for a text given for the placeholder: an IRI is taken as it is, a name
     * gets the prefix of the placeholder in front, or else the namespace of the
     * nanopublication.
     */
    IRI toIri(IRI placeholder, String text, BindingContext context) {
        String v;
        if (IriUtils.isUriValue(text)) {
            v = text;
        } else {
            // What follows a prefix or a namespace is a name, and an IRI in another scheme is
            // not one that can stand here:
            if (text.contains(":") || text.contains("#")) {
                throw new InvalidRawValueException(Violation.Kind.NOT_AN_IRI_OR_NAME, "'" + text + "' is neither an IRI in one of the schemes "
                        + UriSchemes.ALLOWED_SCHEMES.stream().sorted().collect(Collectors.joining(", "))
                        + " nor a name without colon or hash");
            }
            checkPrefixResolved(placeholder, context);
            v = getMintingPrefix(placeholder, context) + text;
        }
        if (IriUtils.isPlainName(v)) v = context.getNamespace() + v;
        return createIri(v);
    }

    void checkPrefixResolved(IRI placeholder, BindingContext context) {
        if (context.hasUnresolvedPrefix(placeholder)) {
            throw new InvalidRawValueException(Violation.Kind.UNRESOLVED_PREFIX, "The prefix of '" + getFieldLabel() + "' depends on the space or resource"
                    + " the nanopublication is made for, which is not given: " + template.getPrefix(placeholder));
        }
    }

    static IRI createIri(String iri) {
        if (!IriUtils.isWellFormedUri(iri)) throw new InvalidRawValueException(Violation.Kind.MALFORMED_IRI, "Not a well-formed IRI: " + iri);
        return vf.createIRI(iri);
    }

    /**
     * Returns the first of the candidate texts that this matcher turns back into the given
     * value, or else the value in full.
     */
    RawValue shortestRaw(Value value, BindingContext context, List<String> candidates) {
        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) continue;
            try {
                if (value.equals(toValue(new RawValue(candidate), context))) return new RawValue(candidate);
            } catch (IllegalArgumentException ex) {
                // not a text for this value
            }
        }
        return new RawValue(value.stringValue());
    }

}
