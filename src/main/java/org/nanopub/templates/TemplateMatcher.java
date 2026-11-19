package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.*;

/**
 * Assigns the triples of one graph of a nanopublication to the statements of a template.
 * <p>
 * The statements of the template are gone through in template order. Each takes, from the
 * triples not yet taken, the first ones that fit, and is not looked at again: a statement
 * takes one set of triples, a repeatable one takes further sets as repetitions until none
 * fits. The triples are tried in the order of their subject, predicate and object.
 * <p>
 * A grouped statement takes one triple for each of its members, and the members have to
 * agree on the values of the placeholders they share. All combinations are tried, and an
 * optional member is left without a triple only if there is no combination that gives it one.
 * <p>
 * A placeholder stands for one value wherever it occurs, with one exception: a placeholder
 * or local resource that occurs in a single top-level statement stands for a value of its
 * own in each repetition of that statement. Its binding has the repetition as a {@code __N}
 * suffix on its key, from the second repetition on.
 * <p>
 * This is the order Nanodash matches in, and it has the same limit: a statement cannot give
 * back a triple that a later statement turns out to need, so the triples of a nanopublication
 * that complies with the template can in rare cases come out as not matching.
 */
public class TemplateMatcher {

    private final Template template;
    private final TemplateStructure structure;

    /**
     * Creates a matcher for a template.
     *
     * @param template the template
     */
    public TemplateMatcher(Template template) {
        this.template = Objects.requireNonNull(template, "template");
        this.structure = new TemplateStructure(template);
    }

    /**
     * @return the template this matcher is for
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * Assigns triples to the statements of the template, binding the values of its
     * placeholders in the given context.
     *
     * @param triples the triples of one graph of the nanopublication
     * @param context the context for the template of this matcher and that nanopublication;
     *                values bound in it already are kept and have to agree with the triples
     * @return the assignment found
     */
    public TemplateMatch match(Collection<Statement> triples, BindingContext context) {
        Objects.requireNonNull(context, "context");
        if (context.getTemplate() != template) throw new IllegalArgumentException("The context is for another template");
        List<Statement> available = new ArrayList<>(triples);
        available.sort(Comparator.comparing(TemplateMatcher::getSortKey));
        List<TemplateMatch.StatementMatch> matches = new ArrayList<>();
        List<IRI> unmatched = new ArrayList<>();
        for (IRI statement : template.getStatementIris()) {
            List<IRI> members = getMembers(statement);
            int repetition = 0;
            while (true) {
                Map<IRI, Statement> assigned = new LinkedHashMap<>();
                Map<IRI, Value> bindingsBefore = context.snapshot();
                List<Statement> availableBefore = new ArrayList<>(available);
                // A match has to take at least one triple: a group whose members are all
                // left out would otherwise match without anything to show for it, any
                // number of times.
                if (!assignMembers(statement, members, 0, repetition, available, context, assigned) || assigned.isEmpty()) {
                    context.restore(bindingsBefore);
                    available = availableBefore;
                    break;
                }
                matches.add(new TemplateMatch.StatementMatch(statement, repetition, assigned));
                repetition++;
                if (!template.isRepeatableStatement(statement)) break;
            }
            if (repetition == 0) unmatched.add(statement);
        }
        return new TemplateMatch(context, matches, unmatched, available);
    }

    // Assigns a triple to each member from the given index on, so that the bindings agree.
    // On success the triples are taken out of the available ones and the bindings stay; on
    // failure both are as before.
    private boolean assignMembers(IRI statement, List<IRI> members, int index, int repetition, List<Statement> available,
                                  BindingContext context, Map<IRI, Statement> assigned) {
        if (index == members.size()) return true;
        IRI member = members.get(index);
        Value[] positions = getPositions(member);
        Value subject = getKey(positions[0], repetition);
        Value predicate = getKey(positions[1], repetition);
        Value object = getKey(positions[2], repetition);
        Map<IRI, Value> bindingsBefore = context.snapshot();
        for (int i = 0; i < available.size(); i++) {
            Statement triple = available.get(i);
            // The predicate first: it is what most often tells a statement apart.
            if (context.bind(predicate, triple.getPredicate()).isBindable()
                    && context.bind(subject, triple.getSubject()).isBindable()
                    && context.bind(object, triple.getObject()).isBindable()) {
                available.remove(i);
                assigned.put(member, triple);
                if (assignMembers(statement, members, index + 1, repetition, available, context, assigned)) return true;
                assigned.remove(member);
                available.add(i, triple);
            }
            context.restore(bindingsBefore);
        }
        // An optional member of a group that no triple can be given to is left out. Having
        // tried all triples first makes sure a member is only left out when it has to be.
        boolean optionalMember = template.isGroupedStatement(statement) && template.isOptionalStatement(member);
        return optionalMember && assignMembers(statement, members, index + 1, repetition, available, context, assigned);
    }

    private List<IRI> getMembers(IRI statement) {
        return structure.getMembers(statement);
    }

    private Value[] getPositions(IRI statement) {
        return structure.getPositions(statement);
    }

    private Value getKey(Value templateValue, int repetition) {
        return structure.getKey(templateValue, repetition);
    }

    private static String getSortKey(Statement triple) {
        return triple.getSubject() + " " + triple.getPredicate() + " " + triple.getObject();
    }

}
