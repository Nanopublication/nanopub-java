package org.nanopub.op.fingerprint;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfHasher;
import net.trustyuri.rdf.RdfPreprocessor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.vocabulary.NPX;

import java.util.ArrayList;
import java.util.List;

import static org.nanopub.SimpleTimestampPattern.isCreationTimeProperty;

/**
 * Generates fingerprints for Wikipathways nanopublications.
 */
public class WikipathwaysFingerprints implements FingerprintHandler {

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
            boolean isInPubinfo = st.getContext().equals(np.getPubinfoUri());
            if (!isInProvenance && !isInAssertion && !isInPubinfo) continue;
            IRI graphURI;
            if (isInAssertion) {
                graphURI = assertionUriPlaceholder;
            } else if (isInProvenance) {
                graphURI = provUriPlaceholder;
            } else {
                graphURI = pubinfoUriPlaceholder;
            }
            Resource subj = st.getSubject();
            IRI pred = st.getPredicate();
            Value obj = st.getObject();
            if (isInPubinfo && subj.equals(np.getUri()) && isCreationTimeProperty(pred)) {
                continue;
            }
            if (isInPubinfo && subj.equals(np.getUri()) && pred.equals(NPX.SUPERSEDES)) {
                continue;
            }
            n.add(SimpleValueFactory.getInstance().createStatement(subj, pred, obj, graphURI));
        }
        return n;
    }

}
