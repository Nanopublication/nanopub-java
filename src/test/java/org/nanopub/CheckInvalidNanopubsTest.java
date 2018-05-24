package org.nanopub;

import java.io.File;

import org.junit.Test;
import org.nanopub.CheckNanopub.Report;

public class CheckInvalidNanopubsTest {
 
	@Test
	public void runTest() throws Exception {
		File testSuiteValidDir = new File("src/main/resources/testsuite/invalid/");
		for (File testFile : testSuiteValidDir.listFiles()) {
			testInvalid(testFile.getName());
		}
	}

	public void testInvalid(String filename) throws Exception {
		Report report = null;
		try {
			CheckNanopub c = new CheckNanopub("src/main/resources/testsuite/invalid/" + filename);
			report = c.check();
			System.out.println(report.getSummary());
		} catch (Exception ex) {}
		assert report == null || !report.areAllValid();
	}

}
