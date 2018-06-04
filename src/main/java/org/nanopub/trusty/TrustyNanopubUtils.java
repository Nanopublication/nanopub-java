package org.nanopub.trusty;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfHasher;
import net.trustyuri.rdf.RdfPreprocessor;
import net.trustyuri.rdf.RdfUtils;

import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

public class TrustyNanopubUtils {

	public static RDFFormat STNP_FORMAT = new RDFFormat("Serialized Trusty Nanopub", "text/plain", Charset.forName("UTF8"), "stnp", false, true);

	private TrustyNanopubUtils() {}  // no instances allowed

	public static void writeNanopub(Nanopub nanopub, OutputStream out, RDFFormat format)
			throws RDFHandlerException {
		RDFWriter writer = Rio.createWriter(format, new OutputStreamWriter(out, Charset.forName("UTF-8")));
		writer.startRDF();
		String s = nanopub.getUri().toString();
		writer.handleNamespace("this", s);
		writer.handleNamespace("sub", s + RdfUtils.postAcChar);
		if (!(RdfUtils.bnodeChar + "").matches("[A-Za-z0-9\\-_]")) {
			writer.handleNamespace("node", s + RdfUtils.postAcChar + RdfUtils.bnodeChar);
		}
		writer.handleNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		writer.handleNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		writer.handleNamespace("rdfg", "http://www.w3.org/2004/03/trix/rdfg-1/");
		writer.handleNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
		writer.handleNamespace("owl", "http://www.w3.org/2002/07/owl#");
		writer.handleNamespace("dct", "http://purl.org/dc/terms/");
		writer.handleNamespace("dce", "http://purl.org/dc/elements/1.1/");
		writer.handleNamespace("pav", "http://swan.mindinformatics.org/ontologies/1.2/pav/");
		writer.handleNamespace("np", "http://www.nanopub.org/nschema#");
		for (Statement st : NanopubUtils.getStatements(nanopub)) {
			writer.handleStatement(st);
		}
		writer.endRDF();
	}

	public static boolean isValidTrustyNanopub(Nanopub nanopub) {
		String artifactCode = TrustyUriUtils.getArtifactCode(nanopub.getUri().toString());
		if (artifactCode == null) return false;
		List<Statement> statements = NanopubUtils.getStatements(nanopub);
		statements = RdfPreprocessor.run(statements, artifactCode);

//		System.err.println("TRUSTY INPUT: ---");
//		System.err.print(RdfHasher.getDigestString(statements));
//		System.err.println("---");

		String ac = RdfHasher.makeArtifactCode(statements);
		return ac.equals(artifactCode);
	}

	public static String getTrustyDigestString(Nanopub nanopub) {
		String artifactCode = TrustyUriUtils.getArtifactCode(nanopub.getUri().toString());
		if (artifactCode == null) return null;
		List<Statement> statements = NanopubUtils.getStatements(nanopub);
		statements = RdfPreprocessor.run(statements, artifactCode);
		return RdfHasher.getDigestString(statements);
	}

}
