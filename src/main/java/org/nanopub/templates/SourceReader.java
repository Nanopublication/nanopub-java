package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.SimpleCreatorPattern;
import org.nanopub.vocabulary.KPXL;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.*;

/**
 * Reads the values of an existing nanopublication, the source, for templates that the caller
 * chooses, as a form does that is opened to make a new version of a nanopublication, to
 * derive from it or to use what it says. The templates need not be the ones the source names:
 * a newer version of its assertion template does, and so do the publication info templates of
 * the form.
 * <p>
 * The assertion and the provenance are each matched against one template, and each template of
 * the publication info against all of the publication info, as two of them may well give the
 * same triple: a nanopublication can name two versions of the template for its license.
 * {@link #getValues(TemplateMatch)} gives the values a match has found, under the names of the
 * placeholders, as a {@link FillRequest} takes them, and the triples nothing took are left for
 * the caller to show ({@link TemplateMatch#getUnusedTriples()}, {@link #getUnreadPubinfo()}).
 * <p>
 * What a publication info template says about the source alone is not read as values: that of
 * a template marked as transient ({@link Template#isTransient()}), and of one that gives nothing
 * but a link to the nanopublication the source is a version of or is derived from. Such a
 * template takes its triples away from the other templates, as many times over as it can, and
 * gives no values, so it is read first.
 * <p>
 * A template that takes any triple, such as the one for statements written by hand, is read
 * last, so that it gets only what the others leave. It does not get what a fill writes into the
 * publication info by itself, which is not reported as left over either: the creation time, the
 * label, the template links, the resources introduced and embedded, the link to the version
 * superseded, the site, the labels from lookups, the role directions, the types the assertion
 * template gives, and the names of the creators. Another template can take these, as one for the
 * label of the nanopublication does. The signature is not read at all.
 * <p>
 * The creator of the nanopublication that is made can be given. The creator placeholder
 * ({@code nt:CREATOR}) then stands for them alone, so that what the source says about another of
 * its creators is not taken as being about them: it is left over, or goes to a template for
 * statements written by hand. What is not read as values, as that of a transient template, is
 * taken whichever creator of the source it is about.
 * <p>
 * A reader is for one source and one nanopublication made from it: what is left of the
 * publication info shrinks as it is read. It is not safe for use by several threads at a time.
 */
public class SourceReader {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final Set<IRI> WRITTEN_ABOUT_NANOPUB = Set.of(DCTERMS.CREATED, RDFS.LABEL, NPX.SUPERSEDES, NPX.INTRODUCES, NPX.EMBEDS, NPX.WAS_CREATED_AT,
            NTEMPLATE.WAS_CREATED_FROM_TEMPLATE, NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE, NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE);

    private static final Set<IRI> SIGNATURE = Set.of(NPX.HAS_ALGORITHM, NPX.HAS_PUBLIC_KEY, NPX.HAS_SIGNATURE, NPX.HAS_SIGNATURE_TARGET, NPX.SIGNED_BY);

    private static final Set<IRI> WRITTEN_ANYWHERE = Set.of(NPX.HAS_ALGORITHM, NPX.HAS_PUBLIC_KEY, NPX.HAS_SIGNATURE, NPX.HAS_SIGNATURE_TARGET, NPX.SIGNED_BY,
            NTEMPLATE.HAS_LABEL_FROM_API);

    private final Nanopub source;
    private final IRI creator;
    private final Set<IRI> named = new HashSet<>();
    private final Set<Value> assertionTypes = new HashSet<>();
    // The publication info without what is about the source alone, and what templates took.
    private final List<Statement> pubinfo = new ArrayList<>();
    private final Set<Statement> taken = new HashSet<>();
    private final Map<Template, Boolean> catchAll = new HashMap<>();

    /**
     * Creates a reader for a source, with the creator placeholder standing for any of its
     * creators.
     *
     * @param source the nanopublication to read
     */
    public SourceReader(Nanopub source) {
        this(source, null);
    }

    /**
     * Creates a reader for a source.
     *
     * @param source  the nanopublication to read
     * @param creator the creator of the nanopublication that is made, which the creator
     *                placeholder then stands for alone; or null, for any creator of the source
     */
    public SourceReader(Nanopub source, IRI creator) {
        this.source = Objects.requireNonNull(source, "source");
        this.creator = creator;
        named.addAll(SimpleCreatorPattern.getCreators(source));
        if (creator != null) named.add(creator);
        // The signature is added after the templates are filled in.
        for (Statement triple : source.getPubinfo()) {
            if (!SIGNATURE.contains(triple.getPredicate())) pubinfo.add(triple);
        }
        pubinfo.sort(Comparator.comparing(SourceReader::getSortKey));
    }

    /**
     * @return the nanopublication that is read
     */
    public Nanopub getSource() {
        return source;
    }

    /**
     * @return the creator the creator placeholder stands for, or null if it stands for any
     * creator of the source
     */
    public IRI getCreator() {
        return creator;
    }

