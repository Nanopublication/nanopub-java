package org.nanopub;

import org.junit.jupiter.api.Test;
import org.nanopub.CheckNanopub.Report;

import java.io.File;

public class CheckValidNanopubsTest {

    @Test
    public void runTest() throws Exception {
        for (File testFile : new File(this.getClass().getResource("/testsuite/valid/plain/").getPath()).listFiles()) {
            testPlain(testFile.getName());
        }
        for (File testFile : new File(this.getClass().getResource("/testsuite/valid/trusty/").getPath()).listFiles()) {
            testTrusty(testFile.getName());
        }
        for (File testFile : new File(this.getClass().getResource("/testsuite/valid/signed/").getPath()).listFiles()) {
            testSigned(testFile.getName());
        }
    }

    public void testPlain(String filename) throws Exception {
        CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{this.getClass().getResource("/testsuite/valid/plain/").getPath() + filename});
        Report report = c.check();
        System.out.println(report.getSummary() + " " + filename);
        assert report.areAllValid();
    }

    public void testTrusty(String filename) throws Exception {
        CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{this.getClass().getResource("/testsuite/valid/trusty/").getPath() + filename});
        Report report = c.check();
        System.out.println(report.getSummary() + " " + filename);
        assert report.areAllTrusty();
    }

    public void testSigned(String filename) throws Exception {
        CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[]{this.getClass().getResource("/testsuite/valid/signed/" + filename).getPath()});
        Report report = c.check();
        System.out.println(report.getSummary() + " " + filename);
        assert report.areAllSigned();
    }

}
