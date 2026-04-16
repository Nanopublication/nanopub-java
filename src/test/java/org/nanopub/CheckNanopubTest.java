package org.nanopub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckNanopubTest {

    @Test
    void constructor_withFileList_doesNotThrow() {
        assertDoesNotThrow(() -> new CheckNanopub(List.of("file1.trig", "file2.trig")));
    }

    @Test
    void constructor_withSparqlEndpointAndIds_doesNotThrow() {
        assertDoesNotThrow(() -> new CheckNanopub("https://example.org/sparql", List.of("https://example.org/np1")));
    }

    @Test
    void setVerbose_true_doesNotThrow() {
        assertDoesNotThrow(() -> new CheckNanopub(List.of()).setVerbose(true));
    }

    @Test
    void setVerbose_false_doesNotThrow() {
        assertDoesNotThrow(() -> new CheckNanopub(List.of()).setVerbose(false));
    }

    @Test
    void setLogPrintStream_doesNotThrow() {
        PrintStream ps = new PrintStream(new ByteArrayOutputStream());
        assertDoesNotThrow(() -> new CheckNanopub(List.of()).setLogPrintStream(ps));
    }

    @Test
    void check_withEmptyInputList_returnsZeroCountReport() throws IOException {
        CheckNanopub.Report r = new CheckNanopub(List.of()).check();
        assertEquals(0, r.getAllCount());
        assertTrue(r.areAllValid());
    }

    @Test
    void check_withNonExistentFile_recordsProblem(@TempDir Path tmp) {
        String missing = tmp.resolve("does_not_exist.trig").toString();
        CheckNanopub checker = new CheckNanopub(List.of(missing));
        checker.setLogPrintStream(new PrintStream(new ByteArrayOutputStream()));
        assertThrows(IOException.class, checker::check);
    }

    @Test
    void check_withEmptyTrigFile_countsNoNanopubFoundAsError(@TempDir Path tmp) throws IOException {
        File empty = tmp.resolve("empty.trig").toFile();
        Files.writeString(empty.toPath(), "");

        ByteArrayOutputStream log = new ByteArrayOutputStream();
        CheckNanopub checker = new CheckNanopub(List.of(empty.getAbsolutePath()));
        checker.setLogPrintStream(new PrintStream(log));

        CheckNanopub.Report r = checker.check();

        assertEquals(1, r.getErrorCount());
        assertTrue(log.toString().contains("NO NANOPUB FOUND"));
    }

    @Nested
    class CheckNanopubReportTest {

        private CheckNanopub.Report report;

        @BeforeEach
        void setUp() throws Exception {
            CheckNanopub instance = new CheckNanopub(List.of());
            var ctor = CheckNanopub.Report.class.getDeclaredConstructor(CheckNanopub.class);
            ctor.setAccessible(true);
            report = ctor.newInstance(instance);
        }

        @Test
        void signedCount_startsAtZero() {
            assertEquals(0, report.getSignedCount());
        }

        @Test
        void signedCount_incrementsCorrectly() throws Exception {
            count("countSigned");
            count("countSigned");
            assertEquals(2, report.getSignedCount());
        }

        @Test
        void legacySignedCount_startsAtZero() {
            assertEquals(0, report.getLegacySignedCount());
        }

        @Test
        void legacySignedCount_incrementsCorrectly() throws Exception {
            count("countLegacySigned");
            assertEquals(1, report.getLegacySignedCount());
        }

        @Test
        void trustyCount_startsAtZero() {
            assertEquals(0, report.getTrustyCount());
        }

        @Test
        void trustyCount_incrementsCorrectly() throws Exception {
            count("countTrusty");
            count("countTrusty");
            count("countTrusty");
            assertEquals(3, report.getTrustyCount());
        }

        @Test
        void notTrustyCount_startsAtZero() {
            assertEquals(0, report.getNotTrustyCount());
        }

        @Test
        void notTrustyCount_incrementsCorrectly() throws Exception {
            count("countNotTrusty");
            assertEquals(1, report.getNotTrustyCount());
        }

        @Test
        void invalidSignatureCount_startsAtZero() {
            assertEquals(0, report.getInvalidSignatureCount());
        }

        @Test
        void invalidSignatureCount_incrementsCorrectly() throws Exception {
            count("countInvalidSignature");
            count("countInvalidSignature");
            assertEquals(2, report.getInvalidSignatureCount());
        }

        @Test
        void invalidCount_startsAtZero() {
            assertEquals(0, report.getInvalidCount());
        }

        @Test
        void invalidCount_incrementsCorrectly() throws Exception {
            count("countInvalid");
            assertEquals(1, report.getInvalidCount());
        }

        @Test
        void errorCount_startsAtZero() {
            assertEquals(0, report.getErrorCount());
        }

        @Test
        void errorCount_incrementsCorrectly() throws Exception {
            count("countError");
            count("countError");
            assertEquals(2, report.getErrorCount());
        }

        @Test
        void getAllValidCount_sumsSigned_legacySigned_trusty_notTrusty() throws Exception {
            count("countSigned");
            count("countLegacySigned");
            count("countTrusty");
            count("countTrusty");
            count("countNotTrusty");
            assertEquals(5, report.getAllValidCount());
        }

        @Test
        void getAllValidCount_excludesInvalidAndError() throws Exception {
            count("countSigned");
            count("countInvalid");
            count("countError");
            assertEquals(1, report.getAllValidCount());
        }

        @Test
        void getAllInvalidCount_sumsInvalidSignature_invalid_error() throws Exception {
            count("countInvalidSignature");
            count("countInvalid");
            count("countError");
            assertEquals(3, report.getAllInvalidCount());
        }

        @Test
        void getAllCount_isValidPlusInvalid() throws Exception {
            count("countSigned");
            count("countTrusty");
            count("countInvalidSignature");
            count("countError");
            assertEquals(4, report.getAllCount());
        }

        @Test
        void getAllCount_zeroWhenNothingRecorded() {
            assertEquals(0, report.getAllCount());
        }

        // -----------------------------------------------------------------------
        // Boolean predicates
        // -----------------------------------------------------------------------

        @Test
        void areAllValid_trueWhenNoInvalids() throws Exception {
            count("countSigned");
            count("countTrusty");
            assertTrue(report.areAllValid());
        }

        @Test
        void areAllValid_trueWhenEmptyReport() {
            assertTrue(report.areAllValid());
        }

        @Test
        void areAllValid_falseWhenInvalidPresent() throws Exception {
            count("countSigned");
            count("countInvalid");
            assertFalse(report.areAllValid());
        }

        @Test
        void areAllValid_falseWhenErrorPresent() throws Exception {
            count("countError");
            assertFalse(report.areAllValid());
        }

        @Test
        void areAllValid_falseWhenInvalidSignaturePresent() throws Exception {
            count("countInvalidSignature");
            assertFalse(report.areAllValid());
        }

        @Test
        void areAllTrusty_trueWhenOnlyTrustySignedAndLegacySigned() throws Exception {
            count("countTrusty");
            count("countSigned");
            count("countLegacySigned");
            assertTrue(report.areAllTrusty());
        }

        @Test
        void areAllTrusty_falseWhenNotTrustyPresent() throws Exception {
            count("countTrusty");
            count("countNotTrusty");
            assertFalse(report.areAllTrusty());
        }

        @Test
        void areAllTrusty_falseWhenErrorPresent() throws Exception {
            count("countTrusty");
            count("countError");
            assertFalse(report.areAllTrusty());
        }

        @Test
        void areAllSigned_trueWhenOnlySignedAndLegacySigned() throws Exception {
            count("countSigned");
            count("countLegacySigned");
            assertTrue(report.areAllSigned());
        }

        @Test
        void areAllSigned_falseWhenTrustyWithoutSignaturePresent() throws Exception {
            count("countSigned");
            count("countTrusty");
            assertFalse(report.areAllSigned());
        }

        @Test
        void areAllSigned_falseWhenNotTrustyPresent() throws Exception {
            count("countSigned");
            count("countNotTrusty");
            assertFalse(report.areAllSigned());
        }

        // -----------------------------------------------------------------------
        // getSummary
        // -----------------------------------------------------------------------

        @Test
        void getSummary_emptyWhenNoCountsRecorded() {
            assertEquals("", report.getSummary());
        }

        @Test
        void getSummary_doesNotStartWithSpace() throws Exception {
            count("countSigned");
            assertFalse(report.getSummary().startsWith(" "));
        }

        @Test
        void getSummary_containsSignedEntry() throws Exception {
            count("countSigned");
            assertTrue(report.getSummary().contains("1 trusty with signature"));
        }

        @Test
        void getSummary_containsLegacySignedEntry() throws Exception {
            count("countLegacySigned");
            assertTrue(report.getSummary().contains("1 trusty with legacy signature"));
        }

        @Test
        void getSummary_containsTrustyWithoutSignatureEntry() throws Exception {
            count("countTrusty");
            assertTrue(report.getSummary().contains("1 trusty (without signature)"));
        }

        @Test
        void getSummary_containsNotTrustyEntry() throws Exception {
            count("countNotTrusty");
            assertTrue(report.getSummary().contains("1 valid (not trusty)"));
        }

        @Test
        void getSummary_containsInvalidSignatureEntry() throws Exception {
            count("countInvalidSignature");
            assertTrue(report.getSummary().contains("1 invalid signature"));
        }

        @Test
        void getSummary_containsInvalidNanopubEntry() throws Exception {
            count("countInvalid");
            assertTrue(report.getSummary().contains("1 invalid nanopubs"));
        }

        @Test
        void getSummary_containsErrorEntry() throws Exception {
            count("countError");
            assertTrue(report.getSummary().contains("1 errors"));
        }

        @Test
        void getSummary_multipleEntriesSeparatedBySemicolon() throws Exception {
            count("countSigned");
            count("countInvalid");
            assertTrue(report.getSummary().contains(";"));
        }

        @Test
        void getSummary_showsCorrectCountsForMultipleNanopubs() throws Exception {
            count("countSigned");
            count("countSigned");
            count("countTrusty");
            String summary = report.getSummary();
            assertTrue(summary.contains("2 trusty with signature"), summary);
            assertTrue(summary.contains("1 trusty (without signature)"), summary);
        }

        // -----------------------------------------------------------------------
        // Helper
        // -----------------------------------------------------------------------

        private void count(String methodName) throws Exception {
            var method = CheckNanopub.Report.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(report);
        }

    }

}