    /**
     * Matches the assertion of the source against an assertion template. The types the
     * template gives a nanopublication are from then on taken as written by the fill itself.
     *
     * @param template the assertion template
     * @return the match; its unused triples are those of the assertion the template has no
     * statement for
     * @throws IllegalArgumentException if the template is one for the provenance or the
     *                                  publication info
     */
    public TemplateMatch readAssertion(Template template) {
        checkKind(template, "assertion");
        TemplateMatch match = match(template, source.getAssertion());
        assertionTypes.addAll(template.getTargetNanopubTypes());
        return match;
    }

    /**
     * Matches the provenance of the source against a provenance template.
     *
     * @param template the provenance template
     * @return the match; its unused triples are those of the provenance the template has no
     * statement for
     * @throws IllegalArgumentException if the template is not one for the provenance
     */
    public TemplateMatch readProvenance(Template template) {
        checkKind(template, "provenance");
        return match(template, source.getProvenance());
    }

    /**
     * Matches the publication info against a publication info template. A template that says
     * something about the source alone takes its triples away from the templates read after
     * it, as many times over as it can, and the match returned is the first. A template that
     * takes any triple only gets what the templates read before it have not taken.
     *
     * @param template the publication info template
     * @return the match; what is left over is in {@link #getUnreadPubinfo()}
     * @throws IllegalArgumentException if the template is not one for the publication info
     */
    public TemplateMatch readPubinfo(Template template) {
        checkKind(template, "publication info");
        if (isAboutSourceOnly(template)) {
            TemplateMatch match = remove(template);
            while (!remove(template).getMatches().isEmpty()) {
                // Each round takes another repetition, such as a further link to a
                // nanopublication the source was derived from.
            }
            return match;
        }
        List<Statement> available = pubinfo;
        if (isCatchAll(template)) {
            available = new ArrayList<>();
            for (Statement triple : withoutWrittenByFill(pubinfo)) {
                if (!taken.contains(triple)) available.add(triple);
            }
        }
        TemplateMatch match = match(template, available);
        for (TemplateMatch.StatementMatch statementMatch : match.getMatches()) taken.addAll(statementMatch.triples().values());
        return match;
    }

    /**
     * Reads the publication info for several templates, in the order that gives each its
     * share: first those that say something about the source alone, then the others, and last
     * those that take any triple.
     *
     * @param templates the publication info templates
     * @return the match of each template, in the order given
     * @throws IllegalArgumentException if one of them is not a publication info template
     */
    public Map<Template, TemplateMatch> readPubinfo(List<Template> templates) {
        for (Template template : templates) checkKind(template, "publication info");
        Map<Template, TemplateMatch> matches = new LinkedHashMap<>();
        for (Template template : templates) matches.put(template, null);
        for (int round = 0; round < 3; round++) {
            for (Template template : matches.keySet()) {
                int order = isAboutSourceOnly(template) ? 0 : isCatchAll(template) ? 2 : 1;
                if (order == round) matches.put(template, readPubinfo(template));
            }
        }
        return matches;
    }

    /**
     * Takes the triples of the publication info that a template gives away from the other
     * templates, as many times over as it can, without reading them as values. This is for a
     * template whose content the caller knows to be about the source alone, as when a later
     * version of the template is marked as transient.
     *
     * @param template the publication info template
     * @return the triples taken
     * @throws IllegalArgumentException if the template is not one for the publication info
     */
    public List<Statement> skipPubinfo(Template template) {
        checkKind(template, "publication info");
        List<Statement> removed = new ArrayList<>();
        while (true) {
            TemplateMatch match = remove(template);
            if (match.getMatches().isEmpty()) return removed;
            for (TemplateMatch.StatementMatch statementMatch : match.getMatches()) removed.addAll(statementMatch.triples().values());
        }
    }

    /**
     * Returns the values a match gives for a nanopublication made from the source: those of
     * {@link TemplateValues#of(TemplateMatch)}, and none for a publication info template that
     * says something about the source alone.
     *
     * @param match a match this reader returned
     * @return the values, by the names of the placeholders
     */
    public TemplateValues getValues(TemplateMatch match) {
        Objects.requireNonNull(match, "match");
        Template template = match.getTemplate();
        if (template.isPubinfoTemplate() && isAboutSourceOnly(template)) return new TemplateValues();
        return TemplateValues.of(match);
    }

    /**
     * @return the triples of the publication info that no template has taken so far, without
     * what a fill writes by itself; in a fixed order
     */
    public List<Statement> getUnreadPubinfo() {
        List<Statement> unread = new ArrayList<>();
        for (Statement triple : withoutWrittenByFill(pubinfo)) {
            if (!taken.contains(triple)) unread.add(triple);
        }
        return unread;
    }

    /**
     * Returns the labels the source states for values that were chosen from a list
     * ({@code nt:hasLabelFromApi}), as {@link FillRequest#setLabelFromApi(String, String)}
     * takes them.
     *
     * @return the labels, by IRI
     */
    public Map<String, String> getLabelsFromApi() {
        Map<String, String> labels = new TreeMap<>();
        for (Statement triple : source.getPubinfo()) {
            if (!triple.getPredicate().equals(NTEMPLATE.HAS_LABEL_FROM_API) || !(triple.getSubject() instanceof IRI iri)) continue;
            String label = triple.getObject().stringValue();
            if (!label.isBlank()) labels.putIfAbsent(iri.stringValue(), label);
        }
        return labels;
    }

