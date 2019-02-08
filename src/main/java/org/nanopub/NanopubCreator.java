package org.nanopub;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.trusty.MakeTrustyNanopub;

/**
 * This class allows for the programmatic creation of nanopubs in a step-wise fashion.
 *
 * @author Tobias Kuhn
 */
public class NanopubCreator {

	private boolean finalized = false;

	private IRI nanopubUri;
	private IRI headUri, assertionUri, provenanceUri, pubinfoUri;
	private boolean nanopubUriFixed, assertionUriFixed;
	private List<Statement> assertion, provenance, pubinfo;

	private List<Statement> statements;
	private List<String> nsPrefixes;
	private Map<String,String> ns;
	private Nanopub nanopub;

	private ValueFactory vf = SimpleValueFactory.getInstance();

	private static final String headSuffix = "Head";
	private static final String assertionSuffix = "assertion";
	private static final String provenanceSuffix = "provenance";
	private static final String pubinfoSuffix = "pubinfo";

	public NanopubCreator() {
		init();
	}

	public NanopubCreator(IRI nanopubUri) {
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

	public void setNanopubUri(IRI nanopubUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		if (nanopubUriFixed) {
			throw new RuntimeException("Cannot change nanopublication URI anymore: has already been used");
		}
		this.nanopubUri = nanopubUri;
		if (headUri == null) headUri = vf.createIRI(nanopubUri + headSuffix);
		if (assertionUri == null) assertionUri = vf.createIRI(nanopubUri + assertionSuffix);
		if (provenanceUri == null) provenanceUri = vf.createIRI(nanopubUri + provenanceSuffix);
		if (pubinfoUri == null) pubinfoUri = vf.createIRI(nanopubUri + pubinfoSuffix);
	}

	public void setNanopubUri(String nanopubUri) {
		setNanopubUri(vf.createIRI(nanopubUri));
	}

	public IRI getNanopubUri() {
		return nanopubUri;
	}

	public void setAssertionUri(IRI assertionUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		if (assertionUriFixed) {
			throw new RuntimeException("Cannot change assertion URI anymore: has already been used");
		}
		this.assertionUri = assertionUri;
	}

	public void setAssertionUri(String assertionUri) {
		setAssertionUri(vf.createIRI(assertionUri));
	}

	public IRI getAssertionUri() {
		return assertionUri;
	}

	public void setProvenanceUri(IRI provenanceUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		this.provenanceUri = provenanceUri;
	}

	public void setProvenanceUri(String provenanceUri) {
		setProvenanceUri(vf.createIRI(provenanceUri));
	}

	public IRI getProvenanceUri() {
		return provenanceUri;
	}

	public void setPubinfoUri(IRI pubinfoUri) {
		if (finalized) throw new RuntimeException("Already finalized");
		this.pubinfoUri = pubinfoUri;
	}

	public void setPubinfoUri(String pubinfoUri) {
		setPubinfoUri(vf.createIRI(pubinfoUri));
	}

	public IRI getPubinfoUri() {
		return pubinfoUri;
	}

	public void addAssertionStatements(Statement... statements) {
		if (finalized) throw new RuntimeException("Already finalized");
		for (Statement st : statements) {
			assertion.add(st);
		}
	}

	public void addAssertionStatement(Resource subj, IRI pred, Value obj) {
		addAssertionStatements(vf.createStatement(subj, pred, obj));
	}

	public void addProvenanceStatements(Statement... statements) {
		if (finalized) throw new RuntimeException("Already finalized");
		for (Statement st : statements) {
			provenance.add(st);
		}
	}

	public void addProvenanceStatement(Resource subj, IRI pred, Value obj) {
		addProvenanceStatements(vf.createStatement(subj, pred, obj));
	}

	public void addProvenanceStatement(IRI pred, Value obj) {
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

	public void addPubinfoStatement(Resource subj, IRI pred, Value obj) {
		addPubinfoStatements(vf.createStatement(subj, pred, obj));
	}

	public void addPubinfoStatement(IRI pred, Value obj) {
		if (nanopubUri == null) throw new RuntimeException("Nanopublication URI not yet set");
		addPubinfoStatement(nanopubUri, pred, obj);
		nanopubUriFixed = true;
	}

	public void addTimestamp(Date date) {
		addPubinfoStatement(SimpleTimestampPattern.DCT_CREATED, vf.createLiteral(date));
	}

	public void addCreator(IRI creator) {
		addPubinfoStatement(SimpleCreatorPattern.PAV_CREATEDBY, creator);
	}

	public void addCreator(String orcidIdentifier) {
		addCreator(getOrcidUri(orcidIdentifier));
	}

	public void addAuthor(IRI author) {
		addPubinfoStatement(SimpleCreatorPattern.PAV_AUTHOREDBY, author);
	}

	public void addAuthor(String orcidIdentifier) {
		addAuthor(getOrcidUri(orcidIdentifier));
	}

	private IRI getOrcidUri(String orcid) {
		if (!orcid.startsWith("http://orcid.org/")) {
			orcid = "http://orcid.org/" + orcid;
		}
		return vf.createIRI(orcid);
		
	}

	public void addNamespace(String prefix, String namespace) {
		if (finalized) throw new RuntimeException("Already finalized");
		nsPrefixes.add(prefix);
		ns.put(prefix, namespace);
	}

	public void addNamespace(String prefix, IRI namespace) {
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
		return MakeTrustyNanopub.transform(preNanopub);
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

	private void addStatement(Resource subj, IRI pred, Value obj, Resource context) {
		statements.add(vf.createStatement(subj, pred, obj, context));
	}

}
