package org.nanopub;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
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
	private boolean nanopubUriFixed, assertionUriFixed;
	private List<Statement> assertion, provenance, pubinfo;

	private List<Statement> statements;
	private List<String> nsPrefixes;
	private Map<String,String> ns;
	private Nanopub nanopub;

	private ValueFactoryImpl vf = new ValueFactoryImpl();

	private static final String headSuffix = "Head";
	private static final String assertionSuffix = "Ass";
	private static final String provenanceSuffix = "Prov";
	private static final String pubinfoSuffix = "Info";

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

		nsPrefixes = new ArrayList<String>();
		ns = new HashMap<String,String>();
	}

	public void setNanopubUri(URI nanopubUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		if (nanopubUriFixed) {
			throw new RuntimeException("Cannot change nanopublication URI anymore: has already been used");
		}
		this.nanopubUri = nanopubUri;
		if (headUri == null) headUri = new URIImpl(nanopubUri + headSuffix);
		if (assertionUri == null) assertionUri = new URIImpl(nanopubUri + assertionSuffix);
		if (provenanceUri == null) provenanceUri = new URIImpl(nanopubUri + provenanceSuffix);
		if (pubinfoUri == null) pubinfoUri = new URIImpl(nanopubUri + pubinfoSuffix);
	}

	public void setNanopubUri(String nanopubUri) {
		setNanopubUri(new URIImpl(nanopubUri));
	}

	public URI getNanopubUri() {
		return nanopubUri;
	}

	public void setAssertionUri(URI assertionUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		if (assertionUriFixed) {
			throw new RuntimeException("Cannot change assertion URI anymore: has already been used");
		}
		this.assertionUri = assertionUri;
	}

	public void setAssertionUri(String assertionUri) {
		setAssertionUri(new URIImpl(assertionUri));
	}

	public URI getAssertionUri() {
		return assertionUri;
	}

	public void setProvenanceUri(URI provenanceUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		this.provenanceUri = provenanceUri;
	}

	public void setProvenanceUri(String provenanceUri) {
		setProvenanceUri(new URIImpl(provenanceUri));
	}

	public URI getProvenanceUri() {
		return provenanceUri;
	}

	public void setPubinfoUri(URI pubinfoUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		this.pubinfoUri = pubinfoUri;
	}

	public void setPubinfoUri(String pubinfoUri) {
		setPubinfoUri(new URIImpl(pubinfoUri));
	}

	public URI getPubinfoUri() {
		return pubinfoUri;
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

	public void addProvenanceStatement(URI pred, Value obj) {
		if (assertionUri == null) throw new RuntimeException("Assertion URI not yet set");
		addProvenanceStatement(assertionUri, pred, obj);
		assertionUriFixed = true;
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

	public void addPubinfoStatement(URI pred, Value obj) {
		if (nanopubUri == null) throw new RuntimeException("Nanopublication URI not yet set");
		addPubinfoStatement(nanopubUri, pred, obj);
		nanopubUriFixed = true;
	}

	public void addTimestamp(Date date) {
		addPubinfoStatement(NanopubVocab.CREATION_TIME, vf.createLiteral(date));
	}

	public void addCreator(URI creator) {
		addPubinfoStatement(NanopubVocab.HAS_CREATOR, creator);
	}

	public void addCreator(String orcidIdentifier) {
		addCreator(getOrcidUri(orcidIdentifier));
	}

	public void addAuthor(URI author) {
		addPubinfoStatement(NanopubVocab.HAS_AUTHOR, author);
	}

	public void addAuthor(String orcidIdentifier) {
		addAuthor(getOrcidUri(orcidIdentifier));
	}

	private URI getOrcidUri(String orcid) {
		if (!orcid.startsWith("http://orcid.org/")) {
			orcid = "http://orcid.org/" + orcid;
		}
		return new URIImpl(orcid);
		
	}

	public void addNamespace(String prefix, String namespace) {
		if (finalized) throw new RuntimeException("Already finalized");
		nsPrefixes.add(prefix);
		ns.put(prefix, namespace);
	}

	public void addNamespace(String prefix, URI namespace) {
		addNamespace(prefix, namespace.toString());
	}

	public Nanopub finalizeNanopub() throws MalformedNanopubException {
		return finalizeNanopub(false);
	}

	public Nanopub finalizeNanopub(boolean addTimestamp) throws MalformedNanopubException {
		if (finalized) {
			return nanopub;
		}
		if (nanopubUri == null) throw new MalformedNanopubException("No nanopub URI specified");
		if (addTimestamp) {
			addTimestamp(new Date());
		}
		collectStatements();
		nanopub = new NanopubImpl(statements, nsPrefixes, ns);
		finalized = true;
		return nanopub;
	}

	/**
	 * Finalizes the nanopub and gives it a trusty URI. See http://arxiv.org/abs/1401.5775 and
	 * https://github.com/trustyuri/trustyuri-java
	 * 
	 * This method dynamically loads the TrustURI classes. Make sure you have the jar installed or
	 * uncomment the entry in the pom file.
	 */
	public Nanopub finalizeTrustyNanopub() throws Exception {
		return finalizeTrustyNanopub(false);
	}

	/**
	 * Finalizes the nanopub and gives it a trusty URI. See http://arxiv.org/abs/1401.5775 and
	 * https://github.com/trustyuri/trustyuri-java
	 * 
	 * This method dynamically loads the TrustURI classes. Make sure you have the jar installed or
	 * uncomment the entry in the pom file.
	 */
	public Nanopub finalizeTrustyNanopub(boolean addTimestamp) throws Exception {
		Nanopub preNanopub = finalizeNanopub(addTimestamp);
		Class<?> c = Class.forName("net.trustyuri.rdf.TransformNanopub");
		return (Nanopub) c.getMethod("transform", Nanopub.class).invoke(null, preNanopub);
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
