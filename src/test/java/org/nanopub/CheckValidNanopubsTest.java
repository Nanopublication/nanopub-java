package org.nanopub;

import org.junit.Test;
import org.nanopub.CheckNanopub.Report;

import java.io.File;

public class CheckValidNanopubsTest {
 
	@Test
	public void runTest() throws Exception {
		for (File testFile : new File("src/main/resources/testsuite/valid/plain/").listFiles()) {
			testPlain(testFile.getName());
		}
		for (File testFile : new File("src/main/resources/testsuite/valid/trusty/").listFiles()) {
			testTrusty(testFile.getName());
		}
		for (File testFile : new File("src/main/resources/testsuite/valid/signed/").listFiles()) {
			testSigned(testFile.getName());
		}
	}

	public void testPlain(String filename) throws Exception {
		CheckNanopub c = Run.initJc(new CheckNanopub(), new String[] {"src/main/resources/testsuite/valid/plain/" + filename});
		Report report = c.check();
		System.out.println(report.getSummary());
		assert report.areAllValid();
	}

	public void testTrusty(String filename) throws Exception {
		CheckNanopub c = Run.initJc(new CheckNanopub(), new String[] {"src/main/resources/testsuite/valid/trusty/" + filename});
		Report report = c.check();
		System.out.println(report.getSummary());
		assert report.areAllTrusty();
	}

	public void testSigned(String filename) throws Exception {
		CheckNanopub c = Run.initJc(new CheckNanopub(), new String[] {"src/main/resources/testsuite/valid/signed/" + filename});
		Report report = c.check();
		System.out.println(report.getSummary());
		assert report.areAllSigned();
	}

}
