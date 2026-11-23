package org.nanopub.extra.checks;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.checks.NetworkFinding.Kind;
import org.nanopub.extra.checks.NetworkFinding.Severity;
import org.nanopub.templates.NanopubResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link NetworkChecker}.
 */
class NetworkCheckerTest {

    private static final String ME = "https://orcid.org/0000-0000-0000-0001";
    private static final String SPACE = "https://w3id.org/spaces/example";

    private static final String V1 = uri("First");
    private static final String V2 = uri("Second");
    private static final String DRAFT = "http://purl.org/nanopub/temp/draft";

    // The URI of a published nanopublication: one with the shape of a trusty URI.
    private static String uri(String name) {
        return "https://w3id.org/np/RA" + (name + "x".repeat(43)).substring(0, 43);
    }

    private final Map<String, Nanopub> network = new HashMap<>();
    private final Map<String, Set<String>> maintained = new HashMap<>();
    private boolean servicesDown = false;

    private final NanopubResolver resolver = id -> network.get(id);
    private final MaintainerLookup maintainers = resource -> {
        if (servicesDown) throw new IllegalStateException("no service answered");
        return maintained.getOrDefault(resource, Set.of());
    };

    private List<NetworkFinding> check(Nanopub np) {
        return new NetworkChecker(resolver, maintainers).check(np);
    }

    private static List<Kind> kinds(List<NetworkFinding> findings, Severity severity) {
        return findings.stream().filter(f -> f.severity() == severity).map(NetworkFinding::kind).toList();
    }

    private static void assertFindings(List<NetworkFinding> findings, List<Kind> errors, List<Kind> notes) {
        assertEquals(errors, kinds(findings, Severity.ERROR), findings.toString());
        assertEquals(notes, kinds(findings, Severity.NOTE), findings.toString());
    }

    // A nanopublication with the given content of its assertion and publication info, in
    // Turtle, and signed with the given key if there is one.
    private static Nanopub nanopub(String uri, String assertion, String pubinfo, String key) throws Exception {
        String signature = key == null ? "" : "sub:sig npx:hasAlgorithm \"RSA\" ; npx:hasPublicKey \"" + key + "\" ; npx:hasSignature \"signature\" ;"
                + " npx:hasSignatureTarget this: ; npx:signedBy <" + ME + "> .";
        String trig = """
                @prefix this: <%1$s> .
                @prefix sub: <%1$s/> .
                @prefix np: <http://www.nanopub.org/nschema#> .
                @prefix dct: <http://purl.org/dc/terms/> .
                @prefix prov: <http://www.w3.org/ns/prov#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix npx: <http://purl.org/nanopub/x/> .
                @prefix nt: <https://w3id.org/np/o/ntemplate/> .
                @prefix gen: <https://w3id.org/kpxl/gen/terms/> .
                @prefix grlc: <https://w3id.org/kpxl/grlc/> .
                @prefix ex: <http://example.com/> .
                @prefix v1: <%2$s/> .
                sub:Head { this: a np:Nanopublication ; np:hasAssertion sub:assertion ;
                    np:hasProvenance sub:provenance ; np:hasPublicationInfo sub:pubinfo . }
                sub:assertion { %3$s }
                sub:provenance { sub:assertion prov:wasAttributedTo <%4$s> . }
                sub:pubinfo { this: dct:creator <%4$s> . %5$s %6$s }
                """.formatted(uri, V1, assertion, ME, pubinfo, signature);
        return new NanopubImpl(trig, RDFFormat.TRIG);
    }

    private void publish(Nanopub np) {
        network.put(np.getUri().stringValue(), np);
    }

    // The first version of a thing, which it introduces; signed with key A.
    private void publishFirstVersion() throws Exception {
        publish(nanopub(V1, "sub:thing rdfs:label \"A thing\" .", "this: npx:introduces sub:thing .", "A"));
    }

    private static final String SUPERSEDES_V1 = "this: npx:supersedes <" + V1 + "> .";

