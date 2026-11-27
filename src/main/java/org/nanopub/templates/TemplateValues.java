package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.nanopub.vocabulary.NTEMPLATE;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * The values given for the placeholders of one template, before they are turned into RDF.
 * <p>
 * A value is given under the name of its placeholder, which is the last part of the
 * placeholder's IRI: {@code title} for {@code sub:title}. A placeholder that stands for a
 * value of its own in each repetition of a statement has one name per repetition: the plain
 * name for the first, {@code title__1} for the second, {@code title__2} for the third, and so
 * on. These are the names of Nanodash's {@code param_} parameters, without that prefix.
 * <p>
 * What the text of a value means depends on the kind of placeholder, see
 * {@link PlaceholderMatcher#toValue(RawValue, BindingContext)}. A value with an empty text
 * says that the placeholder is to have no value, not even the default one of the template.
 */
public class TemplateValues implements Serializable {

    /**
     * What follows the name of a placeholder in the name of the parameter that gives the
     * language tag of its value, as in {@code title__lang}.
     */
    public static final String LANGUAGE_SUFFIX = "__lang";

    private final Map<String, RawValue> values = new LinkedHashMap<>();

    /**
     * Creates an empty set of values.
     */
    public TemplateValues() {
    }

    /**
     * Returns the name under which a value is given for a repetition.
     *
     * @param name       the name of the placeholder
     * @param repetition the repetition, counted from 0
     * @return the name, with a {@code __N} suffix from the second repetition on
     */
    public static String getName(String name, int repetition) {
        return repetition == 0 ? name : name + "__" + repetition;
    }

    /**
     * Returns the name under which the value of a position is given.
     *
     * @param key the key of the position: the IRI of its placeholder, with a {@code __N}
     *            suffix for a repetition
     * @return the last part of the IRI
     */
    public static String getName(IRI key) {
        return IriUtils.getPostfix(key.stringValue());
    }

    /**
     * Sets the value of a placeholder.
     *
     * @param name the name of the placeholder, with a {@code __N} suffix for a repetition
     * @param text the text of the value
     * @return this object
     */
    public TemplateValues set(String name, String text) {
        return set(name, new RawValue(text));
    }

    /**
     * Sets the value of a placeholder.
     *
     * @param name  the name of the placeholder, with a {@code __N} suffix for a repetition
     * @param value the value
     * @return this object
     */
    public TemplateValues set(String name, RawValue value) {
        values.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
        return this;
    }

    /**
     * Sets the value of a placeholder for the first repetition that has none yet.
     *
     * @param name the name of the placeholder, without a suffix
     * @param text the text of the value
     * @return this object
     */
    public TemplateValues add(String name, String text) {
        int repetition = 0;
        while (values.containsKey(getName(name, repetition))) repetition++;
        return set(getName(name, repetition), text);
    }

    /**
     * @param name the name of the placeholder, with a {@code __N} suffix for a repetition
     * @return the value given under that name, or null if there is none
     */
    public RawValue get(String name) {
        return values.get(name);
    }

    /**
     * @param name the name of the placeholder, with a {@code __N} suffix for a repetition
     * @return true if a value is given under that name, be it an empty one
     */
    public boolean has(String name) {
        return values.containsKey(name);
    }

    /**
     * @return the names values are given under, in the order they were given; unmodifiable
     */
    public Set<String> getNames() {
        return Collections.unmodifiableSet(values.keySet());
    }

    /**
     * @return true if no value is given
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Returns the values as parameters: the text of each value under its name, and its
     * language tag, if it has one, under the name followed by {@link #LANGUAGE_SUFFIX}.
     *
     * @return the parameters, in the order the values were given
     */
    public Map<String, String> toParams() {
        Map<String, String> params = new LinkedHashMap<>();
        for (Map.Entry<String, RawValue> e : values.entrySet()) {
            params.put(e.getKey(), e.getValue().text());
            if (e.getValue().languageTag() != null) params.put(e.getKey() + LANGUAGE_SUFFIX, e.getValue().languageTag());
        }
        return params;
    }

    /**
     * Reads values from parameters, as {@link #toParams()} writes them.
     *
     * @param params the parameters
     * @return the values
     */
    public static TemplateValues fromParams(Map<String, String> params) {
        TemplateValues values = new TemplateValues();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String name = e.getKey();
            if (name.endsWith(LANGUAGE_SUFFIX)) {
                // A language tag belongs to the value of the same name; one without such a
                // value is kept, so that it is reported as a name nothing answers to.
                String base = name.substring(0, name.length() - LANGUAGE_SUFFIX.length());
                if (params.containsKey(base)) continue;
            }
            values.set(name, new RawValue(e.getValue(), params.get(name + LANGUAGE_SUFFIX)));
        }
        return values;
    }

    /**
     * Reads the values of a template's placeholders out of a match, so that filling the
     * template with them gives the matched triples again.
     * <p>
     * Left out are the values that follow from the nanopublication rather than being given:
     * the creator, the positions in a sequence, and a root definition that is the
     * nanopublication itself. An IRI within the nanopublication is given by its name there,
     * so that it is minted again in the nanopublication that is made, and so is a resource it
     * introduces under the IRI an earlier version of it gave: a new version made with
     * {@link FillRequest.Mode#SUPERSEDE} or {@link FillRequest.Mode#OVERRIDE} keeps such
     * resources, and anything else mints them anew. A placeholder that has
     * a default value in the template but no value in the nanopublication gets an empty
     * value, so that the default is not taken.
     *
     * @param match the match of a nanopublication against a template
     * @return the values, by the names of the placeholders
     */
    public static TemplateValues of(TemplateMatch match) {
        Objects.requireNonNull(match, "match");
        BindingContext context = match.getContext();
        Template template = match.getTemplate();
        TemplateValues values = new TemplateValues();
        for (Map.Entry<IRI, Value> e : context.getBindings().entrySet()) {
            IRI key = e.getKey();
            if (key.stringValue().startsWith(NTEMPLATE.NAMESPACE)) continue;
            if (template.isSequenceElementPlaceholder(key)) continue;
            PlaceholderMatcher matcher = PlaceholderMatcher.of(template, key);
            if (!matcher.isPlaceholder()) continue;
            if (template.isRootNanopubPlaceholder(key) && e.getValue().equals(context.getNanopubUri())) continue;
            RawValue raw = matcher.toRaw(e.getValue(), context);
            // A resource the nanopublication kept from an earlier version of itself is given
            // by its name too: a new version keeps it again, anything else mints it anew.
            if (matcher instanceof IriMatcher iriMatcher && raw.text().equals(e.getValue().stringValue())
                    && iriMatcher.isCarriedOver(raw.text(), context)) {
                raw = new RawValue(IriUtils.getPostfix(raw.text()));
            }
            values.set(getName(key), raw);
        }
        // A placeholder that has a default value but no value in the nanopublication was
        // left empty on purpose, which an empty value says.
        TemplateStructure structure = new TemplateStructure(template);
        Map<IRI, Set<Integer>> repetitions = new HashMap<>();
        for (TemplateMatch.StatementMatch statementMatch : match.getMatches()) {
            repetitions.computeIfAbsent(statementMatch.statement(), k -> new TreeSet<>()).add(statementMatch.repetition());
        }
        for (IRI statement : template.getStatementIris()) {
            for (int repetition : repetitions.getOrDefault(statement, Set.of(0))) {
                for (IRI member : structure.getMembers(statement)) {
                    for (Value position : structure.getPositions(member)) {
                        if (!(structure.getKey(position, repetition) instanceof IRI key)) continue;
                        if (context.isBound(key) || template.getDefault(key) == null) continue;
                        if (key.stringValue().startsWith(NTEMPLATE.NAMESPACE) || !PlaceholderMatcher.of(template, key).isPlaceholder()) continue;
                        values.set(getName(key), "");
                    }
                }
            }
        }
        return values;
    }

    void copyTo(TemplateValues target) {
        target.values.putAll(values);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TemplateValues other && values.equals(other.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return toParams().toString();
    }

}
