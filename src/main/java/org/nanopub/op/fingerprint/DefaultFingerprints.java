package org.nanopub.op.fingerprint;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfHasher;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.security.NanopubSignatureElement;

import java.util.ArrayList;
import java.util.List;

import static org.nanopub.SimpleTimestampPattern.isCreationTimeProperty;

/**
 * Default implementation of the FingerprintHandler interface.
 */
public class DefaultFingerprints implements FingerprintHandler {

    private boolean ignoreHead;
    private boolean ignoreProv;
    private boolean ignorePubinfo;

    /**
     * Default constructor that does not ignore any parts of the nanopub.
     */
    public DefaultFingerprints() {
        this(false, false, false);
    }

    /**
     * Constructor that allows ignoring specific parts of the nanopub.
     *
     * @param ignoreHead    whether to ignore the head part of the nanopub
     * @param ignoreProv    whether to ignore the provenance part of the nanopub
     * @param ignorePubinfo whether to ignore the publication information part of the nanopub
     */
    public DefaultFingerprints(boolean ignoreHead, boolean ignoreProv, boolean ignorePubinfo) {
        this.ignoreHead = ignoreHead;
        this.ignoreProv = ignoreProv;
        this.ignorePubinfo = ignorePubinfo;
    }

    /**
     * Returns the fingerprint of the nanopub.
     *
     * @return the fingerprint as a string
     */
    @Override
    public String getFingerprint(Nanopub np) {
        List<Statement> statements = getNormalizedStatements(np);
        String fingerprint = RdfHasher.makeArtifactCode(statements);
        return fingerprint.substring(2);
    }

    /**
     * Returns the normalized statements of the nanopub.
     *
     * @param np the nanopub to normalize statements from
     * @return a list of normalized statements
     */
    protected List<Statement> getNormalizedStatements(Nanopub np) {
        List<Statement> statements = NanopubUtils.getStatements(np);
        List<Statement> n = new ArrayList<>();
        String ac = TrustyUriUtils.getArtifactCode(np.getUri().stringValue());
        for (Statement st : statements) {
            boolean isInHead = st.getContext().equals(np.getHeadUri());
            if (isInHead && ignoreHead) continue;
            boolean isInProv = st.getContext().equals(np.getProvenanceUri());
            if (isInProv && ignoreProv) continue;
            boolean isInPubInfo = st.getContext().equals(np.getPubinfoUri());
            if (isInPubInfo && ignorePubinfo) continue;
            Resource subj = st.getSubject();
            IRI pred = st.getPredicate();
            if (isInPubInfo) {
                if (subj.equals(np.getUri()) && isCreationTimeProperty(pred)) continue;
                if (subj.equals(np.getUri()) && pred.equals(Nanopub.SUPERSEDES)) continue;
                if (pred.equals(NanopubSignatureElement.HAS_SIGNATURE)) continue;
                if (pred.equals(NanopubSignatureElement.HAS_ALGORITHM)) continue;
                if (pred.equals(NanopubSignatureElement.HAS_PUBLIC_KEY)) continue;
                if (pred.equals(NanopubSignatureElement.HAS_SIGNATURE_TARGET)) continue;
                if (pred.equals(NanopubSignatureElement.SIGNED_BY)) continue;
            }
            Statement newSt = transform(st, np.getUri().stringValue(), ac);
            n.add(newSt);
        }
        return n;
    }

    private Statement transform(Statement st, String buri, String ac) {
        return vf.createStatement(
                (Resource) transform(st.getSubject(), buri, ac),
                (IRI) transform(st.getPredicate(), buri, ac),
                transform(st.getObject(), buri, ac),
                (Resource) transform(st.getContext(), buri, ac)
        );
    }

    private Value transform(Value v, String buri, String ac) {
        // This can lead to identical fingerprints for semantically different statements in edge cases.
        // Should hardly ever be a problem with bona fide changes, but we should probably issue warnings or so.
        // TODO Add documentation/warnings for this.

        if (v instanceof Literal) return v;
        // Treating all blank nodes as the same, so fingerprinting will miss rearranged blank nodes:
        if (v instanceof BNode) return vf.createIRI(buri + "  blank-node");
        // Removing slashes and hashes around artifact code position to make pre-trusty nanopub match with already-trusty ones:
        if (ac != null && v.stringValue().contains(ac)) {
            String s = v.stringValue().replace(ac, " ").replaceFirst("[/#]+ ", " ").replaceFirst(" [/#]+", " ");
            return vf.createIRI(s);
        } else if (v.stringValue().startsWith(buri)) {
            int l = buri.length();
            String s = (buri + " " + v.stringValue().substring(l)).replaceFirst("[/#]+ ", " ").replaceFirst(" [/#]+", " ");
            return vf.createIRI(s);
        }
        return v;
    }

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

}