    @Test
    void nanopublicationThatIsNoNewVersionHasNoFindings() throws Exception {
        assertFindings(check(nanopub(V1, "sub:thing rdfs:label \"A thing\" .", "this: npx:introduces sub:thing .", "A")), List.of(), List.of());
    }

    // ---------- introduced resources ----------

    @Test
    void newVersionThatKeepsItsResourceHasNoFindings() throws Exception {
        publishFirstVersion();
        Nanopub second = nanopub(V2, "v1:thing rdfs:label \"A thing, improved\" .", SUPERSEDES_V1 + " this: npx:introduces v1:thing .", "A");
        assertFindings(check(second), List.of(), List.of());
    }

    @Test
    void resourceWithANewIriInTheNewVersionIsAnError() throws Exception {
        publishFirstVersion();
        Nanopub second = nanopub(V2, "sub:thing rdfs:label \"A thing, improved\" .", SUPERSEDES_V1 + " this: npx:introduces sub:thing .", "A");
        List<NetworkFinding> findings = check(second);
        assertFindings(findings, List.of(Kind.REMINTED_RESOURCE), List.of());
        assertTrue(findings.get(0).message().contains("introduces <" + V1 + "/thing>, and this version introduces <" + V2 + "/thing> instead"), findings.get(0).message());
    }

    @Test
    void queryWithANewIriIsANote() throws Exception {
        publish(nanopub(V1, "sub:q a grlc:grlc-query .", "this: npx:introduces sub:q .", "A"));
        Nanopub second = nanopub(V2, "sub:q a grlc:grlc-query .", SUPERSEDES_V1 + " this: npx:introduces sub:q .", "A");
        assertFindings(check(second), List.of(), List.of(Kind.REMINTED_QUERY));
    }

    @Test
    void assertionWithANewIriIsANote() throws Exception {
        // A template in the old shape, whose node is the assertion of its nanopublication.
        publish(nanopub(V1, "sub:assertion a nt:AssertionTemplate .", "this: npx:introduces sub:assertion .", "A"));
        Nanopub second = nanopub(V2, "sub:assertion a nt:AssertionTemplate .", SUPERSEDES_V1 + " this: npx:introduces sub:assertion .", "A");
        assertFindings(check(second), List.of(), List.of(Kind.REMINTED_ASSERTION));
    }

    @Test
    void resourceThatIsNotIntroducedAnyMoreIsANote() throws Exception {
        publishFirstVersion();
        Nanopub second = nanopub(V2, "v1:thing rdfs:label \"A thing\" .", SUPERSEDES_V1 + " this: npx:embeds v1:thing .", "A");
        assertFindings(check(second), List.of(), List.of(Kind.DROPPED_RESOURCE));
    }

    @Test
    void versionThatCannotBeRetrievedIsNotChecked() throws Exception {
        Nanopub second = nanopub(V2, "sub:thing rdfs:label \"A thing\" .", SUPERSEDES_V1 + " this: npx:introduces sub:thing .", "A");
        List<NetworkFinding> findings = check(second);
        assertFindings(findings, List.of(), List.of(Kind.UNCHECKED));
        assertTrue(findings.get(0).message().contains("could not be retrieved"));
        // A resolver that fails is the same as one that finds nothing.
        NanopubResolver failing = id -> {
            throw new IllegalStateException("no network");
        };
        assertFindings(new NetworkChecker(failing, maintainers).check(second), List.of(), List.of(Kind.UNCHECKED));
    }

    // ---------- template links ----------

    private static final String ASSERTION_TEMPLATE = uri("AssertionTemplate");
    private static final String PROVENANCE_TEMPLATE = uri("ProvenanceTemplate");
    private static final String PUBINFO_TEMPLATE = uri("PubinfoTemplate");

    private void publishTemplates() throws Exception {
        String statement = "sub:assertion nt:hasStatement sub:st1 . sub:st1 rdf:subject ex:s ; rdf:predicate rdfs:label ; rdf:object sub:name . sub:name a nt:LiteralPlaceholder .";
        String rdf = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . ";
        publish(new NanopubImpl(rdf + trig(ASSERTION_TEMPLATE, "sub:assertion a nt:AssertionTemplate . " + statement), RDFFormat.TRIG));
        publish(new NanopubImpl(rdf + trig(PROVENANCE_TEMPLATE, "sub:assertion a nt:ProvenanceTemplate . " + statement), RDFFormat.TRIG));
        publish(new NanopubImpl(rdf + trig(PUBINFO_TEMPLATE, "sub:assertion a nt:PubinfoTemplate . " + statement), RDFFormat.TRIG));
    }

