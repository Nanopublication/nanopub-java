package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SourceReader}.
 */
class SourceReaderTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String SOURCE = "https://w3id.org/np/RASourceSourceSourceSourceSourceSourceSource0";
    private static final String ME = "https://orcid.org/0000-0000-0000-0001";
    private static final String OTHER = "https://orcid.org/0000-0000-0000-0002";
    private static final IRI THING = vf.createIRI("http://example.com/thing");
    private static final IRI THING_TYPE = vf.createIRI("http://example.com/Thing");
    private static final IRI EXTRA_TYPE = vf.createIRI("http://example.com/Extra");
    private static final IRI REVIEWED_BY = vf.createIRI("http://example.com/reviewedBy");
    private static final IRI CC0 = vf.createIRI("https://creativecommons.org/publicdomain/zero/1.0/");

    private static final Map<String, Nanopub> templates = new HashMap<>();
    private static Template assertion;
    private static Template newerAssertion;
    private static Template provenance;
    private static Template creator;
    private static Template license;
    private static Template otherLicense;
    private static Template derived;
    private static Template reviewers;
    private static Template names;
    private static Template byHand;
    private static Template label;

    private static String id(String name) {
        String code = "RA" + name;
        while (code.length() < 45) code += "0";
        return "https://w3id.org/np/" + code;
    }

    private static Template register(TemplateBuilder builder) throws Exception {
        Nanopub np = builder.buildNanopub();
        templates.put(builder.npUri, np);
        return new Template(np, NanopubResolver.NONE);
    }

    @BeforeAll
    static void makeTemplates() throws Exception {
        TemplateBuilder a = new TemplateBuilder(id("Assertion"), NTEMPLATE.ASSERTION_TEMPLATE);
        a.add(a.templateNode, NTEMPLATE.HAS_TARGET_NANOPUB_TYPE, THING_TYPE);
        a.statement(THING, RDFS.LABEL, a.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER));
        a.statement(THING, RDFS.COMMENT, a.declare("comment", NTEMPLATE.LITERAL_PLACEHOLDER), NTEMPLATE.OPTIONAL_STATEMENT);
        assertion = register(a);

        // A newer version without the comment, but with a topic.
        TemplateBuilder a2 = new TemplateBuilder(id("NewerAssertion"), NTEMPLATE.ASSERTION_TEMPLATE);
        a2.statement(THING, RDFS.LABEL, a2.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER));
        a2.statement(THING, DCTERMS.SUBJECT, a2.declare("topic", NTEMPLATE.URI_PLACEHOLDER), NTEMPLATE.OPTIONAL_STATEMENT);
        newerAssertion = register(a2);

        TemplateBuilder pr = new TemplateBuilder(id("Provenance"), NTEMPLATE.PROVENANCE_TEMPLATE);
        pr.statement(NTEMPLATE.ASSERTION_PLACEHOLDER, PROV.WAS_ATTRIBUTED_TO, NTEMPLATE.CREATOR_PLACEHOLDER);
        provenance = register(pr);

        TemplateBuilder c = new TemplateBuilder(id("Creator"), NTEMPLATE.PUBINFO_TEMPLATE);
        c.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, DCTERMS.CREATOR, NTEMPLATE.CREATOR_PLACEHOLDER);
        creator = register(c);

        TemplateBuilder l = new TemplateBuilder(id("License"), NTEMPLATE.PUBINFO_TEMPLATE);
        l.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, DCTERMS.LICENSE, l.declare("license", NTEMPLATE.EXTERNAL_URI_PLACEHOLDER));
        license = register(l);

        TemplateBuilder l2 = new TemplateBuilder(id("OtherLicense"), NTEMPLATE.PUBINFO_TEMPLATE);
        l2.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, DCTERMS.LICENSE, l2.declare("rights", NTEMPLATE.EXTERNAL_URI_PLACEHOLDER));
        otherLicense = register(l2);

        TemplateBuilder d = new TemplateBuilder(id("Derived"), NTEMPLATE.PUBINFO_TEMPLATE);
        d.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, PROV.WAS_DERIVED_FROM, d.declare("np", NTEMPLATE.URI_PLACEHOLDER));
        derived = register(d);

        // Transient, and with a statement that is not repeatable.
        TemplateBuilder r = new TemplateBuilder(id("Reviewers"), NTEMPLATE.PUBINFO_TEMPLATE);
        r.add(r.templateNode, RDF.TYPE, NTEMPLATE.TRANSIENT_TEMPLATE);
        r.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, REVIEWED_BY, r.declare("reviewer", NTEMPLATE.URI_PLACEHOLDER));
        reviewers = register(r);

        TemplateBuilder n = new TemplateBuilder(id("Names"), NTEMPLATE.PUBINFO_TEMPLATE);
        n.statement(NTEMPLATE.CREATOR_PLACEHOLDER, FOAF.NAME, n.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER));
        names = register(n);

        TemplateBuilder h = new TemplateBuilder(id("ByHand"), NTEMPLATE.PUBINFO_TEMPLATE);
        h.statement(h.declare("subj", NTEMPLATE.URI_PLACEHOLDER), h.declare("pred", NTEMPLATE.URI_PLACEHOLDER), h.declare("obj", NTEMPLATE.VALUE_PLACEHOLDER),
                NTEMPLATE.REPEATABLE_STATEMENT, NTEMPLATE.OPTIONAL_STATEMENT);
        byHand = register(h);

        TemplateBuilder lb = new TemplateBuilder(id("Label"), NTEMPLATE.PUBINFO_TEMPLATE);
        lb.statement(NTEMPLATE.NANOPUB_PLACEHOLDER, RDFS.LABEL, lb.declare("label", NTEMPLATE.LITERAL_PLACEHOLDER), NTEMPLATE.OPTIONAL_STATEMENT);
        label = register(lb);
    }

    private static Nanopub source(String... pubinfoTemplates) throws Exception {
        StringBuilder links = new StringBuilder();
        for (String t : pubinfoTemplates) links.append(" nt:wasCreatedFromPubinfoTemplate <").append(t).append("> ;");
        return new NanopubImpl("""
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
                @prefix ex: <http://example.com/> .
                sub:Head { this: a np:Nanopublication ; np:hasAssertion sub:assertion ;
                    np:hasProvenance sub:provenance ; np:hasPublicationInfo sub:pubinfo . }
                sub:assertion { ex:thing rdfs:label "A thing" ; rdfs:comment "Round" . }
                sub:provenance { sub:assertion prov:wasAttributedTo <%2$s> . }
                sub:pubinfo {
                    this: dct:created "2026-11-27T10:00:00Z"^^xsd:dateTime ; dct:creator <%2$s> , <%3$s> ; rdfs:label "A thing" ;
                        dct:license <https://creativecommons.org/publicdomain/zero/1.0/> ;
                        prov:wasDerivedFrom <https://example.com/np/one> , <https://example.com/np/two> ;
                        ex:reviewedBy <https://example.com/reviewer/1> , <https://example.com/reviewer/2> ;
                        npx:hasNanopubType ex:Thing , ex:Extra ;
                        rdfs:seeAlso ex:more ;
                        nt:wasCreatedFromTemplate <%4$s> ; nt:wasCreatedFromProvenanceTemplate <%5$s> ;%6$s
                        npx:introduces ex:thing .
                    <%2$s> foaf:name "Me" .
                    <%3$s> foaf:name "Other" .
                    ex:topic nt:hasLabelFromApi "A topic" .
                    sub:sig npx:hasAlgorithm "RSA" ; npx:hasPublicKey "key" ; npx:hasSignature "signature" ; npx:hasSignatureTarget this: ; npx:signedBy <%2$s> .
                }
                """.formatted(SOURCE, ME, OTHER, id("Assertion"), id("Provenance"), links), RDFFormat.TRIG);
    }

    private static Set<String> describe(Collection<Statement> triples) {
        Set<String> result = new TreeSet<>();
        for (Statement st : triples) {
            String s = st.getSubject().stringValue().equals(SOURCE) ? "this:" : st.getSubject().stringValue();
            result.add(s + " " + st.getPredicate().getLocalName() + " " + st.getObject().stringValue());
        }
        return result;
    }

    private static Set<String> taken(TemplateMatch match) {
        List<Statement> triples = new ArrayList<>();
        for (TemplateMatch.StatementMatch m : match.getMatches()) triples.addAll(m.triples().values());
        return describe(triples);
    }

    @Test
    void assertionIsReadForAnotherVersionOfItsTemplate() throws Exception {
        SourceReader reader = new SourceReader(source());
        TemplateMatch match = reader.readAssertion(newerAssertion);

        assertEquals(Map.of("name", "A thing"), reader.getValues(match).toParams());
        assertEquals(Set.of("http://example.com/thing comment Round"), describe(match.getUnusedTriples()));
    }

    @Test
    void whatAFillWritesByItselfIsNotRead() throws Exception {
        SourceReader reader = new SourceReader(source());
        Set<String> before = describe(reader.getUnreadPubinfo());
        // Neither the creation time, the label, the template links, what is introduced, the
        // label from a lookup and the signature, nor the names of the creators:
        assertEquals(new TreeSet<>(Set.of(
                "this: creator " + ME, "this: creator " + OTHER, "this: license " + CC0,
                "this: wasDerivedFrom https://example.com/np/one", "this: wasDerivedFrom https://example.com/np/two",
                "this: reviewedBy https://example.com/reviewer/1", "this: reviewedBy https://example.com/reviewer/2",
                "this: hasNanopubType http://example.com/Thing", "this: hasNanopubType http://example.com/Extra",
                "this: seeAlso http://example.com/more")), before);

        // The type the assertion template gives is written by the fill too.
        reader.readAssertion(assertion);
        assertFalse(describe(reader.getUnreadPubinfo()).contains("this: hasNanopubType http://example.com/Thing"));
        assertTrue(describe(reader.getUnreadPubinfo()).contains("this: hasNanopubType http://example.com/Extra"));
    }

    @Test
    void templatesCanGiveTheSameTriple() throws Exception {
        // As two versions of the template for the license do:
        SourceReader reader = new SourceReader(source());
        TemplateMatch first = reader.readPubinfo(license);
        TemplateMatch second = reader.readPubinfo(otherLicense);

        assertEquals(Map.of("license", CC0.stringValue()), reader.getValues(first).toParams());
        assertEquals(Map.of("rights", CC0.stringValue()), reader.getValues(second).toParams());
        assertFalse(describe(reader.getUnreadPubinfo()).contains("this: license " + CC0));
        // But not what a template about the source alone has taken.
        SourceReader skipping = new SourceReader(source());
        skipping.skipPubinfo(license);
        assertTrue(skipping.readPubinfo(otherLicense).getMatches().isEmpty());
    }

    @Test
    void whatIsAboutTheSourceAloneIsTakenWithoutValues() throws Exception {
        SourceReader reader = new SourceReader(source());
        // A link to what the source was derived from, twice over:
        assertTrue(reader.getValues(reader.readPubinfo(derived)).isEmpty());
        // A transient template, whose statement is taken as often as it occurs:
        TemplateMatch match = reader.readPubinfo(reviewers);
        assertFalse(match.getMatches().isEmpty());
        assertTrue(reader.getValues(match).isEmpty());

        Set<String> unread = describe(reader.getUnreadPubinfo());
        assertTrue(unread.stream().noneMatch(s -> s.contains("wasDerivedFrom") || s.contains("reviewedBy")), unread.toString());

        // A template can also be skipped on purpose.
        assertEquals(Set.of("this: license " + CC0), describe(new SourceReader(source()).skipPubinfo(license)));
    }

    @Test
    void templatesAreReadInTheOrderThatGivesEachItsShare() throws Exception {
        SourceReader reader = new SourceReader(source(), vf.createIRI(ME));
        reader.readAssertion(assertion);
        Map<Template, TemplateMatch> matches = reader.readPubinfo(List.of(byHand, license, derived, creator));

        assertEquals(List.of(byHand, license, derived, creator), List.copyOf(matches.keySet()));
        assertEquals(Set.of("this: license " + CC0), taken(matches.get(license)));
        assertEquals(Set.of("this: creator " + ME), taken(matches.get(creator)));
        // What the other templates leave, without the names of the creators and what is
        // about the source alone:
        assertEquals(Set.of("this: creator " + OTHER, "this: hasNanopubType http://example.com/Extra", "this: seeAlso http://example.com/more",
                "this: reviewedBy https://example.com/reviewer/1", "this: reviewedBy https://example.com/reviewer/2"), taken(matches.get(byHand)));
        assertTrue(reader.getUnreadPubinfo().isEmpty());
    }

    @Test
    void creatorPlaceholderStandsForTheCreatorGiven() throws Exception {
        Nanopub source = source();
        assertTrue(new SourceReader(source, vf.createIRI(ME)).readProvenance(provenance).isComplete());

        // Someone else who makes a new version is not the one the source names.
        SourceReader reader = new SourceReader(source, vf.createIRI("https://orcid.org/0000-0000-0000-0009"));
        TemplateMatch match = reader.readProvenance(provenance);
        assertEquals(1, match.getUnusedTriples().size());
        assertTrue(reader.readPubinfo(creator).getMatches().isEmpty());

        // Without a creator, the placeholder stands for any creator of the source.
        assertEquals(1, new SourceReader(source).readPubinfo(creator).getMatches().size());
    }

    @Test
    void whatIsSkippedIsTakenForAnyCreator() throws Exception {
        // As when a later version of the template for the creator is transient: what the
        // source says of all its creators is about the source alone.
        SourceReader reader = new SourceReader(source(), vf.createIRI(ME));
        assertEquals(Set.of("this: creator " + ME, "this: creator " + OTHER), describe(reader.skipPubinfo(creator)));
    }

    @Test
    void resourceKeptFromAnEarlierVersionIsGivenByItsName() throws Exception {
        String earlier = id("Earlier");
        TemplateBuilder b = new TemplateBuilder(id("Introducing"), NTEMPLATE.ASSERTION_TEMPLATE);
        IRI thing = b.declare("thing", NTEMPLATE.LOCAL_RESOURCE, NTEMPLATE.URI_PLACEHOLDER, NTEMPLATE.INTRODUCED_RESOURCE);
        b.statement(thing, RDFS.LABEL, b.declare("name", NTEMPLATE.LITERAL_PLACEHOLDER));
        Template introducing = register(b);
        // A version that kept the IRI the one before it gave the thing:
        Nanopub source = new NanopubImpl("""
                @prefix this: <%1$s> .
                @prefix sub: <%1$s/> .
                @prefix np: <http://www.nanopub.org/nschema#> .
                @prefix dct: <http://purl.org/dc/terms/> .
                @prefix prov: <http://www.w3.org/ns/prov#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix npx: <http://purl.org/nanopub/x/> .
                @prefix nt: <https://w3id.org/np/o/ntemplate/> .
                sub:Head { this: a np:Nanopublication ; np:hasAssertion sub:assertion ;
                    np:hasProvenance sub:provenance ; np:hasPublicationInfo sub:pubinfo . }
                sub:assertion { <%2$s/thing> rdfs:label "A thing" . }
                sub:provenance { sub:assertion prov:wasAttributedTo <%3$s> . }
                sub:pubinfo { this: dct:creator <%3$s> ; npx:introduces <%2$s/thing> ; nt:wasCreatedFromTemplate <%4$s> . }
                """.formatted(SOURCE, earlier, ME, introducing.getId()), RDFFormat.TRIG);

        SourceReader reader = new SourceReader(source, vf.createIRI(ME));
        TemplateValues values = reader.getValues(reader.readAssertion(introducing));
        assertEquals(Map.of("thing", "thing", "name", "A thing"), values.toParams());

        // A new version keeps the IRI, and what is derived mints its own.
        NanopubResolver resolver = id -> id.equals(SOURCE) ? source : templates.containsKey(id) ? templates.get(id) : TemplateComplianceCheckerTest.RESOURCES.getNanopub(id);
        for (FillRequest.Mode mode : FillRequest.Mode.values()) {
            FillRequest request = new FillRequest(introducing.getId()).setCreator(vf.createIRI(ME)).setSource(mode, SOURCE);
            for (String name : values.getNames()) request.getValues().set(name, values.get(name));
            String subject = new TemplateFiller(resolver).fill(request).getAssertion().iterator().next().getSubject().stringValue();
            if (mode == FillRequest.Mode.DERIVE) {
                assertNotEquals(earlier + "/thing", subject);
                assertTrue(subject.endsWith("/thing"), subject);
            } else {
                assertEquals(earlier + "/thing", subject, mode.toString());
            }
        }
    }

    @Test
    void aTemplateCanTakeWhatAFillWritesByItself() throws Exception {
        SourceReader reader = new SourceReader(source());
        // The label of the nanopublication, which a template for it gives instead:
        assertEquals(Map.of("label", "A thing"), reader.getValues(reader.readPubinfo(label)).toParams());
    }

    @Test
    void aTemplateCanTakeTheNameOfACreator() throws Exception {
        SourceReader reader = new SourceReader(source(), vf.createIRI(OTHER));
        assertEquals(Map.of("name", "Other"), reader.getValues(reader.readPubinfo(names)).toParams());
    }

    @Test
    void labelsFromLookupsAreRead() throws Exception {
        assertEquals(Map.of("http://example.com/topic", "A topic"), new SourceReader(source()).getLabelsFromApi());
    }

    @Test
    void templateForAnotherGraphIsRefused() throws Exception {
        SourceReader reader = new SourceReader(source());
        assertThrows(IllegalArgumentException.class, () -> reader.readAssertion(provenance));
        assertThrows(IllegalArgumentException.class, () -> reader.readProvenance(license));
        assertThrows(IllegalArgumentException.class, () -> reader.readPubinfo(assertion));
        assertThrows(IllegalArgumentException.class, () -> reader.readPubinfo(List.of(license, assertion)));
    }

    @Test
    void requestReadBackHasNothingOfWhatIsAboutTheSourceAlone() throws Exception {
        Nanopub source = source(id("Creator"), id("License"), id("Derived"), id("Reviewers"), id("ByHand"));
        ComplianceReport report = new TemplateComplianceChecker(templates::get).check(source);
        FillRequest request = FillRequest.of(report);

        // In the order the source names them, which is by IRI, without the link to what it was
        // derived from, and with the transient template, but without its values:
        assertEquals(List.of(id("ByHand"), id("Creator"), id("License"), id("Reviewers")), request.getPubinfoTemplates());
        assertTrue(request.getPubinfoValues(id("Reviewers")).isEmpty());
        assertEquals(ME, request.getCreator().stringValue());
        assertEquals("Me", request.getCreatorName());
        assertEquals(Map.of("http://example.com/topic", "A topic"), request.getLabelsFromApi());
        // By hand: the other creator, the type the assertion template does not give, and the
        // link to more, but not the reviewers of the transient template.
        Map<String, String> byHandValues = request.getPubinfoValues(id("ByHand")).toParams();
        assertEquals(3, byHandValues.keySet().stream().filter(k -> k.startsWith("pred")).count(), byHandValues.toString());
        assertFalse(byHandValues.containsValue("https://example.com/reviewer/1"));
        assertFalse(byHandValues.containsValue(THING_TYPE.stringValue()));
        assertTrue(byHandValues.containsValue(OTHER));
    }

}
