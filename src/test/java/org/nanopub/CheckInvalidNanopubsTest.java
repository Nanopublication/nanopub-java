package org.nanopub;

import org.junit.jupiter.api.Test;
import org.nanopub.CheckNanopub.Report;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteEntry;
import org.nanopub.testsuite.TestSuiteSubfolder;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class CheckInvalidNanopubsTest {

    @Test
    public void runTest() throws Exception {
        for (TestSuiteEntry testFile : NanopubTestSuite.getLatest().getInvalid(TestSuiteSubfolder.PLAIN)) {
            testPlain(testFile);
        }
        for (TestSuiteEntry testFile : NanopubTestSuite.getLatest().getInvalid(TestSuiteSubfolder.TRUSTY)) {
            testTrusty(testFile);
        }
        for (TestSuiteEntry testFile : NanopubTestSuite.getLatest().getInvalid(TestSuiteSubfolder.SIGNED)) {
            testSigned(testFile);
        }
    }


    public void testPlain(TestSuiteEntry testSuiteEntry) throws RuntimeException {
        Report report;
        try {
            CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{testSuiteEntry.toFile().getPath()});
            report = c.check();
            System.out.println(report.getSummary());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        assertFalse(report.areAllValid());
    }

    public void testTrusty(TestSuiteEntry testSuiteEntry) throws RuntimeException {
        Report report;
        try {
            CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{testSuiteEntry.toFile().getPath()});
            report = c.check();
            System.out.println(report.getSummary());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        assertFalse(report.areAllTrusty());
    }

    public void testSigned(TestSuiteEntry testSuiteEntry) throws RuntimeException {
        Report report;
        try {
            CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{testSuiteEntry.toFile().getPath()});
            report = c.check();
            System.out.println(report.getSummary());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        assertFalse(report.areAllSigned());
    }

}
