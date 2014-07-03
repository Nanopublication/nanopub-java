package org.nanopub.trusty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;

import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

public class TransformMultiNanopub {

	public static void main(String[] args)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		if (args.length == 0) {
			System.out.println("ERROR: No file given");
		}
		for (String arg : args) {
			File file = new File(arg);
			String path = "";
			if (file.getParent() != null) {
				path = file.getParent() + "/";
			}
			OutputStream out = new FileOutputStream(path + "trusty." + file.getName());
			TrustyUriResource r = new TrustyUriResource(new File(arg));
			RDFFormat format = r.getFormat(RDFFormat.TRIG);
			transform(format, file, out);
		}
	}

	public static void transform(final RDFFormat format, File file, final OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		InputStream in = new FileInputStream(file);
		transform(format, in, out);
	}

	public static void transform(final RDFFormat format, InputStream in, final OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		MultiNanopubRdfHandler.process(format, in, new NanopubHandler() {

			@Override
			public void handleNanopub(Nanopub np) {
				try {
					np = TransformNanopub.transform(np);
					RDFWriter w = Rio.createWriter(format, out);
					NanopubUtils.propagateToHandler(np, w);
				} catch (RDFHandlerException ex) {
					throw new RuntimeException(ex);
				} catch (TrustyUriException ex) {
					throw new RuntimeException(ex);
				}
			}

		});
		out.close();
	}

}