    private static String trig(String uri, String assertion) {
        return """
                @prefix this: <%1$s> .
                @prefix sub: <%1$s/> .
                @prefix np: <http://www.nanopub.org/nschema#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix nt: <https://w3id.org/np/o/ntemplate/> .
                @prefix ex: <http://example.com/> .
                sub:Head { this: a np:Nanopublication ; np:hasAssertion sub:assertion ;
                    np:hasProvenance sub:provenance ; np:hasPublicationInfo sub:pubinfo . }
                sub:assertion { %2$s }
                sub:provenance { sub:assertion rdfs:seeAlso sub:assertion . }
                sub:pubinfo { this: rdfs:seeAlso this: . }
                """.formatted(uri, assertion);
    }

    private static String links(String assertion, String provenance, String pubinfo) {
        return (assertion == null ? "" : "this: nt:wasCreatedFromTemplate <" + assertion + "> . ")
                + (provenance == null ? "" : "this: nt:wasCreatedFromProvenanceTemplate <" + provenance + "> . ")
                + (pubinfo == null ? "" : "this: nt:wasCreatedFromPubinfoTemplate <" + pubinfo + "> . ");
    }

    @Test
    void templateLinksOfTheRightKindHaveNoFindings() throws Exception {
        publishTemplates();
        assertFindings(check(nanopub(V1, "ex:s rdfs:label \"A\" .", links(ASSERTION_TEMPLATE, PROVENANCE_TEMPLATE, PUBINFO_TEMPLATE), "A")), List.of(), List.of());
    }

    @Test
    void templateLinkOfAnotherKindIsAnError() throws Exception {
        publishTemplates();
        publish(nanopub(uri("NoTemplate"), "ex:s rdfs:label \"A\" .", "", "A"));
        List<NetworkFinding> findings = check(nanopub(V1, "ex:s rdfs:label \"A\" .", links(ASSERTION_TEMPLATE, ASSERTION_TEMPLATE, uri("NoTemplate")), "A"));
        assertFindings(findings, List.of(Kind.WRONG_TEMPLATE_KIND, Kind.WRONG_TEMPLATE_KIND), List.of());
        assertEquals("The link to the provenance template names <" + ASSERTION_TEMPLATE + ">, which is a template for the assertion", findings.get(0).message());
        assertEquals("The link to the publication info template names <" + uri("NoTemplate") + ">, which is not a template", findings.get(1).message());
    }

    @Test
    void missingAndUnavailableTemplatesAreNotes() throws Exception {
        publishTemplates();
        assertFindings(check(nanopub(V1, "ex:s rdfs:label \"A\" .", links(ASSERTION_TEMPLATE, null, uri("Unknown")), "A")),
                List.of(), List.of(Kind.MISSING_TEMPLATE_LINK, Kind.UNCHECKED));
        // A nanopublication that names no template at all is not one made from templates.
        assertFindings(check(nanopub(V1, "ex:s rdfs:label \"A\" .", "", "A")), List.of(), List.of());
    }

    // ---------- governance ----------

    private static final String KIND = "https://example.org/kinds/thing-view";

    private static Nanopub governed(String uri, String kind, String more, String key) throws Exception {
        return nanopub(uri, "sub:view dct:isVersionOf <" + kind + "> ; gen:governedBy <" + SPACE + "> .", "this: npx:embeds sub:view . " + more, key);
    }

    @Test
    void governanceBySpaceThatMaintainsTheKindHasNoFindings() throws Exception {
        maintained.put(KIND, Set.of(SPACE));
        assertFindings(check(governed(V1, KIND, "", "A")), List.of(), List.of());
    }

