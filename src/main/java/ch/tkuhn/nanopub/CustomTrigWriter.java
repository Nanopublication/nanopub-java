package ch.tkuhn.nanopub;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.openrdf.model.URI;
import org.openrdf.rio.trig.TriGWriter;

public class CustomTrigWriter extends TriGWriter {

	public CustomTrigWriter(OutputStream out) {
		super(out);
	}

	public CustomTrigWriter(Writer writer) {
		super(writer);
	}

	protected void writeURI(URI uri) throws IOException {
		String prefix = namespaceTable.get(uri.toString());
		if (prefix != null) {
			writer.write(prefix);
			writer.write(":");
		} else {
			super.writeURI(uri);
		}
	}

}
