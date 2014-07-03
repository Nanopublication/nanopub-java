package org.nanopub.trusty;

import java.io.OutputStream;

import net.trustyuri.rdf.UriTransformConfig;

import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

public class TrustyNanopubUtils {

	private TrustyNanopubUtils() {}  // no instances allowed

	public static void writeNanopub(Nanopub nanopub, OutputStream out, RDFFormat format)
			throws RDFHandlerException {
		UriTransformConfig c = UriTransformConfig.getDefault();
		RDFWriter writer = Rio.createWriter(format, out);
		writer.startRDF();
		String s = nanopub.getUri().toString();
		writer.handleNamespace("this", s);
		writer.handleNamespace("sub", s + c.getPostHashChar());
		if (!(c.getBnodeChar() + "").matches("[A-Za-z0-9\\-_]")) {
			writer.handleNamespace("node", s + c.getPostHashChar() + c.getBnodeChar());
		}
		writer.handleNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		writer.handleNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		writer.handleNamespace("rdfg", "http://www.w3.org/2004/03/trix/rdfg-1/");
		writer.handleNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
		writer.handleNamespace("owl", "http://www.w3.org/2002/07/owl#");
		writer.handleNamespace("dc", "http://purl.org/dc/terms/");
		writer.handleNamespace("pav", "http://swan.mindinformatics.org/ontologies/1.2/pav/");
		writer.handleNamespace("np", "http://www.nanopub.org/nschema#");
		for (Statement st : NanopubUtils.getStatements(nanopub)) {
			writer.handleStatement(st);
		}
		writer.endRDF();
	}

}
