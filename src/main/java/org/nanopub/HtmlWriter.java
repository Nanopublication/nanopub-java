package org.nanopub;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.util.Literals;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.BasicWriterSettings;
import org.openrdf.rio.turtle.TurtleUtil;
import org.openrdf.rio.turtle.TurtleWriter;

// Contains copied code from TurtleWriter and TrigWriter

public class HtmlWriter extends TurtleWriter {

	private boolean inActiveContext;

	private Resource currentContext;

	private String openPart;

	public HtmlWriter(OutputStream out) {
		super(out);
		init();
	}

	public HtmlWriter(Writer writer) {
		super(writer);
		init();
	}

	public static RDFFormat HTML_FORMAT = new RDFFormat("TriG HTML", "text/html", Charset.forName("UTF8"), "html", true, true);

	private void init() {
		writer.setIndentationString("&nbsp;&nbsp;");
	}

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
				writer.write("<br/>");
				writer.writeEOL();

				if (openPart != null) {
					writer.write(openPart);
					writer.writeEOL();
					openPart = null;
				}

				if (context != null) {
					writeResource(context);
					writer.write(" ");
				}

				writer.write("{<br/>");
				writer.writeEOL();
				writer.increaseIndentation();

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
		URI pred = st.getPredicate();
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
				writeResource(subj);
				writer.write(" ");
				lastWrittenSubject = subj;

				// Write new predicate
				writePredicate(pred);
				writer.write(" ");
				lastWrittenPredicate = pred;

				statementClosed = false;
				writer.increaseIndentation();
			}

			writeValue(obj);

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
		String uriString = TurtleUtil.encodeURIString(name);
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
			writer.decreaseIndentation();
			writer.write("}<br/>");
			writer.writeEOL();

			inActiveContext = false;
			currentContext = null;
		}
	}

	protected void writePredicate(URI predicate)
		throws IOException
	{
		if (predicate.equals(RDF.TYPE)) {
			// Write short-cut for rdf:type
			writer.write("a");
		}
		else {
			writeURI(predicate);
		}
	}

	protected void writeURI(URI uri)
		throws IOException
	{
		String uriString = uri.toString();

		// Try to find a prefix for the URI's namespace
		String prefix = null;

		int splitIdx = TurtleUtil.findURISplitIndex(uriString);
		if (splitIdx > 0) {
			String namespace = uriString.substring(0, splitIdx);
			prefix = namespaceTable.get(namespace);
		}

		if (prefix != null) {
			// Namespace is mapped to a prefix; write abbreviated URI
			writer.write("<a href=\"" + uriString + "\">");
			writer.write(prefix);
			writer.write(":");
			writer.write(uriString.substring(splitIdx));
			writer.write("</a>");
		}
		else {
			// Write full URI
			writer.write("&lt;");
			writer.write("<a href=\"" + uriString + "\">");
			writer.write(TurtleUtil.encodeURIString(uriString));
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
		URI datatype = lit.getDatatype();

		if (getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT)) {
			if (XMLSchema.INTEGER.equals(datatype) || XMLSchema.DECIMAL.equals(datatype)
					|| XMLSchema.DOUBLE.equals(datatype) || XMLSchema.BOOLEAN.equals(datatype))
			{
				try {
					writer.write(XMLDatatypeUtil.normalize(label, datatype));
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
			writer.write(TurtleUtil.encodeLongString(label));
			writer.write("\"\"\"");
		}
		else {
			// Write label as normal string
			writer.write("\"");
			writer.write(TurtleUtil.encodeString(label));
			writer.write("\"");
		}

		if (Literals.isLanguageLiteral(lit)) {
			// Append the literal's language
			writer.write("@");
			writer.write(lit.getLanguage());
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
		writenl("body { margin: 20px; font-family: monaco,monospace; font-size: 11pt; color: #444; }");
		writenl("a { color: #000; text-decoration: none; }");
		writenl("a:hover { color: #666; }");
		writenl(".nanopub { margin: 0 0 30px 0; padding: 10px; border-radius: 10px; border: solid; border-width: 1px; }");
		writenl(".nanopub-head { background: #e8e8e8; padding: 10px; border-radius: 10px; }");
		writenl(".nanopub-assertion { background: #99ccff; padding: 10px; border-radius: 10px; }");
		writenl(".nanopub-provenance { background: #f3a08c; padding: 10px; border-radius: 10px; }");
		writenl(".nanopub-pubinfo { background: #ffff66; padding: 10px; border-radius: 10px; }");
		writenl("</style>");
		writenl("</head>");
		writenl("<body>");
	}

	public void writeHtmlEnd()
		throws IOException
	{
		writenl("</body>");
		writenl("</htmls>");
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
