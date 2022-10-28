package org.nanopub;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.turtle.TurtleUtil;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

// Contains copied code from TurtleWriter, TrigWriter, and CustomTrigWriter

public class HtmlWriter extends TurtleWriter {

	private boolean inActiveContext;
	private Resource currentContext;
	private String openPart;
	private Set<String> usedPrefixes;
	private boolean indentContexts = true;

	public HtmlWriter(OutputStream out, boolean indentContexts) {
		super(out);
		this.indentContexts = indentContexts;
	}

	public HtmlWriter(OutputStream out) {
		super(out);
	}

	public HtmlWriter(Writer writer, boolean indentContexts) {
		super(writer);
		this.indentContexts = indentContexts;
	}

	public HtmlWriter(Writer writer) {
		super(writer);
	}

	public static RDFFormat HTML_FORMAT = new RDFFormat("TriG HTML", "text/html", Charset.forName("UTF8"), "html", true, true);

	@Override
	public RDFFormat getRDFFormat()
	{
		return HTML_FORMAT;
	}

	@Override
	public void startRDF()
		throws RDFHandlerException
	{
		super.startRDF();
		writer.setIndentationString("&nbsp;&nbsp;");

		inActiveContext = false;
		currentContext = null;
	}

	@Override
	public void endRDF()
		throws RDFHandlerException
	{
		super.endRDF();

		try {
			closeActiveContext();
			writer.flush();
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		if (!writingStarted) {
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
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}

		if (!writingStarted) {
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
				}
				else {
					// Identical subject, new predicate
					writer.write(" ;<br/>");
					writer.writeEOL();

					// Write new predicate
					writePredicate(pred);
					writer.write(" ");
					lastWrittenPredicate = pred;
				}
			}
			else {
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
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	protected void writeCommentLine(String line)
		throws IOException
	{
		closeActiveContext();
		// We ignore comments:
		//super.writeCommentLine(line);
	}

	@Override
	protected void writeNamespace(String prefix, String name)
		throws IOException
	{
		closeActiveContext();

		if (openPart != null) {
			writer.write(openPart);
			writer.writeEOL();
			openPart = null;
		}

		writer.write("@prefix ");
		writer.write(prefix);
		writer.write(": &lt;");
		String uriString = escapeHtml4(TurtleUtil.encodeURIString(name));
		writer.write("<a href=\"" + uriString + "\">");
		writer.write(uriString);
		writer.write("</a>");
		writer.write("&gt; .<br/>");
		writer.writeEOL();
	}

	protected void closeActiveContext()
		throws IOException
	{
		if (inActiveContext) {
			if (indentContexts) writer.decreaseIndentation();
			writer.write("<span class=\"nanopub-context-switch\">}<br/></span>");
			writer.writeEOL();

			inActiveContext = false;
			currentContext = null;
		}
	}

	protected void writePredicate(IRI predicate)
		throws IOException
	{
		if (predicate.equals(RDF.TYPE)) {
			// Write short-cut for rdf:type
			writer.write("<a href=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\">a</a>");
		}
		else {
			writeURI(predicate);
		}
	}

	protected void writeURI(IRI uri)
		throws IOException
	{
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
		}
		else {
			// Write full URI
			writer.write("&lt;");
			writer.write("<a href=\"" + escapeHtml4(uriString) + "\">");
			writer.write(escapeHtml4(TurtleUtil.encodeURIString(uriString)));
			writer.write("</a>");
			writer.write("&gt;");
		}
	}

	protected void writeBNode(BNode bNode)
		throws IOException
	{
		throw new RuntimeException("Unexpected blank node");
	}

	protected void writeLiteral(Literal lit)
		throws IOException
	{
		String label = lit.getLabel();
		IRI datatype = lit.getDatatype();

		if (getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT)) {
			if (XMLSchema.INTEGER.equals(datatype) || XMLSchema.DECIMAL.equals(datatype)
					|| XMLSchema.DOUBLE.equals(datatype) || XMLSchema.BOOLEAN.equals(datatype))
			{
				try {
					writer.write(escapeHtml4(XMLDatatypeUtil.normalize(label, datatype)));
					return; // done
				}
				catch (IllegalArgumentException e) {
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
		}
		else {
			// Write label as normal string
			writer.write("\"");
			writer.write(escapeHtml4(TurtleUtil.encodeString(label)).replace("  ", "&nbsp; ").replace("  ", " &nbsp;"));
			writer.write("\"");
		}

		if (Literals.isLanguageLiteral(lit)) {
			// Append the literal's language
			writer.write("@");
			writer.write(lit.getLanguage().get());
		}
		else if (!XMLSchema.STRING.equals(datatype) || !xsdStringToPlainLiteral()) {
			// Append the literal's datatype (possibly written as an abbreviated
			// URI)
			writer.write("^^");
			writeURI(datatype);
		}
	}

	protected void closePreviousStatement()
		throws IOException
	{
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

	public void startPart(String partName)
		throws IOException
	{
		closePreviousStatement();
		closeActiveContext();
		if (openPart != null) {
			openPart += "\n<div class=\"" + partName + "\">";
		} else {
			openPart = "<div class=\"" + partName + "\">";
		}
	}

	public void endPart()
		throws IOException
	{
		closePreviousStatement();
		closeActiveContext();
		writer.write("</div>");
		writer.writeEOL();
	}

	public void writeHtmlStart()
		throws IOException
	{
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

	public void writeHtmlEnd()
		throws IOException
	{
		writenl("</body>");
		writenl("</html>");
	}

	private void writenl(String s)
		throws IOException
	{
		writer.write(s);
		writer.writeEOL();
	}

	private boolean xsdStringToPlainLiteral() {
		return getWriterConfig().get(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL);
	}

	private static final boolean contextsEquals(Resource context1, Resource context2) {
		if (context1 == null) {
			return context2 == null;
		}
		else {
			return context1.equals(context2);
		}
	}

}
