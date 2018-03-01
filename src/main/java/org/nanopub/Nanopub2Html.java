package org.nanopub;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
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

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File outputFile;

	private OutputStream outputStream = System.out;

	private static final Charset utf8Charset = Charset.forName("UTF8");

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
			obj.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private Nanopub2Html() {
	}

	private Nanopub2Html(OutputStream outputStream, boolean standalone) {
		this.outputStream = outputStream;
		this.standalone = standalone;
	}


	private void run() throws IOException {
		if (outputFile != null) {
			outputStream = new FileOutputStream(outputFile);
		}

		for (String s : inputNanopubs) {
			try {
				MultiNanopubRdfHandler.process(new File(s), new NanopubHandler() {
					@Override
					public void handleNanopub(Nanopub np) {
						try {
							createHtml(np);
						} catch (IOException | RDFHandlerException ex) {
							ex.printStackTrace();
						}
					}
				});
			} catch (OpenRDFException | MalformedNanopubException ex) {
				ex.printStackTrace();
			}
		}
		outputStream.flush();
		if (outputStream != System.out) {
			outputStream.close();
		}
	}

	private void createHtml(Nanopub np) throws IOException, RDFHandlerException {
		PrintStream printHtml;
		if (outputStream instanceof PrintStream) {
			printHtml = (PrintStream) outputStream;
		} else {
			printHtml = new PrintStream(outputStream);
		}
		HtmlWriter htmlWriter = new HtmlWriter(printHtml);
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

	public static void createHtml(Collection<Nanopub> nanopubs, OutputStream htmlOut, boolean standalone) {
		Nanopub2Html nanopub2html = new Nanopub2Html(htmlOut, standalone);
		try {
			for (Nanopub np : nanopubs) {
				nanopub2html.createHtml(np);
			}
		} catch (IOException | RDFHandlerException ex) {
			ex.printStackTrace();
		}
	}

	public static void createHtml(Nanopub np, OutputStream htmlOut, boolean standalone) {
		Nanopub2Html nanopub2html = new Nanopub2Html(htmlOut, standalone);
		try {
			nanopub2html.createHtml(np);
		} catch (IOException | RDFHandlerException ex) {
			ex.printStackTrace();
		}
	}

	public static String createHtmlString(Collection<Nanopub> nanopubs, boolean standalone) {
		ByteArrayOutputStream htmlOut = null;
		try {
			htmlOut = new ByteArrayOutputStream();
			createHtml(nanopubs, htmlOut, standalone);
			htmlOut.flush();
			htmlOut.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return new String(htmlOut.toByteArray(), utf8Charset);
	}

	public static String createHtmlString(Nanopub np, boolean standalone) {
		ByteArrayOutputStream htmlOut = null;
		try {
			htmlOut = new ByteArrayOutputStream();
			createHtml(np, htmlOut, standalone);
			htmlOut.flush();
			htmlOut.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return new String(htmlOut.toByteArray(), utf8Charset);
	}

}
