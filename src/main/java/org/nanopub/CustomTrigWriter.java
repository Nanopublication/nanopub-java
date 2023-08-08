package org.nanopub;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Set;

import org.apache.commons.io.output.NullOutputStream;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.trig.TriGWriter;
import org.eclipse.rdf4j.rio.turtle.TurtleUtil;

/**
 * @author Tobias Kuhn
 */
public class CustomTrigWriter extends TriGWriter {

	private Set<String> usedPrefixes;

	public CustomTrigWriter(OutputStream out) {
		super(out);
	}

	public CustomTrigWriter(Writer writer) {
		super(writer);
	}

	public CustomTrigWriter(OutputStream out, Set<String> usedPrefixes) {
		super(out);
		this.usedPrefixes = usedPrefixes;
	}

	public CustomTrigWriter(Writer writer, Set<String> usedPrefixes) {
		super(writer);
		this.usedPrefixes = usedPrefixes;
	}

	public CustomTrigWriter(Set<String> usedPrefixes) {
		super(NullOutputStream.NULL_OUTPUT_STREAM);
		this.usedPrefixes = usedPrefixes;
	}

	@Override
	protected void writeURI(IRI uri) throws IOException {
		String uriString = uri.toString();

		String prefix = namespaceTable.get(uriString);
		if (prefix != null) {
			// Exact match: no suffix required
			writer.write(prefix);
			writer.write(":");
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

		// ... and *before* hash signs:
		int splitIdxHashsign = uriString.lastIndexOf("#");
		if (splitIdx > 0 && splitIdxHashsign > splitIdx - 2) {
			String namespace = uriString.substring(0, splitIdxHashsign);
			String p = namespaceTable.get(namespace);
			String postHashPrefix = namespaceTable.get(namespace + "#");
			if (p != null && postHashPrefix == null) {
				splitIdx = splitIdxHashsign;
				prefix = p;
			}
		}

		if (uriString.endsWith(".")) {
			prefix = null;
		}

		if (prefix != null) {
			// Namespace is mapped to a prefix; write abbreviated URI
			writer.write(prefix);
			writer.write(":");
			writer.write(uriString.substring(splitIdx).replaceFirst("^#", "\\\\#"));
			if (usedPrefixes != null) {
				usedPrefixes.add(prefix);
			}
		} else {
			// Write full URI
			writer.write("<");
			writer.write(TurtleUtil.encodeURIString(uriString));
			writer.write(">");
		}
	}

	// Overriding this method to *not* normalize/pretty-print literals.
	@Override
	protected void writeLiteral(Literal lit) throws IOException {
		String label = lit.getLabel();
		IRI datatype = lit.getDatatype();

		if (label.indexOf('\n') != -1 || label.indexOf('\r') != -1 || label.indexOf('\t') != -1) {
			// Write label as long string
			writer.write("\"\"\"");
			writer.write(TurtleUtil.encodeLongString(label));
			writer.write("\"\"\"");
		} else {
			// Write label as normal string
			writer.write("\"");
			writer.write(TurtleUtil.encodeString(label));
			writer.write("\"");
		}

		if (Literals.isLanguageLiteral(lit)) {
			// Append the literal's language
			writer.write("@");
			writer.write(lit.getLanguage().get());
		} else if (!XSD.STRING.equals(datatype)) {
			// Append the literal's datatype (possibly written as an abbreviated
			// URI)
			writer.write("^^");
			writeURI(datatype);
		}
	}
}
