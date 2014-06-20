package org.nanopub;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.openrdf.model.URI;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.rio.turtle.TurtleUtil;

/**
 * @author Tobias Kuhn
 */
public class CustomTrigWriter extends TriGWriter {

	public CustomTrigWriter(OutputStream out) {
		super(out);
	}

	public CustomTrigWriter(Writer writer) {
		super(writer);
	}

	@Override
	protected void writeURI(URI uri) throws IOException {
		String uriString = uri.toString();

		String prefix = namespaceTable.get(uriString);
		if (prefix != null) {
			// Exact match: no suffix required
			writer.write(prefix);
			writer.write(":");
			return;
		}

		prefix = null;

		int splitIdx  = TurtleUtil.findURISplitIndex(uriString);
		if (splitIdx > 0) {
			String namespace = uriString.substring(0, splitIdx);
			prefix = namespaceTable.get(namespace);
		}

		// Do also split at dots:
		int splitIdxDot = uriString.lastIndexOf(".") + 1;
		if (uriString.length() == splitIdxDot) splitIdxDot = -1;
		if (splitIdxDot > splitIdx) {
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
		if (splitIdxColon > splitIdx) {
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
		if (splitIdxUnderscore > splitIdx) {
			String namespace = uriString.substring(0, splitIdxUnderscore);
			String p = namespaceTable.get(namespace);
			if (p != null) {
				splitIdx = splitIdxUnderscore;
				prefix = p;
			}
		}

		if (prefix != null) {
			// Namespace is mapped to a prefix; write abbreviated URI
			writer.write(prefix);
			writer.write(":");
			writer.write(uriString.substring(splitIdx));
		} else {
			// Write full URI
			writer.write("<");
			writer.write(TurtleUtil.encodeURIString(uriString));
			writer.write(">");
		}
	}

}
