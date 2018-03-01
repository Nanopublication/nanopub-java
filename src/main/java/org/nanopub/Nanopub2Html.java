package org.nanopub;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandlerException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

// TODO: Work in progress...

public class Nanopub2Html {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<String> inputNanopubs = new ArrayList<String>();

	@com.beust.jcommander.Parameter(names = "-s", description = "Stand-alone HTML")
	private boolean standalone = false;

	public static void main(String[] args) {
		NanopubImpl.ensureLoaded();
		Nanopub2Html obj = new Nanopub2Html();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		try {
			obj.generateHtml();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public void generateHtml() throws IOException {
		for (String s : inputNanopubs) {
			try {
				MultiNanopubRdfHandler.process(new File(s), new NanopubHandler() {
					@Override
					public void handleNanopub(Nanopub np) {
						createHtml(np);
					}
				});
			} catch (OpenRDFException | MalformedNanopubException ex) {
				ex.printStackTrace();
			}
		}
	}

	private void createHtml(Nanopub np) {
		try {
			createHtml(np, System.out);
		} catch (IOException | RDFHandlerException ex) {
			ex.printStackTrace();
		}
	}

	public void createHtml(Nanopub np, PrintStream htmlOut) throws IOException, RDFHandlerException {
		HtmlWriter htmlWriter = new HtmlWriter(htmlOut);
		if (np instanceof NanopubWithNs) {
			NanopubWithNs npNs = (NanopubWithNs) np;
			for (String prefix : npNs.getNsPrefixes()) {
				htmlWriter.handleNamespace(prefix, npNs.getNamespace(prefix));
			}
		}
		if (standalone) {
			htmlWriter.writeHtmlStart();
		}
		htmlWriter.startPart("nanopub");
		htmlWriter.startPart("nanopub-prefixes");
		htmlWriter.startRDF();
		htmlWriter.endPart();
		htmlWriter.startPart("nanopub-head");
		for (Statement st : np.getHead()) {
			htmlWriter.handleStatement(st);
		}
		htmlWriter.endPart();
		htmlWriter.startPart("nanopub-assertion");
		for (Statement st : np.getAssertion()) {
			htmlWriter.handleStatement(st);
		}
		htmlWriter.endPart();
		htmlWriter.startPart("nanopub-provenance");
		for (Statement st : np.getProvenance()) {
			htmlWriter.handleStatement(st);
		}
		htmlWriter.endPart();
		htmlWriter.startPart("nanopub-pubinfo");
		for (Statement st : np.getPubinfo()) {
			htmlWriter.handleStatement(st);
		}
		htmlWriter.endPart();
		htmlWriter.endPart();
		htmlWriter.endRDF();
		if (standalone) {
			htmlWriter.writeHtmlEnd();
		}
	}

}
