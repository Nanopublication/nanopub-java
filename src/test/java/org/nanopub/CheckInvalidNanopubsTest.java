package org.nanopub;

import org.junit.jupiter.api.Test;
import org.nanopub.CheckNanopub.Report;

import java.io.File;

public class CheckInvalidNanopubsTest {
 
	@Test
	public void runTest() throws Exception {
		for (File testFile : new File("src/main/resources/testsuite/invalid/plain/").listFiles()) {
			testPlain(testFile.getName());
		}
		for (File testFile : new File("src/main/resources/testsuite/invalid/trusty/").listFiles()) {
			testTrusty(testFile.getName());
		}
		for (File testFile : new File("src/main/resources/testsuite/invalid/signed/").listFiles()) {
			testSigned(testFile.getName());
		}
	}

	public void testPlain(String filename) throws Exception {
		Report report = null;
		try {
			CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[] {"src/main/resources/testsuite/invalid/plain/" + filename});
			report = c.check();
			System.out.println(report.getSummary());
		} catch (Exception ex) {}
		assert report == null || !report.areAllValid();
	}

	public void testTrusty(String filename) throws Exception {
		Report report = null;
		try {
			CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[] {"src/main/resources/testsuite/invalid/trusty/" + filename});
			report = c.check();
			System.out.println(report.getSummary());
		} catch (Exception ex) {}
		assert report == null || !report.areAllTrusty();
	}

	public void testSigned(String filename) throws Exception {
		Report report = null;
		try {
			CheckNanopub c = CliRunner.initJc(new CheckNanopub(), new String[] {"src/main/resources/testsuite/invalid/signed/" + filename});
			report = c.check();
			System.out.println(report.getSummary());
		} catch (Exception ex) {}
		assert report == null || !report.areAllSigned();
	}

}
