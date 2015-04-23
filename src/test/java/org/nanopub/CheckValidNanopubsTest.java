package org.nanopub;

import java.io.File;

import org.junit.Test;
import org.nanopub.CheckNanopub.Report;

public class CheckValidNanopubsTest {
 
	@Test
	public void runTest() throws Exception {
		File testSuiteValidDir = new File("src/main/resources/testsuite/valid/");
		for (File testFile : testSuiteValidDir.listFiles()) {
			testValid(testFile.getName());
		}
	}

	public void testValid(String filename) throws Exception {
		CheckNanopub c = new CheckNanopub("src/main/resources/testsuite/valid/" + filename);
		Report report = c.check();
		System.out.println(report.getSummary());
		assert report.areAllValid();
	}

}
