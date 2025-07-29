package org.nanopub;

import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.trig.TriGWriterFactory;

import java.io.OutputStream;
import java.io.Writer;

/**
 * A custom TriG writer factory.
 *
 * @author Tobias Kuhn
 */
public class CustomTrigWriterFactory extends TriGWriterFactory {

    /**
     * Creates a new RDFWriter for the given output stream.
     *
     * @param out the output stream to write to
     * @return a new RDFWriter instance
     */
    public RDFWriter getWriter(OutputStream out) {
        return new CustomTrigWriter(out);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Creates a new RDFWriter for the given writer.
     */
    public RDFWriter getWriter(Writer writer) {
        return new CustomTrigWriter(writer);
    }

}
