package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nanopub.vocabulary.FDOF;
import org.nanopub.vocabulary.KPXL;
import org.nanopub.vocabulary.NPX;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class RoCrateParserTest {

    static final String BASE_ROHUB_URL = "https://w3id.org/ro-id/";
    static final String RO_CRATE_ID = "7ad44bec-6784-437f-b5f3-2199b43a5303";
    static final String RO_CRATE_URL = BASE_ROHUB_URL + RO_CRATE_ID + "/";
    static final String RO_CRATE_METADATA_PATH = Objects.requireNonNull(RoCrateParserTest.class.getResource("/")).getPath() + RO_CRATE_ID + ".jsonld";
    static final String EXPECTED_PROV_URL = RO_CRATE_URL + "ro-crate-metadata.json";

    private RoCrateParser parser;

    private static InputStream fixtureStream() throws Exception {
        return new FileInputStream(RO_CRATE_METADATA_PATH);
    }

    @BeforeEach
    void setUp() {
        parser = new RoCrateParser();
    }

    @Nested
    class PubinfoStatements {

        @Test
        void exactlyTwoTypeStatementsInPubinfo() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            List<Statement> types = np.getPubinfo().stream()
                    .filter(st -> st.getPredicate().equals(RDF.TYPE))
                    .toList();

            assertEquals(2, types.size(),
                    "Exactly two rdf:type statements expected in pubinfo");
        }

        @Test
        void pubinfoDeclaresRoCrateNanopubType() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            boolean hasType = np.getPubinfo().stream()
                    .anyMatch(st -> st.getPredicate().equals(RDF.TYPE)
                                    && st.getObject().equals(KPXL.RO_CRATE_NANOPUB));

            assertTrue(hasType, "pubinfo must carry rdf:type kpxl:RoCrateNanopub");
        }

        @Test
        void pubinfoDeclaresRoCrateNanopubTypeAsFirstType() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            Statement firstType = np.getPubinfo().stream()
                    .filter(st -> st.getPredicate().equals(RDF.TYPE))
                    .findFirst()
                    .orElseThrow();

            assertEquals(KPXL.RO_CRATE_NANOPUB, firstType.getObject());
        }

        @Test
        void pubinfoDeclaressFairDigitalObjectType() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            boolean hasType = np.getPubinfo().stream()
                    .anyMatch(st -> st.getPredicate().equals(RDF.TYPE)
                                    && st.getObject().equals(FDOF.FAIR_DIGITAL_OBJECT));

            assertTrue(hasType, "pubinfo must carry rdf:type fdof:FairDigitalObject");
        }

        @Test
        void exactlyOneIntroducesStatement() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            List<Statement> introduces = np.getPubinfo().stream()
                    .filter(st -> st.getPredicate().equals(NPX.INTRODUCES))
                    .toList();

            assertEquals(1, introduces.size(),
                    "Exactly one npx:introduces statement expected in pubinfo");
        }

        @Test
        void introducesObjectIsAnIri() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            boolean objectIsIri = np.getPubinfo().stream()
                    .filter(st -> st.getPredicate().equals(NPX.INTRODUCES))
                    .map(Statement::getObject)
                    .allMatch(obj -> obj instanceof org.eclipse.rdf4j.model.IRI);

            assertTrue(objectIsIri,
                    "npx:introduces object must be an IRI, not a blank node or literal");
        }

        @Test
        void introducesObjectMatchesRoCrateUrl() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            String introducedIri = np.getPubinfo().stream()
                    .filter(st -> st.getPredicate().equals(NPX.INTRODUCES))
                    .map(st -> st.getObject().stringValue())
                    .findFirst()
                    .orElseThrow();

            assertEquals(RO_CRATE_URL, introducedIri,
                    "npx:introduces must point to the crate base URL for this fixture");
        }

        @Test
        void exactlyOneRdfsLabelStatement() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            List<Statement> labels = np.getPubinfo().stream()
                    .filter(st -> st.getPredicate().equals(RDFS.LABEL))
                    .toList();

            assertEquals(1, labels.size(),
                    "Exactly one rdfs:label statement expected in pubinfo");
        }

        @Test
        void rdfsLabelIsNonBlank() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            String label = np.getPubinfo().stream()
                    .filter(st -> st.getPredicate().equals(RDFS.LABEL))
                    .map(st -> st.getObject().stringValue())
                    .findFirst()
                    .orElseThrow();

            assertFalse(label.isBlank(), "rdfs:label must not be blank");
        }

        @Test
        void rdfsLabelIsAtMost212Chars() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            String label = np.getPubinfo().stream()
                    .filter(st -> st.getPredicate().equals(RDFS.LABEL))
                    .map(st -> st.getObject().stringValue())
                    .findFirst()
                    .orElseThrow();

            assertTrue(label.length() <= 212,
                    "rdfs:label must be truncated to at most 212 chars, got: " + label.length());
        }
    }

    @Nested
    class AssertionGraph {

        @Test
        void assertionGraphIsNonEmpty() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream()).finalizeNanopub(true);

            assertFalse(np.getAssertion().isEmpty(), "Assertion graph must not be empty");
        }

        @Test
        void totalTripleCountMatchesFixture() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            assertEquals(313, np.getTripleCount(),
                    "Total triple count must match the known fixture value");
        }
    }

    @Nested
    class ProvenanceStatements {

        @Test
        void exactlyOneWasDerivedFromStatement() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            List<Statement> derived = np.getProvenance().stream()
                    .filter(st -> st.getPredicate().equals(PROV.WAS_DERIVED_FROM))
                    .toList();

            assertEquals(1, derived.size(),
                    "Exactly one prov:wasDerivedFrom statement expected in provenance");
        }

        @Test
        void wasDerivedFromPointsToCanonicalMetadataUrl() throws Exception {
            Nanopub np = parser.parseRoCreate(RO_CRATE_URL, fixtureStream())
                    .finalizeNanopub(true);

            String derivedFrom = np.getProvenance().stream()
                    .filter(st -> st.getPredicate().equals(PROV.WAS_DERIVED_FROM))
                    .map(st -> st.getObject().stringValue())
                    .findFirst()
                    .orElseThrow();

            assertEquals(EXPECTED_PROV_URL, derivedFrom, "wasDerivedFrom must be <baseUrl>ro-crate-metadata.json");
        }
    }

    @Nested
    class ReturnValueContract {

        @Test
        void returnsNonNullNanopubCreator() throws Exception {
            NanopubCreator creator = parser.parseRoCreate(RO_CRATE_URL, fixtureStream());
            assertNotNull(creator, "parseRoCreate must never return null");
        }

        @Test
        void firstFinalizationSucceeds() {
            assertDoesNotThrow(() -> parser.parseRoCreate(RO_CRATE_URL, fixtureStream()).finalizeNanopub(true));
        }

    }

    @Nested
    class ConstructRoCrateUrl {

        @Test
        void testConstructRoCrateUrl() {
            String suffix = "crate/download/";
            String id = "55a1b422-f279-4765-9ba7-d27268059844/";
            String fullUrl = RoCrateParser.BASE_ROCRATE_API_URL + id + suffix;
            IRI res = RoCrateParser.constructRoCrateUrl(fullUrl, null);
            assertEquals("https://w3id.org/ro-id/" + id, res.stringValue());
        }

        @Test
        void testConstructRoHubApiUrl() {
            String roHubId = "302b4ebf-db38-49d5-8ab4-4561181f4e94";
            String downloadUrl = RoCrateParser.BASE_ROCRATE_API_URL + roHubId + "/crate/download/";
            IRI res = RoCrateParser.constructRoCrateUrl(downloadUrl, null);
            assertEquals(BASE_ROHUB_URL + roHubId + "/", res.stringValue());
        }

        @Test
        void testConstructSimpleRoCrateUrlWithMetadataJustOneSlash() {
            String url = "https://zenodo.org/records/3541888/files/";
            IRI res = RoCrateParser.constructRoCrateUrl(url, null);
            assertEquals(url, res.stringValue());
        }

        @Test
        void testConstructSimpleRoCrateUrl() {
            String url = "https://zenodo.org/records/3541888/files/";
            String metadataUrl = "ro-crate-metadata.jsonld";
            IRI res = RoCrateParser.constructRoCrateUrl(url + metadataUrl, null);
            assertEquals(url, res.stringValue());
        }

        @Test
        void metadataFileIsStripped_json() {
            String url = "https://example.org/data/ro-crate-metadata.json";

            IRI result = RoCrateParser.constructRoCrateUrl(url, null);
            assertEquals("https://example.org/data/", result.stringValue());
        }

        @Test
        void arbitraryFileIsStrippedToBasePath() {
            String url = "https://example.org/path/to/file.txt";

            IRI result = RoCrateParser.constructRoCrateUrl(url, null);
            assertEquals("https://example.org/path/to/", result.stringValue());
        }

        @Test
        void doubleSlashPath_isPreserved() {
            String url = "https://zenodo.org/records/3541888/files//";

            IRI result = RoCrateParser.constructRoCrateUrl(url, null);
            assertEquals(url, result.stringValue());
        }

        @Test
        void dotSegmentPath_isPreserved() {
            String url = "https://abc.ziz/testrecord/./";

            IRI result = RoCrateParser.constructRoCrateUrl(url, null);
            assertEquals(url, result.stringValue());
        }

        @Test
        void nonHttpUrl_isReturnedAsIri() {
            String url = "ftp://example.org/data/";

            IRI result = RoCrateParser.constructRoCrateUrl(url, null);
            assertEquals(url, result.stringValue());
        }

        @Test
        void nonMatchingHttpPattern_returnsOriginalUrl() {
            String url = "https://example.org";

            IRI result = RoCrateParser.constructRoCrateUrl(url, null);
            assertEquals(url, result.stringValue());
        }

        @Test
        void testConstructSimpleRoCrateUrlWithMetadataSpecialCaseDoubleSlash() {
            // TODO discuss standard-conformity of this, ...//
            String url = "https://zenodo.org/records/3541888/files//";
            IRI res = RoCrateParser.constructRoCrateUrl(url, null);
            assertEquals(url, res.stringValue());
        }

        @Test
        void testConstructSimpleRoCrateUrlWithDotReferenceInPath() {
            String url = "https://abc.ziz/testrecord/./";
            IRI res = RoCrateParser.constructRoCrateUrl(url, null);
            assertEquals(url, res.stringValue());
        }

        @Test
        void testConstructNonIdRoCrateUrl() {
            String urlWithoutIdNorMetadata = "https://raw.githubusercontent.com/FAIR2Adapt/saarland-flooding/refs/heads/main/notebooks/get_typename_from_WFS.ipynb";
            String expectedUrlNoMetadata__ = "https://raw.githubusercontent.com/FAIR2Adapt/saarland-flooding/refs/heads/main/notebooks/";
            IRI res = RoCrateParser.constructRoCrateUrl(urlWithoutIdNorMetadata, null);
            assertEquals(expectedUrlNoMetadata__, res.stringValue());
        }

    }

}
