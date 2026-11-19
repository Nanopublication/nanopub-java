package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What a {@link TemplateMatcher} found when it assigned the triples of a nanopublication to
 * the statements of a template.
 */
public class TemplateMatch implements Serializable {

    /**
     * One repetition of a statement of the template, with the triples assigned to it.
     *
     * @param statement  the IRI of the top-level statement, which may be a grouped statement
     * @param repetition the repetition, counted from 0
     * @param triples    the triple assigned to each member statement, by the IRI of the
     *                   member; for a statement that is not grouped, the one triple under the
     *                   IRI of the statement itself. An optional member nothing was assigned
     *                   to is not in the map.
     */
    public record StatementMatch(IRI statement, int repetition, Map<IRI, Statement> triples) implements Serializable {

        /**
         * Creates a statement match.
         *
         * @param statement  the IRI of the top-level statement
         * @param repetition the repetition, counted from 0
         * @param triples    the triple assigned to each member statement
         */
        public StatementMatch {
            triples = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(triples));
        }

    }

    private final BindingContext context;
    private final List<StatementMatch> matches;
    private final List<IRI> unmatchedStatements;
    private final List<Statement> unusedTriples;

    TemplateMatch(BindingContext context, List<StatementMatch> matches, List<IRI> unmatchedStatements, List<Statement> unusedTriples) {
        this.context = context;
        this.matches = List.copyOf(matches);
        this.unmatchedStatements = List.copyOf(unmatchedStatements);
        this.unusedTriples = List.copyOf(unusedTriples);
    }

    /**
     * @return the template the triples were matched against
     */
    public Template getTemplate() {
        return context.getTemplate();
    }

    /**
     * @return the context holding the values the match bound
     */
    public BindingContext getContext() {
        return context;
    }

    /**
     * @return the statements of the template that triples were assigned to, in template order,
     * with one entry for each repetition
     */
    public List<StatementMatch> getMatches() {
        return matches;
    }

    /**
     * @return the IRIs of the top-level statements of the template that no triples were
     * assigned to, optional or not
     */
    public List<IRI> getUnmatchedStatements() {
        return unmatchedStatements;
    }

    /**
     * @return the IRIs of the top-level statements of the template that no triples were
     * assigned to although the template does not mark them as optional
     */
    public List<IRI> getMissingStatements() {
        List<IRI> missing = new ArrayList<>();
        for (IRI statement : unmatchedStatements) {
            if (!getTemplate().isOptionalStatement(statement)) missing.add(statement);
        }
        return missing;
    }

    /**
     * @return the triples that were assigned to no statement of the template
     */
    public List<Statement> getUnusedTriples() {
        return unusedTriples;
    }

    /**
     * Returns what is wrong with the values the match bound, without keeping them from being
     * bound: a literal of another datatype than the template declares, a value outside a
     * restricted choice that refers to other placeholders, and the like.
     *
     * @return the violations of the bound values
     */
    public List<Violation> getViolations() {
        return context.getViolations();
    }

    /**
     * Whether the triples are what filling in the template would give: every statement that
     * is not optional has triples, no triple is left over, and no value breaks a constraint.
     *
     * @return true if the triples comply with the template
     */
    public boolean isComplete() {
        return getMissingStatements().isEmpty() && unusedTriples.isEmpty() && getViolations().isEmpty();
    }

}
