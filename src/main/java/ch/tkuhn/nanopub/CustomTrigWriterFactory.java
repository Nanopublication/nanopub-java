package ch.tkuhn.nanopub;

import java.io.OutputStream;
import java.io.Writer;

import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.trig.TriGWriterFactory;

public class CustomTrigWriterFactory extends TriGWriterFactory {
    
    public RDFWriter getWriter(OutputStream out) {
        return new CustomTrigWriter(out);
    }

    public RDFWriter getWriter(Writer writer) {
	return new CustomTrigWriter(writer);
    }
}
