package org.nanopub.templates;

import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubUtils;
import org.nanopub.NanopubWithNs;
import org.nanopub.templates.FillRequest.Mode;
import org.nanopub.vocabulary.KPXL;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Makes a nanopublication from templates and the values of their placeholders, as the
 * publishing form of Nanodash does for a person who fills it in.
 * <p>
 * The templates say which triples the values give: optional, repeatable and grouped
 * statements, local and introduced resources, the types and the label of the
 * nanopublication. A {@link FillRequest} names the templates and gives the values.
 * <ul>
 *   <li>A statement that is not optional needs all its values. An optional statement, or an
 *       optional member of a grouped statement, is left out if a value is missing.</li>
 *   <li>A repeatable statement is repeated as often as values are given for the placeholders
 *       that have a value of their own in each repetition ({@code name}, {@code name__1},
 *       {@code name__2}, ...).</li>
 *   <li>A placeholder without a value takes the default value of the template, if there is
 *       one.</li>
 * </ul>
 * Nothing is made if something is wrong; {@link TemplateFillException} then lists all of it:
 * values that are missing, values that break a constraint of their placeholder, names no
 * placeholder answers to, and values that would be lost because the statement they belong to
 * is left out.
 * <p>
 * The provenance template is the one the request names, or the default one of the assertion
 * template, or {@link #DEFAULT_PROVENANCE_TEMPLATE}. The publication info is made from
 * {@link #CREATOR_PUBINFO_TEMPLATE} and {@link #LICENSE_PUBINFO_TEMPLATE}, the template for
 * the link to an existing nanopublication if there is one (see {@link Mode}), the templates
 * the assertion template requires, and those the request names. A template the request names
 * takes the place of one of these if it is another version of it, or gives the same
 * statements. All templates are used at exactly the version named.
 * <p>
 * The nanopublication is not signed, and its URI is a temporary one that it loses when it is
 * signed. What a form adds by looking things up is not added: the labels of values chosen
 * from a list ({@code nt:hasLabelFromApi}).
 * <p>
 * A filler keeps the templates it has retrieved. It is not safe for use by several threads
 * at a time.
 */
public class TemplateFiller {

    /**
     * The provenance template used if neither the request nor the assertion template names
     * one: "Attributed to myself/others".
     */
    public static final String DEFAULT_PROVENANCE_TEMPLATE = "https://w3id.org/np/RA7lSq6MuK_TIC6JMSHvLtee3lpLoZDOqLJCLXevnrPoU";

    /**
     * The publication info template that states the creator.
     */
    public static final String CREATOR_PUBINFO_TEMPLATE = "https://w3id.org/np/RAukAcWHRDlkqxk7H2XNSegc1WnHI569INvNr-xdptDGI";

    /**
     * The publication info template that states the license.
     */
    public static final String LICENSE_PUBINFO_TEMPLATE = "https://w3id.org/np/RACJ58Gvyn91LqCKIO9zu1eijDQIeEff28iyDrJgjSJF8";

    /**
     * The publication info template that links to the nanopublication that is superseded.
     */
    public static final String SUPERSEDES_PUBINFO_TEMPLATE = "https://w3id.org/np/RAoTD7udB2KtUuOuAe74tJi1t3VzK0DyWS7rYVAq1GRvw";

    /**
     * The publication info template that links to the nanopublication that is overridden or
     * derived from.
     */
    public static final String DERIVED_FROM_PUBINFO_TEMPLATE = "https://w3id.org/np/RARW4MsFkHuwjycNElvEVtuMjpf4yWDL10-0C5l2MqqRQ";

    /**
     * The namespace a nanopublication gets its URI in if nothing says otherwise.
     */
    public static final String DEFAULT_TARGET_NAMESPACE = "https://w3id.org/np/";

    // The name of the placeholder for the linked nanopublication in the two templates above.
    private static final String SOURCE_PLACEHOLDER = "np";

    private static final int MAX_REPETITIONS = 10000;
    private static final Pattern REPETITION_NAME = Pattern.compile("^(.+)__([0-9]{1,5})$");
    private static final Pattern LABEL_VARIABLE = Pattern.compile("\\$\\{([_a-zA-Z0-9-]+)\\}");

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private final NanopubResolver resolver;
    private final Map<String, Template> templates = new HashMap<>();

    /**
     * Creates a filler that retrieves templates from the nanopublication network.
     */
    public TemplateFiller() {
        this(NanopubResolver.DEFAULT);
    }

    /**
     * Creates a filler that retrieves templates, and the nanopublication a new one is a
     * version of, with the given resolver.
     *
     * @param resolver retrieves nanopublications by their ID
     */
    public TemplateFiller(NanopubResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    /**
     * Makes the nanopublication a request describes.
     *
     * @param request the templates, the values and the creator
     * @return the nanopublication, not signed
     * @throws TemplateFillException if it cannot be made as requested; nothing is left out or
     *                               guessed to get around a problem
     */
    public Nanopub fill(FillRequest request) throws TemplateFillException {
        Objects.requireNonNull(request, "request");
        List<String> problems = new ArrayList<>();

        Template assertionTemplate = getTemplate(request.getTemplate(), "assertion", problems);
        if (assertionTemplate == null) throw new TemplateFillException(problems);
        Nanopub source = getSource(request, problems);

        IRI defaultProvenance = assertionTemplate.getDefaultProvenance();
        String provenanceId = request.getProvenanceTemplate() != null ? request.getProvenanceTemplate()
                : defaultProvenance != null ? defaultProvenance.stringValue() : DEFAULT_PROVENANCE_TEMPLATE;
        Template provenanceTemplate = getTemplate(provenanceId, "provenance", problems);
        Map<Template, TemplateValues> pubinfoTemplates = getPubinfoTemplates(request, assertionTemplate, problems);
        if (!problems.isEmpty()) throw new TemplateFillException(problems);

        String nanopubUri = getNanopubUri(request, assertionTemplate, source);
        GraphFill assertion = new GraphFill(assertionTemplate, request.getValues(), "param_", "assertion", nanopubUri, request, source);
        assertion.run(problems);
        GraphFill provenance = new GraphFill(provenanceTemplate, request.getProvenanceValues(), "prparam_", "provenance", nanopubUri, request, null);
        provenance.run(problems);
        List<GraphFill> pubinfos = new ArrayList<>();
        for (Map.Entry<Template, TemplateValues> e : pubinfoTemplates.entrySet()) {
            // The number is the one the template has in the request, for the names in messages.
            int number = request.getPubinfoTemplates().indexOf(e.getKey().getId()) + 1;
            boolean named = number > 0;
            if (!named) number = request.getPubinfoTemplates().size() + 1;
            GraphFill pubinfo = new GraphFill(e.getKey(), e.getValue(), "piparam" + number + "_", "publication info", nanopubUri, request, null);
            int problemsBefore = problems.size();
            pubinfo.run(problems);
            if (!named && problems.size() > problemsBefore) {
                problems.add("To give values for the publication info template <" + e.getKey().getId() + ">, which is used without being named, name it as 'pitemplate" + number + "'");
            }
            pubinfos.add(pubinfo);
        }
        if (!problems.isEmpty()) throw new TemplateFillException(problems);

        try {
            return assemble(request, nanopubUri, assertion, provenance, pubinfos);
        } catch (MalformedNanopubException | NanopubAlreadyFinalizedException ex) {
            throw new TemplateFillException("The values do not give a well-formed nanopublication: " + ex.getMessage());
        }
    }

    // ---------- the templates ----------

    private Template getTemplate(String id, String kind, List<String> problems) {
        Template template = templates.get(id);
        if (template == null && !templates.containsKey(id)) {
            try {
                template = new Template(id, resolver);
            } catch (MalformedTemplateException ex) {
                problems.add("The template <" + id + "> cannot be used: " + ex.getMessage());
            } catch (RuntimeException ex) {
                problems.add("The template <" + id + "> cannot be retrieved: " + ex);
            }
            if (template != null) templates.put(id, template);
        }
        if (template == null) return null;
        String actual = template.isProvenanceTemplate() ? "provenance" : template.isPubinfoTemplate() ? "publication info" : "assertion";
        if (!actual.equals(kind)) {
            problems.add("The template <" + id + "> is a template for the " + actual + ", not for the " + kind);
            return null;
        }
        for (String error : template.getStatementErrors()) {
            problems.add("The template <" + id + "> has an error: " + error);
        }
        return template;
    }

    private Nanopub getSource(FillRequest request, List<String> problems) {
        if (request.getSource() == null) return null;
        Nanopub source = null;
        try {
            source = resolver.getNanopub(request.getSource());
        } catch (RuntimeException ex) {
            problems.add("The nanopublication <" + request.getSource() + "> cannot be retrieved: " + ex);
            return null;
        }
        if (source == null) problems.add("The nanopublication <" + request.getSource() + "> cannot be retrieved");
        return source;
    }

    // The templates of the publication info with their values: those a nanopublication always
    // gets, with a template of the request in the place of another version of itself, and
    // then the other templates of the request.
    private Map<Template, TemplateValues> getPubinfoTemplates(FillRequest request, Template assertionTemplate, List<String> problems) {
        Map<Template, TemplateValues> requested = new LinkedHashMap<>();
        for (String id : request.getPubinfoTemplates()) {
            Template template = getTemplate(id, "publication info", problems);
            if (template != null) requested.put(template, request.getPubinfoValues(id));
        }
        List<String> standard = new ArrayList<>(List.of(CREATOR_PUBINFO_TEMPLATE, LICENSE_PUBINFO_TEMPLATE));
        String sourceTemplate = request.getMode() == null ? null
                : request.getMode() == Mode.SUPERSEDE ? SUPERSEDES_PUBINFO_TEMPLATE : DERIVED_FROM_PUBINFO_TEMPLATE;
        if (sourceTemplate != null) standard.add(sourceTemplate);
        for (IRI required : assertionTemplate.getRequiredPubInfoElements()) {
            if (!standard.contains(required.stringValue())) standard.add(required.stringValue());
        }
        Map<Template, TemplateValues> result = new LinkedHashMap<>();
        for (String id : standard) {
            Template template = getTemplate(id, "publication info", problems);
            if (template == null) continue;
            Template inRequest = null;
            for (Template t : requested.keySet()) {
                if (inRequest == null && isInPlaceOf(t, template)) inRequest = t;
            }
            Template used = inRequest != null ? inRequest : template;
            TemplateValues values = inRequest != null ? requested.get(inRequest) : new TemplateValues();
            if (id.equals(sourceTemplate) && hasPlaceholder(used, SOURCE_PLACEHOLDER)) {
                // The link to the existing nanopublication is given by the request as a whole.
                RawValue given = values.get(SOURCE_PLACEHOLDER);
                if (given != null && !given.isEmpty() && !given.text().equals(request.getSource())) {
                    problems.add("The request names <" + request.getSource() + "> with '" + request.getMode().getParam() + "', but gives the template for that link <"
                            + given.text() + ">; leave that value out");
                }
                TemplateValues withSource = new TemplateValues();
                values.copyTo(withSource);
                values = withSource.set(SOURCE_PLACEHOLDER, request.getSource());
            }
            if (!result.containsKey(used)) result.put(used, values);
        }
        requested.forEach(result::putIfAbsent);
        return result;
    }

    // Whether a template of the request stands in the place of a standard one: it is the
    // same template, possibly at another version, or it gives the same statements. The
    // latter is how another version of a template is recognised that does not say what it
    // is a version of: without it, a nanopublication would get its license twice.
    private static boolean isInPlaceOf(Template requested, Template standard) {
        if (requested == standard || requested.hasId(standard.getId()) || standard.hasId(requested.getId())) return true;
        IRI kind = requested.getTemplateKindIri();
        if (kind != null && kind.equals(standard.getTemplateKindIri())) return true;
        Set<String> given = new HashSet<>();
        TemplateStructure requestedStructure = new TemplateStructure(requested);
        for (IRI statement : requested.getStatementIris()) {
            for (IRI member : requestedStructure.getMembers(statement)) given.add(getFixedPart(requested, requestedStructure.getPositions(member)));
        }
        TemplateStructure standardStructure = new TemplateStructure(standard);
        boolean any = false;
        for (IRI statement : standard.getStatementIris()) {
            for (IRI member : standardStructure.getMembers(statement)) {
                String fixedPart = getFixedPart(standard, standardStructure.getPositions(member));
                if (fixedPart == null || !given.contains(fixedPart)) return false;
                any = true;
            }
        }
        return any;
    }

    // The subject and predicate of a statement, if the template fixes both.
    private static String getFixedPart(Template template, Value[] positions) {
        if (!(positions[0] instanceof IRI subject) || !(positions[1] instanceof IRI predicate)) return null;
        if (isOpen(template, subject) || isOpen(template, predicate)) return null;
        return subject.stringValue() + " " + predicate.stringValue();
    }

    private static boolean isOpen(Template template, IRI iri) {
        return template.isPlaceholder(iri) || template.isLocalResource(iri);
    }

    private static boolean hasPlaceholder(Template template, String name) {
        TemplateStructure structure = new TemplateStructure(template);
        for (IRI statement : template.getStatementIris()) {
            for (IRI member : structure.getMembers(statement)) {
                for (Value position : structure.getPositions(member)) {
                    if (position instanceof IRI iri && template.isPlaceholder(iri) && TemplateValues.getName(iri).equals(name)) return true;
                }
            }
        }
        return false;
    }

    // The URI of the nanopublication to make: a temporary one, or one with the marker that
    // signing replaces by the artifact code where the namespace is not the usual one.
    private static String getNanopubUri(FillRequest request, Template assertionTemplate, Nanopub source) {
        String namespace = request.getTargetNamespace();
        if (namespace == null) namespace = assertionTemplate.getTargetNamespace();
        if (namespace == null && source != null && request.getMode() == Mode.SUPERSEDE) {
            String artifactCode = TrustyUriUtils.getArtifactCode(source.getUri().stringValue());
            if (artifactCode != null && source.getUri().stringValue().endsWith(artifactCode)) {
                String uri = source.getUri().stringValue();
                namespace = uri.substring(0, uri.length() - artifactCode.length());
            }
        }
        if (namespace == null || namespace.equals(DEFAULT_TARGET_NAMESPACE)) return NanopubUtils.createTempNanopubIri().stringValue();
        return namespace + IriUtils.ARTIFACT_CODE_TARGET_MARKER + "/";
    }

    // ---------- one graph ----------

    // Fills one template: binds the values to its placeholders and gives the triples.
    private class GraphFill {

        final Template template;
        final TemplateStructure structure;
        final TemplateValues values;
        final String paramPrefix;
        final String graphName;
        final FillRequest request;
        final BindingContext context;
        final List<Statement> triples = new ArrayList<>();

        // Only for the assertion of a nanopublication that is a version of another one:
        final Nanopub source;
        final BindingContext sourceContext;
        final Set<String> sourceIntroduced = new HashSet<>();
        Value sourceRoot;

        // The names of values that a position has taken:
        final Set<String> usedNames = new HashSet<>();
        // The positions that have no value, with or without having been given one:
        final Set<IRI> withoutValue = new HashSet<>();
        // The positions whose given value cannot stand there, which has been reported:
        final Set<IRI> rejected = new HashSet<>();
        // The positions whose value was given, with the name it was given under:
        final Map<IRI, String> given = new LinkedHashMap<>();
        // Of those, the ones that stand in a triple:
        final Set<IRI> inTriple = new HashSet<>();
        // For the others, why the statement they belong to was left out:
        final Map<IRI, String> leftOut = new HashMap<>();

        GraphFill(Template template, TemplateValues values, String paramPrefix, String graphName, String nanopubUri, FillRequest request, Nanopub source) {
            this.template = template;
            this.structure = new TemplateStructure(template);
            this.values = values;
            this.paramPrefix = paramPrefix;
            this.graphName = graphName;
            this.request = request;
            this.context = BindingContext.forNewNanopub(template, nanopubUri, request.getCreator());
            context.setPrefixBase(BindingContext.SPACE_TOKEN, request.getSpace());
            context.setPrefixBase(BindingContext.NAMESPACE_TOKEN, request.getNamespace());
            this.source = source;
            if (source != null) {
                sourceContext = BindingContext.forNanopub(template, source);
                sourceContext.setPrefixBase(BindingContext.SPACE_TOKEN, request.getSpace());
                sourceContext.setPrefixBase(BindingContext.NAMESPACE_TOKEN, request.getNamespace());
                if (request.getMode() != Mode.DERIVE) {
                    sourceIntroduced.addAll(NanopubUtils.getIntroducedIriIds(source));
                    sourceIntroduced.removeAll(NanopubUtils.getEmbeddedIriIds(source));
                }
            } else {
                sourceContext = null;
            }
        }

        void run(List<String> problems) {
            for (IRI statement : template.getStatementIris()) {
                int repetitions = getRepetitions(statement);
                for (int repetition = 0; repetition < repetitions; repetition++) {
                    fillRepetition(statement, repetition, problems);
                }
            }
            for (String name : values.getNames()) {
                if (!usedNames.contains(name)) {
                    problems.add("No placeholder of the " + graphName + " template answers to '" + paramPrefix + name + "'" + describeNames());
                }
            }
            for (Map.Entry<IRI, String> e : given.entrySet()) {
                if (inTriple.contains(e.getKey()) || rejected.contains(e.getKey())) continue;
                problems.add("The value of '" + paramPrefix + e.getValue() + "' would be lost: " + leftOut.getOrDefault(e.getKey(), "its statement is left out"));
            }
            for (Violation violation : context.getViolations()) {
                if (violation.kind() == Violation.Kind.RESERVED_NAME && isReservedNameCarriedOver(violation.value())) continue;
                String name = violation.templateValue() instanceof IRI key && given.containsKey(key) ? " (" + paramPrefix + given.get(key) + ")" : "";
                String problem = violation.message() + name;
                if (!problems.contains(problem)) problems.add(problem);
            }
        }

        // How often a statement is repeated: as often as values are given for the
        // placeholders that have a value of their own in each of its repetitions.
        private int getRepetitions(IRI statement) {
            if (!template.isRepeatableStatement(statement)) return 1;
            Set<String> ownNames = new HashSet<>();
            for (IRI member : structure.getMembers(statement)) {
                for (Value position : structure.getPositions(member)) {
                    if (position instanceof IRI iri && structure.hasOwnValuePerRepetition(iri) && takesValue(iri)) ownNames.add(TemplateValues.getName(iri));
                }
            }
            int repetitions = 1;
            for (String name : values.getNames()) {
                Matcher m = REPETITION_NAME.matcher(name);
                if (!m.matches() || !ownNames.contains(m.group(1))) continue;
                int index = Integer.parseInt(m.group(2));
                if (index >= MAX_REPETITIONS) continue;
                if (values.get(name).isEmpty()) {
                    // No value on purpose is not a further repetition.
                    usedNames.add(name);
                } else {
                    repetitions = Math.max(repetitions, index + 1);
                }
            }
            return repetitions;
        }

        private void fillRepetition(IRI statement, int repetition, List<String> problems) {
            boolean grouped = template.isGroupedStatement(statement);
            List<Statement> found = new ArrayList<>();
            Set<IRI> foundKeys = new LinkedHashSet<>();
            Set<IRI> droppedKeys = new LinkedHashSet<>();
            List<String> missing = new ArrayList<>();
            for (IRI member : structure.getMembers(statement)) {
                Value[] positions = structure.getPositions(member);
                Value[] resolved = new Value[3];
                Set<IRI> memberKeys = new LinkedHashSet<>();
                List<String> memberMissing = new ArrayList<>();
                boolean memberRejected = false;
                for (int i = 0; i < 3; i++) {
                    Value key = structure.getKey(positions[i], repetition);
                    if (key instanceof IRI iri) {
                        memberKeys.add(iri);
                        resolved[i] = getValue(iri, problems);
                        if (resolved[i] == null) {
                            if (rejected.contains(iri)) {
                                memberRejected = true;
                            } else {
                                memberMissing.add(describeMissing(iri));
                            }
                        }
                    } else {
                        resolved[i] = key;
                    }
                }
                boolean complete = resolved[0] != null && resolved[1] != null && resolved[2] != null;
                if (complete && !(resolved[0] instanceof Resource && resolved[1] instanceof IRI)) {
                    problems.add("The statement " + describeStatement(member) + " of the " + graphName + " template gives a triple with a literal as its subject or predicate");
                    complete = false;
                    memberRejected = true;
                }
                if (complete) {
                    found.add(vf.createStatement((Resource) resolved[0], (IRI) resolved[1], resolved[2]));
                    foundKeys.addAll(memberKeys);
                } else if (grouped && template.isOptionalStatement(member)) {
                    // An optional member of a group is left out by itself.
                    String reason = "the statement " + describeStatement(member) + " it belongs to lacks " + String.join(" and ", memberMissing);
                    for (IRI key : memberKeys) leftOut.putIfAbsent(key, reason);
                    // What is wrong with a rejected value has been said already.
                    if (memberRejected) inTriple.addAll(memberKeys);
                } else {
                    droppedKeys.addAll(memberKeys);
                    missing.addAll(memberMissing);
                    if (memberRejected) missing.add(null);
                }
            }
            if (missing.isEmpty()) {
                triples.addAll(found);
                inTriple.addAll(foundKeys);
                return;
            }
            // The statement is left out as a whole. That is fine for an optional one; a value
            // given for it is then reported as lost, unless it stands in another statement.
            boolean anyRejected = missing.removeIf(Objects::isNull);
            String name = describeStatement(statement) + (repetition > 0 ? " (repetition " + (repetition + 1) + ")" : "");
            if (anyRejected) {
                // What is wrong with a rejected value has been said already.
                inTriple.addAll(foundKeys);
                inTriple.addAll(droppedKeys);
            }
            if (missing.isEmpty()) return;
            String lacks = String.join(" and ", new LinkedHashSet<>(missing));
            if (template.isOptionalStatement(statement) && repetition == 0) {
                for (IRI key : foundKeys) leftOut.putIfAbsent(key, "the statement " + name + " it belongs to lacks " + lacks);
                for (IRI key : droppedKeys) leftOut.putIfAbsent(key, "the statement " + name + " it belongs to lacks " + lacks);
            } else {
                problems.add("The statement " + name + " of the " + graphName + " template lacks " + lacks);
                // Reported here, so not again as lost values:
                inTriple.addAll(foundKeys);
                inTriple.addAll(droppedKeys);
            }
        }

        // Whether the position takes its value from the request.
        private boolean takesValue(IRI key) {
            if (key.stringValue().startsWith(NTEMPLATE.NAMESPACE)) return false;
            if (template.isSequenceElementPlaceholder(key)) return false;
            return PlaceholderMatcher.of(template, key).isPlaceholder();
        }

        // Returns the value of a position, binding it when the position is first met.
        private Value getValue(IRI key, List<String> problems) {
            if (context.isBound(key)) return context.get(key);
            if (withoutValue.contains(key)) return null;
            Value value = takesValue(key) ? getGivenValue(key, problems) : getFixedValue(key);
            if (value == null) withoutValue.add(key);
            return value;
        }

        // The value of a position the template fixes: a fixed IRI, a local resource, the
        // nanopublication, its assertion, its creator, a position in a sequence.
        private Value getFixedValue(IRI key) {
            PlaceholderMatcher matcher = PlaceholderMatcher.of(template, key);
            Value value = matcher.resolve(context);
            if (value == null) return null;
            Value carriedOver = getCarriedOver(key, value, sourceContext == null ? null : matcher.resolve(sourceContext));
            if (carriedOver != null) {
                context.pin(key, carriedOver);
                return carriedOver;
            }
            context.bind(key, value);
            return value;
        }

        private Value getGivenValue(IRI key, List<String> problems) {
            PlaceholderMatcher matcher = PlaceholderMatcher.of(template, key);
            String name = TemplateValues.getName(key);
            RawValue raw = values.get(name);
            if (raw != null) usedNames.add(name);
            if (template.isRootNanopubPlaceholder(key)) {
                Value root = getRoot(key, raw, matcher, problems);
                if (root != null) context.pin(key, root);
                return root;
            }
            if (raw != null && raw.isEmpty()) return null;
            Value value;
            Value inSource = null;
            if (raw != null) {
                given.put(key, name);
                try {
                    value = matcher.toValue(raw, context);
                    if (sourceContext != null) inSource = matcher.toValue(raw, sourceContext);
                } catch (IllegalArgumentException ex) {
                    problems.add("The value of '" + paramPrefix + name + "' cannot be used: " + ex.getMessage());
                    rejected.add(key);
                    return null;
                }
            } else {
                value = getDefault(key, matcher, context);
                if (value != null && sourceContext != null) inSource = getDefault(key, matcher, sourceContext);
            }
            if (value == null) return null;
            Value carriedOver = getCarriedOver(key, value, inSource);
            if (carriedOver != null) {
                context.pin(key, carriedOver);
                return carriedOver;
            }
            MatchResult result = context.bind(key, value);
            if (result.isBindable()) return value;
            if (raw == null) return null;
            for (Violation violation : result.violations()) {
                if (violation.blocking()) problems.add(violation.message() + " (" + paramPrefix + name + ")");
            }
            rejected.add(key);
            return null;
        }

        // The default value of a placeholder as it stands in a nanopublication, or null if
        // there is none that can stand there.
        private Value getDefault(IRI key, PlaceholderMatcher matcher, BindingContext context) {
            Value value = template.getDefault(key);
            if (value == null) return null;
            if (value.equals(NTEMPLATE.CREATOR_PLACEHOLDER)) return request.getCreator();
            String templateUri = template.getNanopub().getUri().stringValue();
            if (value instanceof IRI iri && iri.stringValue().startsWith(templateUri) && !iri.stringValue().equals(templateUri)) {
                // An IRI within the template stands for the one of that name within the
                // nanopublication that is made.
                try {
                    value = matcher.toValue(new RawValue(IriUtils.getPostfix(iri.stringValue())), context);
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }
            return value != null && matcher.match(value, context).isBindable() ? value : null;
        }

        // A resource that the existing nanopublication introduces keeps its IRI in a new
        // version. Returns that IRI for a position, or null if the position does not stand
        // for a resource the existing nanopublication introduces. It is the IRI the position
        // stands for in the existing nanopublication, or, as that one may itself have kept
        // the IRI from a version before it, the one IRI it introduces under the same name.
        private Value getCarriedOver(IRI key, Value value, Value inSource) {
            if (sourceIntroduced.isEmpty() || !template.isIntroducedResource(key)) return null;
            String artifactCode = TrustyUriUtils.getArtifactCode(source.getUri().stringValue());
            if (inSource instanceof IRI) {
                String iri = inSource.stringValue();
                if (artifactCode != null) iri = iri.replace(IriUtils.ARTIFACT_CODE_TARGET_MARKER, artifactCode);
                if (sourceIntroduced.contains(iri)) return vf.createIRI(iri);
            }
            String name = context.getLocalName(value);
            String withMarker = template.getTemplateIri(key).stringValue();
            if (!IriUtils.hasArtifactCodeMarker(withMarker) || takesValue(key)) withMarker = null;
            List<String> sameName = new ArrayList<>();
            for (String iri : sourceIntroduced) {
                if (name != null && name.equals(IriUtils.getPostfix(iri))) {
                    sameName.add(iri);
                } else if (withMarker != null && IriUtils.matchesWithArtifactCode(withMarker, vf.createIRI(iri))) {
                    sameName.add(iri);
                }
            }
            return sameName.size() == 1 ? vf.createIRI(sameName.get(0)) : null;
        }

        // The root definition: the nanopublication itself, unless it is a version of one
        // that has another root, or the request names one.
        private Value getRoot(IRI key, RawValue raw, PlaceholderMatcher matcher, List<String> problems) {
            if (request.getMode() == Mode.DERIVE) return context.getNanopubUri();
            if (raw != null && !raw.isEmpty()) {
                try {
                    return matcher.toValue(raw, context);
                } catch (IllegalArgumentException ex) {
                    problems.add("The value of '" + paramPrefix + TemplateValues.getName(key) + "' cannot be used: " + ex.getMessage());
                    rejected.add(key);
                    return null;
                }
            }
            if (source == null) return context.getNanopubUri();
            if (sourceRoot == null) {
                // What the existing nanopublication has in that position, and the
                // nanopublication itself if it has nothing there.
                TemplateMatch match = new TemplateMatcher(template).match(source.getAssertion(), BindingContext.forNanopub(template, source));
                sourceRoot = match.getContext().get(template.getTemplateIri(key));
                if (sourceRoot == null) sourceRoot = source.getUri();
            }
            return sourceRoot;
        }

        // A name the nanopublication keeps for itself can stand if the existing
        // nanopublication has it already: a new version keeps the shape of the old one.
        private boolean isReservedNameCarriedOver(Value value) {
            if (source == null || request.getMode() == Mode.DERIVE) return false;
            String localName = context.getLocalName(value);
            if (localName == null) return false;
            IRI inSource = vf.createIRI(source.getUri().stringValue() + "/" + localName);
            // Only the assertion counts: every nanopublication names its own parts in its
            // head and its provenance, which says nothing about a resource of that name.
            for (Statement st : source.getAssertion()) {
                if (st.getSubject().equals(inSource) || st.getPredicate().equals(inSource) || st.getObject().equals(inSource)) return true;
            }
            return false;
        }

        private String describeStatement(IRI statement) {
            return IriUtils.getPostfix(statement.stringValue());
        }

        private String describeMissing(IRI key) {
            if (key.equals(NTEMPLATE.CREATOR_PLACEHOLDER)) return "the creator (creator)";
            if (!takesValue(key)) return "a value for <" + key.stringValue() + ">";
            String name = TemplateValues.getName(key);
            String label = template.getLabel(key);
            return "a value for '" + paramPrefix + name + "'" + (label == null ? "" : " (" + label + ")");
        }

        // The names values can be given under, for the message about one that is not known.
        private String describeNames() {
            Set<String> names = new TreeSet<>();
            for (IRI statement : template.getStatementIris()) {
                for (IRI member : structure.getMembers(statement)) {
                    for (Value position : structure.getPositions(member)) {
                        if (position instanceof IRI iri && takesValue(iri)) names.add(TemplateValues.getName(iri));
                    }
                }
            }
            if (names.isEmpty()) return "; the template has no placeholders";
            if (names.size() > 40) return "";
            return "; its placeholders are: " + String.join(", ", names);
        }

    }

    // ---------- the nanopublication ----------

    private Nanopub assemble(FillRequest request, String nanopubUri, GraphFill assertion, GraphFill provenance, List<GraphFill> pubinfos)
            throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = new NanopubCreator(nanopubUri);
        creator.setAssertionUri(assertion.context.getAssertionUri());
        List<GraphFill> all = new ArrayList<>(List.of(assertion, provenance));
        all.addAll(pubinfos);
        for (GraphFill fill : all) {
            if (!(fill.template.getNanopub() instanceof NanopubWithNs withNs)) continue;
            for (String prefix : withNs.getNsPrefixes()) {
                if (prefix.equals("this") || prefix.equals("sub")) continue;
                creator.addNamespace(prefix, withNs.getNamespace(prefix));
            }
        }
        creator.addNamespace("this", nanopubUri);
        creator.addNamespace("sub", assertion.context.getNamespace());
        creator.addNamespace("np", "http://www.nanopub.org/nschema#");
        creator.addNamespace("npx", NPX.NAMESPACE);
        creator.addNamespace("nt", NTEMPLATE.NAMESPACE);
        creator.addNamespace("dct", "http://purl.org/dc/terms/");
        creator.addNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
        creator.addNamespace("rdfs", RDFS.NAMESPACE);
        creator.addNamespace("foaf", FOAF.NAMESPACE);
        creator.setRemoveUnusedPrefixesEnabled(true);

        for (Statement st : assertion.triples) creator.addAssertionStatement(st.getSubject(), st.getPredicate(), st.getObject());
        for (Statement st : provenance.triples) creator.addProvenanceStatement(st.getSubject(), st.getPredicate(), st.getObject());
        for (GraphFill pubinfo : pubinfos) {
            for (Statement st : pubinfo.triples) creator.addPubinfoStatement(st.getSubject(), st.getPredicate(), st.getObject());
        }
        for (IRI introduced : assertion.context.getIntroducedIris()) creator.addPubinfoStatement(NPX.INTRODUCES, introduced);
        for (IRI embedded : assertion.context.getEmbeddedIris()) creator.addPubinfoStatement(NPX.EMBEDS, embedded);
        Map<IRI, IRI> rolePins = assertion.context.getRoleDirectionPins();
        if (!rolePins.isEmpty()) {
            // A role predicate with a direction pin makes this a role instantiation, which
            // is what the pins are read for.
            creator.addPubinfoStatement(NPX.HAS_NANOPUB_TYPE, KPXL.ROLE_INSTANTIATION);
            for (Map.Entry<IRI, IRI> pin : rolePins.entrySet()) creator.addPubinfoStatement(pin.getKey(), RDF.TYPE, pin.getValue());
        }
        if (request.getCreationTime() != null) {
            creator.addTimestamp(request.getCreationTime());
        } else {
            creator.addTimestampNow();
        }
        creator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_TEMPLATE, vf.createIRI(assertion.template.getId()));
        creator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE, vf.createIRI(provenance.template.getId()));
        for (GraphFill pubinfo : pubinfos) {
            creator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE, vf.createIRI(pubinfo.template.getId()));
        }
        String label = getNanopubLabel(request, assertion, creator.getCurrentPubinfoStatements());
        if (label != null) creator.addPubinfoStatement(RDFS.LABEL, vf.createLiteral(label));
        for (IRI type : assertion.template.getTargetNanopubTypes()) creator.addPubinfoStatement(NPX.HAS_NANOPUB_TYPE, type);
        if (request.getCreator() != null && request.getCreatorName() != null) {
            creator.addPubinfoStatement(request.getCreator(), FOAF.NAME, vf.createLiteral(request.getCreatorName()));
        }
        return creator.finalizeNanopub();
    }

    // The label of the nanopublication, from the pattern of the assertion template: each
    // ${name} in it stands for the value of that placeholder, or for a label of that value
    // if it is an IRI.
    private static String getNanopubLabel(FillRequest request, GraphFill assertion, List<Statement> pubinfo) {
        String pattern = assertion.template.getNanopubLabelPattern();
        if (pattern == null) return null;
        Map<String, Value> valuesByName = new HashMap<>();
        for (Map.Entry<IRI, Value> e : assertion.context.getBindings().entrySet()) {
            valuesByName.putIfAbsent(TemplateValues.getName(e.getKey()), e.getValue());
        }
        Map<Value, String> labels = new HashMap<>();
        for (Statement st : pubinfo) {
            if (st.getPredicate().equals(RDFS.LABEL) && st.getObject() instanceof Literal literal) labels.put(st.getSubject(), literal.stringValue());
        }
        Matcher m = LABEL_VARIABLE.matcher(pattern);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Value value = valuesByName.get(m.group(1));
            String text = "";
            if (value instanceof IRI iri) {
                String label = labels.containsKey(iri) ? labels.get(iri) : assertion.template.getLabel(iri);
                if (iri.equals(request.getCreator()) && request.getCreatorName() != null) label = request.getCreatorName();
                text = label != null && !label.isEmpty() ? label.replaceFirst(" - .*$", "") : getShortName(iri.stringValue());
            } else if (value != null) {
                text = value.stringValue();
            }
            // Markup has no place in a label.
            text = text.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
            if (text.length() > 100) text = text.substring(0, 97) + "...";
            m.appendReplacement(sb, Matcher.quoteReplacement(text));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // A short name for an IRI: its last part, without an artifact code in front.
    private static String getShortName(String iri) {
        if (iri.startsWith("https://doi.org/") || iri.startsWith("http://dx.doi.org/")) {
            return iri.replaceFirst("^https?://(dx\\.)?doi.org/", "doi:");
        }
        String name = iri.replaceFirst("\\?.*$", "").replaceFirst("[/#]$", "");
        name = name.replaceFirst("^.*[/#]([^/#]*)[/#]([0-9]+)$", "$1/$2");
        if (name.contains("#")) {
            name = name.replaceFirst("^.*#(.*[^0-9].*)$", "$1");
        } else {
            name = name.replaceFirst("^.*/([^/]*[^0-9/][^/]*)$", "$1");
        }
        name = name.replaceFirst("((^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{8})[A-Za-z0-9\\-_]{35}$", "$1");
        name = name.replaceFirst("(^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{43}[^A-Za-z0-9\\-_](.+)$", "$2");
        try {
            return URLDecoder.decode(name, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return name;
        }
    }

}
