package org.nanopub;

import org.junit.jupiter.api.Test;
import org.nanopub.CheckNanopub.Report;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteEntry;
import org.nanopub.testsuite.TestSuiteSubfolder;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CheckValidNanopubsTest {

    @Test
    public void runTest() throws Exception {
        for (TestSuiteEntry testFile : NanopubTestSuite.getLatest().getValid(TestSuiteSubfolder.PLAIN)) {
            testPlain(testFile);
        }
        for (TestSuiteEntry testFile : NanopubTestSuite.getLatest().getValid(TestSuiteSubfolder.TRUSTY)) {
            testTrusty(testFile);
        }
        for (TestSuiteEntry testFile : NanopubTestSuite.getLatest().getValid(TestSuiteSubfolder.SIGNED)) {
            testSigned(testFile);
        }
    }

    public void testPlain(TestSuiteEntry testSuiteEntry) throws Exception {
        CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{testSuiteEntry.toFile().getPath()});
        Report report = c.check();
        System.out.println(report.getSummary() + " " + testSuiteEntry);
        assertTrue(report.areAllValid());
    }

    public void testTrusty(TestSuiteEntry testSuiteEntry) throws Exception {
        CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{testSuiteEntry.toFile().getPath()});
        Report report = c.check();
        System.out.println(report.getSummary() + " " + testSuiteEntry);
        assertTrue(report.areAllTrusty());
    }

    public void testSigned(TestSuiteEntry testSuiteEntry) throws Exception {
        CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{testSuiteEntry.toFile().getPath()});
        Report report = c.check();
        System.out.println(report.getSummary() + " " + testSuiteEntry);
        assert report.areAllSigned();
        assertTrue(report.areAllSigned());
    }

}
