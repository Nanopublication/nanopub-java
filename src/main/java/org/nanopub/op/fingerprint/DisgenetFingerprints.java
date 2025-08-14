package org.nanopub.op.fingerprint;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfHasher;
import net.trustyuri.rdf.RdfPreprocessor;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.vocabulary.PAV;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates fingerprints for Disgenet nanopublications.
 */
public class DisgenetFingerprints implements FingerprintHandler {

    private static ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * Placeholder for the disgenet gda.
     */
    public static final IRI disgenetGdaPlaceholder = vf.createIRI("http://purl.org/nanopub/placeholders/disgenet-gda");

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFingerprint(Nanopub np) {
        String artifactCode = TrustyUriUtils.getArtifactCode(np.getUri().toString());
        if (artifactCode == null) {
            throw new RuntimeException("Not a trusty URI: " + np.getUri());
        }
        List<Statement> statements = getNormalizedStatements(np);
        statements = RdfPreprocessor.run(statements, artifactCode);
        String fingerprint = RdfHasher.makeArtifactCode(statements);
        return fingerprint.substring(2);
    }

    private List<Statement> getNormalizedStatements(Nanopub np) {
        List<Statement> statements = NanopubUtils.getStatements(np);
        List<Statement> n = new ArrayList<>();
        for (Statement st : statements) {
            boolean isInAssertion = st.getContext().equals(np.getAssertionUri());
            boolean isInProvenance = st.getContext().equals(np.getProvenanceUri());
            if (!isInProvenance && !isInAssertion) continue;
            IRI graphURI;
            if (isInAssertion) {
                graphURI = assertionUriPlaceholder;
            } else {
                graphURI = provUriPlaceholder;
            }
            Resource subj = st.getSubject();
            IRI pred = st.getPredicate();
            Value obj = st.getObject();
            if (isInAssertion) {
                String subjS = subj.stringValue();
                if (subjS.startsWith("http://rdf.disgenet.org/resource/gda/DGN") ||
                        subjS.startsWith("http://rdf.disgenet.org/gene-disease-association.ttl#DGN")) {
                    subj = disgenetGdaPlaceholder;
                }
            } else if (isInProvenance) {
                if (pred.equals(PAV.IMPORTED_ON) || pred.equals(PAV.IMPORTED_ON_V2)) {
                    pred = PAV.IMPORTED_ON_V2;
                    obj = timestampPlaceholder;
                }
                if (subj.equals(np.getAssertionUri())) {
                    subj = assertionUriPlaceholder;
                }
            }
            n.add(vf.createStatement((Resource) transform(subj), (IRI) transform(pred), transform(obj), graphURI));
        }
        return n;
    }

    private Value transform(Value v) {
        if (v instanceof IRI) {
            String s = v.stringValue();
            if (s.matches("http://rdf.disgenet.org/v.*/void.*")) {
                if (s.matches("http://rdf.disgenet.org/v.*/void.*-20[0-9]*")) {
                    String r = s.replaceFirst("^http://rdf.disgenet.org/v.*/void.*(/|#)(.*)-20[0-9]*$", "http://rdf.disgenet.org/vx.x.x/void/$2");
                    return vf.createIRI(r);
                } else {
                    String r = s.replaceFirst("^http://rdf.disgenet.org/v.*/void.*(/|#)", "http://rdf.disgenet.org/vx.x.x/void/");
                    return vf.createIRI(r);
                }
            } else if (s.startsWith("http://purl.obolibrary.org/obo/eco.owl#")) {
                return vf.createIRI(s.replace("http://purl.obolibrary.org/obo/eco.owl#", "http://purl.obolibrary.org/obo/"));
            }
        }
        return v;
    }

}
