package org.nanopub.templates;

import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.vocabulary.KPXL;
import org.nanopub.vocabulary.KPXL_GRLC;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * A nanopublication template: a nanopublication that describes, with placeholders,
 * statements and constraints, how nanopublications of a certain kind are structured.
 * <p>
 * A template is read from its nanopublication, in one of these shapes:
 * <ul>
 *   <li><b>legacy</b>: the template node is the assertion graph URI, and the template is
 *       referred to by the URI of its nanopublication;</li>
 *   <li><b>embedded identity</b>: the template node is an IRI of its own in the
 *       nanopublication's namespace, and the template is referred to by that IRI;</li>
 *   <li><b>SHACL-based</b> (experimental): the assertion holds no template node but a
 *       SHACL node shape with a target class.</li>
 * </ul>
 * Reading a template needs no network access. The one thing that is retrieved later, and
 * only when asked for, are the nanopublications a restricted-choice placeholder takes its
 * possible values from ({@code nt:possibleValuesFrom}); see {@link #getPossibleValues(IRI)}.
 * <p>
 * Most methods take the IRI of a placeholder, resource or statement as it appears in the
 * template. They also accept the form such an IRI takes in a nanopublication being filled
 * in, with a {@code __N} repetition suffix or an expanded artifact-code marker; see
 * {@link #getTemplateIri(IRI)}.
 * <p>
 * This class was moved here from Nanodash, and keeps the names and the behaviour of its
 * methods, with these differences:
 * <ul>
 *   <li>{@link #getDescription()} and {@link #getStatementDescription(IRI)} return the text
 *       as published, which may contain HTML and is <b>not sanitized</b>.</li>
 *   <li>The nanopublications holding possible values are retrieved on first use, through a
 *       {@link NanopubResolver}, and not while the template is read. A label the template
 *       itself gives to an IRI takes precedence over one from such a nanopublication.</li>
 *   <li>Looking up possible values in the APIs a template names is left to the caller;
 *       {@link #getPossibleValuesApis(IRI)} returns their URLs.</li>
 *   <li>A template nanopublication need not have a trusty URI, and statements with a
 *       non-IRI subject are passed over. {@link #getStatementIris()} returns an empty list
 *       for a template without statements, and the lists returned cannot be modified.</li>
 * </ul>
 */
public class Template implements Serializable {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(Template.class);

    /**
     * Default target namespace for templates.
     */
    public static final String DEFAULT_TARGET_NAMESPACE = "https://w3id.org/np/";

    private final Nanopub nanopub;
    private final NanopubResolver resolver;
    private String label;
    private String description;

    // TODO: Make all these maps more generic and the code simpler:
    private IRI templateIri;
    private boolean embeddedIdentity = false;
    private IRI templateKindIri;
    private IRI governingSpace;
    private Map<IRI, List<IRI>> typeMap = new HashMap<>();
    private Map<IRI, List<Value>> possibleValueMap = new HashMap<>();
    private Map<IRI, List<IRI>> possibleValuesToLoadMap = new HashMap<>();
    private Map<IRI, List<String>> apiMap = new HashMap<>();
    private Map<IRI, String> labelMap = new HashMap<>();
    // Labels taken from the nanopublications holding possible values, which are only known
    // once those have been retrieved. Consulted after labelMap.
    private Map<IRI, String> possibleValueLabelMap = new HashMap<>();
    // The nanopublications holding possible values that an attempt to retrieve has failed for.
    private Set<IRI> unretrievedValueNanopubs = new HashSet<>();
    private Map<IRI, String> statementDescriptionMap = new HashMap<>();
    private Map<IRI, IRI> datatypeMap = new HashMap<>();
    private Map<IRI, List<IRI>> declaredDatatypesMap = new HashMap<>();
    private Map<IRI, List<DatePrecision>> datePrecisionMap = new HashMap<>();
    private Map<IRI, String> languageTagMap = new HashMap<>();
    private Map<IRI, List<String>> possibleLanguageTagMap = new HashMap<>();
    private Map<IRI, String> prefixMap = new HashMap<>();
    private Map<IRI, String> prefixLabelMap = new HashMap<>();
    private Map<IRI, String> regexMap = new HashMap<>();
    private Map<IRI, List<IRI>> statementMap = new HashMap<>();
    private Map<IRI, IRI> statementSubjects = new HashMap<>();
    private Map<IRI, IRI> statementPredicates = new HashMap<>();
    private Map<IRI, Value> statementObjects = new HashMap<>();
    // Subject/predicate values that are not IRIs (i.e. literals), which is invalid RDF but
    // does occur in published templates. Kept apart from the maps above so the rest of the
    // code keeps seeing IRIs, and reported by getStatementErrors().
    private Map<IRI, Value> nonIriStatementSubjects = new HashMap<>();
    private Map<IRI, Value> nonIriStatementPredicates = new HashMap<>();
    private Map<IRI, Integer> statementOrder = new HashMap<>();
    private IRI defaultProvenance;
    private List<IRI> requiredPubInfoElements = new ArrayList<>();
    private String tag = null;
    private Map<IRI, Value> defaultValues = new HashMap<>();
    private String targetNamespace = null;
    private String nanopubLabelPattern;
    private List<IRI> targetNanopubTypes = new ArrayList<>();

    /**
     * Reads the template with the given ID, retrieving its nanopublication from the
     * nanopublication network.
     *
     * @param templateId the ID of the template: the URI of its nanopublication, or the IRI
     *                   of a template node embedded in it
     * @throws MalformedTemplateException if the nanopublication cannot be retrieved or is not
     *                                    a valid template
     */
    public Template(String templateId) throws MalformedTemplateException {
        this(templateId, NanopubResolver.DEFAULT);
    }

    /**
     * Reads the template with the given ID, retrieving its nanopublication with the given
     * resolver.
     *
     * @param templateId the ID of the template: the URI of its nanopublication, or the IRI
     *                   of a template node embedded in it
     * @param resolver   retrieves the template's nanopublication and, later, those holding
     *                   possible values
     * @throws MalformedTemplateException if the nanopublication cannot be retrieved or is not
     *                                    a valid template
     */
    public Template(String templateId, NanopubResolver resolver) throws MalformedTemplateException {
        this(retrieveTemplateNanopub(templateId, resolver), resolver);
    }

    /**
     * A referenced template may not be retrievable at all, for instance because it was never
     * published to the registries that are asked. Saying so is what lets the caller treat the
     * reference as "no template".
     */
    private static Nanopub retrieveTemplateNanopub(String templateId, NanopubResolver resolver) throws MalformedTemplateException {
        if (templateId == null) throw new MalformedTemplateException("no template ID given");
        Nanopub np = Objects.requireNonNull(resolver, "resolver").getNanopub(templateId);
        if (np == null) throw new MalformedTemplateException("nanopublication could not be retrieved");
        return np;
    }

    /**
     * Reads the template the given nanopublication defines. Nanopublications holding
     * possible values are retrieved from the nanopublication network when first needed.
     *
     * @param np the nanopublication containing the template definition
     * @throws MalformedTemplateException if the nanopublication is not a valid template
     */
    public Template(Nanopub np) throws MalformedTemplateException {
        this(np, NanopubResolver.DEFAULT);
    }

    /**
     * Reads the template the given nanopublication defines.
     *
     * @param np       the nanopublication containing the template definition
     * @param resolver retrieves the nanopublications holding possible values when they are
     *                 first needed; {@link NanopubResolver#NONE} to never retrieve any
     * @throws MalformedTemplateException if the nanopublication is not a valid template
     */
    public Template(Nanopub np, NanopubResolver resolver) throws MalformedTemplateException {
        nanopub = Objects.requireNonNull(np, "nanopub");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        processTemplate(nanopub);
    }

    /**
     * Checks if the template is unlisted.
     *
     * @return true if the template is unlisted, false otherwise.
     */
    public boolean isUnlisted() {
        List<IRI> types = typeMap.get(templateIri);
        return types != null && types.contains(NTEMPLATE.UNLISTED_TEMPLATE);
    }

    /**
     * Checks if the template is transient, meaning its filled content applies only to the
     * specific nanopublication it was published with and is not carried over when that
     * nanopublication is superseded, overridden, derived from, or used as a fill source.
     *
     * @return true if the template is transient, false otherwise.
     */
    public boolean isTransient() {
        List<IRI> types = typeMap.get(templateIri);
        return types != null && types.contains(NTEMPLATE.TRANSIENT_TEMPLATE);
    }

    /**
     * Checks if the template is a provenance template, i.e. one for the provenance graph.
     *
     * @return true if the template is typed as a provenance template, false otherwise.
     */
    public boolean isProvenanceTemplate() {
        List<IRI> types = typeMap.get(templateIri);
        return types != null && types.contains(NTEMPLATE.PROVENANCE_TEMPLATE);
    }

    /**
     * Checks if the template is a publication info template, i.e. one for the pubinfo graph.
     *
     * @return true if the template is typed as a publication info template, false otherwise.
     */
    public boolean isPubinfoTemplate() {
        List<IRI> types = typeMap.get(templateIri);
        return types != null && types.contains(NTEMPLATE.PUBINFO_TEMPLATE);
    }

    /**
     * Returns the Nanopub object representing the template.
     *
     * @return the Nanopub object of the template.
     */
    public Nanopub getNanopub() {
        return nanopub;
    }

    /**
     * Returns the ID of the template: for a template with embedded identity (the
     * template node is a regular embedded IRI in the assertion), that embedded IRI;
     * for a legacy template (the template node is the assertion graph URI), the URI
     * of the nanopublication.
     *
     * @return the ID of the template as a string.
     */
    public String getId() {
        if (embeddedIdentity) return templateIri.stringValue();
        return nanopub.getUri().toString();
    }

    /**
     * Checks whether the given ID refers to this template, in either form: the
     * template's canonical ID ({@link #getId()}) or its nanopublication URI. Use
     * this instead of comparing against {@link #getId()} when the ID to compare
     * comes from outside (page parameters, published references), as those may
     * carry either form.
     *
     * @param id the ID to check
     * @return true if the ID refers to this template
     */
    public boolean hasId(String id) {
        return id != null && (id.equals(getId()) || id.equals(nanopub.getUri().stringValue()));
    }

    /**
     * Returns the template kind IRI, i.e. the stable identifier this template
     * version declares itself a version of ({@code dct:isVersionOf} on the template
     * node), or null if it doesn't declare one.
     *
     * @return the template kind IRI, or null
     */
    public IRI getTemplateKindIri() {
        return templateKindIri;
    }

    /**
     * Returns the space governing this template version ({@code gen:governedBy} on
     * the template node), or null if it doesn't declare one. A governed version's
     * latest-resolution floats space-based within its (kind, space) pair instead of
     * following the supersedes chain.
     *
     * @return the governing space IRI, or null
     */
    public IRI getGoverningSpace() {
        return governingSpace;
    }

    /**
     * Returns the label of the template.
     *
     * @return the label of the template, or a default label if not set.
     */
    public String getLabel() {
        if (label == null) {
            String artifactCode = TrustyUriUtils.getArtifactCode(nanopub.getUri().stringValue());
            // The nanopublication of a template that is not published yet has no artifact code:
            if (artifactCode == null || artifactCode.length() < 10) return "Template " + nanopub.getUri().stringValue();
            return "Template " + artifactCode.substring(0, 10);
        }
        return label;
    }

    /**
     * Returns the description of the template, as published. It may contain HTML and is
     * <b>not sanitized</b>: sanitize or escape it before showing it in a web page.
     *
     * @return the description of the template, or null if not set.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the description that tells how to fill in a statement or statement group, as
     * published. It may contain HTML and is <b>not sanitized</b>: sanitize or escape it
     * before showing it in a web page.
     *
     * @param statementId the IRI of the statement or statement group
     * @return the description, or null if the template gives none
     */
    public String getStatementDescription(IRI statementId) {
        return statementDescriptionMap.get(transform(statementId));
    }

    /**
     * Returns the label of a placeholder, resource, statement or possible value.
     * <p>
     * A label the template itself gives is returned as is. Otherwise the label may come from
     * a nanopublication holding possible values ({@code nt:possibleValuesFrom}), and those
     * that have not been retrieved yet are retrieved first. Unlike
     * {@link #getPossibleValues(IRI)}, this method does not try again to retrieve a
     * nanopublication that could not be retrieved before, so that asking for labels stays
     * cheap when the network is not available.
     *
     * @param iri the IRI to get the label for.
     * @return the label, or null if none is known.
     */
    public String getLabel(IRI iri) {
        iri = transform(iri);
        String l = labelMap.get(iri);
        if (l != null || iri == null) return l;
        synchronized (this) {
            for (IRI placeholder : new ArrayList<>(possibleValuesToLoadMap.keySet())) {
                loadPossibleValues(placeholder, false);
            }
            return possibleValueLabelMap.get(iri);
        }
    }

    /**
     * Returns the first statement of the template the given IRI occurs in, as subject,
     * predicate or object.
     *
     * @param iri the IRI to look for.
     * @return the IRI of that statement, or null if the IRI occurs in none.
     */
    public IRI getFirstOccurrence(IRI iri) {
        iri = transform(iri);
        for (IRI i : getStatementIris()) {
            if (statementMap.containsKey(i)) {
                // grouped statement
                for (IRI g : getStatementIris(i)) {
                    if (iri.equals(statementSubjects.get(g))) return g;
                    if (iri.equals(statementPredicates.get(g))) return g;
                    if (iri.equals(statementObjects.get(g))) return g;
                }
            } else {
                // non-grouped statement
                if (iri.equals(statementSubjects.get(i))) return i;
                if (iri.equals(statementPredicates.get(i))) return i;
                if (iri.equals(statementObjects.get(i))) return i;
            }
        }
        return null;
    }

    /**
     * Returns the datatype for the given literal placeholder IRI.
     *
     * @param iri the literal placeholder IRI.
     * @return the datatype for the literal.
     */
    public IRI getDatatype(IRI iri) {
        iri = transform(iri);
        return datatypeMap.get(iri);
    }

    /**
     * Returns the language tag for the given literal placeholder IRI.
     *
     * @param iri the literal placeholder IRI.
     * @return the language tag for the literal.
     */
    public String getLanguageTag(IRI iri) {
        iri = transform(iri);
        return languageTagMap.get(iri);
    }

    /**
     * Returns the language tags a language-tag-selectable placeholder is restricted to.
     *
     * @param iri the literal placeholder IRI.
     * @return the normalized allowed language tags, or null if unrestricted.
     */
    public List<String> getPossibleLanguageTags(IRI iri) {
        iri = transform(iri);
        return possibleLanguageTagMap.get(iri);
    }

    /**
     * Returns the prefix for the given IRI.
     *
     * @param iri the IRI.
     * @return the prefix for the IRI.
     */
    public String getPrefix(IRI iri) {
        iri = transform(iri);
        return prefixMap.get(iri);
    }

    /**
     * Returns the prefix label for a given IRI.
     *
     * @param iri the IRI to get the prefix label for.
     * @return the prefix label for the IRI, or null if not found.
     */
    public String getPrefixLabel(IRI iri) {
        iri = transform(iri);
        return prefixLabelMap.get(iri);
    }

    /**
     * Returns the regex pattern for a given IRI.
     *
     * @param iri the IRI to get the regex for.
     * @return the regex pattern for the IRI, or null if not found.
     */
    public String getRegex(IRI iri) {
        iri = transform(iri);
        return regexMap.get(iri);
    }

    /**
     * Returns the default value of a placeholder ({@code nt:hasDefaultValue}). For a
     * placeholder in a repetition (an IRI with a {@code __N} suffix) whose default is an IRI
     * in the template's own namespace, the repetition is appended to that IRI, so that each
     * repetition gets a default of its own.
     *
     * @param iri the placeholder IRI.
     * @return the default value, or null if the placeholder has none.
     */
    public Value getDefault(IRI iri) {
        if (iri.stringValue().matches(".*__[0-9]+")) {
            String baseIri = iri.stringValue().replaceFirst("__[0-9]+$", "");
            Value v = defaultValues.get(vf.createIRI(baseIri));
            if (v instanceof IRI vIri) {
                // Append repeat suffix only for local URIs; fixed URIs stay unchanged
                if (vIri.stringValue().startsWith(nanopub.getUri().stringValue())) {
                    int repeatSuffix = Integer.parseInt(iri.stringValue().replaceFirst("^.*__([0-9]+)$", "$1"));
                    return vf.createIRI(vIri.stringValue() + (repeatSuffix + 1));
                }
                return vIri;
            }
        }
        iri = transform(iri);
        return defaultValues.get(iri);
    }

    /**
     * Returns the IRIs of the top-level statements of the template, in template order. A
     * grouped statement is listed as one entry; its members are returned by
     * {@link #getStatementIris(IRI)}.
     *
     * @return the unmodifiable list of statement IRIs, empty if the template has none.
     */
    public List<IRI> getStatementIris() {
        List<IRI> l = statementMap.get(templateIri);
        if (l == null) return List.of();
        return Collections.unmodifiableList(l);
    }

    /**
     * Returns the IRIs of the member statements of a grouped statement, in template order.
     *
     * @param groupIri the IRI of the grouped statement.
     * @return the unmodifiable list of member statement IRIs, or null if the given IRI is not a group with members.
     */
    public List<IRI> getStatementIris(IRI groupIri) {
        List<IRI> l = statementMap.get(groupIri);
        if (l == null) return null;
        return Collections.unmodifiableList(l);
    }

    /**
     * Returns the subject of a statement given its IRI.
     *
     * @param statementIri the IRI of the statement to retrieve.
     * @return the subject of the statement, or null if not found or not an IRI.
     */
    public IRI getSubject(IRI statementIri) {
        return statementSubjects.get(statementIri);
    }

    /**
     * Returns the predicate of a statement given its IRI.
     *
     * @param statementIri the IRI of the statement to retrieve.
     * @return the predicate of the statement, or null if not found.
     */
    public IRI getPredicate(IRI statementIri) {
        return statementPredicates.get(statementIri);
    }

    /**
     * Returns the object of a statement given its IRI.
     *
     * @param statementIri the IRI of the statement to retrieve.
     * @return the object of the statement, or null if not found.
     */
    public Value getObject(IRI statementIri) {
        return statementObjects.get(statementIri);
    }

    /**
     * Returns the subject of a statement as the template declares it, which may be a literal
     * and therefore invalid in that position (in which case {@link #getSubject(IRI)} is null).
     * Use this for rendering, so that an invalid subject is shown to the user instead of
     * silently disappearing; see {@link #getStatementErrors()}.
     *
     * @param statementIri the IRI of the statement to retrieve.
     * @return the declared subject of the statement, or null if the statement declares none.
     */
    public Value getDeclaredSubject(IRI statementIri) {
        IRI subj = statementSubjects.get(statementIri);
        if (subj != null) return subj;
        return nonIriStatementSubjects.get(statementIri);
    }

    /**
     * Returns the predicate of a statement as the template declares it, which may be a literal
     * and therefore invalid in that position (in which case {@link #getPredicate(IRI)} is null).
     * See {@link #getDeclaredSubject(IRI)}.
     *
     * @param statementIri the IRI of the statement to retrieve.
     * @return the declared predicate of the statement, or null if the statement declares none.
     */
    public Value getDeclaredPredicate(IRI statementIri) {
        IRI pred = statementPredicates.get(statementIri);
        if (pred != null) return pred;
        return nonIriStatementPredicates.get(statementIri);
    }

    /**
     * Returns human-readable descriptions of the statements of this template that cannot
     * produce valid RDF, because their subject or predicate is a literal, is a placeholder
     * that can only be filled with a literal, or is missing altogether. Only URIs are allowed
     * in subject and predicate position. Also reported is a resource marked as introduced or
     * embedded under a fixed IRI, which every nanopublication made from the template would
     * then declare as its own.
     *
     * @return the error descriptions, empty if all statements are well-formed.
     */
    public List<String> getStatementErrors() {
        List<String> errors = new ArrayList<>();
        List<IRI> topIris = getStatementIris();
        Set<IRI> reportedFixedIris = new HashSet<>();
        int i = 0;
        for (IRI top : topIris) {
            i++;
            List<IRI> memberIris = getStatementIris(top);
            if (memberIris == null) {
                collectStatementErrors(top, "Statement " + i, errors, reportedFixedIris);
            } else {
                int j = 0;
                for (IRI member : memberIris) {
                    j++;
                    collectStatementErrors(member, "Statement " + i + "." + j, errors, reportedFixedIris);
                }
            }
        }
        return errors;
    }

    private void collectStatementErrors(IRI statementIri, String statementNumber, List<String> errors, Set<IRI> reportedFixedIris) {
        String statementName = statementNumber + " (" + getLocalName(statementIri) + ")";
        collectPositionError(getDeclaredSubject(statementIri), "subject", statementName, errors);
        collectPositionError(getDeclaredPredicate(statementIri), "predicate", statementName, errors);
        collectReservedNameError(getDeclaredSubject(statementIri), "subject", statementName, errors);
        collectReservedNameError(getDeclaredPredicate(statementIri), "predicate", statementName, errors);
        collectReservedNameError(getObject(statementIri), "object", statementName, errors);
        collectFixedIntroductionError(getDeclaredSubject(statementIri), "subject", statementName, errors, reportedFixedIris);
        collectFixedIntroductionError(getDeclaredPredicate(statementIri), "predicate", statementName, errors, reportedFixedIris);
        collectFixedIntroductionError(getObject(statementIri), "object", statementName, errors, reportedFixedIris);
    }

    /**
     * Reports a resource marked as introduced or embedded that every nanopublication made from
     * the template would publish under the same IRI: one that is neither a placeholder, nor
     * minted under the nanopublication, nor given an artifact code with
     * {@code ~~ARTIFACTCODE~~}. Each of those nanopublications would declare the same resource
     * as its own, while display and supersede take a resource to be introduced only once
     * (nanodash#749). Each such IRI is reported only at the first statement it appears in.
     *
     * @param value             the value in this position, or null
     * @param position          the position the value is in, for the message
     * @param statementName     the statement the value belongs to, for the message
     * @param errors            the list the error is added to
     * @param reportedFixedIris the IRIs reported so far, to which this one is added
     */
    private void collectFixedIntroductionError(Value value, String position, String statementName, List<String> errors, Set<IRI> reportedFixedIris) {
        if (!(value instanceof IRI iri) || !isFixedIntroduction(iri) || !reportedFixedIris.add(iri)) return;
        boolean introduced = isIntroducedResource(iri);
        errors.add(statementName + ": the " + position + " " + describeValue(iri) + " is marked as "
                + (introduced ? "introduced" : "embedded") + " but is a fixed IRI outside the template, so every"
                + " nanopublication would " + (introduced ? "introduce" : "embed") + " the same resource. Make it a placeholder, an IRI under the template's own namespace, or add the"
                + " ~~ARTIFACTCODE~~ marker to it.");
    }

    private boolean isFixedIntroduction(IRI iri) {
        if (!isIntroducedResource(iri) && !isEmbeddedResource(iri)) return false;
        if (isPlaceholder(iri) || isLocalResource(iri)) return false;
        return !iri.stringValue().contains("~~ARTIFACTCODE~~");
    }

    /**
     * Reports a resource the template mints under a name the nanopublication keeps for one of
     * its own parts. Such a resource is not a resource of its own but the graph or signature
     * whose name it took, and nothing published that way can be corrected afterwards, so the
     * template is reported as invalid rather than filled in.
     *
     * @param value         the value in this position, or null
     * @param position      the position the value is in, for the message
     * @param statementName the statement the value belongs to, for the message
     * @param errors        the list the error is added to
     */
    private void collectReservedNameError(Value value, String position, String statementName, List<String> errors) {
        if (!(value instanceof IRI iri) || !isLocalResource(iri)) return;
        String s = iri.stringValue();
        String localName = s.substring((s.contains("#") ? s.lastIndexOf('#') : s.lastIndexOf('/')) + 1);
        if (!NanopubUtils.isReservedLocalName(localName)) return;
        errors.add(statementName + ": the " + position + " is minted as '" + localName + "', which is what a"
                + " nanopublication calls one of its own parts, so the published resource would be that part"
                + " rather than a resource of its own.");
    }

    private void collectPositionError(Value value, String position, String statementName, List<String> errors) {
        if (value == null) {
            errors.add(statementName + ": no " + position + " is defined.");
        } else if (value instanceof Literal) {
            errors.add(statementName + ": the literal " + describeValue(value) + " is used in " + position +
                    " position, but only URIs are allowed there.");
        } else if (isLiteralPlaceholder((IRI) value)) {
            errors.add(statementName + ": the literal placeholder " + describeValue(value) + " is used in " + position +
                    " position, but only URIs are allowed there.");
        }
    }

    private String describeValue(Value value) {
        if (value instanceof Literal) return "\"" + value.stringValue() + "\"";
        String label = getLabel((IRI) value);
        String localName = getLocalName(value);
        if (label == null) return localName;
        return "\"" + label + "\" (" + localName + ")";
    }

    private String getLocalName(Value value) {
        return value.stringValue().replaceFirst("^.*[/#]", "");
    }

    /**
     * Checks if the template is a local resource.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a local resource, false otherwise.
     */
    public boolean isLocalResource(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.LOCAL_RESOURCE);
    }

    /**
     * Checks if the template is an introduced resource.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is an introduced resource, false otherwise.
     */
    public boolean isIntroducedResource(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.INTRODUCED_RESOURCE);
    }

    /**
     * Returns the role-instantiation direction pin declared on a (predicate) IRI, if
     * any: {@link KPXL#INVERSE_ROLE_PROPERTY} or
     * {@link KPXL#REGULAR_ROLE_PROPERTY}. Templates declare the pin as an
     * {@code rdf:type} on the predicate (constant or placeholder); nanodash lifts it into
     * pubinfo at publish time so nanopub-query can resolve a custom role predicate's
     * direction (see nanodash#525).
     *
     * @param iri the IRI to check (a predicate constant or placeholder).
     * @return the pin class, or null if the IRI carries no role-direction pin.
     */
    public IRI getRoleDirectionPin(IRI iri) {
        iri = transform(iri);
        if (!typeMap.containsKey(iri)) return null;
        List<IRI> types = typeMap.get(iri);
        if (types.contains(KPXL.INVERSE_ROLE_PROPERTY)) return KPXL.INVERSE_ROLE_PROPERTY;
        if (types.contains(KPXL.REGULAR_ROLE_PROPERTY)) return KPXL.REGULAR_ROLE_PROPERTY;
        return null;
    }

    /**
     * Checks if the template is an embedded resource.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is an embedded resource, false otherwise.
     */
    public boolean isEmbeddedResource(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.EMBEDDED_RESOURCE);
    }

    /**
     * Checks if the IRI is a value placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a value placeholder, false otherwise.
     */
    public boolean isValuePlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.VALUE_PLACEHOLDER);
    }

    /**
     * Checks if the IRI is a URI placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a URI placeholder, false otherwise.
     */
    public boolean isUriPlaceholder(IRI iri) {
        iri = transform(iri);
        if (!typeMap.containsKey(iri)) return false;
        for (IRI t : typeMap.get(iri)) {
            if (t.equals(NTEMPLATE.URI_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.EXTERNAL_URI_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.TRUSTY_URI_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.AUTO_ESCAPE_URI_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.RESTRICTED_CHOICE_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.GUIDED_CHOICE_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.AGENT_PLACEHOLDER)) return true;
        }
        return false;
    }

    /**
     * Checks if the IRI is an external URI placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is an external URI placeholder, false otherwise.
     */
    public boolean isExternalUriPlaceholder(IRI iri) {
        iri = transform(iri);
        if (!typeMap.containsKey(iri)) return false;
        for (IRI t : typeMap.get(iri)) {
            if (t.equals(NTEMPLATE.EXTERNAL_URI_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.TRUSTY_URI_PLACEHOLDER)) return true;
        }
        return false;
    }

    /**
     * Checks if the IRI is a trusty URI placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a trusty URI placeholder, false otherwise.
     */
    public boolean isTrustyUriPlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.TRUSTY_URI_PLACEHOLDER);
    }

    /**
     * Checks if the IRI is a placeholder for a URI that does not exist yet, i.e. one the
     * template marks with {@link NTEMPLATE#NEW_URI_PLACEHOLDER}.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a new-URI placeholder, false otherwise.
     */
    public boolean isNewUriPlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.NEW_URI_PLACEHOLDER);
    }

    /**
     * Checks if the IRI is an auto-escape URI placeholder.
     *
     * @param iri the IRI to check
     * @return true if it is an auto-escape placeholder
     */
    public boolean isAutoEscapePlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.AUTO_ESCAPE_URI_PLACEHOLDER);
    }

    /**
     * Checks if the IRI is a date placeholder, one that takes a point in time at whatever
     * precision the user has it (see {@link NTEMPLATE#DATE_PLACEHOLDER}).
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a date placeholder, false otherwise.
     */
    public boolean isDatePlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.DATE_PLACEHOLDER);
    }

    /**
     * Returns the precisions a date placeholder accepts, coarsest first.
     *
     * @param iri the date placeholder IRI.
     * @return the accepted precisions, or an empty list if the IRI is not a date placeholder.
     */
    public List<DatePrecision> getDatePrecisions(IRI iri) {
        iri = transform(iri);
        return datePrecisionMap.getOrDefault(iri, List.of());
    }

    /**
     * Checks if the IRI is a literal placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a literal placeholder, false otherwise.
     */
    public boolean isLiteralPlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && (typeMap.get(iri).contains(NTEMPLATE.LITERAL_PLACEHOLDER) || typeMap.get(iri).contains(NTEMPLATE.LONG_LITERAL_PLACEHOLDER)
                || typeMap.get(iri).contains(NTEMPLATE.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER) || typeMap.get(iri).contains(NTEMPLATE.DATE_PLACEHOLDER));
    }

    /**
     * Checks if the given literal placeholder lets the user select the language tag at fill time.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a language-tag-selectable literal placeholder, false otherwise.
     */
    public boolean isLanguageTagSelectable(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
    }

    /**
     * Checks if the IRI is a long literal placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a long literal placeholder, false otherwise.
     */
    public boolean isLongLiteralPlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.LONG_LITERAL_PLACEHOLDER);
    }

    /**
     * Checks whether the given placeholder is filled with the SPARQL code of a query, i.e.
     * whether the template puts it in object position of a {@code kpxl_grlc:sparql} statement.
     * <p>
     * This is what tells a SPARQL field apart from any other long literal, and it has to be
     * read off the template rather than guessed from the field: only the template knows what
     * the value it collects is going to mean.
     *
     * @param iri the placeholder IRI to check.
     * @return true if the placeholder holds the SPARQL code of a query.
     */
    public boolean isSparqlPlaceholder(IRI iri) {
        iri = transform(iri);
        for (IRI i : getStatementIris()) {
            if (statementMap.containsKey(i)) {
                // grouped statement
                for (IRI g : getStatementIris(i)) {
                    if (isSparqlStatementFor(g, iri)) return true;
                }
            } else {
                // non-grouped statement
                if (isSparqlStatementFor(i, iri)) return true;
            }
        }
        return false;
    }

    private boolean isSparqlStatementFor(IRI statementIri, IRI iri) {
        return KPXL_GRLC.SPARQL.equals(statementPredicates.get(statementIri))
                && iri.equals(statementObjects.get(statementIri));
    }

    /**
     * Checks if the IRI is a restricted choice placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a restricted choice placeholder, false otherwise.
     */
    public boolean isRestrictedChoicePlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.RESTRICTED_CHOICE_PLACEHOLDER);
    }

    /**
     * Checks if the IRI is a guided choice placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a guided choice placeholder, false otherwise.
     */
    public boolean isGuidedChoicePlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.GUIDED_CHOICE_PLACEHOLDER);
    }

    /**
     * Checks if the IRI is an agent placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is an agent placeholder, false otherwise.
     */
    public boolean isAgentPlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.AGENT_PLACEHOLDER);
    }

    /**
     * Checks if the IRI is a sequence element placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a sequence element placeholder, false otherwise.
     */
    public boolean isSequenceElementPlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.SEQUENCE_ELEMENT_PLACEHOLDER);
    }

    /**
     * Checks if the IRI is a root nanopub placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a root nanopub placeholder, false otherwise.
     */
    public boolean isRootNanopubPlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.ROOT_NANOPUB_PLACEHOLDER);
    }

    /**
     * Checks if the IRI is a placeholder of any type.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a placeholder, false otherwise.
     */
    public boolean isPlaceholder(IRI iri) {
        iri = transform(iri);
        if (!typeMap.containsKey(iri)) return false;
        for (IRI t : typeMap.get(iri)) {
            if (t.equals(NTEMPLATE.VALUE_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.URI_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.EXTERNAL_URI_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.TRUSTY_URI_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.AUTO_ESCAPE_URI_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.RESTRICTED_CHOICE_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.GUIDED_CHOICE_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.AGENT_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.LITERAL_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.LONG_LITERAL_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.DATE_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.SEQUENCE_ELEMENT_PLACEHOLDER)) return true;
            if (t.equals(NTEMPLATE.ROOT_NANOPUB_PLACEHOLDER)) return true;
        }
        return false;
    }

    /**
     * Checks if the IRI is an optional statement.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is an optional statement, false otherwise.
     */
    public boolean isOptionalStatement(IRI iri) {
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.OPTIONAL_STATEMENT);
    }

    /**
     * Whether the placeholder with the given parameter name is a required field of
     * this template: it appears (as subject or object) in at least one non-optional
     * statement — i.e. a value must be provided to publish. A repetition-index
     * suffix on the name (e.g. {@code public-key__.1}) is ignored, matching the base
     * placeholder.
     *
     * @param paramName the parameter name (placeholder local name, optionally with a
     *                  {@code __.N} repetition suffix)
     * @return true if the placeholder is required (appears in a non-optional statement)
     */
    public boolean isRequiredField(String paramName) {
        if (paramName == null) return false;
        String base = paramName.replaceFirst("__\\..*$", "");
        for (IRI top : getStatementIris()) {
            List<IRI> members = getStatementIris(top);
            if (members != null) {
                if (isOptionalStatement(top)) continue; // whole group optional
                for (IRI st : members) {
                    if (!isOptionalStatement(st) && statementReferencesPlaceholder(st, base)) return true;
                }
            } else if (!isOptionalStatement(top) && statementReferencesPlaceholder(top, base)) {
                return true;
            }
        }
        return false;
    }

    private boolean statementReferencesPlaceholder(IRI statementIri, String localName) {
        return matchesPlaceholderLocalName(getSubject(statementIri), localName)
                || matchesPlaceholderLocalName(getObject(statementIri), localName);
    }

    private boolean matchesPlaceholderLocalName(Value value, String localName) {
        if (!(value instanceof IRI iri) || !isPlaceholder(iri)) return false;
        String s = iri.stringValue();
        int i = Math.max(s.lastIndexOf('/'), s.lastIndexOf('#'));
        return localName.equals(i < 0 ? s : s.substring(i + 1));
    }

    /**
     * Checks if the IRI is an advanced statement, which are only show upon expanding to full view in form mode.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is an advanced statement, false otherwise.
     */
    public boolean isAdvancedStatement(IRI iri) {
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.ADVANCED_STATEMENT);
    }

    /**
     * Checks if the IRI is a grouped statement.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a grouped statement, false otherwise.
     */
    public boolean isGroupedStatement(IRI iri) {
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.GROUPED_STATEMENT);
    }

    /**
     * Checks if the IRI is a repeatable statement.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a repeatable statement, false otherwise.
     */
    public boolean isRepeatableStatement(IRI iri) {
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(NTEMPLATE.REPEATABLE_STATEMENT);
    }

    /**
     * Returns the possible values of a restricted-choice placeholder: those the template
     * lists itself ({@code nt:possibleValue}), followed by those found in the
     * nanopublications it names ({@code nt:possibleValuesFrom}), which are the subjects of
     * the {@code rdfs:label} statements in their assertions.
     * <p>
     * These nanopublications are retrieved with the template's {@link NanopubResolver} the
     * first time this method is called for the placeholder. One that cannot be retrieved is
     * left out, and retrieving it is tried again on the next call.
     *
     * @param iri the placeholder IRI.
     * @return the unmodifiable list of possible values, empty if there are none.
     */
    public synchronized List<Value> getPossibleValues(IRI iri) {
        iri = transform(iri);
        loadPossibleValues(iri, true);
        List<Value> l = possibleValueMap.get(iri);
        if (l == null) return List.of();
        return List.copyOf(l);
    }

    // Retrieves the nanopublications the given placeholder takes possible values from, as far
    // as they have not been retrieved yet. With retry set to false, those that could not be
    // retrieved in an earlier attempt are left alone. Must be called with the lock on this
    // object held.
    private void loadPossibleValues(IRI placeholder, boolean retry) {
        List<IRI> nanopubList = possibleValuesToLoadMap.get(placeholder);
        if (nanopubList == null) return;
        for (IRI npIri : new ArrayList<>(nanopubList)) {
            if (!retry && unretrievedValueNanopubs.contains(npIri)) continue;
            Nanopub valuesNanopub = null;
            try {
                valuesNanopub = resolver.getNanopub(npIri.stringValue());
            } catch (Exception ex) {
                logger.error("Could not load possible values from nanopub {}", npIri.stringValue(), ex);
            }
            if (valuesNanopub == null) {
                if (unretrievedValueNanopubs.add(npIri)) {
                    logger.warn("Could not retrieve nanopub with possible values: {}", npIri.stringValue());
                }
                continue;
            }
            unretrievedValueNanopubs.remove(npIri);
            List<Value> values = possibleValueMap.computeIfAbsent(placeholder, k -> new ArrayList<>());
            for (Statement st : valuesNanopub.getAssertion()) {
                if (!st.getPredicate().equals(RDFS.LABEL)) continue;
                if (!(st.getSubject() instanceof IRI valueIri)) continue;
                values.add(valueIri);
                possibleValueLabelMap.putIfAbsent(valueIri, st.getObject().stringValue());
            }
            nanopubList.remove(npIri);
        }
        if (nanopubList.isEmpty()) possibleValuesToLoadMap.remove(placeholder);
    }

    /**
     * Returns the IRI of the default provenance for the template.
     *
     * @return the IRI of the default provenance, or null if not set.
     */
    public IRI getDefaultProvenance() {
        return defaultProvenance;
    }

    /**
     * Returns the target namespace for the template.
     *
     * @return the target namespace as a string, or null if not set.
     */
    public String getTargetNamespace() {
        return targetNamespace;
    }

    /**
     * Returns the nanopub label pattern.
     *
     * @return the nanopub label pattern as a string, or null if not set.
     */
    public String getNanopubLabelPattern() {
        return nanopubLabelPattern;
    }

    /**
     * Returns the list of target nanopub types.
     *
     * @return the unmodifiable list of target nanopub types.
     */
    public List<IRI> getTargetNanopubTypes() {
        return Collections.unmodifiableList(targetNanopubTypes);
    }

    /**
     * Returns the list of the required pubInfo elements for the template.
     *
     * @return the unmodifiable list of required pubinfo elements.
     */
    public List<IRI> getRequiredPubInfoElements() {
        return Collections.unmodifiableList(requiredPubInfoElements);
    }

    /**
     * Returns the APIs a placeholder looks its possible values up in, as given by
     * {@code nt:possibleValuesFromApi}.
     *
     * @param iri the IRI of the placeholder.
     * @return the API URLs, in the order the template gives them; empty if there are none.
     */
    public List<String> getPossibleValuesApis(IRI iri) {
        List<String> apiList = apiMap.get(transform(iri));
        if (apiList == null) return new ArrayList<>();
        return new ArrayList<>(apiList);
    }

    /**
     * Returns the tag associated with the template.
     *
     * @return the tag as a string, or null if not set.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Checks whether the given nanopublication contains a full template definition,
     * i.e. whether it can serve as the source of a fill-in form. Nanopublications
     * that merely refer to a template - most notably the registration of a template
     * kind as a maintained resource of a space, which types the kind IRI as a
     * template but carries no template body - do not qualify.
     *
     * @param np the nanopublication to check
     * @return true if the nanopublication defines a template, false otherwise
     */
    public static boolean hasFullTemplateDefinition(Nanopub np) {
        if (np == null) return false;
        Set<IRI> templateNodes = getTemplateNodes(np);
        IRI node;
        if (templateNodes.contains(np.getAssertionUri())) {
            node = np.getAssertionUri();
        } else if (templateNodes.size() == 1) {
            node = templateNodes.iterator().next();
            // A template node outside the nanopub's namespace is a reference to a
            // template defined elsewhere, not a definition (see nanodash#597):
            if (!node.stringValue().startsWith(np.getUri().stringValue())) return false;
        } else if (templateNodes.size() > 1) {
            return false;
        } else {
            // Experimental SHACL-based template: the base node shape carries the definition.
            for (Statement st : np.getAssertion()) {
                if (st.getPredicate().equals(SHACL.TARGET_CLASS)) return true;
            }
            return false;
        }
        // A definition has a body; a bare declaration of the template node does not:
        for (Statement st : np.getAssertion()) {
            if (st.getSubject().equals(node) && st.getPredicate().equals(NTEMPLATE.HAS_STATEMENT)) return true;
        }
        return false;
    }

    // The nodes of the assertion that are typed as templates, in the order encountered.
    private static Set<IRI> getTemplateNodes(Nanopub np) {
        Set<IRI> templateNodes = new LinkedHashSet<>();
        for (Statement st : np.getAssertion()) {
            if (!st.getPredicate().equals(RDF.TYPE)) continue;
            if (!(st.getSubject() instanceof IRI subjIri)) continue;
            if (st.getObject().equals(NTEMPLATE.ASSERTION_TEMPLATE) || st.getObject().equals(NTEMPLATE.PROVENANCE_TEMPLATE) || st.getObject().equals(NTEMPLATE.PUBINFO_TEMPLATE)) {
                templateNodes.add(subjIri);
            }
        }
        return templateNodes;
    }

    private void processTemplate(Nanopub templateNp) throws MalformedTemplateException {
        IRI assertionUri = templateNp.getAssertionUri();
        Set<IRI> templateNodes = getTemplateNodes(templateNp);

        if (templateNodes.contains(assertionUri)) {
            // Legacy shape (template node = assertion graph URI) takes precedence
            // over any other typed node, so every template that parsed before
            // embedded identity existed keeps parsing identically.
            templateIri = assertionUri;
            processNpTemplate(templateNp);
        } else if (templateNodes.size() == 1) {
            IRI node = templateNodes.iterator().next();
            if (!node.stringValue().startsWith(templateNp.getUri().stringValue())) {
                throw new MalformedTemplateException("Embedded template node must be in the nanopub's namespace: " + node);
            }
            templateIri = node;
            embeddedIdentity = true;
            processNpTemplate(templateNp);
        } else if (templateNodes.size() > 1) {
            throw new MalformedTemplateException("Multiple embedded template nodes found");
        } else {
            // Experimental SHACL-based template:
            processShaclTemplate(templateNp);
        }
        tagUntypedLocalIrisAsLocalResources(templateNp);
        checkLanguageTagPlaceholders();
        collectDatePrecisions();
    }

    /**
     * Turns the datatypes a date placeholder lists into the precisions it accepts. A date
     * placeholder has no single datatype, so it is left without one: whatever reads the
     * datatype of a placeholder would otherwise take one of the listed precisions for the only
     * one allowed.
     */
    private void collectDatePrecisions() {
        for (Map.Entry<IRI, List<IRI>> e : typeMap.entrySet()) {
            if (!e.getValue().contains(NTEMPLATE.DATE_PLACEHOLDER)) continue;
            IRI placeholder = e.getKey();
            datePrecisionMap.put(placeholder, getListedDatePrecisions(placeholder));
            datatypeMap.remove(placeholder);
        }
    }

    /**
     * Returns the precisions a date placeholder lists, coarsest first, or all of them if it
     * lists none it can take.
     *
     * @param placeholder the date placeholder
     * @return the precisions the placeholder accepts
     */
    private List<DatePrecision> getListedDatePrecisions(IRI placeholder) {
        List<DatePrecision> listed = new ArrayList<>();
        for (IRI datatype : declaredDatatypesMap.getOrDefault(placeholder, List.of())) {
            DatePrecision precision = DatePrecision.of(datatype);
            if (precision == null) {
                logger.warn("Ignoring datatype {} on date placeholder {}", datatype, placeholder);
            } else if (!listed.contains(precision)) {
                listed.add(precision);
            }
        }
        if (listed.isEmpty()) return DatePrecision.ALL;
        listed.sort(null);
        return listed;
    }

    // A literal is either language-tagged or datatyped, never both; the language-tag
    // picker wins so the placeholder keeps rendering as a text field.
    private void checkLanguageTagPlaceholders() {
        for (Map.Entry<IRI, List<IRI>> e : typeMap.entrySet()) {
            if (e.getValue().contains(NTEMPLATE.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER) && datatypeMap.containsKey(e.getKey())) {
                logger.warn("Ignoring datatype {} on language-tag-selectable placeholder {}", datatypeMap.get(e.getKey()), e.getKey());
                datatypeMap.remove(e.getKey());
            }
        }
    }

    // Local IRIs in used statement positions that carry no minting-relevant type (no
    // placeholder type, not a Local Resource) are automatically treated as Local
    // Resources, so every produced nanopub mints them under its own namespace instead of
    // copying the template-local IRI verbatim (see nanodash#551). Introduced/Embedded
    // Resource tags do not exempt an IRI: they only record the resulting IRI for pubinfo
    // (npx:introduces / npx:embeds) and say nothing about minting; a template-local IRI
    // tagged only nt:IntroducedResource would otherwise be shared by all produced
    // nanopubs (see nanodash#602).
    private void tagUntypedLocalIrisAsLocalResources(Nanopub templateNp) {
        List<IRI> topIris = statementMap.get(templateIri);
        if (topIris == null) return;
        String npUri = templateNp.getUri().stringValue();
        List<IRI> statementIris = new ArrayList<>();
        for (IRI iri : topIris) {
            if (statementMap.containsKey(iri)) {
                // grouped statement
                statementIris.addAll(statementMap.get(iri));
            } else {
                statementIris.add(iri);
            }
        }
        for (IRI st : statementIris) {
            tagIfUntypedLocal(statementSubjects.get(st), npUri);
            tagIfUntypedLocal(statementPredicates.get(st), npUri);
            tagIfUntypedLocal(statementObjects.get(st), npUri);
        }
    }

    private void tagIfUntypedLocal(Value value, String npUri) {
        if (!(value instanceof IRI iri)) return;
        String s = iri.stringValue();
        // Only sub-IRIs of the template nanopub are local; the nanopub URI itself is a
        // global reference to the template and stays verbatim:
        if (!s.startsWith(npUri) || s.length() == npUri.length()) return;
        if (iri.equals(templateIri)) return;
        // Statement/group identifiers referenced as values stay untouched:
        if (statementMap.containsKey(iri) || statementSubjects.containsKey(iri)) return;
        if (isPlaceholder(iri) || isLocalResource(iri)) return;
        addType(iri, NTEMPLATE.LOCAL_RESOURCE);
    }

    private void processNpTemplate(Nanopub templateNp) throws MalformedTemplateException {
        for (Statement st : templateNp.getAssertion()) {
            if (!(st.getSubject() instanceof IRI subj)) continue;
            final IRI pred = st.getPredicate();
            final Value obj = st.getObject();

            if (subj.equals(templateIri)) {
                processTemplateNodeStatement(pred, obj);
                if (obj instanceof IRI objIri) {
                    if (pred.equals(DCTERMS.IS_VERSION_OF)) {
                        templateKindIri = objIri;
                    } else if (pred.equals(KPXL.GOVERNED_BY)) {
                        governingSpace = objIri;
                    }
                }
            }
            if (processCommonStatement(subj, pred, obj)) continue;
            if (pred.equals(NTEMPLATE.HAS_STATEMENT) && obj instanceof IRI objIri) {
                statementMap.computeIfAbsent(subj, k -> new ArrayList<>()).add(objIri);
            } else if (pred.equals(RDF.SUBJECT)) {
                if (obj instanceof IRI objIri) {
                    statementSubjects.put(subj, objIri);
                } else {
                    nonIriStatementSubjects.put(subj, obj);
                }
            } else if (pred.equals(RDF.PREDICATE)) {
                if (obj instanceof IRI objIri) {
                    statementPredicates.put(subj, objIri);
                } else {
                    nonIriStatementPredicates.put(subj, obj);
                }
            } else if (pred.equals(RDF.OBJECT)) {
                statementObjects.put(subj, obj);
            }
        }
        List<IRI> groupIris = new ArrayList<>();
        for (IRI iri : typeMap.keySet()) {
            if (typeMap.get(iri).contains(NTEMPLATE.GROUPED_STATEMENT)) groupIris.add(iri);
        }
        // Nested groups and repeatable members are out of scope (the group is the unit of
        // repetition); ignore such member flags with a warning:
        for (IRI iri : groupIris) {
            if (!isGroupedStatement(iri)) continue;
            List<IRI> memberIris = statementMap.get(iri);
            if (memberIris == null) {
                throw new MalformedTemplateException("Grouped statement has no member statements: " + iri);
            }
            for (IRI member : memberIris) {
                List<IRI> memberTypes = typeMap.get(member);
                if (memberTypes == null) continue;
                if (memberTypes.remove(NTEMPLATE.GROUPED_STATEMENT)) {
                    logger.warn("Ignoring nested nt:GroupedStatement flag on member {} of group {}", member, iri);
                }
                if (memberTypes.remove(NTEMPLATE.REPEATABLE_STATEMENT)) {
                    logger.warn("Ignoring nt:RepeatableStatement flag on member {} of group {}", member, iri);
                }
            }
        }
        for (IRI iri : groupIris) {
            if (!isGroupedStatement(iri)) continue;
            List<IRI> memberIris = statementMap.get(iri);
            // A group needs at least one required member: an all-optional group would match zero
            // statements, making its presence meaningless and repetition detection non-terminating.
            // Degrade to group-level optionality instead of rejecting:
            boolean hasRequiredMember = false;
            for (IRI member : memberIris) {
                if (!isOptionalStatement(member)) hasRequiredMember = true;
            }
            if (!hasRequiredMember) {
                logger.warn("All members of group {} are optional; treating the group itself as optional instead", iri);
                if (!isOptionalStatement(iri)) {
                    addType(iri, NTEMPLATE.OPTIONAL_STATEMENT);
                }
                for (IRI member : memberIris) {
                    typeMap.get(member).remove(NTEMPLATE.OPTIONAL_STATEMENT);
                }
            }
            // Grouped IRI are added via group, so direct link from template is redundant:
            List<IRI> topIris = statementMap.get(templateIri);
            if (topIris != null) topIris.removeAll(memberIris);
        }
        for (List<IRI> l : statementMap.values()) {
            l.sort(statementComparator);
        }
    }

    private void processShaclTemplate(Nanopub templateNp) throws MalformedTemplateException {
        templateIri = null;
        for (Statement st : templateNp.getAssertion()) {
            if (st.getPredicate().equals(SHACL.TARGET_CLASS) && st.getSubject() instanceof IRI shapeIri) {
                templateIri = shapeIri;
                break;
            }
        }
        if (templateIri == null) {
            throw new MalformedTemplateException("Base node shape not found");
        }

        IRI baseSubj = vf.createIRI(templateIri.stringValue() + "+subj");
        addType(baseSubj, NTEMPLATE.INTRODUCED_RESOURCE);

        List<IRI> statementList = new ArrayList<>();
        Map<IRI, Integer> minCounts = new HashMap<>();
        Map<IRI, Integer> maxCounts = new HashMap<>();

        for (Statement st : templateNp.getAssertion()) {
            if (!(st.getSubject() instanceof IRI subj)) continue;
            final IRI pred = st.getPredicate();
            final Value obj = st.getObject();

            if (subj.equals(templateIri)) {
                processTemplateNodeStatement(pred, obj);
            }
            if (processCommonStatement(subj, pred, obj)) continue;
            if (pred.equals(SHACL.PROPERTY) && obj instanceof IRI objIri) {
                statementList.add(objIri);
                statementMap.computeIfAbsent(subj, k -> new ArrayList<>()).add(objIri);
                IRI stSubjIri = vf.createIRI(subj.stringValue() + "+subj");
                statementSubjects.put(objIri, stSubjIri);
                addType(stSubjIri, NTEMPLATE.LOCAL_RESOURCE);
                addType(stSubjIri, NTEMPLATE.URI_PLACEHOLDER);
            } else if (pred.equals(SHACL.PATH) && obj instanceof IRI objIri) {
                statementPredicates.put(subj, objIri);
            } else if (pred.equals(SHACL.HAS_VALUE) && obj instanceof IRI objIri) {
                statementObjects.put(subj, objIri);
            } else if (pred.equals(SHACL.TARGET_CLASS) && obj instanceof IRI objIri) {
                IRI stIri = vf.createIRI(templateNp.getUri() + "/$type");
                statementList.add(stIri);
                statementMap.computeIfAbsent(subj, k -> new ArrayList<>()).add(stIri);
                statementSubjects.put(stIri, baseSubj);
                statementPredicates.put(stIri, RDF.TYPE);
                statementObjects.put(stIri, objIri);
            } else if (pred.equals(SHACL.MIN_COUNT)) {
                try {
                    minCounts.put(subj, Integer.parseInt(obj.stringValue()));
                } catch (NumberFormatException ex) {
                    logger.error("Could not parse <{}> value: {}", SHACL.MIN_COUNT, obj.stringValue());
                }
            } else if (pred.equals(SHACL.MAX_COUNT)) {
                try {
                    maxCounts.put(subj, Integer.parseInt(obj.stringValue()));
                } catch (NumberFormatException ex) {
                    logger.error("Could not parse <{}> value: {}", SHACL.MAX_COUNT, obj.stringValue());
                }
            }
        }
        for (List<IRI> l : statementMap.values()) {
            l.sort(statementComparator);
        }
        for (IRI iri : statementList) {
            if (!statementObjects.containsKey(iri)) {
                IRI stObjIri = vf.createIRI(iri.stringValue() + "+obj");
                statementObjects.put(iri, stObjIri);
                addType(stObjIri, NTEMPLATE.VALUE_PLACEHOLDER);
                if (!minCounts.containsKey(iri) || minCounts.get(iri) <= 0) {
                    addType(iri, NTEMPLATE.OPTIONAL_STATEMENT);
                }
                if (!maxCounts.containsKey(iri) || maxCounts.get(iri) > 1) {
                    addType(iri, NTEMPLATE.REPEATABLE_STATEMENT);
                }
            }
        }
        if (!labelMap.containsKey(baseSubj) && typeMap.get(baseSubj).contains(NTEMPLATE.URI_PLACEHOLDER) && typeMap.get(baseSubj).contains(NTEMPLATE.LOCAL_RESOURCE)) {
            labelMap.put(baseSubj, "short ID as URI suffix");
        }

        if (label == null) {
            label = NanopubUtils.getLabel(templateNp);
        }
    }

    // Reads a statement about the template node itself, as far as both template shapes
    // understand it.
    private void processTemplateNodeStatement(IRI pred, Value obj) {
        final String objS = obj.stringValue();
        if (pred.equals(RDFS.LABEL)) {
            label = objS;
        } else if (pred.equals(DCTERMS.DESCRIPTION)) {
            description = objS;
        } else if (obj instanceof IRI objIri) {
            if (pred.equals(NTEMPLATE.HAS_DEFAULT_PROVENANCE)) {
                defaultProvenance = objIri;
            } else if (pred.equals(NTEMPLATE.HAS_REQUIRED_PUBINFO_ELEMENT)) {
                requiredPubInfoElements.add(objIri);
            } else if (pred.equals(NTEMPLATE.HAS_TARGET_NAMESPACE)) {
                targetNamespace = objS;
            } else if (pred.equals(NTEMPLATE.HAS_TARGET_NANOPUB_TYPE)) {
                targetNanopubTypes.add(objIri);
            }
        } else if (obj instanceof Literal) {
            if (pred.equals(NTEMPLATE.HAS_TAG)) {
                // TODO This should be replaced at some point with a more sophisticated mechanism based on classes.
                // We are assuming that there is at most one tag.
                this.tag = objS;
            } else if (pred.equals(NTEMPLATE.HAS_NANOPUB_LABEL_PATTERN)) {
                nanopubLabelPattern = objS;
            }
        }
    }

    // Reads a statement about a placeholder, resource or statement of the template, as far as
    // both template shapes understand it. Returns whether the predicate is one of those
    // handled here, so that the caller does not look at the statement any further.
    private boolean processCommonStatement(IRI subj, IRI pred, Value obj) {
        final String objS = obj.stringValue();
        if (pred.equals(RDF.TYPE)) {
            if (obj instanceof IRI objIri) addType(subj, objIri);
        } else if (pred.equals(NTEMPLATE.POSSIBLE_VALUE)) {
            possibleValueMap.computeIfAbsent(subj, k -> new ArrayList<>()).add(obj);
        } else if (pred.equals(NTEMPLATE.POSSIBLE_VALUES_FROM)) {
            // Only noted here; the nanopublication is retrieved when the values are asked for:
            if (obj instanceof IRI objIri) possibleValuesToLoadMap.computeIfAbsent(subj, k -> new ArrayList<>()).add(objIri);
        } else if (pred.equals(NTEMPLATE.POSSIBLE_VALUES_FROM_API)) {
            if (obj instanceof Literal) apiMap.computeIfAbsent(subj, k -> new ArrayList<>()).add(objS);
        } else if (pred.equals(RDFS.LABEL)) {
            if (obj instanceof Literal) labelMap.put(subj, objS);
        } else if (pred.equals(DCTERMS.DESCRIPTION)) {
            if (obj instanceof Literal && !subj.equals(templateIri)) statementDescriptionMap.put(subj, objS);
        } else if (pred.equals(NTEMPLATE.HAS_DATATYPE)) {
            if (obj instanceof IRI objIri) {
                datatypeMap.put(subj, objIri);
                declaredDatatypesMap.computeIfAbsent(subj, k -> new ArrayList<>()).add(objIri);
            }
        } else if (pred.equals(NTEMPLATE.HAS_LANGUAGE_TAG)) {
            if (obj instanceof Literal) languageTagMap.put(subj, Literals.normalizeLanguageTag(objS));
        } else if (pred.equals(NTEMPLATE.POSSIBLE_LANGUAGE_TAG)) {
            if (obj instanceof Literal) possibleLanguageTagMap.computeIfAbsent(subj, k -> new ArrayList<>()).add(Literals.normalizeLanguageTag(objS));
        } else if (pred.equals(NTEMPLATE.HAS_PREFIX)) {
            if (obj instanceof Literal) prefixMap.put(subj, objS);
        } else if (pred.equals(NTEMPLATE.HAS_PREFIX_LABEL)) {
            if (obj instanceof Literal) prefixLabelMap.put(subj, objS);
        } else if (pred.equals(NTEMPLATE.HAS_REGEX)) {
            if (obj instanceof Literal) regexMap.put(subj, objS);
        } else if (pred.equals(NTEMPLATE.HAS_DEFAULT_VALUE)) {
            defaultValues.put(subj, obj);
        } else if (pred.equals(NTEMPLATE.STATEMENT_ORDER)) {
            if (obj instanceof Literal && objS.matches("[0-9]+")) statementOrder.put(subj, Integer.valueOf(objS));
        } else {
            return false;
        }
        return true;
    }

    private void addType(IRI thing, IRI type) {
        typeMap.computeIfAbsent(thing, k -> new ArrayList<>()).add(type);
    }

    /**
     * Maps a rendered IRI back to its template form, undoing the artifact-code expansion and the
     * {@code __N} repetition suffix that are added while rendering a repeatable statement group.
     *
     * @param iri the rendered IRI.
     * @return the corresponding template IRI, or the given IRI if no transformation is needed.
     */
    public IRI getTemplateIri(IRI iri) {
        return transform(iri);
    }

    private IRI transform(IRI iri) {
        // A template can leave a statement part undefined (see getStatementErrors()), in which
        // case the null propagates here through getSubject()/getPredicate(). Let the type and
        // label lookups below answer "no" for it instead of failing to render the whole form.
        if (iri == null) return null;
        String s = iri.stringValue();
        // Map a rendered IRI back to its template form for map lookups (label, type,
        // datatype, …). RepetitionGroup.transform expands the template's "~~ARTIFACTCODE~~"
        // placeholder to the target-namespace "~~~ARTIFACTCODE~~~" form and appends a "__N"
        // repetition suffix; both must be undone here or the lookup keyed on the template
        // form (e.g. an attached rdfs:label) is missed.
        if (s.contains("~~~ARTIFACTCODE~~~")) {
            s = s.replace("~~~ARTIFACTCODE~~~", "~~ARTIFACTCODE~~");
        }
        if (s.matches(".*__[0-9]+")) {
            // TODO: Check that this double-underscore pattern isn't used otherwise:
            s = s.replaceFirst("__[0-9]+$", "");
        }
        if (s.equals(iri.stringValue())) return iri;
        return vf.createIRI(s);
    }

    private StatementComparator statementComparator = new StatementComparator();

    private class StatementComparator implements Comparator<IRI>, Serializable {

        /**
         * Compares two IRIs based on their order in the template.
         *
         * @param arg0 the first object to be compared.
         * @param arg1 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
         */
        @Override
        public int compare(IRI arg0, IRI arg1) {
            Integer i0 = statementOrder.get(arg0);
            Integer i1 = statementOrder.get(arg1);
            if (i0 == null && i1 == null) return arg0.stringValue().compareTo(arg1.stringValue());
            if (i0 == null) return 1;
            if (i1 == null) return -1;
            return i0 - i1;
        }

    }

}
