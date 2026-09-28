package org.nanopub.trusty;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nanopub.*;
import org.nanopub.utils.TestUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

class MakeTrustyNanopubTest {

    private static Nanopub plainNanopub() throws Exception {
        return TestUtils.createNanopub("https://example.org/np1#");
    }

    private static Nanopub tempUriNanopub() throws Exception {
        NanopubCreator creator = new NanopubCreator(true);
        creator.addAssertionStatement(anyIri, anyIri, anyIri);
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        return creator.finalizeNanopub();
    }

    private static File write(File directory, String name, Nanopub... nanopubs) throws Exception {
        StringBuilder trig = new StringBuilder();
        for (Nanopub np : nanopubs) {
            trig.append(np.writeToString(RDFFormat.TRIG));
        }
        File file = new File(directory, name);
        Files.writeString(file.toPath(), trig.toString(), StandardCharsets.UTF_8);
        return file;
    }

    /**
     * Parses back what was written and asserts that every nanopub in it is trusty.
     */
    private static void assertAllTrusty(ByteArrayOutputStream out, int expectedCount) throws Exception {
        List<Nanopub> written = new ArrayList<>();
        MultiNanopubRdfHandler.process(RDFFormat.TRIG,
                new ByteArrayInputStream(out.toByteArray()), written::add);

        assertEquals(expectedCount, written.size());
        for (Nanopub np : written) {
            assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(np), np.getUri() + " is not trusty");
        }
    }

    // ------------------------------------------------------------- transform

    @Test
    void transformTurnsAPlainNanopubIntoATrustyOne() throws Exception {
        Nanopub trusty = MakeTrustyNanopub.transform(plainNanopub());

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(trusty));
    }

    @Test
    void transformResolvesTemporaryUris() throws Exception {
        Nanopub temp = tempUriNanopub();
        assertTrue(TempUriReplacer.hasTempUri(temp));

        Nanopub trusty = MakeTrustyNanopub.transform(temp);

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(trusty));
        assertFalse(TempUriReplacer.hasTempUri(trusty));
    }

    @Test
    void transformRefusesGraphUrisOutsideTheNanopubUri() {
        Nanopub nanopub = mock(Nanopub.class);
        when(nanopub.getUri()).thenReturn(vf.createIRI("https://example.org/np1#"));
        when(nanopub.getHeadUri()).thenReturn(vf.createIRI("https://elsewhere.example.com/head"));

        TrustyUriException ex = assertThrows(TrustyUriException.class, () -> MakeTrustyNanopub.transform(nanopub));
        assertTrue(ex.getMessage().startsWith("Graph URIs need have the nanopub URI as prefix: "), ex.getMessage());
    }

    @Test
    void transformChecksEveryGraphUri() throws Exception {
        Nanopub good = plainNanopub();
        IRI outside = vf.createIRI("https://elsewhere.example.com/graph");

        // each of the four graph URIs is checked in turn
        assertThrows(TrustyUriException.class, () -> MakeTrustyNanopub.transform(
                nanopubWithGraphUris(good, good.getHeadUri(), outside, good.getProvenanceUri(), good.getPubinfoUri())));
        assertThrows(TrustyUriException.class, () -> MakeTrustyNanopub.transform(
                nanopubWithGraphUris(good, good.getHeadUri(), good.getAssertionUri(), outside, good.getPubinfoUri())));
        assertThrows(TrustyUriException.class, () -> MakeTrustyNanopub.transform(
                nanopubWithGraphUris(good, good.getHeadUri(), good.getAssertionUri(), good.getProvenanceUri(), outside)));
    }

    private static Nanopub nanopubWithGraphUris(Nanopub original, IRI head, IRI assertion, IRI provenance, IRI pubinfo) {
        Nanopub nanopub = mock(Nanopub.class);
        when(nanopub.getUri()).thenReturn(original.getUri());
        when(nanopub.getHeadUri()).thenReturn(head);
        when(nanopub.getAssertionUri()).thenReturn(assertion);
        when(nanopub.getProvenanceUri()).thenReturn(provenance);
        when(nanopub.getPubinfoUri()).thenReturn(pubinfo);
        return nanopub;
    }

    @Test
    void transformRecordsTheTransformationInTheReferenceMap() throws Exception {
        Map<Resource, IRI> tempRefMap = new HashMap<>();

        Nanopub trusty = MakeTrustyNanopub.transform(tempUriNanopub(), tempRefMap, null);

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(trusty));
        assertFalse(tempRefMap.isEmpty());
    }

    @Test
    void transformRecordsTheTransformationInThePrefixMap() throws Exception {
        Map<String, String> tempPrefixMap = new HashMap<>();

        Nanopub trusty = MakeTrustyNanopub.transform(plainNanopub(), null, tempPrefixMap);

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(trusty));
        assertFalse(tempPrefixMap.isEmpty());
    }

    @Test
    void transformFillsBothMapsAtOnce() throws Exception {
        Map<Resource, IRI> tempRefMap = new HashMap<>();
        Map<String, String> tempPrefixMap = new HashMap<>();

        Nanopub trusty = MakeTrustyNanopub.transform(plainNanopub(), tempRefMap, tempPrefixMap);

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(trusty));
        assertFalse(tempRefMap.isEmpty());
        assertFalse(tempPrefixMap.isEmpty());
    }

    @Test
    void writeAsTrustyNanopubWritesTheTransformedNanopub() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Nanopub trusty = MakeTrustyNanopub.writeAsTrustyNanopub(plainNanopub(), RDFFormat.TRIG, out, null, null);

        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(trusty));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains(trusty.getUri().toString()));
    }

    // --------------------------------------------------- the merge helpers

    @Test
    void mergeTransformMapsIgnoresNullMaps() {
        Map<Resource, IRI> map = new HashMap<>();
        map.put(anyIri, anyIri);

        assertDoesNotThrow(() -> MakeTrustyNanopub.mergeTransformMaps(null, map));
        assertDoesNotThrow(() -> MakeTrustyNanopub.mergeTransformMaps(map, null));
        assertEquals(1, map.size());
    }

    @Test
    void mergeTransformMapsChainsTheTransformations() {
        IRI first = vf.createIRI("https://example.org/first");
        IRI second = vf.createIRI("https://example.org/second");
        IRI third = vf.createIRI("https://example.org/third");
        Map<Resource, IRI> mainMap = new HashMap<>();
        mainMap.put(first, second);
        Map<Resource, IRI> mapToMerge = new HashMap<>();
        mapToMerge.put(second, third);

        MakeTrustyNanopub.mergeTransformMaps(mainMap, mapToMerge);

        // first now points straight at third, and the intermediate entry is gone
        assertEquals(third, mainMap.get(first));
        assertEquals(1, mainMap.size());
        assertTrue(mapToMerge.isEmpty());
    }

    @Test
    void mergeTransformMapsAddsUnrelatedEntries() {
        IRI first = vf.createIRI("https://example.org/first");
        IRI second = vf.createIRI("https://example.org/second");
        IRI other = vf.createIRI("https://example.org/other");
        IRI otherTarget = vf.createIRI("https://example.org/otherTarget");
        Map<Resource, IRI> mainMap = new HashMap<>();
        mainMap.put(first, second);
        Map<Resource, IRI> mapToMerge = new HashMap<>();
        mapToMerge.put(other, otherTarget);

        MakeTrustyNanopub.mergeTransformMaps(mainMap, mapToMerge);

        assertEquals(second, mainMap.get(first));
        assertEquals(otherTarget, mainMap.get(other));
    }

    @Test
    void mergePrefixTransformMapsIgnoresNullMaps() {
        assertDoesNotThrow(() -> MakeTrustyNanopub.mergePrefixTransformMaps(null, new HashMap<>()));
        assertDoesNotThrow(() -> MakeTrustyNanopub.mergePrefixTransformMaps(new HashMap<>(), null));
    }

    @Test
    void mergePrefixTransformMapsKeepsOnlyTrustyTargets() {
        IRI temp = vf.createIRI("http://purl.org/nanopub/temp/1234/");
        IRI trusty = vf.createIRI("https://w3id.org/np/RAO30EliKt55zd1CjWpKBE9q3KeJfoy9q0Q5x-XaSNxRk");
        IRI plain = vf.createIRI("https://example.org/plain/");
        Map<Resource, IRI> mapToMerge = new HashMap<>();
        mapToMerge.put(temp, trusty);
        mapToMerge.put(vf.createIRI("https://example.org/other/"), plain);
        mapToMerge.put(vf.createBNode(), trusty);
        Map<String, String> mainPrefixMap = new HashMap<>();

        MakeTrustyNanopub.mergePrefixTransformMaps(mainPrefixMap, mapToMerge);

        assertTrue(TrustyUriUtils.isPotentialTrustyUri(trusty.stringValue()));
        assertEquals(trusty.stringValue(), mainPrefixMap.get(temp.stringValue()));
        assertFalse(mainPrefixMap.containsValue(plain.stringValue()));
    }

    // -------------------------------------------------- transformMultiNanopub

    @Test
    void transformMultiNanopubFromAStream() throws Exception {
        String trig = plainNanopub().writeToString(RDFFormat.TRIG)
                      + TestUtils.createNanopub("https://example.org/np2#").writeToString(RDFFormat.TRIG);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        MakeTrustyNanopub.transformMultiNanopub(RDFFormat.TRIG,
                new ByteArrayInputStream(trig.getBytes(StandardCharsets.UTF_8)), out);

        assertAllTrusty(out, 2);
    }

    @Test
    void transformMultiNanopubFromAStreamResolvingCrossRefs() throws Exception {
        String trig = plainNanopub().writeToString(RDFFormat.TRIG);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        MakeTrustyNanopub.transformMultiNanopub(RDFFormat.TRIG,
                new ByteArrayInputStream(trig.getBytes(StandardCharsets.UTF_8)), out, true);

        assertAllTrusty(out, 1);
    }

    @Test
    void transformMultiNanopubFromAFile(@TempDir File tempDir) throws Exception {
        File input = write(tempDir, "input.trig", plainNanopub());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        MakeTrustyNanopub.transformMultiNanopub(RDFFormat.TRIG, input, out);

        assertAllTrusty(out, 1);
    }

    @Test
    void transformMultiNanopubFromAFileResolvingCrossRefs(@TempDir File tempDir) throws Exception {
        File input = write(tempDir, "input.trig", plainNanopub());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        MakeTrustyNanopub.transformMultiNanopub(RDFFormat.TRIG, input, out, true);

        assertAllTrusty(out, 1);
    }

    // ------------------------------------------------------------- the CLI

    private static void assertIsTrusty(File file) throws Exception {
        assertTrue(file.exists(), file + " was not written");
        assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(new NanopubImpl(file, RDFFormat.TRIG)));
    }

    @Test
    void writesNextToTheInputWhenNoOutputFileIsGiven(@TempDir File tempDir) throws Exception {
        File input = write(tempDir, "input.trig", plainNanopub());

        MakeTrustyNanopub.main(new String[]{input.getPath()});

        assertIsTrusty(new File(tempDir, "trusty.input.trig"));
    }

    @Test
    void writesToTheGivenOutputFile(@TempDir File tempDir) throws Exception {
        File input = write(tempDir, "input.trig", plainNanopub());
        File output = new File(tempDir, "out.trig");

        MakeTrustyNanopub.main(new String[]{"-o", output.getPath(), input.getPath()});

        assertIsTrusty(output);
    }

    @Test
    void writesAGzippedSingleOutputFile(@TempDir File tempDir) throws Exception {
        File input = write(tempDir, "input.trig", plainNanopub());
        File output = new File(tempDir, "out.trig.gz");

        MakeTrustyNanopub.main(new String[]{"-o", output.getPath(), input.getPath()});

        assertTrue(output.exists());
        try (InputStream in = new GZIPInputStream(new FileInputStream(output))) {
            assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(new NanopubImpl(in, RDFFormat.TRIG)));
        }
    }

    @Test
    void readsAndWritesGzippedFiles(@TempDir File tempDir) throws Exception {
        File plain = write(tempDir, "plain.trig", plainNanopub());
        File input = new File(tempDir, "input.trig.gz");
        try (OutputStream out = new GZIPOutputStream(new FileOutputStream(input))) {
            Files.copy(plain.toPath(), out);
        }

        MakeTrustyNanopub.main(new String[]{input.getPath()});

        File output = new File(tempDir, "trusty.input.trig.gz");
        assertTrue(output.exists());
        try (InputStream in = new GZIPInputStream(new FileInputStream(output))) {
            assertTrue(TrustyNanopubUtils.isValidTrustyNanopub(new NanopubImpl(in, RDFFormat.TRIG)));
        }
    }

    @Test
    void resolvesCrossReferences(@TempDir File tempDir) throws Exception {
        File input = write(tempDir, "input.trig", tempUriNanopub(), tempUriNanopub());
        File output = new File(tempDir, "out.trig");

        MakeTrustyNanopub.main(new String[]{"-r", "-o", output.getPath(), input.getPath()});

        assertTrue(output.exists());
        assertTrue(Files.readString(output.toPath()).contains("https://w3id.org/np/"));
    }

    @Test
    void resolvesCrossReferencesByPrefix(@TempDir File tempDir) throws Exception {
        File input = write(tempDir, "input.trig", tempUriNanopub(), tempUriNanopub());
        File output = new File(tempDir, "out.trig");

        MakeTrustyNanopub.main(new String[]{"-R", "-o", output.getPath(), input.getPath()});

        assertTrue(output.exists());
        assertTrue(Files.readString(output.toPath()).contains("https://w3id.org/np/"));
    }

    @Test
    void printsTheNanopubUriInVerboseMode(@TempDir File tempDir) throws Exception {
        File input = write(tempDir, "input.trig", plainNanopub());
        File output = new File(tempDir, "out.trig");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            MakeTrustyNanopub.main(new String[]{"-v", "-o", output.getPath(), input.getPath()});
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(captured.toString(StandardCharsets.UTF_8).contains("Nanopub URI: "), captured.toString());
    }


}
