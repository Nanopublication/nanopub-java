package org.nanopub;

import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.PAV;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

class SimpleTimestampPatternTest {

    private final SimpleTimestampPattern pattern = new SimpleTimestampPattern();

    private static Nanopub nanopubWithTimestamp(org.eclipse.rdf4j.model.IRI predicate, String value) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, anyIri, anyIri);
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(predicate, vf.createLiteral(value, XSD.DATETIME));
        return creator.finalizeNanopub();
    }

    @Test
    void getName() {
        assertEquals("Basic timestamp information", pattern.getName());
    }

    @Test
    void appliesToEveryNanopub() throws Exception {
        assertTrue(pattern.appliesTo(TestUtils.createNanopub()));
    }

    @Test
    void getPatternInfoUrl() throws Exception {
        assertEquals("https://github.com/Nanopublication/nanopub-java/blob/master/src/main/java/org/nanopub/SimpleTimestampPattern.java",
                pattern.getPatternInfoUrl().toString());
    }

    @Test
    void isCorrectlyUsedByANanopubWithATimestamp() throws Exception {
        assertTrue(pattern.isCorrectlyUsedBy(nanopubWithTimestamp(DCTERMS.CREATED, "2024-01-02T03:04:05Z")));
    }

    @Test
    void isNotCorrectlyUsedByANanopubWithoutATimestamp() throws Exception {
        assertFalse(pattern.isCorrectlyUsedBy(TestUtils.createNanopub()));
    }

    @Test
    void getDescriptionForANanopubWithATimestamp() throws Exception {
        String description = pattern.getDescriptionFor(nanopubWithTimestamp(DCTERMS.CREATED, "2024-01-02T03:04:05Z"));
        assertTrue(description.startsWith("Timestamp: "), description);
    }

    @Test
    void getDescriptionForANanopubWithoutATimestamp() throws Exception {
        assertEquals("No timestamp found", pattern.getDescriptionFor(TestUtils.createNanopub()));
    }

    @Test
    void getCreationTimeReadsTheTimestamp() throws Exception {
        Calendar creationTime = SimpleTimestampPattern.getCreationTime(
                nanopubWithTimestamp(DCTERMS.CREATED, "2024-01-02T03:04:05Z"));

        assertNotNull(creationTime);
        assertEquals(2024, creationTime.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, creationTime.get(Calendar.MONTH));
        assertEquals(2, creationTime.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    void getCreationTimeAcceptsEveryCreationTimeProperty() throws Exception {
        assertNotNull(SimpleTimestampPattern.getCreationTime(nanopubWithTimestamp(PROV.GENERATED_AT_TIME, "2024-01-02T03:04:05Z")));
        assertNotNull(SimpleTimestampPattern.getCreationTime(nanopubWithTimestamp(PAV.CREATED_ON, "2024-01-02T03:04:05Z")));
    }

    @Test
    void getCreationTimeWithoutATimestampIsNull() throws Exception {
        assertNull(SimpleTimestampPattern.getCreationTime(TestUtils.createNanopub()));
    }

    @Test
    void getCreationTimeIgnoresStatementsThatDoNotQualify() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(anyIri, anyIri, anyIri);
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        // not about the nanopub itself
        creator.addPubinfoStatement(vf.createStatement(anyIri, DCTERMS.CREATED, vf.createLiteral("2024-01-02T03:04:05Z", XSD.DATETIME)));
        // not a creation time property
        creator.addPubinfoStatement(RDFS.LABEL, vf.createLiteral("2024-01-02T03:04:05Z", XSD.DATETIME));
        // not a literal
        creator.addPubinfoStatement(DCTERMS.CREATED, anyIri);
        // not an xsd:dateTime
        creator.addPubinfoStatement(DCTERMS.CREATED, vf.createLiteral("2024-01-02"));

        assertNull(SimpleTimestampPattern.getCreationTime(creator.finalizeNanopub()));
    }

    @Test
    void isCreationTimeProperty() {
        assertTrue(SimpleTimestampPattern.isCreationTimeProperty(DCTERMS.CREATED));
        assertTrue(SimpleTimestampPattern.isCreationTimeProperty(PROV.GENERATED_AT_TIME));
        assertTrue(SimpleTimestampPattern.isCreationTimeProperty(PAV.CREATED_ON));
        assertFalse(SimpleTimestampPattern.isCreationTimeProperty(RDFS.LABEL));
    }

}
