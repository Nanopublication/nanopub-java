package org.nanopub;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.turtle.TurtleUtil;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Set;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

// Contains copied code from TurtleWriter, TrigWriter, and CustomTrigWriter

/**
 * An RDF writer that outputs RDF in HTML format, suitable for displaying.
 */
public class HtmlWriter extends TurtleWriter {

    private boolean inActiveContext;
    private Resource currentContext;
    private String openPart;
    private Set<String> usedPrefixes;
    private boolean indentContexts = true;

    /**
     * Creates a new HTML writer that will write to the specified output stream.
     *
     * @param out            the output stream to write to
     * @param indentContexts if true, contexts will be indented in the output
     */
    public HtmlWriter(OutputStream out, boolean indentContexts) {
        super(out);
        this.indentContexts = indentContexts;
    }

    /**
     * Creates a new HTML writer that will write to the specified output stream.
     *
     * @param out the output stream to write to
     */
    public HtmlWriter(OutputStream out) {
        super(out);
    }

    /**
     * Creates a new HTML writer that will write to the specified writer.
     *
     * @param writer         the writer to write to
     * @param indentContexts if true, contexts will be indented in the output
     */
    public HtmlWriter(Writer writer, boolean indentContexts) {
        super(writer);
        this.indentContexts = indentContexts;
    }

    /**
     * Creates a new HTML writer that will write to the specified writer.
     *
     * @param writer the writer to write to
     */
    public HtmlWriter(Writer writer) {
        super(writer);
    }

    /**
     * The RDF format for HTML output.
     */
    public static RDFFormat HTML_FORMAT = new RDFFormat("TriG HTML", "text/html", Charset.forName("UTF8"), "html", true, true, false);

