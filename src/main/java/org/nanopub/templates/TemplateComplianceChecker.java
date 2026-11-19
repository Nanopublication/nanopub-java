package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.SimpleCreatorPattern;
import org.nanopub.templates.ComplianceIssue.Graph;
import org.nanopub.templates.ComplianceIssue.Kind;
import org.nanopub.templates.ComplianceIssue.Severity;
import org.nanopub.vocabulary.KPXL;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.*;

/**
 * Checks whether a nanopublication is what filling in the templates it names would give.
 * <p>
 * A nanopublication names its templates in its publication info: one for the assertion
 * ({@code nt:wasCreatedFromTemplate}), one for the provenance
 * ({@code nt:wasCreatedFromProvenanceTemplate}), and any number for the publication info
 * ({@code nt:wasCreatedFromPubinfoTemplate}). Each template is retrieved at exactly the
 * version named, and the triples of the graph are assigned to its statements with a
 * {@link TemplateMatcher}.
 * <p>
 * The nanopublication fails to comply if
 * <ul>
 *   <li>a statement of a template that is not optional has no triples;</li>
 *   <li>a triple of the assertion or the provenance belongs to no statement;</li>
 *   <li>a value breaks a constraint of its placeholder;</li>
 *   <li>a template it names cannot be retrieved, or is for another graph;</li>
 *   <li>it lacks a type the assertion template sets.</li>
 * </ul>
 * The templates of the publication info are each matched against all of its triples, since
 * two of them may give the same triple. A triple that none of them has a statement for is
 * only a warning, as publication info is often added to. A value under a name the
 * nanopublication keeps for one of its own parts, such as {@code assertion}, is a warning too:
 * templates in the old shape, whose node is their own assertion graph, have one. The triples
 * that tools write to the publication info without a template are not reported at all: the
 * creation time, label, types and template links of the nanopublication, what it introduces,
 * embeds and supersedes, where it was created, its signature, the labels and role directions
 * of things it mentions, and the names of its creators.
 * <p>
 * A checker keeps the templates it has retrieved, so that checking many nanopublications
 * retrieves each template once. It is not safe for use by several threads at a time.
 */
public class TemplateComplianceChecker {

    // What tools write about the nanopublication itself without a template.
    private static final Set<IRI> STANDARD_NANOPUB_PREDICATES = Set.of(
            DCTERMS.CREATED, RDFS.LABEL, NPX.INTRODUCES, NPX.EMBEDS, NPX.SUPERSEDES, NPX.WAS_CREATED_AT, NPX.HAS_NANOPUB_TYPE,
            NTEMPLATE.WAS_CREATED_FROM_TEMPLATE, NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE, NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE);

    private static final Set<IRI> SIGNATURE_PREDICATES = Set.of(
            NPX.HAS_ALGORITHM, NPX.HAS_PUBLIC_KEY, NPX.HAS_SIGNATURE, NPX.HAS_SIGNATURE_TARGET, NPX.SIGNED_BY);

    private final NanopubResolver resolver;
    private final Map<String, Template> templates = new HashMap<>();
    private final Map<String, String> templateProblems = new HashMap<>();
    private final Map<Template, TemplateMatcher> matchers = new HashMap<>();

    /**
     * Creates a checker that retrieves templates from the nanopublication network.
     */
    public TemplateComplianceChecker() {
        this(NanopubResolver.DEFAULT);
    }

