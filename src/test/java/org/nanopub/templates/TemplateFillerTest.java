package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.templates.FillRequest.Mode;
import org.nanopub.trusty.MakeTrustyNanopub;
import org.nanopub.vocabulary.KPXL;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.templates.TemplateComplianceCheckerTest.QUERY;
import static org.nanopub.templates.TemplateComplianceCheckerTest.RESOURCES;
import static org.nanopub.templates.TemplateComplianceCheckerTest.VIEW;
import static org.nanopub.templates.TemplateComplianceCheckerTest.load;

/**
 * Tests for {@link TemplateFiller}.
 */
class TemplateFillerTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String TEMPLATE = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final String OTHER_TEMPLATE = "https://w3id.org/np/RAOtherOtherOtherOtherOtherOtherOtherOther000";
    private static final String QUERY_TEMPLATE = "https://w3id.org/np/RAEFAt-QcFK0ZhqfvlsmS10BnzGJA0xwOICZXkO-ai87k";
    private static final String OLD_LICENSE_TEMPLATE = "https://w3id.org/np/RA0J4vUn_dekg-U1kK3AOEt02p9mT2WO03uGxLDec1jLw";
    private static final IRI ME = vf.createIRI("https://orcid.org/0000-0000-0000-0001");
    private static final IRI EX_THING = vf.createIRI("http://example.com/Thing");
    private static final IRI EX_ROOT = vf.createIRI("http://example.com/hasRoot");

    private final Map<String, Nanopub> nanopubs = new HashMap<>();

    // Templates and nanopublications made in a test, and otherwise those of the test resources.
    private final NanopubResolver resolver = id -> nanopubs.containsKey(id) ? nanopubs.get(id) : RESOURCES.getNanopub(id);

    private TemplateFiller filler() {
        return new TemplateFiller(resolver);
    }

    private void register(TemplateBuilder builder) throws Exception {
        nanopubs.put(builder.npUri, builder.buildNanopub());
    }

    private static FillRequest request(String template) {
        return new FillRequest(template).setCreator(ME).setCreationTime(new Date(0));
    }

    private Nanopub fill(FillRequest request) throws Exception {
        return filler().fill(request);
    }

    private List<String> problems(FillRequest request) {
        return assertThrows(TemplateFillException.class, () -> filler().fill(request)).getProblems();
    }

    private static void assertProblem(List<String> problems, String part) {
        assertTrue(problems.stream().anyMatch(p -> p.contains(part)), "No problem with '" + part + "' among: " + problems);
    }

    // The triples in a readable form, with the nanopublication as "this:" and what is
    // within it as "sub:".
    private static Set<String> triples(Collection<Statement> statements, Nanopub np) {
        Set<String> triples = new TreeSet<>();
        for (Statement st : statements) triples.add(str(st.getSubject(), np) + " " + str(st.getPredicate(), np) + " " + str(st.getObject(), np));
        return triples;
    }

    private static String str(Value value, Nanopub np) {
        if (!(value instanceof IRI)) return value.toString();
        String s = value.stringValue();
        String uri = np.getUri().stringValue();
        String base = uri.replaceFirst("[/#]$", "");
        if (s.equals(base) || s.equals(base + "/")) return "this:";
        if (s.startsWith(base + "/")) return "sub:" + s.substring(base.length() + 1);
        return s.replace(RDFS.NAMESPACE, "rdfs:").replace(RDF.NAMESPACE, "rdf:").replace(DCTERMS.NAMESPACE, "dct:")
                .replace(PROV.NAMESPACE, "prov:").replace(NPX.NAMESPACE, "npx:").replace(NTEMPLATE.NAMESPACE, "nt:")
                .replace("http://example.com/", "ex:");
    }

    private static Set<String> objects(Nanopub np, IRI predicate) {
        Set<String> objects = new TreeSet<>();
        for (Statement st : np.getPubinfo()) {
            if (st.getSubject().equals(np.getUri()) && st.getPredicate().equals(predicate)) objects.add(str(st.getObject(), np));
        }
        return objects;
    }

    // A template for a thing with a name, an optional comment, and any number of keywords:
    //   ?thing rdfs:label ?name .
    //   ?thing rdfs:comment ?comment .         (optional)
    //   ?thing dct:subject ?keyword .          (optional, repeatable)
    // The thing is minted within the nanopublication, which introduces it.
    private TemplateBuilder thingTemplate() throws Exception {
        TemplateBuilder t = new TemplateBuilder(TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE);
        IRI thing = t.declare("thing", NTEMPLATE.URI_PLACEHOLDER, NTEMPLATE.LOCAL_RESOURCE, NTEMPLATE.INTRODUCED_RESOURCE);
        t.add(thing, RDFS.LABEL, "identifier of the thing");
        t.statement(thing, RDFS.LABEL, t.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER));
        t.statement(thing, RDFS.COMMENT, t.declare("comment", NTEMPLATE.LITERAL_PLACEHOLDER), NTEMPLATE.OPTIONAL_STATEMENT);
        t.statement(thing, DCTERMS.SUBJECT, t.declare("keyword", NTEMPLATE.LITERAL_PLACEHOLDER), NTEMPLATE.OPTIONAL_STATEMENT, NTEMPLATE.REPEATABLE_STATEMENT);
        return t;
    }

    // ---------- what a fill gives ----------

    @Test
    void fillsATemplateWithItsValues() throws Exception {
        TemplateBuilder t = thingTemplate();
        t.add(t.templateNode, NTEMPLATE.HAS_NANOPUB_LABEL_PATTERN, "Thing: ${name}");
        t.add(t.templateNode, NTEMPLATE.HAS_TARGET_NANOPUB_TYPE, EX_THING);
        register(t);

        Nanopub np = fill(request(TEMPLATE).set("thing", "apple").set("name", "Apple").setCreatorName("Me"));

        assertEquals(Set.of("sub:apple rdfs:label \"Apple\""), triples(np.getAssertion(), np));
        assertEquals(Set.of("sub:assertion prov:wasAttributedTo " + ME), triples(np.getProvenance(), np));
        assertEquals(Set.of(ME.stringValue()), objects(np, DCTERMS.CREATOR));
        assertEquals(Set.of("https://creativecommons.org/licenses/by/4.0/"), objects(np, DCTERMS.LICENSE));
        assertEquals(Set.of("sub:apple"), objects(np, NPX.INTRODUCES));
        assertEquals(Set.of("\"Thing: Apple\""), objects(np, RDFS.LABEL));
        assertEquals(Set.of("ex:Thing"), objects(np, NPX.HAS_NANOPUB_TYPE));
        assertEquals(new Date(0), NanopubUtils.getStatements(np).stream().filter(st -> st.getPredicate().equals(DCTERMS.CREATED))
                .map(st -> ((Literal) st.getObject()).calendarValue().toGregorianCalendar().getTime()).findFirst().orElse(null));
        assertEquals(Set.of(TEMPLATE), objects(np, NTEMPLATE.WAS_CREATED_FROM_TEMPLATE));
        assertEquals(Set.of(TemplateFiller.DEFAULT_PROVENANCE_TEMPLATE), objects(np, NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE));
        assertEquals(Set.of(TemplateFiller.CREATOR_PUBINFO_TEMPLATE, TemplateFiller.LICENSE_PUBINFO_TEMPLATE), objects(np, NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE));
        assertTrue(np.getPubinfo().contains(vf.createStatement(ME, FOAF.NAME, vf.createLiteral("Me"), np.getPubinfoUri())));
        assertTrue(np.getUri().stringValue().startsWith("http://purl.org/nanopub/temp/"));
    }

    @Test
    void whatIsFilledCompliesWithItsTemplates() throws Exception {
        register(thingTemplate());
        Nanopub np = fill(request(TEMPLATE).set("thing", "apple").set("name", "Apple").set("keyword", "fruit").set("keyword__1", "red"));
        ComplianceReport report = new TemplateComplianceChecker(resolver).check(np);
        assertTrue(report.isCompliant(), report.getIssues().toString());
        assertEquals(List.of(), report.getIssues());
    }

    @Test
    void whatIsFilledCanBeSigned() throws Exception {
        register(thingTemplate());
        Nanopub np = fill(request(TEMPLATE).set("thing", "apple").set("name", "Apple"));
        Nanopub trusty = MakeTrustyNanopub.transform(np);
        String uri = trusty.getUri().stringValue();
        assertTrue(uri.matches("https://w3id\\.org/np/RA[A-Za-z0-9_-]{43}"), uri);
        assertEquals(Set.of("sub:apple rdfs:label \"Apple\""), triples(trusty.getAssertion(), trusty));
        assertTrue(trusty.getPubinfo().contains(vf.createStatement(trusty.getUri(), NPX.INTRODUCES, vf.createIRI(uri + "/apple"), trusty.getPubinfoUri())));
    }

    @Test
    void optionalStatementWithoutValueIsLeftOut() throws Exception {
        register(thingTemplate());
        Nanopub with = fill(request(TEMPLATE).set("thing", "apple").set("name", "Apple").set("comment", "A fruit."));
        assertEquals(Set.of("sub:apple rdfs:label \"Apple\"", "sub:apple rdfs:comment \"A fruit.\""), triples(with.getAssertion(), with));
        Nanopub without = fill(request(TEMPLATE).set("thing", "apple").set("name", "Apple"));
        assertEquals(Set.of("sub:apple rdfs:label \"Apple\""), triples(without.getAssertion(), without));
    }

    @Test
    void repeatableStatementIsRepeatedForEachValue() throws Exception {
        register(thingTemplate());
        FillRequest request = request(TEMPLATE).set("thing", "apple").set("name", "Apple");
        request.getValues().add("keyword", "fruit").add("keyword", "red").add("keyword", "sweet");
        assertEquals(Set.of("keyword", "keyword__1", "keyword__2", "thing", "name"), request.getValues().getNames());
        Nanopub np = fill(request);
        assertEquals(Set.of("sub:apple rdfs:label \"Apple\"", "sub:apple dct:subject \"fruit\"", "sub:apple dct:subject \"red\"",
                "sub:apple dct:subject \"sweet\""), triples(np.getAssertion(), np));
    }

    @Test
    void groupedStatementKeepsItsValuesTogether() throws Exception {
        // Each repetition has a part of its own with a title, and an optional page count.
        TemplateBuilder t = new TemplateBuilder(TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE);
        IRI book = t.declare("book", NTEMPLATE.URI_PLACEHOLDER);
        IRI part = t.declare("part", NTEMPLATE.URI_PLACEHOLDER, NTEMPLATE.LOCAL_RESOURCE);
        t.statement(book, RDF.TYPE, EX_THING);
        t.group("parts", new Object[][]{
                {book, DCTERMS.HAS_PART, part},
                {part, DCTERMS.TITLE, t.declare("title", NTEMPLATE.LITERAL_PLACEHOLDER)},
                {part, DCTERMS.EXTENT, t.declare("pages", NTEMPLATE.LITERAL_PLACEHOLDER), true}
        }, NTEMPLATE.REPEATABLE_STATEMENT);
        register(t);

        Nanopub np = fill(request(TEMPLATE).set("book", "http://example.com/book")
                .set("part", "one").set("title", "Beginning").set("pages", "12")
                .set("part__1", "two").set("title__1", "End"));

        assertEquals(Set.of("ex:book rdf:type ex:Thing",
                "ex:book dct:hasPart sub:one", "sub:one dct:title \"Beginning\"", "sub:one dct:extent \"12\"",
                "ex:book dct:hasPart sub:two", "sub:two dct:title \"End\""), triples(np.getAssertion(), np));
    }

    @Test
    void sequenceElementsCountTheRepetitions() throws Exception {
        TemplateBuilder t = new TemplateBuilder(TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE);
        IRI list = t.declare("list", NTEMPLATE.URI_PLACEHOLDER);
        t.statement(list, RDF.TYPE, RDF.SEQ);
        t.statement(list, t.declare("position", NTEMPLATE.SEQUENCE_ELEMENT_PLACEHOLDER), t.declare("item", NTEMPLATE.LITERAL_PLACEHOLDER), NTEMPLATE.REPEATABLE_STATEMENT);
        register(t);

        Nanopub np = fill(request(TEMPLATE).set("list", "http://example.com/list").set("item", "first").set("item__1", "second"));

        assertEquals(Set.of("ex:list rdf:type rdf:Seq", "ex:list rdf:_1 \"first\"", "ex:list rdf:_2 \"second\""), triples(np.getAssertion(), np));
    }

    @Test
    void defaultValueIsTakenUnlessAnEmptyValueIsGiven() throws Exception {
        TemplateBuilder t = thingTemplate();
        t.add(t.iri("comment"), NTEMPLATE.HAS_DEFAULT_VALUE, "No comment.");
        register(t);

        Nanopub byDefault = fill(request(TEMPLATE).set("thing", "apple").set("name", "Apple"));
        assertEquals(Set.of("sub:apple rdfs:label \"Apple\"", "sub:apple rdfs:comment \"No comment.\""), triples(byDefault.getAssertion(), byDefault));
        Nanopub given = fill(request(TEMPLATE).set("thing", "apple").set("name", "Apple").set("comment", "Mine."));
        assertEquals(Set.of("sub:apple rdfs:label \"Apple\"", "sub:apple rdfs:comment \"Mine.\""), triples(given.getAssertion(), given));
        Nanopub empty = fill(request(TEMPLATE).set("thing", "apple").set("name", "Apple").set("comment", ""));
        assertEquals(Set.of("sub:apple rdfs:label \"Apple\""), triples(empty.getAssertion(), empty));
    }

    @Test
    void languageTagIsTakenFromTheValue() throws Exception {
        TemplateBuilder t = new TemplateBuilder(TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE);
        t.statement(EX_THING, RDFS.LABEL, t.declare("title", NTEMPLATE.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER));
        register(t);

        FillRequest request = FillRequest.fromParams(Map.of("template", TEMPLATE, "param_title", "Titel", "param_title__lang", "de")).setCreator(ME);
        Nanopub np = fill(request);

        assertEquals(Set.of("ex:Thing rdfs:label \"Titel\"@de"), triples(np.getAssertion(), np));
    }

    @Test
    void embeddedResourceAndRolePinAreDeclared() throws Exception {
        TemplateBuilder t = new TemplateBuilder(TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE);
        IRI item = t.declare("item", NTEMPLATE.LOCAL_RESOURCE, NTEMPLATE.EMBEDDED_RESOURCE);
        IRI role = t.declare("role", NTEMPLATE.URI_PLACEHOLDER, KPXL.INVERSE_ROLE_PROPERTY);
        t.statement(item, role, t.declare("agent", NTEMPLATE.URI_PLACEHOLDER));
        register(t);

        Nanopub np = fill(request(TEMPLATE).set("role", "http://example.com/hasMember").set("agent", ME.stringValue()));

        assertEquals(Set.of("sub:item ex:hasMember " + ME), triples(np.getAssertion(), np));
        assertEquals(Set.of("sub:item"), objects(np, NPX.EMBEDS));
        assertEquals(Set.of(KPXL.ROLE_INSTANTIATION.stringValue()), objects(np, NPX.HAS_NANOPUB_TYPE));
        assertTrue(np.getPubinfo().contains(vf.createStatement(vf.createIRI("http://example.com/hasMember"), RDF.TYPE, KPXL.INVERSE_ROLE_PROPERTY, np.getPubinfoUri())));
    }

    @Test
    void targetNamespaceGivesAUriWithTheArtifactCodeMarker() throws Exception {
        register(thingTemplate());
        Nanopub np = fill(request(TEMPLATE).set("thing", "apple").set("name", "Apple").setTargetNamespace("https://example.org/np/"));
        assertEquals("https://example.org/np/~~~ARTIFACTCODE~~~/", np.getUri().stringValue());
        Nanopub trusty = MakeTrustyNanopub.transform(np);
        assertTrue(trusty.getUri().stringValue().matches("https://example\\.org/np/RA[A-Za-z0-9_-]{43}"), trusty.getUri().stringValue());
    }

    // ---------- the templates of the provenance and the publication info ----------

    @Test
    void templatesAndValuesOfTheOtherGraphsCanBeGiven() throws Exception {
        register(thingTemplate());
        FillRequest request = request(TEMPLATE).set("thing", "apple").set("name", "Apple");
        request.getProvenanceValues().set("agent", "https://orcid.org/0000-0000-0000-0002").set("agent__1", "https://orcid.org/0000-0000-0000-0003");
        request.addPubinfoTemplate(TemplateFiller.LICENSE_PUBINFO_TEMPLATE).set("license", "https://creativecommons.org/publicdomain/zero/1.0/");

        Nanopub np = fill(request);

        assertEquals(Set.of("sub:assertion prov:wasAttributedTo https://orcid.org/0000-0000-0000-0002",
                "sub:assertion prov:wasAttributedTo https://orcid.org/0000-0000-0000-0003"), triples(np.getProvenance(), np));
        assertEquals(Set.of("https://creativecommons.org/publicdomain/zero/1.0/"), objects(np, DCTERMS.LICENSE));
        assertEquals(Set.of(TemplateFiller.CREATOR_PUBINFO_TEMPLATE, TemplateFiller.LICENSE_PUBINFO_TEMPLATE), objects(np, NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE));
    }

    @Test
    void anotherVersionOfAStandardTemplateTakesItsPlace() throws Exception {
        register(thingTemplate());
        FillRequest request = request(TEMPLATE).set("thing", "apple").set("name", "Apple");
        request.addPubinfoTemplate(OLD_LICENSE_TEMPLATE).set("license", "https://creativecommons.org/publicdomain/zero/1.0/");

        Nanopub np = fill(request);

        // One license, not also the default one of the current license template.
        assertEquals(Set.of("https://creativecommons.org/publicdomain/zero/1.0/"), objects(np, DCTERMS.LICENSE));
        assertEquals(Set.of(TemplateFiller.CREATOR_PUBINFO_TEMPLATE, OLD_LICENSE_TEMPLATE), objects(np, NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE));
    }

    @Test
    void defaultProvenanceAndRequiredPubinfoOfTheTemplateAreUsed() throws Exception {
        TemplateBuilder provenance = new TemplateBuilder(OTHER_TEMPLATE, NTEMPLATE.PROVENANCE_TEMPLATE);
        provenance.statement(NTEMPLATE.ASSERTION_PLACEHOLDER, PROV.WAS_GENERATED_BY, EX_THING);
        register(provenance);
        TemplateBuilder t = thingTemplate();
        t.add(t.templateNode, NTEMPLATE.HAS_DEFAULT_PROVENANCE, vf.createIRI(OTHER_TEMPLATE));
        t.add(t.templateNode, NTEMPLATE.HAS_REQUIRED_PUBINFO_ELEMENT, vf.createIRI(TemplateFiller.SUPERSEDES_PUBINFO_TEMPLATE));
        register(t);

        // The required element needs a value, which is not given:
        List<String> problems = problems(request(TEMPLATE).set("thing", "apple").set("name", "Apple"));
        assertEquals(List.of("The statement st1 of the publication info template lacks a value for 'piparam1_np' (URI of superseded nanopublication)",
                "To give values for the publication info template <" + TemplateFiller.SUPERSEDES_PUBINFO_TEMPLATE + ">, which is used without being named,"
                        + " name it as 'pitemplate1'"), problems);

        FillRequest request = request(TEMPLATE).set("thing", "apple").set("name", "Apple");
        request.addPubinfoTemplate(TemplateFiller.SUPERSEDES_PUBINFO_TEMPLATE).set("np", "http://example.com/old");
        Nanopub np = fill(request);
        assertEquals(Set.of("sub:assertion prov:wasGeneratedBy ex:Thing"), triples(np.getProvenance(), np));
        assertEquals(Set.of("ex:old"), objects(np, NPX.SUPERSEDES));
    }

    // ---------- what is refused ----------

    @Test
    void missingValueIsReported() throws Exception {
        register(thingTemplate());
        List<String> problems = problems(request(TEMPLATE).set("thing", "apple"));
        assertEquals(List.of("The statement st1 of the assertion template lacks a value for 'param_name'"), problems);
    }

    @Test
    void allProblemsAreReportedAtOnce() throws Exception {
        TemplateBuilder t = thingTemplate();
        t.add(t.iri("name"), NTEMPLATE.HAS_REGEX, "[A-Z][a-z]+");
        IRI kind = t.declare("kind", NTEMPLATE.RESTRICTED_CHOICE_PLACEHOLDER);
        t.add(kind, NTEMPLATE.POSSIBLE_VALUE, vf.createIRI("http://example.com/Fruit")).add(kind, NTEMPLATE.POSSIBLE_VALUE, vf.createIRI("http://example.com/Tool"));
        t.statement(t.iri("thing"), RDF.TYPE, kind);
        register(t);

        List<String> problems = problems(request(TEMPLATE).set("thing", "an apple").set("name", "apple").set("kind", "http://example.com/Animal").set("colour", "red"));

        assertProblem(problems, "The value of 'param_thing' cannot be used: Not a well-formed IRI");
        assertProblem(problems, "Value 'apple' of 'name' doesn't match the pattern '[A-Z][a-z]+' (param_name)");
        assertProblem(problems, "(param_kind)");
        assertProblem(problems, "No placeholder of the assertion template answers to 'param_colour'; its placeholders are: comment, keyword, kind, name, thing");
        assertEquals(4, problems.size(), problems.toString());
    }

    @Test
    void valueOfAStatementThatIsLeftOutIsNotLostSilently() throws Exception {
        // The optional statement needs both the unit and the amount.
        TemplateBuilder t = new TemplateBuilder(TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE);
        t.statement(EX_THING, RDFS.LABEL, t.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER));
        t.statement(EX_THING, t.declare("unit", NTEMPLATE.URI_PLACEHOLDER), t.declare("amount", NTEMPLATE.LITERAL_PLACEHOLDER), NTEMPLATE.OPTIONAL_STATEMENT);
        register(t);

        List<String> problems = problems(request(TEMPLATE).set("name", "Apple").set("amount", "3"));

        assertEquals(List.of("The value of 'param_amount' would be lost: the statement st2 it belongs to lacks a value for 'param_unit'"), problems);
    }

    @Test
    void furtherRepetitionNeedsAllItsValues() throws Exception {
        TemplateBuilder t = new TemplateBuilder(TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE);
        t.statement(EX_THING, t.declare("unit", NTEMPLATE.URI_PLACEHOLDER), t.declare("amount", NTEMPLATE.LITERAL_PLACEHOLDER), NTEMPLATE.REPEATABLE_STATEMENT);
        register(t);

        List<String> problems = problems(request(TEMPLATE).set("unit", "http://example.com/kg").set("amount", "3").set("amount__1", "4"));

        assertEquals(List.of("The statement st1 (repetition 2) of the assertion template lacks a value for 'param_unit__1'"), problems);
    }

    @Test
    void nameOfARepetitionIsNotKnownForAStatementThatIsNotRepeated() throws Exception {
        register(thingTemplate());
        assertProblem(problems(request(TEMPLATE).set("thing", "apple").set("name", "Apple").set("name__1", "Pear")),
                "No placeholder of the assertion template answers to 'param_name__1'");
    }

    @Test
    void creatorIsNeeded() throws Exception {
        register(thingTemplate());
        List<String> problems = problems(new FillRequest(TEMPLATE).set("thing", "apple").set("name", "Apple"));
        assertProblem(problems, "of the publication info template lacks the creator (creator)");
    }

    @Test
    void templateThatCannotBeUsedIsReported() throws Exception {
        register(thingTemplate());
        assertProblem(problems(request("https://w3id.org/np/RANoSuchTemplateNoSuchTemplateNoSuchTemplat0")), "cannot be used");
        assertProblem(problems(request(TemplateFiller.LICENSE_PUBINFO_TEMPLATE)), "is a template for the publication info, not for the assertion");
        assertProblem(problems(request(TEMPLATE).setProvenanceTemplate(TEMPLATE)), "is a template for the assertion, not for the provenance");
    }

    @Test
    void reservedNameIsRefusedForANewResource() throws Exception {
        register(thingTemplate());
        assertProblem(problems(request(TEMPLATE).set("thing", "assertion").set("name", "Apple")), "'assertion' is what the nanopublication calls one of its own parts");
    }

    // ---------- new versions ----------

    // A template for a thing that says where its root definition is, and that has an item
    // of its own which the template names.
    private TemplateBuilder versionedTemplate() throws Exception {
        TemplateBuilder t = thingTemplate();
        t.statement(t.iri("thing"), EX_ROOT, t.declare("root", NTEMPLATE.ROOT_NANOPUB_PLACEHOLDER));
        t.statement(t.iri("thing"), DCTERMS.HAS_PART, t.declare("item", NTEMPLATE.LOCAL_RESOURCE, NTEMPLATE.INTRODUCED_RESOURCE));
        return t;
    }

    // Fills, signs and keeps a nanopublication, as one that was published.
    private Nanopub publish(FillRequest request) throws Exception {
        Nanopub np = MakeTrustyNanopub.transform(fill(request));
        nanopubs.put(np.getUri().stringValue(), np);
        return np;
    }

    @Test
    void nanopublicationIsItsOwnRootDefinition() throws Exception {
        register(versionedTemplate());
        Nanopub np = fill(request(TEMPLATE).set("thing", "apple").set("name", "Apple"));
        assertEquals(Set.of("sub:apple rdfs:label \"Apple\"", "sub:apple ex:hasRoot this:", "sub:apple dct:hasPart sub:item"), triples(np.getAssertion(), np));
        assertEquals(Set.of("sub:apple", "sub:item"), objects(np, NPX.INTRODUCES));
    }

    @Test
    void supersedingKeepsIntroducedResourcesAndRoot() throws Exception {
        register(versionedTemplate());
        Nanopub first = publish(request(TEMPLATE).set("thing", "apple").set("name", "Apple"));
        String v1 = first.getUri().stringValue();

        Nanopub second = publish(request(TEMPLATE).set("thing", "apple").set("name", "Apple, improved").setSource(Mode.SUPERSEDE, v1));

        assertEquals(Set.of(v1 + "/apple rdfs:label \"Apple, improved\"", v1 + "/apple ex:hasRoot " + v1, v1 + "/apple dct:hasPart " + v1 + "/item"),
                triples(second.getAssertion(), second));
        assertEquals(Set.of(v1 + "/apple", v1 + "/item"), objects(second, NPX.INTRODUCES));
        assertEquals(Set.of(v1), objects(second, NPX.SUPERSEDES));
        assertTrue(objects(second, NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE).contains(TemplateFiller.SUPERSEDES_PUBINFO_TEMPLATE));

        // A third version keeps the root of the first, which the second does not hold itself.
        Nanopub third = fill(request(TEMPLATE).set("thing", "apple").set("name", "Apple, final").setSource(Mode.SUPERSEDE, second.getUri().stringValue()));
        assertEquals(Set.of(v1 + "/apple rdfs:label \"Apple, final\"", v1 + "/apple ex:hasRoot " + v1, v1 + "/apple dct:hasPart " + v1 + "/item"),
                triples(third.getAssertion(), third));
        assertEquals(Set.of(second.getUri().stringValue()), objects(third, NPX.SUPERSEDES));
    }

    @Test
    void newVersionCompliesWithItsTemplatesThoughItsResourceIsNamedInTheOldOne() throws Exception {
        // The name of the thing has a pattern, which the IRI kept from the old version is
        // not held against.
        TemplateBuilder t = versionedTemplate();
        t.add(t.iri("thing"), NTEMPLATE.HAS_REGEX, "[a-z]+");
        register(t);
        Nanopub first = publish(request(TEMPLATE).set("thing", "apple").set("name", "Apple"));

        Nanopub second = publish(request(TEMPLATE).set("thing", "apple").set("name", "Apple, improved").setSource(Mode.SUPERSEDE, first.getUri().stringValue()));

        assertTrue(triples(second.getAssertion(), second).contains(first.getUri() + "/apple rdfs:label \"Apple, improved\""));
        ComplianceReport report = new TemplateComplianceChecker(resolver).check(second);
        assertEquals(List.of(), report.getIssues());
        // Read back and filled again as a third version, it is still that resource.
        Nanopub third = fill(FillRequest.of(report).setSource(Mode.SUPERSEDE, second.getUri().stringValue()));
        assertEquals(triples(second.getAssertion(), second), triples(third.getAssertion(), third));
    }

    @Test
    void supersedingMintsWhatTheOldVersionDidNotIntroduce() throws Exception {
        register(versionedTemplate());
        Nanopub first = publish(request(TEMPLATE).set("thing", "apple").set("name", "Apple"));
        String v1 = first.getUri().stringValue();

        Nanopub second = fill(request(TEMPLATE).set("thing", "pear").set("name", "Pear").setSource(Mode.SUPERSEDE, v1));

        assertEquals(Set.of("sub:pear rdfs:label \"Pear\"", "sub:pear ex:hasRoot " + v1, "sub:pear dct:hasPart " + v1 + "/item"), triples(second.getAssertion(), second));
    }

    @Test
    void overridingKeepsIntroducedResourcesAndIsLinkedAsDerived() throws Exception {
        register(versionedTemplate());
        Nanopub first = publish(request(TEMPLATE).set("thing", "apple").set("name", "Apple"));
        String v1 = first.getUri().stringValue();

        Nanopub second = fill(request(TEMPLATE).set("thing", "apple").set("name", "Apple, as I see it").setSource(Mode.OVERRIDE, v1));

        assertEquals(Set.of(v1 + "/apple rdfs:label \"Apple, as I see it\"", v1 + "/apple ex:hasRoot " + v1, v1 + "/apple dct:hasPart " + v1 + "/item"),
                triples(second.getAssertion(), second));
        assertEquals(Set.of(v1), objects(second, PROV.WAS_DERIVED_FROM));
        assertEquals(Set.of(), objects(second, NPX.SUPERSEDES));
        assertTrue(objects(second, NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE).contains(TemplateFiller.DERIVED_FROM_PUBINFO_TEMPLATE));
    }

    @Test
    void derivingMintsAnewAndIsItsOwnRoot() throws Exception {
        register(versionedTemplate());
        Nanopub first = publish(request(TEMPLATE).set("thing", "apple").set("name", "Apple"));
        String v1 = first.getUri().stringValue();

        Nanopub second = fill(request(TEMPLATE).set("thing", "apple").set("name", "Another apple").setSource(Mode.DERIVE, v1));

        assertEquals(Set.of("sub:apple rdfs:label \"Another apple\"", "sub:apple ex:hasRoot this:", "sub:apple dct:hasPart sub:item"), triples(second.getAssertion(), second));
        assertEquals(Set.of("sub:apple", "sub:item"), objects(second, NPX.INTRODUCES));
        assertEquals(Set.of(v1), objects(second, PROV.WAS_DERIVED_FROM));
    }

    @Test
    void reservedNameIsKeptFromTheVersionThatHasIt() throws Exception {
        // A template in the old shape has its own assertion graph as its node.
        TemplateBuilder t = new TemplateBuilder(TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE);
        t.statement(t.declare("node", NTEMPLATE.URI_PLACEHOLDER, NTEMPLATE.LOCAL_RESOURCE), RDFS.LABEL, t.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER));
        register(t);
        String old = "https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV";
        nanopubs.put(old, new NanopubImpl("""
                @prefix this: <%1$s> .
                @prefix sub: <%1$s/> .
                @prefix np: <http://www.nanopub.org/nschema#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix dct: <http://purl.org/dc/terms/> .
                sub:Head { this: a np:Nanopublication ; np:hasAssertion sub:assertion ;
                    np:hasProvenance sub:provenance ; np:hasPublicationInfo sub:pubinfo . }
                sub:assertion { sub:assertion rdfs:label "Old" . }
                sub:provenance { sub:assertion dct:creator <%2$s> . }
                sub:pubinfo { this: dct:creator <%2$s> . }
                """.formatted(old, ME), RDFFormat.TRIG));

        Nanopub np = fill(request(TEMPLATE).set("node", "assertion").set("name", "New").setSource(Mode.SUPERSEDE, old));
        assertEquals(Set.of("sub:assertion rdfs:label \"New\""), triples(np.getAssertion(), np));
        // A name the old version does not have as a resource is not excused:
        assertProblem(problems(request(TEMPLATE).set("node", "provenance").set("name", "New").setSource(Mode.SUPERSEDE, old)), "'provenance' is what the nanopublication calls");
    }

    @Test
    void derivingDoesNotExcuseAReservedName() throws Exception {
        register(thingTemplate());
        Nanopub first = publish(request(TEMPLATE).set("thing", "apple").set("name", "Apple"));
        assertProblem(problems(request(TEMPLATE).set("thing", "assertion").set("name", "New").setSource(Mode.DERIVE, first.getUri().stringValue())),
                "'assertion' is what the nanopublication calls");
    }

    @Test
    void linkGivenAsAValueHasToAgreeWithTheMode() throws Exception {
        register(versionedTemplate());
        Nanopub first = publish(request(TEMPLATE).set("thing", "apple").set("name", "Apple"));
        String v1 = first.getUri().stringValue();

        FillRequest agreeing = request(TEMPLATE).set("thing", "apple").set("name", "Apple").setSource(Mode.SUPERSEDE, v1);
        agreeing.addPubinfoTemplate(TemplateFiller.SUPERSEDES_PUBINFO_TEMPLATE).set("np", v1);
        assertEquals(Set.of(v1), objects(fill(agreeing), NPX.SUPERSEDES));

        FillRequest other = request(TEMPLATE).set("thing", "apple").set("name", "Apple").setSource(Mode.SUPERSEDE, v1);
        other.addPubinfoTemplate(TemplateFiller.SUPERSEDES_PUBINFO_TEMPLATE).set("np", "http://example.com/older");
        assertEquals(List.of("The request names <" + v1 + "> with 'supersede', but gives the template for that link <http://example.com/older>; leave that value out"),
                problems(other));
    }

    @Test
    void nanopublicationToSupersedeHasToBeThere() throws Exception {
        register(thingTemplate());
        assertProblem(problems(request(TEMPLATE).set("thing", "apple").set("name", "Apple").setSource(Mode.SUPERSEDE, "https://w3id.org/np/RANoSuchNanopubNoSuchNanopubNoSuchNanopub000")),
                "cannot be retrieved");
    }

    // ---------- round trip on published nanopublications ----------

    // Reads the request out of a published nanopublication, fills it again, and compares
    // the result with the original.
    private void assertRoundTrip(String artifactCode) throws Exception {
        Nanopub original = load(artifactCode);
        ComplianceReport report = new TemplateComplianceChecker(resolver).check(original);
        assertTrue(report.isCompliant(), report.getIssues().toString());

        FillRequest request = FillRequest.fromJson(FillRequest.of(report).toJson());
        Nanopub filled = fill(request);

        assertEquals(triples(original.getAssertion(), original), triples(filled.getAssertion(), filled));
        assertEquals(triples(original.getProvenance(), original), triples(filled.getProvenance(), filled));
        for (IRI predicate : List.of(DCTERMS.CREATOR, DCTERMS.LICENSE, NPX.INTRODUCES, NPX.EMBEDS, NPX.HAS_NANOPUB_TYPE, RDFS.LABEL,
                NTEMPLATE.WAS_CREATED_FROM_TEMPLATE, NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE, NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE)) {
            assertEquals(objects(original, predicate), objects(filled, predicate), predicate.toString());
        }
        assertTrue(new TemplateComplianceChecker(resolver).check(filled).isCompliant());
    }

    @Test
    void publishedQueryIsFilledAgainFromItsValues() throws Exception {
        assertRoundTrip(QUERY);
    }

    @Test
    void publishedViewWithRepeatedAndGroupedStatementsIsFilledAgainFromItsValues() throws Exception {
        assertRoundTrip(VIEW);
    }

    @Test
    void queryIsFilledFromALink() throws Exception {
        FillRequest request = FillRequest.fromUrl("https://nanodash.example.org/publish?template=" + QUERY_TEMPLATE
                + "&param_query=get-things&param_title=Get+things&param_description=Returns+three+things."
                + "&param_endpoint=https%3A%2F%2Fw3id.org%2Fnp%2Fl%2Fnanopub-query-1.1%2Frepo%2Ffull"
                + "&param_license=http%3A%2F%2Fwww.apache.org%2Flicenses%2FLICENSE-2.0"
                + "&param_sparql=select+*+where+%7B+%3Fs+%3Fp+%3Fo+%7D+limit+3&creator=" + ME);
        Nanopub np = fill(request);
        assertTrue(triples(np.getAssertion(), np).contains("sub:get-things https://w3id.org/kpxl/grlc/sparql \"select * where { ?s ?p ?o } limit 3\""),
                triples(np.getAssertion(), np).toString());
        assertEquals(Set.of("sub:get-things"), objects(np, NPX.EMBEDS));
        assertTrue(new TemplateComplianceChecker(resolver).check(np).isCompliant());
        // Written out and read again, it is the same nanopublication.
        Nanopub reread = new NanopubImpl(NanopubUtils.writeToString(np, RDFFormat.TRIG), RDFFormat.TRIG);
        assertEquals(triples(np.getAssertion(), np), triples(reread.getAssertion(), reread));
    }

}
