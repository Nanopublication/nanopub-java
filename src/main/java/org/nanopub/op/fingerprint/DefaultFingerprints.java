package org.nanopub.op.fingerprint;

import static org.nanopub.SimpleTimestampPattern.isCreationTimeProperty;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.security.NanopubSignatureElement;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.RdfHasher;

public class DefaultFingerprints implements FingerprintHandler {

	private boolean ignoreHead;
	private boolean ignoreProv;
	private boolean ignorePubinfo;

	public DefaultFingerprints() {
		this(false, false, false);
	}

	public DefaultFingerprints(boolean ignoreHead, boolean ignoreProv, boolean ignorePubinfo) {
		this.ignoreHead = ignoreHead;
		this.ignoreProv = ignoreProv;
		this.ignorePubinfo = ignorePubinfo;
	}

	@Override
	public String getFingerprint(Nanopub np) {
		List<Statement> statements = getNormalizedStatements(np);
		String fingerprint = RdfHasher.makeArtifactCode(statements);
		return fingerprint.substring(2);
	}

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
			System.err.println(newSt);
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
		if (v instanceof Literal) return v;
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
