package org.nanopub;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;

/**
 * This class allows for the programmatic creation of nanopubs in a step-wise fashion.
 *
 * @author Tobias Kuhn
 */
public class NanopubCreator {

	private boolean finalized = false;

	private URI nanopubUri;
	private URI headUri, assertionUri, provenanceUri, pubinfoUri;
	private List<Statement> assertion, provenance, pubinfo;

	private List<Statement> statements;
	private Nanopub nanopub;

	private static final String headSuffix = ".Head";
	private static final String assertionSuffix = ".Ass";
	private static final String provenanceSuffix = ".Prov";
	private static final String pubinfoSuffix = ".Info";

	public NanopubCreator() {
		init();
	}

	public NanopubCreator(URI nanopubUri) {
		this();
		setNanopubUri(nanopubUri);
	}

	public NanopubCreator(String nanopubUri) {
		this();
		setNanopubUri(nanopubUri);
	}

	private void init() {
		assertion = new ArrayList<Statement>();
		provenance = new ArrayList<Statement>();
		pubinfo = new ArrayList<Statement>();
	}

	public void setNanopubUri(URI nanopubUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		this.nanopubUri = nanopubUri;
		if (headUri == null) headUri = new URIImpl(nanopubUri + headSuffix);
		if (assertionUri == null) assertionUri = new URIImpl(nanopubUri + assertionSuffix);
		if (provenanceUri == null) provenanceUri = new URIImpl(nanopubUri + provenanceSuffix);
		if (pubinfoUri == null) pubinfoUri = new URIImpl(nanopubUri + pubinfoSuffix);
	}

	public void setNanopubUri(String nanopubUri) {
		setNanopubUri(new URIImpl(nanopubUri));
	}

	public void setAssertionUri(URI assertionUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		this.assertionUri = assertionUri;
	}

	public void setAssertionUri(String assertionUri) {
		setAssertionUri(new URIImpl(assertionUri));
	}

	public void setProvenanceUri(URI provenanceUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		this.provenanceUri = provenanceUri;
	}

	public void setProvenanceUri(String provenanceUri) {
		setProvenanceUri(new URIImpl(provenanceUri));
	}

	public void setPubinfoUri(URI pubinfoUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		this.pubinfoUri = pubinfoUri;
	}

	public void setPubinfoUri(String pubinfoUri) {
		setPubinfoUri(new URIImpl(pubinfoUri));
	}

	public void addAssertionStatements(Statement... statements) {
		if (finalized) throw new RuntimeException("Already finalized");
		for (Statement st : statements) {
			assertion.add(st);
		}
	}

	public void addAssertionStatement(Resource subj, URI pred, Value obj) {
		addAssertionStatements(new StatementImpl(subj, pred, obj));
	}

	public void addProvenanceStatements(Statement... statements) {
		if (finalized) throw new RuntimeException("Already finalized");
		for (Statement st : statements) {
			provenance.add(st);
		}
	}

	public void addProvenanceStatement(Resource subj, URI pred, Value obj) {
		addProvenanceStatements(new StatementImpl(subj, pred, obj));
	}

	public void addPubinfoStatements(Statement... statements) {
		if (finalized) throw new RuntimeException("Already finalized");
		for (Statement st : statements) {
			pubinfo.add(st);
		}
	}

	public void addPubinfoStatement(Resource subj, URI pred, Value obj) {
		addPubinfoStatements(new StatementImpl(subj, pred, obj));
	}

	public Nanopub finalizeNanopub() throws MalformedNanopubException {
		if (finalized) {
			return nanopub;
		}
		if (nanopubUri == null) throw new MalformedNanopubException("No nanopub URI specified");
		collectStatements();
		nanopub = new NanopubImpl(statements);
		finalized = true;
		return nanopub;
	}

	private void collectStatements() {
		statements = new ArrayList<Statement>();
		addStatement(nanopubUri, RDF.TYPE, Nanopub.NANOPUB_TYPE_URI, headUri);
		addStatement(nanopubUri, Nanopub.HAS_ASSERTION_URI, assertionUri, headUri);
		addStatement(nanopubUri, Nanopub.HAS_PROVENANCE_URI, provenanceUri, headUri);
		addStatement(nanopubUri, Nanopub.HAS_PUBINFO_URI, pubinfoUri, headUri);
		for (Statement st : assertion) {
			addStatement(st.getSubject(), st.getPredicate(), st.getObject(), assertionUri);
		}
		for (Statement st : provenance) {
			addStatement(st.getSubject(), st.getPredicate(), st.getObject(), provenanceUri);
		}
		for (Statement st : pubinfo) {
			addStatement(st.getSubject(), st.getPredicate(), st.getObject(), pubinfoUri);
		}
	}

	private void addStatement(Resource subj, URI pred, Value obj, Resource context) {
		statements.add(new ContextStatementImpl(subj, pred, obj, context));
	}

}
