package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.templates.ComplianceIssue.Graph;
import org.nanopub.templates.ComplianceIssue.Kind;
import org.nanopub.templates.ComplianceIssue.Severity;
import org.nanopub.vocabulary.KPXL;
import org.nanopub.vocabulary.KPXL_GRLC;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TemplateComplianceChecker}.
 */
class TemplateComplianceCheckerTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static IRI iri(String s) {
        return vf.createIRI(s);
    }

    // ---------- published nanopublications and their templates ----------

    // A query made with Nanodash, and a view with repeated and grouped statements.
    static final String QUERY = "RAa4_F5rX1q3-E840jnabqGA9GuwTBa1hnJQPNC9fp_P0";
    static final String VIEW = "RA5U7rrFB9KaCAQi3mSfQlzJAbc44clN-gmrxZf6v2lJA";

    static File resourceFile(String artifactCode) throws Exception {
        return new File(TemplateComplianceCheckerTest.class.getResource("/templates/" + artifactCode + ".trig").toURI());
    }

    static Nanopub load(String artifactCode) throws Exception {
        return new NanopubImpl(resourceFile(artifactCode));
    }

    /**
     * Retrieves nanopublications from the test resources, by the artifact code in the
     * IRI asked for.
     */
    static final NanopubResolver RESOURCES = id -> {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("RA[A-Za-z0-9_-]{43}").matcher(id);
        if (!m.find() || TemplateComplianceCheckerTest.class.getResource("/templates/" + m.group() + ".trig") == null) return null;
        try {
            return load(m.group());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    };

    // The nanopublication with some of its statements taken out and others added.
    private static Nanopub change(Nanopub np, Predicate<Statement> remove, Statement... add) throws Exception {
        List<Statement> statements = new ArrayList<>();
        for (Statement st : NanopubUtils.getStatements(np)) {
            if (!remove.test(st)) statements.add(st);
        }
        statements.addAll(List.of(add));
        return new NanopubImpl(statements);
    }

    private static List<Kind> errors(ComplianceReport report) {
        return report.getErrors().stream().map(ComplianceIssue::kind).toList();
    }

    private static List<Kind> warnings(ComplianceReport report) {
        return report.getWarnings().stream().map(ComplianceIssue::kind).toList();
    }

    @Test
    void publishedNanopublicationsComply() throws Exception {
        TemplateComplianceChecker checker = new TemplateComplianceChecker(RESOURCES);

        ComplianceReport query = checker.check(load(QUERY));
        assertEquals(List.of(), query.getIssues());
        assertTrue(query.isCompliant());
        assertEquals(QUERY, query.getNanopub().getUri().stringValue().substring("https://w3id.org/np/".length()));
        assertTrue(query.getAssertionMatch().isComplete());
        assertTrue(query.getProvenanceMatch().isComplete());
        assertEquals(2, query.getPubinfoMatches().size());

        ComplianceReport view = checker.check(load(VIEW));
        assertEquals(List.of(), view.getIssues());
        // The view has a repeated statement and grouped ones:
        assertTrue(view.getAssertionMatch().getMatches().stream().anyMatch(m -> m.repetition() > 0));
        assertTrue(view.getAssertionMatch().getMatches().stream().anyMatch(m -> m.triples().size() > 1));
    }

    @Test
    void missingRequiredTripleIsAnError() throws Exception {
        Nanopub np = load(QUERY);
        Nanopub changed = change(np, st -> st.getContext().equals(np.getAssertionUri()) && st.getPredicate().equals(DCTERMS.DESCRIPTION));

        ComplianceReport report = new TemplateComplianceChecker(RESOURCES).check(changed);
        assertFalse(report.isCompliant());
        assertEquals(List.of(Kind.MISSING_STATEMENT), errors(report));
        ComplianceIssue issue = report.getErrors().get(0);
        assertEquals(Graph.ASSERTION, issue.graph());
        assertEquals(Severity.ERROR, issue.severity());
        assertEquals(iri("https://w3id.org/np/RAEFAt-QcFK0ZhqfvlsmS10BnzGJA0xwOICZXkO-ai87k/st2"), issue.statement());
        assertEquals("Nothing matches the statement st2 (?query <http://purl.org/dc/terms/description> ?description) of the assertion template", issue.message());
        // An optional statement may go:
        Nanopub withoutLicense = change(np, st -> st.getContext().equals(np.getAssertionUri()) && st.getPredicate().equals(DCTERMS.LICENSE));
        assertEquals(List.of(), new TemplateComplianceChecker(RESOURCES).check(withoutLicense).getIssues());
    }

    @Test
    void foreignTripleIsAnErrorInAssertionAndProvenanceAndAWarningInPubinfo() throws Exception {
        Nanopub np = load(QUERY);
        IRI query = iri(np.getUri() + "/html-test");
        TemplateComplianceChecker checker = new TemplateComplianceChecker(RESOURCES);

        Statement inAssertion = vf.createStatement(query, RDFS.COMMENT, vf.createLiteral("extra"), np.getAssertionUri());
        ComplianceReport assertion = checker.check(change(np, st -> false, inAssertion));
        assertEquals(List.of(Kind.UNMATCHED_TRIPLE), errors(assertion));
        assertEquals(Graph.ASSERTION, assertion.getErrors().get(0).graph());
        assertEquals(inAssertion.getObject(), assertion.getErrors().get(0).triple().getObject());

        Statement inProvenance = vf.createStatement(np.getAssertionUri(), PROV.WAS_DERIVED_FROM, iri("https://example.com/source"), np.getProvenanceUri());
        ComplianceReport provenance = checker.check(change(np, st -> false, inProvenance));
        assertEquals(List.of(Kind.UNMATCHED_TRIPLE), errors(provenance));
        assertEquals(Graph.PROVENANCE, provenance.getErrors().get(0).graph());

        Statement inPubinfo = vf.createStatement(np.getUri(), RDFS.SEEALSO, iri("https://example.com/more"), np.getPubinfoUri());
        ComplianceReport pubinfo = checker.check(change(np, st -> false, inPubinfo));
        assertTrue(pubinfo.isCompliant());
        assertEquals(List.of(Kind.UNMATCHED_TRIPLE), warnings(pubinfo));
        assertEquals(Graph.PUBINFO, pubinfo.getWarnings().get(0).graph());
    }

    @Test
    void valueThatBreaksAConstraintIsAnError() throws Exception {
        Nanopub np = load(QUERY);
        IRI query = iri(np.getUri() + "/html-test");
        Nanopub changed = change(np, st -> st.getPredicate().equals(KPXL_GRLC.SPARQL),
                vf.createStatement(query, KPXL_GRLC.SPARQL, vf.createLiteral("select * where {"), np.getAssertionUri()));

        ComplianceReport report = new TemplateComplianceChecker(RESOURCES).check(changed);
        assertEquals(List.of(Kind.VIOLATION), errors(report));
        assertTrue(report.getErrors().get(0).message().startsWith("The value of 'SPARQL query template' is not valid SPARQL"));
    }

    @Test
    void undeclaredEmbeddedResourceIsAWarning() throws Exception {
        Nanopub np = load(QUERY);
        Nanopub changed = change(np, st -> st.getPredicate().equals(NPX.EMBEDS));

        ComplianceReport report = new TemplateComplianceChecker(RESOURCES).check(changed);
        assertTrue(report.isCompliant());
        assertEquals(List.of(Kind.UNDECLARED_EMBEDDED_RESOURCE), warnings(report));
    }

    @Test
    void templateThatCannotBeRetrievedIsAnError() throws Exception {
        ComplianceReport report = new TemplateComplianceChecker(NanopubResolver.NONE).check(load(QUERY));
        assertFalse(report.isCompliant());
        assertEquals(List.of(Kind.TEMPLATE_UNAVAILABLE, Kind.TEMPLATE_UNAVAILABLE, Kind.TEMPLATE_UNAVAILABLE, Kind.TEMPLATE_UNAVAILABLE), errors(report));
        assertNull(report.getAssertionMatch());
        assertNull(report.getProvenanceMatch());
        assertEquals(List.of(), report.getPubinfoMatches());
        // With a template missing, the triples it might have explained are not reported:
        assertEquals(List.of(), report.getWarnings());
        assertTrue(report.getErrors().get(0).message().contains("nanopublication could not be retrieved"));
    }

    @Test
    void templatesAreRetrievedOnce() throws Exception {
        List<String> requests = new ArrayList<>();
        TemplateComplianceChecker checker = new TemplateComplianceChecker(id -> {
            requests.add(id);
            return RESOURCES.getNanopub(id);
        });
        checker.check(load(QUERY));
        int afterFirst = requests.size();
        checker.check(load(QUERY));
        assertEquals(4, afterFirst);
        assertEquals(afterFirst, requests.size());
    }

    // ---------- nanopublications and templates made for the test ----------

    private static final String ASSERTION_TEMPLATE = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final String PROVENANCE_TEMPLATE = "https://w3id.org/np/RAProvProvProvProvProvProvProvProvProvProvPro";
    private static final String CREATOR_TEMPLATE = "https://w3id.org/np/RACreatorCreatorCreatorCreatorCreatorCreator0";
    private static final String CREATOR_TEMPLATE_2 = "https://w3id.org/np/RACreatorCreatorCreatorCreatorCreatorCreator2";
    private static final String NP = "https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV";
    private static final String ME = "https://orcid.org/0000-0000-0000-0001";

    private final Map<String, Nanopub> templates = new HashMap<>();

    private TemplateComplianceChecker checker() {
        return new TemplateComplianceChecker(templates::get);
    }

    private TemplateBuilder template(String uri, IRI type) throws Exception {
        return new TemplateBuilder(uri, type);
    }

    private void register(TemplateBuilder builder) throws Exception {
        templates.put(builder.npUri, builder.buildNanopub());
    }

    // The templates most tests use: "<thing> rdfs:label ?name" for the assertion,
    // "nt:ASSERTION prov:wasAttributedTo nt:CREATOR" for the provenance, and
    // "nt:NANOPUB dct:creator nt:CREATOR" for the publication info.
    private TemplateBuilder standardTemplates() throws Exception {
        TemplateBuilder provenance = template(PROVENANCE_TEMPLATE, NTEMPLATE.PROVENANCE_TEMPLATE);
        provenance.statement(NTEMPLATE.ASSERTION_PLACEHOLDER, PROV.WAS_ATTRIBUTED_TO, NTEMPLATE.CREATOR_PLACEHOLDER);
        register(provenance);
        TemplateBuilder creator = template(CREATOR_TEMPLATE, NTEMPLATE.PUBINFO_TEMPLATE);
        creator.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, DCTERMS.CREATOR, NTEMPLATE.CREATOR_PLACEHOLDER);
        register(creator);
        TemplateBuilder assertion = template(ASSERTION_TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE);
        assertion.statement(assertion.declare("thing", NTEMPLATE.URI_PLACEHOLDER), RDFS.LABEL, assertion.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER));
        return assertion;
    }

    // A nanopublication with the given content of its graphs, in Turtle.
    private static Nanopub nanopub(String assertion, String provenance, String pubinfo) throws Exception {
        String trig = """
                @prefix this: <%1$s> .
                @prefix sub: <%1$s/> .
                @prefix np: <http://www.nanopub.org/nschema#> .
                @prefix dct: <http://purl.org/dc/terms/> .
                @prefix prov: <http://www.w3.org/ns/prov#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                @prefix foaf: <http://xmlns.com/foaf/0.1/> .
                @prefix npx: <http://purl.org/nanopub/x/> .
                @prefix nt: <https://w3id.org/np/o/ntemplate/> .
                @prefix gen: <https://w3id.org/kpxl/gen/terms/> .
                @prefix ex: <http://example.com/> .
                @prefix me: <%2$s> .
                sub:Head { this: a np:Nanopublication ; np:hasAssertion sub:assertion ;
                    np:hasProvenance sub:provenance ; np:hasPublicationInfo sub:pubinfo . }
                sub:assertion { %3$s }
                sub:provenance { %4$s }
                sub:pubinfo { %5$s }
                """.formatted(NP, ME, assertion, provenance, pubinfo);
        return new NanopubImpl(trig, RDFFormat.TRIG);
    }

    private static final String ATTRIBUTED = "sub:assertion prov:wasAttributedTo me: .";

    // The publication info most tests use: what tools write, and the links to the templates.
    private static String pubinfo(String more) {
        return """
                this: dct:created "2026-11-19T10:00:00Z"^^xsd:dateTime ; dct:creator me: ; rdfs:label "A thing" ;
                    npx:hasNanopubType ex:Thing ; npx:wasCreatedAt <https://example.com/tool> ;
                    nt:wasCreatedFromTemplate <%s> ; nt:wasCreatedFromProvenanceTemplate <%s> ; nt:wasCreatedFromPubinfoTemplate <%s> .
                me: foaf:name "Me" .
                sub:sig npx:hasAlgorithm "RSA" ; npx:hasPublicKey "key" ; npx:hasSignature "signature" ; npx:hasSignatureTarget this: ; npx:signedBy me: .
                """.formatted(ASSERTION_TEMPLATE, PROVENANCE_TEMPLATE, CREATOR_TEMPLATE) + more;
    }

    @Test
    void nanopublicationFromItsTemplatesComplies() throws Exception {
        register(standardTemplates());
        ComplianceReport report = checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo("")));
        // What tools write into the publication info without a template is not reported:
        assertEquals(List.of(), report.getIssues());
        assertTrue(report.isCompliant());
    }

    @Test
    void nanopublicationWithoutTemplatesIsNotChecked() throws Exception {
        ComplianceReport report = checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, "this: dct:creator me: ."));
        assertTrue(report.isCompliant());
        assertEquals(List.of(Kind.NO_TEMPLATE, Kind.NO_TEMPLATE, Kind.NO_TEMPLATE), warnings(report));
        assertEquals(List.of(Graph.ASSERTION, Graph.PROVENANCE, Graph.PUBINFO), report.getWarnings().stream().map(ComplianceIssue::graph).toList());
        assertNull(report.getAssertionMatch());
    }

    @Test
    void templateForAnotherGraphIsAnError() throws Exception {
        register(standardTemplates());
        // The assertion template is named for the provenance as well:
        String pubinfo = pubinfo("").replace("nt:wasCreatedFromProvenanceTemplate <" + PROVENANCE_TEMPLATE + ">",
                "nt:wasCreatedFromProvenanceTemplate <" + ASSERTION_TEMPLATE + ">");

        ComplianceReport report = checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo));
        assertEquals(List.of(Kind.WRONG_TEMPLATE_KIND), errors(report));
        assertEquals(Graph.PROVENANCE, report.getErrors().get(0).graph());
        assertEquals("<" + ASSERTION_TEMPLATE + "> is named as provenance template but is a template for the assertion", report.getErrors().get(0).message());
        assertNull(report.getProvenanceMatch());
    }

    @Test
    void onlyOneAssertionTemplateIsUsed() throws Exception {
        register(standardTemplates());
        String pubinfo = pubinfo("this: nt:wasCreatedFromTemplate <https://w3id.org/np/RAZzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz> .");

        ComplianceReport report = checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo));
        // The first by IRI is checked against, which is the one that can be retrieved here:
        assertTrue(report.isCompliant());
        assertEquals(List.of(Kind.SEVERAL_TEMPLATES), warnings(report));
        assertEquals(2, TemplateComplianceChecker.getTemplateLinks(report.getNanopub(), Graph.ASSERTION).size());
    }

    @Test
    void missingTypeIsAnError() throws Exception {
        TemplateBuilder assertion = standardTemplates();
        assertion.add(assertion.templateNode, NTEMPLATE.HAS_TARGET_NANOPUB_TYPE, iri("http://example.com/Thing"));
        assertion.add(assertion.templateNode, NTEMPLATE.HAS_TARGET_NANOPUB_TYPE, iri("http://example.com/Other"));
        register(assertion);

        ComplianceReport report = checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo("")));
        assertEquals(List.of(Kind.MISSING_TYPE), errors(report));
        assertTrue(report.getErrors().get(0).message().contains("<http://example.com/Other>"));
    }

    @Test
    void requiredPubinfoElementIsLookedForByVersion() throws Exception {
        IRI creatorKind = iri("https://w3id.org/np/RACreatorCreatorCreatorCreatorCreatorCreator0/creator-template");
        // A later version of the template for the creator, which says what it is a version of:
        TemplateBuilder later = template(CREATOR_TEMPLATE_2, NTEMPLATE.PUBINFO_TEMPLATE);
        later.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, DCTERMS.CREATOR, NTEMPLATE.CREATOR_PLACEHOLDER);
        later.add(later.templateNode, DCTERMS.IS_VERSION_OF, creatorKind);
        register(later);
        IRI unrelated = iri("https://w3id.org/np/RAUnrelatedUnrelatedUnrelatedUnrelatedUnrel");

        TemplateBuilder assertion = standardTemplates();
        assertion.add(assertion.templateNode, NTEMPLATE.HAS_REQUIRED_PUBINFO_ELEMENT, iri(CREATOR_TEMPLATE));
        register(assertion);
        // The required template is the one named:
        assertEquals(List.of(), checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo(""))).getIssues());

        // Another version of it is named, and both say what they are versions of:
        TemplateBuilder first = template(CREATOR_TEMPLATE, NTEMPLATE.PUBINFO_TEMPLATE);
        first.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, DCTERMS.CREATOR, NTEMPLATE.CREATOR_PLACEHOLDER);
        first.add(first.templateNode, DCTERMS.IS_VERSION_OF, creatorKind);
        register(first);
        String laterNamed = pubinfo("").replace("nt:wasCreatedFromPubinfoTemplate <" + CREATOR_TEMPLATE + ">", "nt:wasCreatedFromPubinfoTemplate <" + CREATOR_TEMPLATE_2 + ">");
        assertEquals(List.of(), checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, laterNamed)).getIssues());

        // A required template that is not named in any version can only be warned about:
        TemplateBuilder other = standardTemplates();
        other.add(other.templateNode, NTEMPLATE.HAS_REQUIRED_PUBINFO_ELEMENT, unrelated);
        register(other);
        ComplianceReport report = checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo("")));
        assertTrue(report.isCompliant());
        assertEquals(List.of(Kind.MISSING_PUBINFO_ELEMENT), warnings(report));
        assertEquals(unrelated, report.getWarnings().get(0).template());
    }

    @Test
    void introducedResourceHasToBeDeclared() throws Exception {
        TemplateBuilder assertion = standardTemplates();
        assertion.add(assertion.iri("thing"), RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        register(assertion);

        ComplianceReport undeclared = checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo("")));
        assertTrue(undeclared.isCompliant());
        assertEquals(List.of(Kind.UNDECLARED_INTRODUCED_RESOURCE), warnings(undeclared));
        assertEquals(List.of(), checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo("this: npx:introduces ex:thing ."))).getIssues());
    }

    @Test
    void pinnedRolePredicateHasToBeStated() throws Exception {
        TemplateBuilder provenance = template(PROVENANCE_TEMPLATE, NTEMPLATE.PROVENANCE_TEMPLATE);
        provenance.statement(NTEMPLATE.ASSERTION_PLACEHOLDER, PROV.WAS_ATTRIBUTED_TO, NTEMPLATE.CREATOR_PLACEHOLDER);
        register(provenance);
        TemplateBuilder creator = template(CREATOR_TEMPLATE, NTEMPLATE.PUBINFO_TEMPLATE);
        creator.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, DCTERMS.CREATOR, NTEMPLATE.CREATOR_PLACEHOLDER);
        register(creator);
        TemplateBuilder assertion = template(ASSERTION_TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE);
        IRI hasOrganizer = iri("http://example.com/hasOrganizer");
        assertion.add(hasOrganizer, RDF.TYPE, KPXL.INVERSE_ROLE_PROPERTY);
        assertion.statement(assertion.declare("space", NTEMPLATE.URI_PLACEHOLDER), hasOrganizer, assertion.declare("agent", NTEMPLATE.URI_PLACEHOLDER));
        register(assertion);
        String grant = "ex:space ex:hasOrganizer me: .";

        ComplianceReport unstated = checker().check(nanopub(grant, ATTRIBUTED, pubinfo("")));
        assertTrue(unstated.isCompliant());
        assertEquals(List.of(Kind.MISSING_ROLE_PIN, Kind.MISSING_ROLE_PIN), warnings(unstated));
        String pins = "ex:hasOrganizer a gen:InverseRoleProperty . this: npx:hasNanopubType gen:RoleInstantiation .";
        assertEquals(List.of(), checker().check(nanopub(grant, ATTRIBUTED, pubinfo(pins))).getIssues());
    }

    @Test
    void twoPubinfoTemplatesCanGiveTheSameTriple() throws Exception {
        register(standardTemplates());
        // Two versions of the template for the creator, as a nanopublication names them that
        // was made from an earlier one: both give the one triple for the creator.
        TemplateBuilder later = template(CREATOR_TEMPLATE_2, NTEMPLATE.PUBINFO_TEMPLATE);
        later.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, DCTERMS.CREATOR, NTEMPLATE.CREATOR_PLACEHOLDER);
        register(later);
        String pubinfo = pubinfo("this: nt:wasCreatedFromPubinfoTemplate <" + CREATOR_TEMPLATE_2 + "> .");

        ComplianceReport report = checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo));
        assertEquals(List.of(), report.getIssues());
        assertEquals(2, report.getPubinfoMatches().size());
        assertTrue(report.getPubinfoMatches().stream().allMatch(m -> m.getMatches().size() == 1));
    }

    @Test
    void pubinfoTemplateWithoutItsTripleIsAnError() throws Exception {
        register(standardTemplates());
        TemplateBuilder supersedes = template(CREATOR_TEMPLATE_2, NTEMPLATE.PUBINFO_TEMPLATE);
        supersedes.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, NPX.SUPERSEDES, supersedes.declare("np", NTEMPLATE.URI_PLACEHOLDER));
        register(supersedes);
        String links = "this: nt:wasCreatedFromPubinfoTemplate <" + CREATOR_TEMPLATE_2 + "> .";

        ComplianceReport without = checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo(links)));
        assertEquals(List.of(Kind.MISSING_STATEMENT), errors(without));
        assertEquals(Graph.PUBINFO, without.getErrors().get(0).graph());
        // The triple is one that tools write as well, and it is still the template's to take:
        String superseding = links + " this: npx:supersedes <https://w3id.org/np/RAEarlierEarlierEarlierEarlierEarlierEarlier> .";
        assertEquals(List.of(), checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo(superseding))).getIssues());
    }

    @Test
    void errorInTheTemplateIsAWarning() throws Exception {
        TemplateBuilder assertion = standardTemplates();
        // A statement with a literal as its subject, which no triple can match:
        IRI broken = assertion.iri("broken");
        assertion.add(assertion.templateNode, NTEMPLATE.HAS_STATEMENT, broken).add(broken, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT)
                .add(broken, RDF.SUBJECT, vf.createLiteral("not a subject")).add(broken, RDF.PREDICATE, RDFS.COMMENT)
                .add(broken, RDF.OBJECT, vf.createLiteral("x"));
        register(assertion);

        ComplianceReport report = checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo("")));
        assertTrue(report.isCompliant());
        assertEquals(List.of(Kind.TEMPLATE_ERROR), warnings(report));
    }

    @Test
    void reservedNameIsAWarningGivenOnce() throws Exception {
        // A template in the old shape has its own assertion graph as its node, and one
        // published as hand-coded statements has it as the subject of several triples.
        standardTemplates();
        TemplateBuilder assertion = template(ASSERTION_TEMPLATE, NTEMPLATE.ASSERTION_TEMPLATE);
        assertion.statement(assertion.declare("subj", NTEMPLATE.URI_PLACEHOLDER), assertion.declare("pred", NTEMPLATE.URI_PLACEHOLDER),
                assertion.declare("obj", NTEMPLATE.VALUE_PLACEHOLDER), NTEMPLATE.REPEATABLE_STATEMENT);
        register(assertion);

        ComplianceReport report = checker().check(nanopub("sub:assertion a nt:AssertionTemplate ; rdfs:label \"A template\" ; nt:hasStatement sub:st1 .",
                ATTRIBUTED, pubinfo("")));
        assertTrue(report.isCompliant());
        assertEquals(List.of(Kind.RESERVED_NAME), warnings(report));
        assertEquals("'assertion' is what the nanopublication calls one of its own parts, so the value of 'subj' is that part"
                + " rather than a resource of its own (in 3 places)", report.getWarnings().get(0).message());
    }

    @Test
    void valueOfAnotherDatatypeIsAnError() throws Exception {
        TemplateBuilder assertion = standardTemplates();
        assertion.add(assertion.iri("name"), NTEMPLATE.HAS_DATATYPE, XSD.TOKEN);
        register(assertion);

        ComplianceReport report = checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", ATTRIBUTED, pubinfo("")));
        assertEquals(List.of(Kind.VIOLATION), errors(report));
        assertEquals("The value of 'name' is of type xsd:string instead of xsd:token", report.getErrors().get(0).message());
    }

    @Test
    void leftoverTripleSaysWhyItDoesNotFit() throws Exception {
        TemplateBuilder assertion = standardTemplates();
        assertion.add(assertion.iri("name"), NTEMPLATE.HAS_REGEX, "[A-Z].*");
        register(assertion);

        // A value that does not match the pattern of its placeholder:
        ComplianceReport pattern = checker().check(nanopub("ex:thing rdfs:label \"a thing\" .", ATTRIBUTED, pubinfo("")));
        assertEquals(List.of(Kind.MISSING_STATEMENT, Kind.UNMATCHED_TRIPLE), errors(pattern));
        assertEquals("The assertion triple <http://example.com/thing> <http://www.w3.org/2000/01/rdf-schema#label> \"a thing\" matches no statement"
                + " of the assertion template; it does not fit the statement st1 because: Value 'a thing' of 'name' doesn't match the pattern '[A-Z].*'",
                pattern.getErrors().get(1).message());

        // A second triple for a statement that takes one:
        ComplianceReport second = checker().check(nanopub("ex:thing rdfs:label \"A thing\", \"Another\" .", ATTRIBUTED, pubinfo("")));
        assertEquals(List.of(Kind.UNMATCHED_TRIPLE), errors(second));
        assertTrue(second.getErrors().get(0).message().endsWith("it does not fit the statement st1 because: 'name' already has the value"
                + " \"A thing\", which differs from \"Another\""), second.getErrors().get(0).message());

        // A triple with a predicate the template does not have gets no such explanation:
        ComplianceReport foreign = checker().check(nanopub("ex:thing rdfs:label \"A thing\" ; rdfs:comment \"x\" .", ATTRIBUTED, pubinfo("")));
        assertTrue(foreign.getErrors().get(0).message().endsWith("matches no statement of the assertion template"), foreign.getErrors().get(0).message());
    }

    @Test
    void creatorIsACreatorOfTheNanopublication() throws Exception {
        register(standardTemplates());
        // Attributed to somebody who is not a creator of the nanopublication:
        ComplianceReport report = checker().check(nanopub("ex:thing rdfs:label \"A thing\" .", "sub:assertion prov:wasAttributedTo ex:somebody .", pubinfo("")));
        assertEquals(List.of(Kind.MISSING_STATEMENT, Kind.UNMATCHED_TRIPLE), errors(report));
        assertTrue(report.getErrors().stream().allMatch(issue -> issue.graph() == Graph.PROVENANCE));
    }

    @Test
    void nullsAreRejected() {
        assertThrows(NullPointerException.class, () -> new TemplateComplianceChecker(null));
        assertThrows(NullPointerException.class, () -> new TemplateComplianceChecker().check(null));
    }

}
