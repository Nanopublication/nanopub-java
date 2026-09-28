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

import static org.junit.jupiter.api.Assertions.assertFalse;

public class CheckInvalidNanopubsTest {

    @DisplayName("Plain invalid nanopubs")
    @ParameterizedTest(name = "plain invalid: {1}")
    @MethodSource("plainInvalid")
    public void testPlain(TestSuiteEntry testSuiteEntry, String name) {
        runAndAssert(testSuiteEntry, Report::areAllValid);
    }

    @DisplayName("Trusty invalid nanopubs")
    @ParameterizedTest(name = "trusty invalid: {1}")
    @MethodSource("trustyInvalid")
    public void testTrusty(TestSuiteEntry testSuiteEntry, String name) {
        runAndAssert(testSuiteEntry, Report::areAllTrusty);
    }

    @DisplayName("Signed invalid nanopubs")
    @ParameterizedTest(name = "signed invalid: {1}")
    @MethodSource("signedInvalid")
    public void testSigned(TestSuiteEntry testSuiteEntry, String name) {
        runAndAssert(testSuiteEntry, Report::areAllSigned);
    }

    static Stream<Arguments> plainInvalid() {
        return NanopubTestSuite.getLatest()
                .getInvalid(TestSuiteSubfolder.PLAIN)
                .stream()
                .map(e -> Arguments.of(e, e.getName()));
    }

    static Stream<Arguments> trustyInvalid() {
        return NanopubTestSuite.getLatest()
                .getInvalid(TestSuiteSubfolder.TRUSTY)
                .stream()
                .map(e -> Arguments.of(e, e.getName()));
    }

    static Stream<Arguments> signedInvalid() {
        return NanopubTestSuite.getLatest()
                .getInvalid(TestSuiteSubfolder.SIGNED)
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
        assertFalse(condition.test(report));
    }

}
