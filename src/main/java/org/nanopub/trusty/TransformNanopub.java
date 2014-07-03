package org.nanopub.trusty;

import java.io.File;
import java.io.IOException;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.rdf.RdfFileContent;
import net.trustyuri.rdf.RdfUtils;
import net.trustyuri.rdf.TransformRdf;

import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubRdfHandler;
import org.nanopub.NanopubUtils;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;

public class TransformNanopub {

	public static void main(String[] args) throws IOException, TrustyUriException {
		if (args.length == 0) {
			System.out.println("ERROR: No file given");
		}
		for (String arg : args) {
			File inputFile = new File(arg);
			TrustyUriResource r = new TrustyUriResource(inputFile);
			RdfFileContent content = RdfUtils.load(r);
			Nanopub nanopub = null;
			try {
				nanopub = new NanopubImpl(content.getStatements());
			} catch (MalformedNanopubException ex) {
				System.out.println("ERROR: Malformed nanopub: " + ex.getMessage());
				System.exit(1);
			}
			TransformRdf.transform(content, inputFile.getParent(), nanopub.getUri().toString());
		}
	}

	public static Nanopub transform(Nanopub nanopub) throws TrustyUriException {
		Nanopub np;
		try {
			RdfFileContent r = new RdfFileContent(RDFFormat.TRIG);
			NanopubUtils.propagateToHandler(nanopub, r);
			NanopubRdfHandler h = new NanopubRdfHandler();
			TransformRdf.transform(r, h, nanopub.getUri().toString());
			np = h.getNanopub();
		} catch (RDFHandlerException ex) {
			throw new TrustyUriException(ex);
		} catch (MalformedNanopubException ex) {
			throw new TrustyUriException(ex);
		}
		return np;
	}

}