    /**
     * {@inheritDoc}
     * <p>
     * Gets the RDF format that this writer supports.
     */
    @Override
    public RDFFormat getRDFFormat() {
        return HTML_FORMAT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Starts the RDF document writing process.
     */
    @Override
    public void startRDF()
            throws RDFHandlerException {
        super.startRDF();
        writer.setIndentationString("&nbsp;&nbsp;");

        inActiveContext = false;
        currentContext = null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Ends the RDF document writing process.
     */
    @Override
    public void endRDF()
            throws RDFHandlerException {
        super.endRDF();

        try {
            closeActiveContext();
            writer.flush();
        } catch (IOException e) {
            throw new RDFHandlerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handles a statement by writing it to the output in HTML format.
     */
    @Override
    public void handleStatement(Statement st)
            throws RDFHandlerException {
        if (!isWritingStarted()) {
            throw new RuntimeException("Document writing has not yet been started");
        }

        try {
            Resource context = st.getContext();

            if (inActiveContext && !contextsEquals(context, currentContext)) {
                closePreviousStatement();
                closeActiveContext();
            }

            if (!inActiveContext) {
                if (openPart != null) {
                    writer.write(openPart);
                    writer.writeEOL();
                    openPart = null;
                }

                writer.write("<span class=\"nanopub-context-switch\">");

                if (context != null) {
                    writeResource(context, false);
                    writer.write(" ");
                }

                writer.write("{<br/></span>");
                writer.writeEOL();
                if (indentContexts) writer.increaseIndentation();

                currentContext = context;
                inActiveContext = true;
            }
        } catch (IOException e) {
            throw new RDFHandlerException(e);
        }

        if (!isWritingStarted()) {
            throw new RuntimeException("Document writing has not yet been started");
        }

        Resource subj = st.getSubject();
        IRI pred = st.getPredicate();
        Value obj = st.getObject();

        try {
            if (subj.equals(lastWrittenSubject)) {
                if (pred.equals(lastWrittenPredicate)) {
                    // Identical subject and predicate
                    writer.write(" , ");
                } else {
                    // Identical subject, new predicate
                    writer.write(" ;<br/>");
                    writer.writeEOL();

                    // Write new predicate
                    writePredicate(pred);
                    writer.write(" ");
                    lastWrittenPredicate = pred;
                }
            } else {
                // New subject
                closePreviousStatement();

                // Write new subject:
                writeResource(subj, false);
                writer.write(" ");
                lastWrittenSubject = subj;

                // Write new predicate
                writePredicate(pred);
                writer.write(" ");
                lastWrittenPredicate = pred;

                statementClosed = false;
                writer.increaseIndentation();
            }

            writeValue(obj, false);

            // Don't close the line just yet. Maybe the next
            // statement has the same subject and/or predicate.
        } catch (IOException e) {
            throw new RDFHandlerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeCommentLine(String line)
            throws IOException {
        closeActiveContext();
        // We ignore comments:
        //super.writeCommentLine(line);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeNamespace(String prefix, String name)
            throws IOException {
        closeActiveContext();

        if (openPart != null) {
            writer.write(openPart);
            writer.writeEOL();
            openPart = null;
        }

        writer.write("@prefix ");
        writer.write(prefix);
        writer.write(": &lt;");
        String uriString = escapeHtml4(encodeURIString(name));
        writer.write("<a href=\"" + uriString + "\">");
        writer.write(uriString);
        writer.write("</a>");
        writer.write("&gt; .<br/>");
        writer.writeEOL();
    }

    private static String encodeURIString(String s) {
        return s.replace("\\", "\\u005C")
                .replace("\t", "\\u0009")
                .replace("\n", "\\u000A")
                .replace("\r", "\\u000D")
                .replace("\"", "\\u0022")
                .replace("`", "\\u0060")
                .replace("^", "\\u005E")
                .replace("|", "\\u007C")
                .replace("<", "\\u003C")
                .replace(">", "\\u003E")
                .replace(" ", "\\u0020");
    }

    /**
     * Closes the currently active context, if any.
     *
     * @throws java.io.IOException if an I/O error occurs while closing the context
     */
    protected void closeActiveContext()
            throws IOException {
        if (inActiveContext) {
            if (indentContexts) writer.decreaseIndentation();
            writer.write("<span class=\"nanopub-context-switch\">}<br/></span>");
            writer.writeEOL();

            inActiveContext = false;
            currentContext = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void writePredicate(IRI predicate)
            throws IOException {
        if (predicate.equals(RDF.TYPE)) {
            writer.write("<a href=\"" + RDF.TYPE + "\">a</a>");
        } else {
            writeURI(predicate);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void writeURI(IRI uri)
            throws IOException {
        String uriString = uri.toString();

        String prefix = namespaceTable.get(uriString);
        if (prefix != null) {
            // Exact match: no suffix required
            writer.write("<a href=\"" + escapeHtml4(uriString) + "\">");
            writer.write(prefix);
            writer.write(":");
            writer.write("</a>");
            if (usedPrefixes != null) {
                usedPrefixes.add(prefix);
            }
            return;
        }

        prefix = null;

        int splitIdx = TurtleUtil.findURISplitIndex(uriString);

        // Sesame bug for URIs that end with a period.
        // Port fix from https://bitbucket.org/openrdf/sesame/pull-request/301/ses-2086-fix-turtlewriter-writing/diff
        if (!TurtleUtil.isNameEndChar(uriString.charAt(uriString.length() - 1))) {
            splitIdx = -1;
        }

        if (splitIdx > 0) {
            String namespace = uriString.substring(0, splitIdx);
            prefix = namespaceTable.get(namespace);
        }

        // Do also split at dots:
        int splitIdxDot = uriString.lastIndexOf(".") + 1;
        if (uriString.length() == splitIdxDot) splitIdxDot = -1;
        if (splitIdx > 0 && splitIdxDot > splitIdx) {
            String namespace = uriString.substring(0, splitIdxDot);
            String p = namespaceTable.get(namespace);
            if (p != null) {
                splitIdx = splitIdxDot;
                prefix = p;
            }
        }

        // ... and colons:
        int splitIdxColon = uriString.lastIndexOf(":") + 1;
        if (uriString.length() == splitIdxColon) splitIdxColon = -1;
        if (splitIdx > 0 && splitIdxColon > splitIdx) {
            String namespace = uriString.substring(0, splitIdxColon);
            String p = namespaceTable.get(namespace);
            if (p != null) {
                splitIdx = splitIdxColon;
                prefix = p;
            }
        }

        // ... and underscores:
        int splitIdxUnderscore = uriString.lastIndexOf("_") + 1;
        if (uriString.length() == splitIdxUnderscore) splitIdxUnderscore = -1;
        if (splitIdx > 0 && splitIdxUnderscore > splitIdx) {
            String namespace = uriString.substring(0, splitIdxUnderscore);
            String p = namespaceTable.get(namespace);
            if (p != null) {
                splitIdx = splitIdxUnderscore;
                prefix = p;
            }
        }

        if (uriString.endsWith(".")) {
            prefix = null;
        }

        if (prefix != null) {
            // Namespace is mapped to a prefix; write abbreviated URI
            writer.write("<a href=\"" + escapeHtml4(uriString) + "\">");
            writer.write(prefix);
            writer.write(":");
            writer.write(escapeHtml4(uriString.substring(splitIdx)));
            writer.write("</a>");
        } else {
            // Write full URI
            writer.write("&lt;");
            writer.write("<a href=\"" + escapeHtml4(uriString) + "\">");
            writer.write(escapeHtml4(encodeURIString(uriString)));
            writer.write("</a>");
            writer.write("&gt;");
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void writeBNode(BNode bNode)
            throws IOException {
        throw new RuntimeException("Unexpected blank node");
    }

    /**
     * {@inheritDoc}
     */
    protected void writeLiteral(Literal lit)
            throws IOException {
        String label = lit.getLabel();
        IRI datatype = lit.getDatatype();

        if (getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT)) {
            if (XSD.INTEGER.equals(datatype) || XSD.DECIMAL.equals(datatype)
                    || XSD.DOUBLE.equals(datatype) || XSD.BOOLEAN.equals(datatype)) {
                try {
                    writer.write(escapeHtml4(XMLDatatypeUtil.normalize(label, datatype)));
                    return; // done
                } catch (IllegalArgumentException e) {
                    // not a valid numeric typed literal. ignore error and write as
                    // quoted string instead.
                }
            }
        }

        if (label.indexOf('\n') != -1 || label.indexOf('\r') != -1 || label.indexOf('\t') != -1) {
            // Write label as long string
            writer.write("\"\"\"");
            writer.write(escapeHtml4(TurtleUtil.encodeLongString(label)).replace("  ", "&nbsp; ").replace("  ", " &nbsp;").replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp").replace("\n", "<br/>"));
            writer.write("\"\"\"");
        } else {
            // Write label as normal string
            writer.write("\"");
            writer.write(escapeHtml4(TurtleUtil.encodeString(label)).replace("  ", "&nbsp; ").replace("  ", " &nbsp;"));
            writer.write("\"");
        }

        if (Literals.isLanguageLiteral(lit)) {
            // Append the literal's language
            writer.write("@");
            writer.write(lit.getLanguage().get());
        } else if (!XSD.STRING.equals(datatype) || !xsdStringToPlainLiteral()) {
            // Append the literal's datatype (possibly written as an abbreviated
            // URI)
            writer.write("^^");
            writeURI(datatype);
        }
    }

    /**
     * <p>closePreviousStatement.</p>
     *
     * @throws java.io.IOException if any.
     */
    protected void closePreviousStatement()
            throws IOException {
        if (!statementClosed) {
            // The previous statement still needs to be closed:
            writer.write(" . <br/>");
            writer.writeEOL();
            writer.decreaseIndentation();

            statementClosed = true;
            lastWrittenSubject = null;
            lastWrittenPredicate = null;
        }
    }

    /**
     * Starts a new part in the HTML output.
     *
     * @param partName the name of the part to start
     * @throws java.io.IOException if an I/O error occurs while starting the part
     */
    public void startPart(String partName)
            throws IOException {
        closePreviousStatement();
        closeActiveContext();
        if (openPart != null) {
            openPart += "\n<div class=\"" + partName + "\">";
        } else {
            openPart = "<div class=\"" + partName + "\">";
        }
    }

    /**
     * Ends the current part in the HTML output.
     *
     * @throws java.io.IOException if an I/O error occurs while ending the part
     */
    public void endPart()
            throws IOException {
        closePreviousStatement();
        closeActiveContext();
        writer.write("</div>");
        writer.writeEOL();
    }

    /**
     * Writes the start of an HTML document, including the necessary HTML tags and styles.
     *
     * @throws java.io.IOException if an I/O error occurs while writing the HTML start
     */
    public void writeHtmlStart()
            throws IOException {
        writenl("<!DOCTYPE html>");
        writenl("<html lang=\"en\">");
        writenl("<head>");
        writenl("<meta charset=\"utf-8\">");
        writenl("<title>Nanopublications</title>");
        writenl("<style>");
        writenl("body { margin: 20px; font-family: monaco,monospace; font-size: 11pt; color: #444; overflow-wrap: break-word; }");
        writenl("a { color: #000; text-decoration: none; }");
        writenl("a:hover { color: #666; }");
        writenl(".nanopub { margin: 0 0 30px 0; padding: 0px 10px 10px 10px; border-radius: 10px; border: solid; border-width: 1px; }");
        writenl(".nanopub-prefixes { margin-top: 10px; }");
        writenl(".nanopub-head { background: #e8e8e8; padding: 10px; margin-top: 10px; border-radius: 10px; }");
        writenl(".nanopub-assertion { background: #99ccff; padding: 10px; margin-top: 10px; border-radius: 10px; }");
        writenl(".nanopub-provenance { background: #f3a08c; padding: 10px; margin-top: 10px; border-radius: 10px; }");
        writenl(".nanopub-pubinfo { background: #ffff66; padding: 10px; margin-top: 10px; border-radius: 10px; }");
        writenl("</style>");
        writenl("</head>");
        writenl("<body>");
    }

    /**
     * Writes the end of an HTML document, closing all open tags.
     *
     * @throws java.io.IOException if an I/O error occurs while writing the HTML end
     */
    public void writeHtmlEnd()
            throws IOException {
        writenl("</body>");
        writenl("</html>");
    }

    private void writenl(String s)
            throws IOException {
        writer.write(s);
        writer.writeEOL();
    }

    private boolean xsdStringToPlainLiteral() {
        return getWriterConfig().get(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL);
    }

    private static final boolean contextsEquals(Resource context1, Resource context2) {
        if (context1 == null) {
            return context2 == null;
        } else {
            return context1.equals(context2);
        }
    }

}
