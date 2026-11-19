package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.*;

/**
 * What matching and filling need to know about the statements of a template beyond what
 * {@link Template} says about each of them: the members of a statement, its positions, and
 * which positions have a value of their own in each repetition.
 * <p>
 * A placeholder stands for one value wherever it occurs, with one exception: a placeholder or
 * local resource that occurs in a single top-level statement has a <em>narrow scope</em>, and
 * stands for a value of its own in each repetition of that statement. Its key then has the
 * repetition as a {@code __N} suffix, from the second repetition on. A sequence-element
 * placeholder always has a value of its own, as its value is the repetition.
 */
final class TemplateStructure {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private final Template template;
    // The IRIs that occur in exactly one top-level statement of the template, with that
    // statement.
    private final Map<IRI, IRI> narrowScope = new HashMap<>();

    TemplateStructure(Template template) {
        this.template = Objects.requireNonNull(template, "template");
        Set<IRI> seen = new HashSet<>();
        for (IRI statement : template.getStatementIris()) {
            Set<IRI> iris = new LinkedHashSet<>();
            for (IRI member : getMembers(statement)) {
                for (Value value : getPositions(member)) {
                    if (value instanceof IRI iri) iris.add(iri);
                }
            }
            for (IRI iri : iris) {
                if (seen.add(iri)) {
                    narrowScope.put(iri, statement);
                } else {
                    narrowScope.remove(iri);
                }
            }
        }
    }

    Template getTemplate() {
        return template;
    }

    /**
     * Returns the statements that give the triples of a top-level statement: its members if
     * it is a group, otherwise the statement itself.
     */
    List<IRI> getMembers(IRI statement) {
        List<IRI> members = template.getStatementIris(statement);
        return members != null && template.isGroupedStatement(statement) ? members : List.of(statement);
    }

    /**
     * Returns the subject, predicate and object the template has for a statement, any of
     * which may be missing or, wrongly, a literal.
     */
    Value[] getPositions(IRI statement) {
        return new Value[]{template.getDeclaredSubject(statement), template.getDeclaredPredicate(statement), template.getObject(statement)};
    }

    /**
     * Whether the value in a position has a value of its own in each repetition of its
     * statement.
     */
    boolean hasOwnValuePerRepetition(Value templateValue) {
        if (!(templateValue instanceof IRI iri)) return false;
        return template.isSequenceElementPlaceholder(iri)
                || (narrowScope.containsKey(iri) && (template.isPlaceholder(iri) || template.isLocalResource(iri)));
    }

    /**
     * Returns the key under which the value of a position is bound in a repetition.
     *
     * @param templateValue the value the template has in the position
     * @param repetition    the repetition, counted from 0
     * @return the template value, with a {@code __N} suffix where the position has a value of
     * its own in each repetition
     */
    Value getKey(Value templateValue, int repetition) {
        if (repetition == 0 || !hasOwnValuePerRepetition(templateValue)) return templateValue;
        return vf.createIRI(templateValue.stringValue() + "__" + repetition);
    }

}
