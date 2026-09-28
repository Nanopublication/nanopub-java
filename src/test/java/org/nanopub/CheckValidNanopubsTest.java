package org.nanopub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.nanopub.CheckNanopub.Report;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteEntry;
import org.nanopub.testsuite.TestSuiteSubfolder;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CheckValidNanopubsTest {

    @DisplayName("Plain valid nanopubs")
    @ParameterizedTest(name = "plain valid: {1}")
    @MethodSource("plainValid")
    public void testPlain(TestSuiteEntry testSuiteEntry, String name) {
        runAndAssert(testSuiteEntry, Report::areAllValid);
    }

    @DisplayName("Trusty valid nanopubs")
    @ParameterizedTest(name = "trusty valid: {1}")
    @MethodSource("trustyValid")
    public void testTrusty(TestSuiteEntry testSuiteEntry, String name) {
        runAndAssert(testSuiteEntry, Report::areAllTrusty);
    }

    @DisplayName("Signed valid nanopubs")
    @ParameterizedTest(name = "signed valid: {1}")
    @MethodSource("signedValid")
    public void testSigned(TestSuiteEntry testSuiteEntry, String name) {
        runAndAssert(testSuiteEntry, Report::areAllSigned);
    }

    static Stream<Arguments> plainValid() {
        return NanopubTestSuite.getLatest()
                .getValid(TestSuiteSubfolder.PLAIN)
                .stream()
                .map(e -> Arguments.of(e, e.getName()));
    }

    static Stream<Arguments> trustyValid() {
        return NanopubTestSuite.getLatest()
                .getValid(TestSuiteSubfolder.TRUSTY)
                .stream()
                .map(e -> Arguments.of(e, e.getName()));
    }

    static Stream<Arguments> signedValid() {
        return NanopubTestSuite.getLatest()
                .getValid(TestSuiteSubfolder.SIGNED)
                .stream()
                .map(e -> Arguments.of(e, e.getName()));
    }

    private void runAndAssert(TestSuiteEntry testSuiteEntry, Predicate<Report> condition) {
        Report report;
        try {
            CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{testSuiteEntry.toFile().getPath()});
            report = c.check();
            System.out.println(report.getSummary());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        assertTrue(condition.test(report));
    }

}
