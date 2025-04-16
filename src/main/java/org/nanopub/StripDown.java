package org.nanopub;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.extra.security.CryptoElement;
import org.nanopub.extra.security.NanopubSignatureElement;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Takes a signed nanopublication and removes the signature and the trusty url.
 */
public class StripDown extends CliRunner {

    @com.beust.jcommander.Parameter(description = "input-nanopub-files", required = true)
    private List<File> inputNanopubFiles = new ArrayList<File>();

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file")
    private File singleOutputFile; // only possible if there is only one inputFile

    private ValueFactory vf = SimpleValueFactory.getInstance();

    public static void main(String[] args) {
        try {
            StripDown obj = CliRunner.initJc(new StripDown(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public void run () throws MalformedNanopubException, IOException {

        final OutputStream singleOut;
        if (singleOutputFile != null) {
            if (singleOutputFile.getName().matches(".*\\.(gz|gzip)")) {
                singleOut = new GZIPOutputStream(new FileOutputStream(singleOutputFile));
            } else {
                singleOut = new FileOutputStream(singleOutputFile);
            }
        } else {
            singleOut = null;
        }

        for (File inputFile : inputNanopubFiles) {
            File outputFile;
            final OutputStream out;
            if (singleOutputFile == null) {
                outputFile = new File(inputFile.getParent(), "plain." + inputFile.getName());
                if (inputFile.getName().matches(".*\\.(gz|gzip)")) {
                    out = new GZIPOutputStream(new FileOutputStream(outputFile));
                } else {
                    out = new FileOutputStream(outputFile);
                }
            } else {
                outputFile = singleOutputFile;
                out = singleOut;
            }
            final RDFFormat inFormat = new TrustyUriResource(inputFile).getFormat(RDFFormat.TRIG);
            final RDFFormat outFormat = new TrustyUriResource(outputFile).getFormat(RDFFormat.TRIG);
            try (out) {
                MultiNanopubRdfHandler.process(inFormat, inputFile, new MultiNanopubRdfHandler.NanopubHandler() {

                    @Override
                    public void handleNanopub(Nanopub np) {
                        try {
                            List<Statement> newStatements = removeHashesAndSignaturesFromStatements(np);

                            NanopubImpl oldNp = (NanopubImpl)np;
                            Map<String, String> namespaces = removeHashFromNamespaces(oldNp);

                            NanopubImpl updatedNp = new NanopubImpl(newStatements, oldNp.getNsPrefixes(), namespaces);

                            RDFWriter w = Rio.createWriter(outFormat, new OutputStreamWriter(out, Charset.forName("UTF-8")));
                            NanopubUtils.propagateToHandler(updatedNp, w);

                        } catch (RDFHandlerException ex) {
                            ex.printStackTrace();
                            throw new RuntimeException(ex);
                        } catch (MalformedNanopubException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        }
    }

    private Map<String,String> removeHashFromNamespaces(NanopubImpl np) {
        String artifactCode = TrustyUriUtils.getArtifactCode(np.getUri().toString());
        if (artifactCode == null) {
            throw new RuntimeException("No artifact code found for " + np.getUri());
        }
        Map<String, String> newNamespaces = new HashMap<String, String>();
        for (String prefix : np.getNsPrefixes()) {
            String ns = np.getNamespace(prefix);
            newNamespaces.put(prefix, ns.replaceFirst(artifactCode+".?", ""));
        }
        return newNamespaces;
    }

    private List<Statement> removeHashesAndSignaturesFromStatements(Nanopub np) {
        String artifactCode = TrustyUriUtils.getArtifactCode(np.getUri().toString());
        if (artifactCode == null) {
            throw new RuntimeException("No artifact code found for " + np.getUri());
        }

        List<Statement> statements = NanopubUtils.getStatements(np);

        List<Statement> newStatements = new ArrayList<>();
        for (Statement st : statements) {
            // skip signatures
            if (st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE_ELEMENT)) continue;
            if (st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE_TARGET)) continue;
            if (st.getPredicate().equals(NanopubSignatureElement.HAS_SIGNATURE)) continue;
            if (st.getPredicate().equals(CryptoElement.HAS_PUBLIC_KEY)) continue;
            if (st.getPredicate().equals(CryptoElement.HAS_ALGORITHM)) continue;
            if (st.getPredicate().equals(NanopubSignatureElement.SIGNED_BY)) continue;

            // remove hashes
            Resource context = transform(st.getContext(), artifactCode);
            Resource subject = transform(st.getSubject(), artifactCode);
            IRI predicate = transform(st.getPredicate(), artifactCode);
            Value object = st.getObject();
            if (object instanceof Resource) {
                object = transform((Resource) object, artifactCode);
            }
            Statement n = vf.createStatement(subject, predicate, object, context);
            newStatements.add(n);
        }
        return newStatements;
    }

    protected IRI transform (Resource r, String artifact) {
        if (r == null) {
            return null;
        } else if (r instanceof BNode) {
            throw new RuntimeException("Unexpected blank node encountered");
        } else {
            IRI transformedURI = vf.createIRI(r.toString().replaceFirst(artifact+".?", ""));
            return transformedURI;
        }
    }

    protected String removeHash(String url) {
        String artifactCode = TrustyUriUtils.getArtifactCode(url);
        if (artifactCode == null) {
            return null;
        }
        return url.replace(artifactCode, "")   ;
    }

}
