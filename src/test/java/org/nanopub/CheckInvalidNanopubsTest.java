package org.nanopub;

import org.junit.jupiter.api.Test;
import org.nanopub.CheckNanopub.Report;

import java.io.File;

public class CheckInvalidNanopubsTest {

    @Test
    public void runTest() {
        for (File testFile : new File(this.getClass().getResource("/testsuite/invalid/plain/").getPath()).listFiles()) {
            testPlain(testFile.getName());
        }
        for (File testFile : new File(this.getClass().getResource("/testsuite/invalid/trusty/").getPath()).listFiles()) {
            testTrusty(testFile.getName());
        }
        for (File testFile : new File(this.getClass().getResource("/testsuite/invalid/signed/").getPath()).listFiles()) {
            testSigned(testFile.getName());
        }
    }

    public void testPlain(String filename) throws RuntimeException {
        Report report = null;
        try {
            CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{this.getClass().getResource("/testsuite/invalid/plain/" + filename).getPath()});
            report = c.check();
            System.out.println(report.getSummary());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        assert report == null || !report.areAllValid();
    }

    public void testTrusty(String filename) throws RuntimeException {
        Report report = null;
        try {
            CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{this.getClass().getResource("/testsuite/invalid/trusty/" + filename).getPath()});
            report = c.check();
            System.out.println(report.getSummary());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        assert report == null || !report.areAllTrusty();
    }

    public void testSigned(String filename) throws RuntimeException {
        Report report = null;
        try {
            CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{this.getClass().getResource("/testsuite/invalid/signed/" + filename).getPath()});
            report = c.check();
            System.out.println(report.getSummary());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        assert report == null || !report.areAllSigned();
    }

}