    // ---------- matching ----------

    private TemplateMatch match(Template template, Collection<Statement> triples) {
        return match(template, triples, false);
    }

    private TemplateMatch match(Template template, Collection<Statement> triples, boolean anyCreator) {
        BindingContext context = BindingContext.forNanopub(template, source);
        if (creator != null && !anyCreator) context.pin(NTEMPLATE.CREATOR_PLACEHOLDER, creator);
        return new TemplateMatcher(template).match(triples, context);
    }

    // Matches a template whose content is about the source alone against the publication
    // info, and removes what it assigns, whichever creator of the source that is about.
    private TemplateMatch remove(Template template) {
        TemplateMatch match = match(template, pubinfo, true);
        for (TemplateMatch.StatementMatch statementMatch : match.getMatches()) pubinfo.removeAll(statementMatch.triples().values());
        return match;
    }

    // The triples without those a fill writes by itself: what it writes in any case, the types
    // of the assertion template, and the names of the creators.
    private List<Statement> withoutWrittenByFill(List<Statement> triples) {
        List<Statement> result = new ArrayList<>();
        for (Statement triple : triples) {
            if (isWrittenWithoutTemplate(triple, source)) continue;
            if (triple.getPredicate().equals(FOAF.NAME) && named.contains(triple.getSubject())) continue;
            if (triple.getSubject().equals(source.getUri()) && triple.getPredicate().equals(NPX.HAS_NANOPUB_TYPE)
                    && assertionTypes.contains(triple.getObject())) continue;
            result.add(triple);
        }
        return result;
    }

    private boolean isCatchAll(Template template) {
        return catchAll.computeIfAbsent(template, t -> isCatchAllTemplate(t, source));
    }

    private static void checkKind(Template template, String kind) {
        Objects.requireNonNull(template, "template");
        String actual = template.isProvenanceTemplate() ? "provenance" : template.isPubinfoTemplate() ? "publication info" : "assertion";
        if (!actual.equals(kind)) {
            throw new IllegalArgumentException("The template <" + template.getId() + "> is a template for the " + actual + ", not for the " + kind);
        }
    }

    private static String getSortKey(Statement triple) {
        return triple.getSubject() + " " + triple.getPredicate() + " " + triple.getObject();
    }

    // ---------- what is not read ----------

    // Whether what a publication info template gives is about the source alone: it is marked
    // as transient, or it gives nothing but the link to the nanopublication the source
    // supersedes or is derived from.
    static boolean isAboutSourceOnly(Template template) {
        return template.isTransient() || isLinkTemplate(template);
    }

    // Whether a publication info template gives nothing but the link to the nanopublication
    // that is superseded or derived from.
    static boolean isLinkTemplate(Template template) {
        TemplateStructure structure = new TemplateStructure(template);
        boolean any = false;
        for (IRI statement : template.getStatementIris()) {
            for (IRI member : structure.getMembers(statement)) {
                Value[] positions = structure.getPositions(member);
                if (!NTEMPLATE.NANOPUB_PLACEHOLDER.equals(positions[0])) return false;
                if (!NPX.SUPERSEDES.equals(positions[1]) && !PROV.WAS_DERIVED_FROM.equals(positions[1])) return false;
                any = true;
            }
        }
        return any;
    }

    // Whether a publication info template has a statement that takes any triple, as the
    // template for statements written by hand has.
    static boolean isCatchAllTemplate(Template template, Nanopub nanopub) {
        IRI predicate = vf.createIRI("https://example.org/any-predicate");
        for (IRI subject : List.of(nanopub.getUri(), vf.createIRI("https://example.org/any-subject"))) {
            for (Value object : List.of(vf.createIRI("https://example.org/any-object"), vf.createLiteral("any"))) {
                List<Statement> triple = List.of(vf.createStatement(subject, predicate, object));
                if (new TemplateMatcher(template).match(triple, BindingContext.forNanopub(template, nanopub)).getUnusedTriples().isEmpty()) return true;
            }
        }
        return false;
    }

    // Whether a triple of the publication info is one that filling writes by itself, without
    // a template statement for it. This is what Nanodash leaves out when it fills a form
    // from a nanopublication.
    static boolean isWrittenWithoutTemplate(Statement triple, Nanopub nanopub) {
        IRI predicate = triple.getPredicate();
        if (WRITTEN_ANYWHERE.contains(predicate)) return true;
        if (predicate.equals(RDF.TYPE) && (triple.getObject().equals(KPXL.INVERSE_ROLE_PROPERTY) || triple.getObject().equals(KPXL.REGULAR_ROLE_PROPERTY))) return true;
        if (!triple.getSubject().equals(nanopub.getUri())) return false;
        if (WRITTEN_ABOUT_NANOPUB.contains(predicate)) return true;
        return predicate.equals(NPX.HAS_NANOPUB_TYPE) && triple.getObject().equals(KPXL.ROLE_INSTANTIATION);
    }

}
