package org.nanopub.trusty;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfHasher;
import net.trustyuri.rdf.RdfPreprocessor;
import net.trustyuri.rdf.TransformRdfSetting;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.vocabulary.NP;
import org.nanopub.vocabulary.PAV;
import org.nanopub.vocabulary.RDFG;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Utility class for handling Trusty Nanopubs.
 */
public class TrustyNanopubUtils {

    /**
     * The RDF format for serialized Trusty Nanopubs.
     */
    public static RDFFormat STNP_FORMAT = new RDFFormat("Serialized Trusty Nanopub", "text/plain", StandardCharsets.UTF_8, "stnp", false, true, false);

    private TrustyNanopubUtils() {
    }  // no instances allowed

    /**
     * The default TransformRdfSetting used for Trusty Nanopubs.
     */
    public static final TransformRdfSetting transformRdfSetting = new TransformRdfSetting('_', '/', '/', '/');

    /**
     * Writes a Nanopub to an OutputStream in the specified RDF format.
     *
     * @param nanopub the Nanopub to write
     * @param out     the OutputStream to write to
     * @param format  the RDF format to use for serialization
     * @throws org.eclipse.rdf4j.rio.RDFHandlerException if there is an error during writing
     * @throws java.io.IOException                       if there is an I/O error
     */
    public static void writeNanopub(Nanopub nanopub, OutputStream out, RDFFormat format) throws RDFHandlerException, IOException {
        try (OutputStreamWriter sw = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            RDFWriter writer = Rio.createWriter(format, sw);
            writer.startRDF();
            String s = nanopub.getUri().toString();
            writer.handleNamespace("this", s);
            writer.handleNamespace("sub", s + transformRdfSetting.getPostAcChar());
            if (!(transformRdfSetting.getBnodeChar() + "").matches("[A-Za-z0-9\\-_]")) {
                writer.handleNamespace("node", s + transformRdfSetting.getPostAcChar() + transformRdfSetting.getBnodeChar());
            }
            writer.handleNamespace(RDF.PREFIX, RDF.NAMESPACE);
            writer.handleNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
            writer.handleNamespace(RDFG.PREFIX, RDFG.NAMESPACE);
            writer.handleNamespace(XSD.PREFIX, XSD.NAMESPACE);
            writer.handleNamespace(OWL.PREFIX, OWL.NAMESPACE);
            writer.handleNamespace("dct", DCTERMS.NAMESPACE);
            writer.handleNamespace("dce", DC.NAMESPACE);
            writer.handleNamespace(PAV.PREFIX, PAV.NAMESPACE);
            writer.handleNamespace(NP.PREFIX, NP.NAMESPACE);
            for (Statement st : NanopubUtils.getStatements(nanopub)) {
                writer.handleStatement(st);
            }
            writer.endRDF();
        }
    }

    /**
     * Checks if a Nanopub is a valid Trusty Nanopub.
     *
     * @param nanopub the Nanopub to check
     * @return true if the Nanopub is valid, false otherwise
     */
    public static boolean isValidTrustyNanopub(Nanopub nanopub) {
        Set<IRI> graphUris = nanopub.getGraphUris();
        IRI nanopubUri = nanopub.getUri();
        for (IRI uri : graphUris) {
            if (!uri.stringValue().startsWith(nanopubUri.stringValue())) {
                return false;
            }
        }

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

    /**
     * Computes the Trusty digest string for a Nanopub.
     *
     * @param nanopub the Nanopub to compute the digest for
     * @return the Trusty digest string, or null if the artifact code is not found
     */
    public static String getTrustyDigestString(Nanopub nanopub) {
        String artifactCode = TrustyUriUtils.getArtifactCode(nanopub.getUri().toString());
        if (artifactCode == null) return null;
        List<Statement> statements = NanopubUtils.getStatements(nanopub);
        statements = RdfPreprocessor.run(statements, artifactCode);
        return RdfHasher.getDigestString(statements);
    }

}