    /**
     * Creates a checker that retrieves templates with the given resolver.
     *
     * @param resolver retrieves the templates, and the nanopublications holding the possible
     *                 values of their restricted choices
     */
    public TemplateComplianceChecker(NanopubResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    /**
     * Checks a nanopublication against the templates it names.
     *
     * @param nanopub the nanopublication, signed or not
     * @return the findings
     */
    public ComplianceReport check(Nanopub nanopub) {
        Objects.requireNonNull(nanopub, "nanopub");
        List<ComplianceIssue> issues = new ArrayList<>();

        Template assertionTemplate = getSingleTemplate(nanopub, Graph.ASSERTION, issues);
        TemplateMatch assertionMatch = null;
        if (assertionTemplate != null) {
            assertionMatch = match(assertionTemplate, nanopub.getAssertion(), nanopub);
            report(assertionMatch, Graph.ASSERTION, getLink(nanopub, Graph.ASSERTION), assertionMatch.getUnusedTriples(), issues);
        }

        Template provenanceTemplate = getSingleTemplate(nanopub, Graph.PROVENANCE, issues);
        TemplateMatch provenanceMatch = null;
        if (provenanceTemplate != null) {
            provenanceMatch = match(provenanceTemplate, nanopub.getProvenance(), nanopub);
            report(provenanceMatch, Graph.PROVENANCE, getLink(nanopub, Graph.PROVENANCE), provenanceMatch.getUnusedTriples(), issues);
        }

        List<TemplateMatch> pubinfoMatches = checkPubinfo(nanopub, issues);

        if (assertionMatch != null) {
            checkTypes(nanopub, assertionTemplate, issues);
            checkRequiredPubinfoElements(nanopub, assertionTemplate, pubinfoMatches, issues);
            checkDeclaredResources(nanopub, assertionMatch.getContext(), issues);
            checkRolePins(nanopub, assertionMatch.getContext(), issues);
        }
        return new ComplianceReport(nanopub, issues, assertionMatch, provenanceMatch, pubinfoMatches);
    }

    // ---------- the templates a nanopublication names ----------

    private static IRI getLinkPredicate(Graph graph) {
        return switch (graph) {
            case ASSERTION -> NTEMPLATE.WAS_CREATED_FROM_TEMPLATE;
            case PROVENANCE -> NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE;
            case PUBINFO -> NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE;
        };
    }

    /**
     * Returns the templates a nanopublication names for a graph.
     *
     * @param nanopub the nanopublication
     * @param graph   the graph
     * @return the IRIs it gives for its templates of that graph, in the order of the IRIs
     */
    public static List<IRI> getTemplateLinks(Nanopub nanopub, Graph graph) {
        IRI predicate = getLinkPredicate(graph);
        TreeMap<String, IRI> links = new TreeMap<>();
        for (Statement st : nanopub.getPubinfo()) {
            if (!st.getSubject().equals(nanopub.getUri()) || !st.getPredicate().equals(predicate)) continue;
            if (st.getObject() instanceof IRI link) links.put(link.stringValue(), link);
        }
        return new ArrayList<>(links.values());
    }

    private static IRI getLink(Nanopub nanopub, Graph graph) {
        List<IRI> links = getTemplateLinks(nanopub, graph);
        return links.isEmpty() ? null : links.get(0);
    }

    // The one template of the assertion or the provenance, or null if there is none to
    // check against.
    private Template getSingleTemplate(Nanopub nanopub, Graph graph, List<ComplianceIssue> issues) {
        List<IRI> links = getTemplateLinks(nanopub, graph);
        if (links.isEmpty()) {
            issues.add(new ComplianceIssue(Severity.WARNING, graph, Kind.NO_TEMPLATE,
                    "No " + getName(graph) + " template is named, so the " + getName(graph) + " is not checked", null, null, null));
            return null;
        }
        if (links.size() > 1) {
            issues.add(new ComplianceIssue(Severity.WARNING, graph, Kind.SEVERAL_TEMPLATES,
                    links.size() + " " + getName(graph) + " templates are named; only <" + links.get(0) + "> is checked against", links.get(0), null, null));
        }
        return getTemplate(links.get(0), graph, issues);
    }

    // Retrieves a template and checks that it is one for the given graph.
    private Template getTemplate(IRI link, Graph graph, List<ComplianceIssue> issues) {
        Template template = loadTemplate(link.stringValue());
        if (template == null) {
            issues.add(new ComplianceIssue(Severity.ERROR, graph, Kind.TEMPLATE_UNAVAILABLE,
                    "The " + getName(graph) + " template <" + link + "> cannot be used: " + templateProblems.get(link.stringValue()), link, null, null));
            return null;
        }
        if (getGraph(template) != graph) {
            issues.add(new ComplianceIssue(Severity.ERROR, graph, Kind.WRONG_TEMPLATE_KIND,
                    "<" + link + "> is named as " + getName(graph) + " template but is a template for the " + getName(getGraph(template)), link, null, null));
            return null;
        }
        for (String error : template.getStatementErrors()) {
            issues.add(new ComplianceIssue(Severity.WARNING, graph, Kind.TEMPLATE_ERROR, "The template <" + link + "> has an error: " + error, link, null, null));
        }
        return template;
    }

    private Template loadTemplate(String link) {
        if (templates.containsKey(link)) return templates.get(link);
        Template template = null;
        try {
            template = new Template(link, resolver);
        } catch (MalformedTemplateException ex) {
            templateProblems.put(link, ex.getMessage());
        } catch (RuntimeException ex) {
            templateProblems.put(link, ex.toString());
        }
        templates.put(link, template);
        return template;
    }

    private static Graph getGraph(Template template) {
        if (template.isProvenanceTemplate()) return Graph.PROVENANCE;
        if (template.isPubinfoTemplate()) return Graph.PUBINFO;
        return Graph.ASSERTION;
    }

    private static String getName(Graph graph) {
        return switch (graph) {
            case ASSERTION -> "assertion";
            case PROVENANCE -> "provenance";
            case PUBINFO -> "publication info";
        };
    }

    // ---------- matching ----------

    private TemplateMatch match(Template template, Collection<Statement> triples, Nanopub nanopub) {
        TemplateMatcher matcher = matchers.computeIfAbsent(template, TemplateMatcher::new);
        return matcher.match(triples, BindingContext.forNanopub(template, nanopub));
    }

    // Reports what is wrong with a match: its statements without triples, the given leftover
    // triples, and the values that break a constraint.
    private void report(TemplateMatch match, Graph graph, IRI link, List<Statement> leftover, List<ComplianceIssue> issues) {
        Template template = match.getTemplate();
        for (IRI statement : match.getMissingStatements()) {
            issues.add(new ComplianceIssue(Severity.ERROR, graph, Kind.MISSING_STATEMENT,
                    "Nothing matches the statement " + describeStatement(template, statement) + " of the " + getName(graph) + " template", link, statement, null));
        }
        for (Statement triple : leftover) {
            Severity severity = graph == Graph.PUBINFO ? Severity.WARNING : Severity.ERROR;
            issues.add(new ComplianceIssue(severity, graph, Kind.UNMATCHED_TRIPLE,
                    "The " + getName(graph) + " triple " + describeTriple(triple) + " matches no statement of "
                            + (graph == Graph.PUBINFO ? "the publication info templates" : "the " + getName(graph) + " template")
                            + explainNearMiss(match, triple), link, null, triple));
        }
        // A value that stands in several places, as in the repetitions of a statement, gives
        // the same violation for each of them, which is reported once.
        Map<String, Violation> violations = new LinkedHashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        for (Violation violation : match.getViolations()) {
            String key = violation.kind() + " " + violation.message();
            violations.putIfAbsent(key, violation);
            counts.merge(key, 1, Integer::sum);
        }
        for (Map.Entry<String, Violation> e : violations.entrySet()) {
            Violation violation = e.getValue();
            int count = counts.get(e.getKey());
            String message = violation.message() + (count > 1 ? " (in " + count + " places)" : "");
            // A name the nanopublication keeps for itself is refused when a value is newly
            // minted, but is no reason for an existing nanopublication not to comply: a template
            // in the old shape, whose node is its own assertion graph, has such a value.
            if (violation.kind() == Violation.Kind.RESERVED_NAME) {
                issues.add(new ComplianceIssue(Severity.WARNING, graph, Kind.RESERVED_NAME, message, link, null, null));
            } else {
                issues.add(new ComplianceIssue(Severity.ERROR, graph, Kind.VIOLATION, message, link, null, null));
            }
        }
    }

    // Each template of the publication info is matched against all of its triples, as two
    // templates may well give the same triple: a nanopublication that names two versions of
    // the template for its creator has one triple for both. A triple is left over if no
    // template has taken it.
    // Says why a leftover triple does not fit the statement it most likely belongs to: the
    // one with its predicate that the fewest things speak against, given the values bound.
    private static String explainNearMiss(TemplateMatch match, Statement triple) {
        Template template = match.getTemplate();
        BindingContext context = match.getContext();
        IRI nearest = null;
        List<Violation> fewest = null;
        for (IRI statement : template.getStatementIris()) {
            List<IRI> members = template.getStatementIris(statement);
            if (members == null || !template.isGroupedStatement(statement)) members = List.of(statement);
            for (IRI member : members) {
                if (!triple.getPredicate().equals(template.getPredicate(member))) continue;
                List<Violation> blocking = new ArrayList<>();
                for (Violation v : PlaceholderMatcher.of(template, template.getDeclaredSubject(member)).match(triple.getSubject(), context).violations()) {
                    if (v.blocking()) blocking.add(v);
                }
                for (Violation v : PlaceholderMatcher.of(template, template.getObject(member)).match(triple.getObject(), context).violations()) {
                    if (v.blocking()) blocking.add(v);
                }
                if (!blocking.isEmpty() && (fewest == null || blocking.size() < fewest.size())) {
                    nearest = member;
                    fewest = blocking;
                }
            }
        }
        if (nearest == null) return "";
        List<String> reasons = new ArrayList<>();
        for (Violation v : fewest) reasons.add(v.message());
        return "; it does not fit the statement " + IriUtils.getPostfix(nearest.stringValue()) + " because: " + String.join("; ", reasons);
    }

    private List<TemplateMatch> checkPubinfo(Nanopub nanopub, List<ComplianceIssue> issues) {
        List<IRI> links = getTemplateLinks(nanopub, Graph.PUBINFO);
        if (links.isEmpty()) {
            issues.add(new ComplianceIssue(Severity.WARNING, Graph.PUBINFO, Kind.NO_TEMPLATE,
                    "No publication info template is named, so the publication info is not checked", null, null, null));
            return List.of();
        }
        // The signature is added when a nanopublication is signed, after its templates have
        // been filled in, so no template describes it.
        List<Statement> triples = new ArrayList<>();
        for (Statement triple : nanopub.getPubinfo()) {
            if (!SIGNATURE_PREDICATES.contains(triple.getPredicate())) triples.add(triple);
        }
        List<TemplateMatch> matches = new ArrayList<>();
        Set<Statement> leftover = new LinkedHashSet<>(triples);
        boolean allTemplatesAvailable = true;
        for (IRI link : links) {
            Template template = getTemplate(link, Graph.PUBINFO, issues);
            if (template == null) {
                allTemplatesAvailable = false;
                continue;
            }
            TemplateMatch match = match(template, triples, nanopub);
            matches.add(match);
            leftover.retainAll(match.getUnusedTriples());
            report(match, Graph.PUBINFO, link, List.of(), issues);
        }
        // With a template missing, what is left over says nothing.
        if (allTemplatesAvailable) {
            Set<IRI> named = getNamedAgents(nanopub);
            for (Statement triple : leftover) {
                if (isStandardPubinfo(triple, nanopub, named)) continue;
                issues.add(new ComplianceIssue(Severity.WARNING, Graph.PUBINFO, Kind.UNMATCHED_TRIPLE,
                        "The publication info triple " + describeTriple(triple) + " matches no statement of the publication info templates", null, null, triple));
            }
        }
        return matches;
    }

    // The agents whose names tools write into the publication info: creators and signers.
    private static Set<IRI> getNamedAgents(Nanopub nanopub) {
        Set<IRI> agents = new HashSet<>(SimpleCreatorPattern.getCreators(nanopub));
        for (Statement st : nanopub.getPubinfo()) {
            if (st.getPredicate().equals(NPX.SIGNED_BY) && st.getObject() instanceof IRI signer) agents.add(signer);
        }
        return agents;
    }

    // Whether a triple of the publication info is one that tools write without a template.
    private static boolean isStandardPubinfo(Statement triple, Nanopub nanopub, Set<IRI> namedAgents) {
        IRI predicate = triple.getPredicate();
        if (triple.getSubject().equals(nanopub.getUri()) && STANDARD_NANOPUB_PREDICATES.contains(predicate)) return true;
        if (predicate.equals(NTEMPLATE.HAS_LABEL_FROM_API)) return true;
        if (predicate.equals(RDF.TYPE) && (triple.getObject().equals(KPXL.INVERSE_ROLE_PROPERTY) || triple.getObject().equals(KPXL.REGULAR_ROLE_PROPERTY))) return true;
        return predicate.equals(FOAF.NAME) && namedAgents.contains(triple.getSubject());
    }

    // ---------- what the assertion template asks of the publication info ----------

    private void checkTypes(Nanopub nanopub, Template assertionTemplate, List<ComplianceIssue> issues) {
        Set<IRI> types = NanopubUtils.getTypes(nanopub);
        for (IRI type : assertionTemplate.getTargetNanopubTypes()) {
            if (types.contains(type)) continue;
            issues.add(new ComplianceIssue(Severity.ERROR, Graph.PUBINFO, Kind.MISSING_TYPE,
                    "The nanopublication lacks the type <" + type + "> that its assertion template sets", null, null, null));
        }
    }

    // A nanopublication names the version of a required template that was current when it
    // was made, which need not be the one the assertion template names. Versions that say
    // what they are versions of can be recognised; for others this can only warn.
    private void checkRequiredPubinfoElements(Nanopub nanopub, Template assertionTemplate, List<TemplateMatch> pubinfoMatches, List<ComplianceIssue> issues) {
        List<IRI> links = getTemplateLinks(nanopub, Graph.PUBINFO);
        for (IRI required : assertionTemplate.getRequiredPubInfoElements()) {
            boolean named = links.contains(required);
            for (TemplateMatch match : pubinfoMatches) {
                if (match.getTemplate().hasId(required.stringValue())) named = true;
            }
            if (!named) {
                Template requiredTemplate = loadTemplate(required.stringValue());
                IRI versionOf = requiredTemplate == null ? null : requiredTemplate.getTemplateKindIri();
                for (TemplateMatch match : pubinfoMatches) {
                    if (versionOf != null && versionOf.equals(match.getTemplate().getTemplateKindIri())) named = true;
                }
            }
            if (named) continue;
            issues.add(new ComplianceIssue(Severity.WARNING, Graph.PUBINFO, Kind.MISSING_PUBINFO_ELEMENT,
                    "The assertion template requires the publication info template <" + required + ">, which is not named;"
                            + " another version of it may be", required, null, null));
        }
    }

    private void checkDeclaredResources(Nanopub nanopub, BindingContext assertionContext, List<ComplianceIssue> issues) {
        for (IRI introduced : assertionContext.getIntroducedIris()) {
            if (hasPubinfo(nanopub, nanopub.getUri(), NPX.INTRODUCES, introduced)) continue;
            issues.add(new ComplianceIssue(Severity.WARNING, Graph.PUBINFO, Kind.UNDECLARED_INTRODUCED_RESOURCE,
                    "<" + introduced + "> is an introduced resource according to the assertion template, but the publication info"
                            + " does not declare it with npx:introduces", null, null, null));
        }
        for (IRI embedded : assertionContext.getEmbeddedIris()) {
            if (hasPubinfo(nanopub, nanopub.getUri(), NPX.EMBEDS, embedded)) continue;
            issues.add(new ComplianceIssue(Severity.WARNING, Graph.PUBINFO, Kind.UNDECLARED_EMBEDDED_RESOURCE,
                    "<" + embedded + "> is an embedded resource according to the assertion template, but the publication info"
                            + " does not declare it with npx:embeds", null, null, null));
        }
    }

    private void checkRolePins(Nanopub nanopub, BindingContext assertionContext, List<ComplianceIssue> issues) {
        Map<IRI, IRI> pins = assertionContext.getRoleDirectionPins();
        for (Map.Entry<IRI, IRI> pin : pins.entrySet()) {
            if (hasPubinfo(nanopub, pin.getKey(), RDF.TYPE, pin.getValue())) continue;
            issues.add(new ComplianceIssue(Severity.WARNING, Graph.PUBINFO, Kind.MISSING_ROLE_PIN,
                    "The assertion template pins the role predicate <" + pin.getKey() + "> as <" + pin.getValue()
                            + ">, but the publication info does not state that", null, null, null));
        }
        if (!pins.isEmpty() && !hasPubinfo(nanopub, nanopub.getUri(), NPX.HAS_NANOPUB_TYPE, KPXL.ROLE_INSTANTIATION)) {
            issues.add(new ComplianceIssue(Severity.WARNING, Graph.PUBINFO, Kind.MISSING_ROLE_PIN,
                    "The assertion template pins a role predicate, but the publication info does not give the nanopublication the type <"
                            + KPXL.ROLE_INSTANTIATION + ">", null, null, null));
        }
    }

    private static boolean hasPubinfo(Nanopub nanopub, IRI subject, IRI predicate, Value object) {
        for (Statement st : nanopub.getPubinfo()) {
            if (st.getSubject().equals(subject) && st.getPredicate().equals(predicate) && st.getObject().equals(object)) return true;
        }
        return false;
    }

    // ---------- descriptions for messages ----------

    // A statement of a template as its name and pattern, e.g. st2 (?paper rdfs:comment ?comment).
    static String describeStatement(Template template, IRI statement) {
        List<IRI> members = template.getStatementIris(statement);
        if (members == null || !template.isGroupedStatement(statement)) members = List.of(statement);
        List<String> patterns = new ArrayList<>();
        for (IRI member : members) {
            patterns.add(describeTerm(template, template.getDeclaredSubject(member)) + " " + describeTerm(template, template.getDeclaredPredicate(member))
                    + " " + describeTerm(template, template.getObject(member)));
        }
        return IriUtils.getPostfix(statement.stringValue()) + " (" + String.join(" . ", patterns) + ")";
    }

    private static String describeTerm(Template template, Value value) {
        if (value == null) return "(undefined)";
        if (value instanceof Literal literal) return LiteralSerialization.serialize(literal);
        IRI iri = (IRI) value;
        if (template.isPlaceholder(iri) || template.isLocalResource(iri)) return "?" + IriUtils.getPostfix(iri.stringValue());
        if (iri.stringValue().startsWith(NTEMPLATE.NAMESPACE)) return "nt:" + iri.stringValue().substring(NTEMPLATE.NAMESPACE.length());
        return "<" + iri + ">";
    }

    static String describeTriple(Statement triple) {
        return PlaceholderMatcher.describe(triple.getSubject()) + " " + PlaceholderMatcher.describe(triple.getPredicate()) + " "
                + PlaceholderMatcher.describe(triple.getObject());
    }

}