    @Test
    void governanceBySpaceThatDoesNotMaintainTheKindIsANote() throws Exception {
        maintained.put(KIND, Set.of("https://w3id.org/spaces/other"));
        List<NetworkFinding> findings = check(governed(V1, KIND, "", "A"));
        assertFindings(findings, List.of(), List.of(Kind.GOVERNANCE_WITHOUT_EFFECT));
        assertTrue(findings.get(0).message().startsWith("<" + SPACE + "> does not maintain <" + KIND + "> (it is maintained by <https://w3id.org/spaces/other>)"), findings.get(0).message());
    }

    @Test
    void governanceThatCannotBeAskedAboutIsNotChecked() throws Exception {
        servicesDown = true;
        assertFindings(check(governed(V1, KIND, "", "A")), List.of(), List.of(Kind.UNCHECKED));
    }

    @Test
    void governanceWithoutWhatItNeedsIsANote() throws Exception {
        maintained.put(KIND, Set.of(SPACE));
        // Not embedded:
        assertFindings(check(nanopub(V1, "sub:view dct:isVersionOf <" + KIND + "> ; gen:governedBy <" + SPACE + "> .", "", "A")),
                List.of(), List.of(Kind.GOVERNANCE_WITHOUT_EFFECT));
        // Not a version of anything:
        assertFindings(check(nanopub(V1, "sub:view gen:governedBy <" + SPACE + "> .", "this: npx:embeds sub:view .", "A")),
                List.of(), List.of(Kind.GOVERNANCE_WITHOUT_EFFECT));
        // A kind the nanopublication mints itself, before it is published:
        Nanopub draft = nanopub(DRAFT, "sub:view dct:isVersionOf sub:kind ; gen:governedBy <" + SPACE + "> .", "this: npx:embeds sub:view .", null);
        assertFindings(check(draft), List.of(), List.of(Kind.GOVERNANCE_WITHOUT_EFFECT));
        // Another kind than the version before it:
        publish(governed(V1, "https://example.org/kinds/other", "", "A"));
        assertFindings(check(governed(V2, KIND, SUPERSEDES_V1, "A")), List.of(), List.of(Kind.GOVERNANCE_WITHOUT_EFFECT));
    }

    // ---------- the key of a new version ----------

    @Test
    void newVersionSignedWithAnotherKeyIsAnError() throws Exception {
        publishFirstVersion();
        Nanopub second = nanopub(V2, "v1:thing rdfs:label \"A thing\" .", SUPERSEDES_V1 + " this: npx:introduces v1:thing .", "B");
        List<NetworkFinding> findings = check(second);
        assertFindings(findings, List.of(Kind.OTHER_KEY), List.of());
        assertTrue(findings.get(0).message().startsWith("The nanopublication is signed with another key than <" + V1 + "> that it supersedes, and no space governs it."));
    }

    @Test
    void newVersionThatIsNotSignedYetIsNotChecked() throws Exception {
        publishFirstVersion();
        Nanopub draft = nanopub(DRAFT, "v1:thing rdfs:label \"A thing\" .", SUPERSEDES_V1 + " this: npx:introduces v1:thing .", null);
        assertFindings(check(draft), List.of(), List.of(Kind.UNCHECKED));
    }

    @Test
    void newVersionGovernedByASpaceNeedsNoParticularKey() throws Exception {
        maintained.put(KIND, Set.of(SPACE));
        publish(governed(V1, KIND, "", "A"));
        assertFindings(check(governed(V2, KIND, SUPERSEDES_V1, "B")), List.of(), List.of());

        // If the space does not maintain the kind, the key counts again:
        maintained.clear();
        assertFindings(check(governed(V2, KIND, SUPERSEDES_V1, "B")), List.of(Kind.OTHER_KEY), List.of(Kind.GOVERNANCE_WITHOUT_EFFECT));

        // And if that cannot be found out, nothing is said to be wrong:
        servicesDown = true;
        assertFindings(check(governed(V2, KIND, SUPERSEDES_V1, "B")), List.of(), List.of(Kind.UNCHECKED, Kind.UNCHECKED));
    }

}
