package org.nanopub;

import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandlerException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class provides methods to convert nanopublications to HTML format.
 */
public class Nanopub2Html extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
    private List<String> inputNanopubs = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-s", description = "Stand-alone HTML")
    private boolean standalone = false;

    @com.beust.jcommander.Parameter(names = "-i", description = "No context indentation")
    private boolean indentContextDisabled = false;

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File outputFile;

    private OutputStream outputStream = System.out;

    private static final Charset utf8Charset = StandardCharsets.UTF_8;

    /**
     * Main method to run the Nanopub2Html command line tool.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Nanopub2Html obj = CliRunner.initJc(new Nanopub2Html(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Default constructor for Nanopub2Html.
     */
    public Nanopub2Html() {
    }

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
                    MultiNanopubRdfHandler.process(new File(s), np -> {
                        try {
                            createHtml(np);
                        } catch (IOException | RDFHandlerException ex) {
                            ex.printStackTrace();
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
            htmlWriter.startRDF();
            if (np instanceof NanopubWithNs npNs) {
                for (String prefix : npNs.getNsPrefixes()) {
                    htmlWriter.handleNamespace(prefix, npNs.getNamespace(prefix));
                }
            }
            if (standalone) {
                htmlWriter.writeHtmlStart();
            }
            htmlWriter.startPart("nanopub");
            htmlWriter.startPart("nanopub-prefixes");
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

    /**
     * Creates HTML output for a collection of nanopublications and writes it to the specified output stream.
     *
     * @param nanopubs      the collection of nanopublications to convert
     * @param htmlOut       the output stream to write the HTML to
     * @param standalone    if true, the HTML will be a standalone document with header and footer; if false, it will be a fragment
     * @param indentContext if true, the context will be indented in the HTML output
     */
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

    /**
     * Creates HTML output for a collection of nanopublications and writes it to the specified output stream.
     *
     * @param nanopubs   the collection of nanopublications to convert
     * @param htmlOut    the output stream to write the HTML to
     * @param standalone if true, the HTML will be a standalone document with header and footer; if false, it will be a fragment
     */
    public static void createHtml(Collection<Nanopub> nanopubs, OutputStream htmlOut, boolean standalone) {
        createHtml(nanopubs, htmlOut, standalone, true);
    }


    /**
     * Creates HTML output for a single nanopublication and writes it to the specified output stream.
     *
     * @param np            the nanopublication to convert
     * @param htmlOut       the output stream to write the HTML to
     * @param standalone    if true, the HTML will be a standalone document with header and footer; if false, it will be a fragment
     * @param indentContext if true, the context will be indented in the HTML output
     */
    public static void createHtml(Nanopub np, OutputStream htmlOut, boolean standalone, boolean indentContext) {
        Nanopub2Html nanopub2html = new Nanopub2Html(htmlOut, standalone, indentContext);
        try {
            nanopub2html.createHtml(np);
        } catch (IOException | RDFHandlerException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Creates HTML output for a single nanopublication and writes it to the specified output stream.
     *
     * @param np         the nanopublication to convert
     * @param htmlOut    the output stream to write the HTML to
     * @param standalone if true, the HTML will be a standalone document with header and footer; if false, it will be a fragment
     */
    public static void createHtml(Nanopub np, OutputStream htmlOut, boolean standalone) {
        createHtml(np, htmlOut, standalone, true);
    }

    /**
     * Creates an HTML string representation of a collection of nanopublications.
     *
     * @param nanopubs      the collection of nanopublications to convert
     * @param standalone    if true, the HTML will be a standalone document with header and footer; if false, it will be a fragment
     * @param indentContext if true, the context will be indented in the HTML output
     * @return the HTML string representation of the nanopublications
     */
    public static String createHtmlString(Collection<Nanopub> nanopubs, boolean standalone, boolean indentContext) {
        try (ByteArrayOutputStream htmlOut = new ByteArrayOutputStream()) {
            createHtml(nanopubs, htmlOut, standalone, indentContext);
            htmlOut.flush();
            return htmlOut.toString(utf8Charset);
        } catch (IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /**
     * Creates an HTML string representation of a collection of nanopublications.
     *
     * @param nanopubs   the collection of nanopublications to convert
     * @param standalone if true, the HTML will be a standalone document with header and footer; if false, it will be a fragment
     * @return the HTML string representation of the nanopublications
     */
    public static String createHtmlString(Collection<Nanopub> nanopubs, boolean standalone) {
        return createHtmlString(nanopubs, standalone, true);
    }

    /**
     * Creates an HTML string representation of a single nanopublication.
     *
     * @param np            the nanopublication to convert
     * @param standalone    if true, the HTML will be a standalone document with header and footer; if false, it will be a fragment
     * @param indentContext if true, the context will be indented in the HTML output
     * @return the HTML string representation of the nanopublication
     */
    public static String createHtmlString(Nanopub np, boolean standalone, boolean indentContext) {
        try (ByteArrayOutputStream htmlOut = new ByteArrayOutputStream()) {
            createHtml(np, htmlOut, standalone, indentContext);
            htmlOut.flush();
            return htmlOut.toString(utf8Charset);
        } catch (IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /**
     * Creates an HTML string representation of a single nanopublication.
     *
     * @param np         the nanopublication to convert
     * @param standalone if true, the HTML will be a standalone document with header and footer; if false, it will be a fragment
     * @return the HTML string representation of the nanopublication
     */
    public static String createHtmlString(Nanopub np, boolean standalone) {
        return createHtmlString(np, standalone, true);
    }

}
