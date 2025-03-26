package org.nanopub;

import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class Nanopub2Html extends CliRunner {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<String> inputNanopubs = new ArrayList<String>();

	@com.beust.jcommander.Parameter(names = "-s", description = "Stand-alone HTML")
	private boolean standalone = false;

	@com.beust.jcommander.Parameter(names = "-i", description = "No context indentation")
	private boolean indentContextDisabled = false;

	@com.beust.jcommander.Parameter(names = "-o", description = "Output file")
	private File outputFile;

	private OutputStream outputStream = System.out;

	private static final Charset utf8Charset = Charset.forName("UTF8");

	public static void main(String[] args) {
		try {
			Nanopub2Html obj = Run.initJc(new Nanopub2Html(), args);
			obj.run();
		} catch (ParameterException ex) {
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public Nanopub2Html() {}

	private Nanopub2Html(OutputStream outputStream, boolean standalone, boolean indentContext) {
		this.outputStream = outputStream;
		this.standalone = standalone;
		this.indentContextDisabled = !indentContext;
	}


	private void run() throws IOException {
		if (outputFile != null) {
			outputStream = new FileOutputStream(outputFile);
		}

		try {
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
				} catch (RDF4JException | MalformedNanopubException ex) {
					ex.printStackTrace();
				}
			}
			outputStream.flush();
		} finally {
			if (outputStream != System.out) {
				outputStream.close();
			}
		}
	}

	private void createHtml(Nanopub np) throws IOException, RDFHandlerException {
		PrintStream printHtml;
		if (outputStream instanceof PrintStream) {
			printHtml = (PrintStream) outputStream;
		} else {
			printHtml = new PrintStream(outputStream);
		}
		try (printHtml) {
			HtmlWriter htmlWriter = new HtmlWriter(printHtml, !indentContextDisabled);
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

	public static void createHtml(Collection<Nanopub> nanopubs, OutputStream htmlOut, boolean standalone, boolean indentContext) {
		Nanopub2Html nanopub2html = new Nanopub2Html(htmlOut, standalone, indentContext);
		try {
			for (Nanopub np : nanopubs) {
				nanopub2html.createHtml(np);
			}
		} catch (IOException | RDFHandlerException ex) {
			ex.printStackTrace();
		}
	}

	public static void createHtml(Collection<Nanopub> nanopubs, OutputStream htmlOut, boolean standalone) {
		createHtml(nanopubs, htmlOut, standalone, true);
	}

	public static void createHtml(Nanopub np, OutputStream htmlOut, boolean standalone, boolean indentContext) {
		Nanopub2Html nanopub2html = new Nanopub2Html(htmlOut, standalone, indentContext);
		try {
			nanopub2html.createHtml(np);
		} catch (IOException | RDFHandlerException ex) {
			ex.printStackTrace();
		}
	}

	public static void createHtml(Nanopub np, OutputStream htmlOut, boolean standalone) {
		createHtml(np, htmlOut, standalone, true);
	}

	public static String createHtmlString(Collection<Nanopub> nanopubs, boolean standalone, boolean indentContext) {
		try (ByteArrayOutputStream htmlOut = new ByteArrayOutputStream()) {
			createHtml(nanopubs, htmlOut, standalone, indentContext);
			htmlOut.flush();
			return new String(htmlOut.toByteArray(), utf8Charset);
		} catch (IOException ex) {
			ex.printStackTrace();
			return "";
		}
	}

	public static String createHtmlString(Collection<Nanopub> nanopubs, boolean standalone) {
		return createHtmlString(nanopubs, standalone, true);
	}

	public static String createHtmlString(Nanopub np, boolean standalone, boolean indentContext) {
		try (ByteArrayOutputStream htmlOut = new ByteArrayOutputStream()) {
			createHtml(np, htmlOut, standalone, indentContext);
			htmlOut.flush();
			return new String(htmlOut.toByteArray(), utf8Charset);
		} catch (IOException ex) {
			ex.printStackTrace();
			return "";
		}
	}

	public static String createHtmlString(Nanopub np, boolean standalone) {
		return createHtmlString(np, standalone, true);
	}

}
