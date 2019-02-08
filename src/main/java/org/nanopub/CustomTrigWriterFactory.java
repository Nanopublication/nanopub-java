package org.nanopub;

import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.trig.TriGWriterFactory;

/**
 * @author Tobias Kuhn
 */
public class CustomTrigWriterFactory extends TriGWriterFactory {

	public RDFWriter getWriter(OutputStream out) {
		return new CustomTrigWriter(out);
	}

	public RDFWriter getWriter(Writer writer) {
		return new CustomTrigWriter(writer);
	}

}
