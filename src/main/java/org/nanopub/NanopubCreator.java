package org.nanopub;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.trusty.MakeTrustyNanopub;
import org.nanopub.vocabulary.NP;
import org.nanopub.vocabulary.PAV;

import java.util.*;

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
    private Map<String, String> ns;
    private Nanopub nanopub;
    private boolean removeUnusedPrefixesEnabled = false;

    private ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String headSuffix = "Head";
    private static final String assertionSuffix = "assertion";
    private static final String provenanceSuffix = "provenance";
    private static final String pubinfoSuffix = "pubinfo";

    /**
     * Creates a new NanopubCreator with an empty nanopub URI.
     */
    public NanopubCreator() {
        init();
    }

    /**
     * Creates a new NanopubCreator with an empty nanopub URI.
     *
     * @param initWithTempNanopubIris if true, initializes the nanopub with temporary IRIs.
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public NanopubCreator(boolean initWithTempNanopubIris) throws NanopubAlreadyFinalizedException {
        this();
        if (initWithTempNanopubIris) {
            setNanopubUri(NanopubUtils.createTempNanopubIri());
        }
    }

    /**
     * Creates a new NanopubCreator with a specified nanopub URI.
     *
     * @param nanopubUri the nanopublication URI
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public NanopubCreator(IRI nanopubUri) throws NanopubAlreadyFinalizedException {
        this();
        setNanopubUri(nanopubUri);
    }

    /**
     * Creates a new NanopubCreator with a specified nanopub URI.
     *
     * @param nanopubUri the nanopublication URI as a string
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public NanopubCreator(String nanopubUri) throws NanopubAlreadyFinalizedException {
        this();
        setNanopubUri(nanopubUri);
    }

    private void init() {
        assertion = new ArrayList<>();
        provenance = new ArrayList<>();
        pubinfo = new ArrayList<>();

        nsPrefixes = new ArrayList<>();
        ns = new HashMap<>();
    }

    /**
     * Sets the nanopublication URI.
     *
     * @param nanopubUri the nanopublication URI
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void setNanopubUri(IRI nanopubUri) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        if (nanopubUriFixed) {
            throw new RuntimeException("Cannot change nanopublication URI anymore: has already been used");
        }
        this.nanopubUri = nanopubUri;
        if (headUri == null) headUri = vf.createIRI(nanopubUri + headSuffix);
        if (assertionUri == null) assertionUri = vf.createIRI(nanopubUri + assertionSuffix);
        if (provenanceUri == null) provenanceUri = vf.createIRI(nanopubUri + provenanceSuffix);
        if (pubinfoUri == null) pubinfoUri = vf.createIRI(nanopubUri + pubinfoSuffix);
    }

    /**
     * Sets the nanopublication URI.
     *
     * @param nanopubUri the nanopublication URI as a string
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void setNanopubUri(String nanopubUri) throws NanopubAlreadyFinalizedException {
        setNanopubUri(vf.createIRI(nanopubUri));
    }

    /**
     * Returns the nanopublication URI.
     *
     * @return the nanopublication URI
     */
    public IRI getNanopubUri() {
        return nanopubUri;
    }

    /**
     * Sets the assertion URI of the nanopublication.
     *
     * @param assertionUri the assertion URI
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void setAssertionUri(IRI assertionUri) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        if (assertionUriFixed) {
            throw new RuntimeException("Cannot change assertion URI anymore: has already been used");
        }
        this.assertionUri = assertionUri;
    }

    /**
     * Sets the assertion URI of the nanopublication.
     *
     * @param assertionUri the assertion URI as a string
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void setAssertionUri(String assertionUri) throws NanopubAlreadyFinalizedException {
        setAssertionUri(vf.createIRI(assertionUri));
    }

    /**
     * Returns the assertion URI of the nanopublication.
     *
     * @return the assertion URI
     */
    public IRI getAssertionUri() {
        return assertionUri;
    }

    /**
     * Sets the provenance URI of the nanopublication.
     *
     * @param provenanceUri the head URI
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void setProvenanceUri(IRI provenanceUri) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        this.provenanceUri = provenanceUri;
    }

    /**
     * Sets the provenance URI of the nanopublication.
     *
     * @param provenanceUri the head URI as a string
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void setProvenanceUri(String provenanceUri) throws NanopubAlreadyFinalizedException {
        setProvenanceUri(vf.createIRI(provenanceUri));
    }

    /**
     * Returns the provenance URI of the nanopublication.
     *
     * @return the provenance URI
     */
    public IRI getProvenanceUri() {
        return provenanceUri;
    }

    /**
     * Sets the pubinfo URI of the nanopublication.
     *
     * @param pubinfoUri the pubinfo URI
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void setPubinfoUri(IRI pubinfoUri) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        this.pubinfoUri = pubinfoUri;
    }

    /**
     * Sets the pubinfo URI of the nanopublication.
     *
     * @param pubinfoUri the pubinfo URI as a string
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void setPubinfoUri(String pubinfoUri) throws NanopubAlreadyFinalizedException {
        setPubinfoUri(vf.createIRI(pubinfoUri));
    }

    /**
     * Returns the pubinfo URI of the nanopublication.
     *
     * @return the pubinfo URI
     */
    public IRI getPubinfoUri() {
        return pubinfoUri;
    }

    /**
     * Adds statements to the assertion part of the nanopublication.
     *
     * @param statements the statements to add
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addAssertionStatements(Statement... statements) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        assertion.addAll(Arrays.asList(statements));
    }

    /**
     * Adds statements to the assertion part of the nanopublication.
     *
     * @param statements the statements to add
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addAssertionStatements(Iterable<Statement> statements) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        for (Statement st : statements) {
            assertion.add(st);
        }
    }

    /**
     * Adds a statement to the assertion part of the nanopublication.
     *
     * @param subj the subject of the statement
     * @param pred the predicate of the statement
     * @param obj  the object of the statement
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addAssertionStatement(Resource subj, IRI pred, Value obj) throws NanopubAlreadyFinalizedException {
        addAssertionStatements(vf.createStatement(subj, pred, obj));
    }

    /**
     * Adds a statement to the assertion part of the nanopublication.
     *
     * @param statement the predicate of the statement
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addAssertionStatement(Statement statement) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        assertion.add(statement);
    }

    /**
     * Adds statements to the provenance part of the nanopublication.
     *
     * @param statements the statements to add
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addProvenanceStatements(Statement... statements) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        provenance.addAll(Arrays.asList(statements));
    }

    /**
     * Adds statements to the provenance part of the nanopublication.
     *
     * @param statements the statements to add
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addProvenanceStatements(Iterable<Statement> statements) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        for (Statement st : statements) {
            provenance.add(st);
        }
    }

    /**
     * Adds a statement to the provenance part of the nanopublication.
     *
     * @param subj the subject of the statement
     * @param pred the predicate of the statement
     * @param obj  the object of the statement
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addProvenanceStatement(Resource subj, IRI pred, Value obj) throws NanopubAlreadyFinalizedException {
        addProvenanceStatements(vf.createStatement(subj, pred, obj));
    }

    /**
     * Adds a statement to the provenance part of the nanopublication.
     *
     * @param pred the predicate of the statement
     * @param obj  the object of the statement
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addProvenanceStatement(IRI pred, Value obj) throws NanopubAlreadyFinalizedException {
        if (assertionUri == null) throw new RuntimeException("Assertion URI not yet set");
        addProvenanceStatement(assertionUri, pred, obj);
        assertionUriFixed = true;
    }

    /**
     * Adds a statement to the provenance part of the nanopublication.
     *
     * @param statement the statement to add
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addProvenanceStatement(Statement statement) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        provenance.add(statement);
    }

    /**
     * Adds statements to the pubinfo part of the nanopublication.
     *
     * @param statements the statements to add
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addPubinfoStatements(Statement... statements) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        pubinfo.addAll(Arrays.asList(statements));
    }

    /**
     * Adds statements to the pubinfo part of the nanopublication.
     *
     * @param statements the statements to add
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addPubinfoStatements(Iterable<Statement> statements) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        for (Statement st : statements) {
            pubinfo.add(st);
        }
    }

    /**
     * Adds a statement to the pubinfo part of the nanopublication.
     *
     * @param subj the subject of the statement
     * @param pred the predicate of the statement
     * @param obj  the object of the statement
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addPubinfoStatement(Resource subj, IRI pred, Value obj) throws NanopubAlreadyFinalizedException {
        addPubinfoStatements(vf.createStatement(subj, pred, obj));
    }

    /**
     * Adds a statement to the pubinfo part of the nanopublication.
     *
     * @param pred the predicate of the statement
     * @param obj  the object of the statement
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addPubinfoStatement(IRI pred, Value obj) throws NanopubAlreadyFinalizedException {
        if (nanopubUri == null) throw new RuntimeException("Nanopublication URI not yet set");
        addPubinfoStatement(nanopubUri, pred, obj);
        nanopubUriFixed = true;
    }

    /**
     * Adds a statement to the pubinfo part of the nanopublication.
     *
     * @param statement the statement to add
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addPubinfoStatement(Statement statement) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        pubinfo.add(statement);
    }

    /**
     * Adds a timestamp to the pubinfo part of the nanopublication.
     *
     * @param date the date to add
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addTimestamp(Date date) throws NanopubAlreadyFinalizedException {
        addPubinfoStatement(DCTERMS.CREATED, vf.createLiteral(date));
    }

    /**
     * Adds a timestamp to the pubinfo part of the nanopublication using the current time.
     *
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addTimestampNow() throws NanopubAlreadyFinalizedException {
        addPubinfoStatement(DCTERMS.CREATED, TimestampNow.getTimestamp());
    }

    /**
     * Adds a creator to the pubinfo part of the nanopublication.
     *
     * @param creator the creator IRI
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addCreator(IRI creator) throws NanopubAlreadyFinalizedException {
        addPubinfoStatement(DCTERMS.CREATOR, creator);
    }

    /**
     * Adds a creator to the pubinfo part of the nanopublication.
     *
     * @param orcidIdentifier the ORCID identifier of the creator
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addCreator(String orcidIdentifier) throws NanopubAlreadyFinalizedException {
        addCreator(getOrcidUri(orcidIdentifier));
    }

    /**
     * Adds an author to the pubinfo part of the nanopublication.
     *
     * @param author the author IRI
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addAuthor(IRI author) throws NanopubAlreadyFinalizedException {
        addPubinfoStatement(PAV.AUTHORED_BY, author);
    }

    /**
     * Adds an author to the pubinfo part of the nanopublication.
     *
     * @param orcidIdentifier the ORCID identifier of the author
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addAuthor(String orcidIdentifier) throws NanopubAlreadyFinalizedException {
        addAuthor(getOrcidUri(orcidIdentifier));
    }

    private IRI getOrcidUri(String orcid) {
        if (!orcid.startsWith("http://orcid.org/")) {
            orcid = "http://orcid.org/" + orcid;
        }
        return vf.createIRI(orcid);

    }

    /**
     * Adds a namespace to the nanopub.
     *
     * @param prefix    the prefix of the namespace
     * @param namespace the namespace URI
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addNamespace(String prefix, String namespace) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        nsPrefixes.add(prefix);
        ns.put(prefix, namespace);
    }

    /**
     * Adds a namespace to the nanopub.
     *
     * @param prefix    the prefix of the namespace
     * @param namespace the namespace URI
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addNamespace(String prefix, IRI namespace) throws NanopubAlreadyFinalizedException {
        addNamespace(prefix, namespace.toString());
    }

    /**
     * Adds a namespace to the nanopub.
     *
     * @param namespace the namespace to add
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addNamespace(Namespace namespace) throws NanopubAlreadyFinalizedException {
        addNamespace(namespace.getPrefix(), namespace.getName());
    }

    /**
     * Adds multiple namespaces to the nanopub.
     *
     * @param namespaces the namespaces to add
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addNamespaces(Namespace... namespaces) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        for (Namespace namespace : namespaces) {
            nsPrefixes.add(namespace.getPrefix());
            ns.put(namespace.getPrefix(), namespace.getName());
        }
    }

    /**
     * Adds multiple namespaces to the nanopub.
     *
     * @param namespaces the namespaces to add
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addNamespaces(Iterable<Namespace> namespaces) throws NanopubAlreadyFinalizedException {
        if (finalized) throw new NanopubAlreadyFinalizedException();
        for (Namespace namespace : namespaces) {
            nsPrefixes.add(namespace.getPrefix());
            ns.put(namespace.getPrefix(), namespace.getName());
        }
    }

    /**
     * Adds the default namespaces to the nanopub.
     * <p>
     * The default namespaces are defined in {@link org.nanopub.NanopubUtils#getDefaultNamespaces()}.
     *
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public void addDefaultNamespaces() throws NanopubAlreadyFinalizedException {
        addNamespace("this", nanopubUri);
        for (Pair<String, String> p : NanopubUtils.getDefaultNamespaces()) {
            addNamespace(p.getLeft(), p.getRight());
        }
    }

    /**
     * Removes unused prefixes from the nanopub.
     *
     * @param removeUnusedPrefixesEnabled whether to remove unused prefixes
     */
    public void setRemoveUnusedPrefixesEnabled(boolean removeUnusedPrefixesEnabled) {
        this.removeUnusedPrefixesEnabled = removeUnusedPrefixesEnabled;
    }

    /**
     * Finalizes the nanopub and returns it.
     * <p>
     * This method does not add a timestamp to the nanopub.
     *
     * @return the finalized nanopub
     * @throws MalformedNanopubException        if the nanopub is malformed
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public Nanopub finalizeNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        return finalizeNanopub(false);
    }

    /**
     * Finalizes the nanopub and returns it.
     * <p>
     * This method adds a timestamp to the nanopub if {@code addTimestamp} is true.
     *
     * @param addTimestamp whether to add a timestamp to the nanopub
     * @return the finalized nanopub
     * @throws MalformedNanopubException        if the nanopub is malformed
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized
     */
    public Nanopub finalizeNanopub(boolean addTimestamp) throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        if (finalized) {
            return nanopub;
        }
        if (nanopubUri == null) throw new MalformedNanopubException("No nanopub URI specified");
        if (addTimestamp) {
            addTimestamp(new Date());
        }
        collectStatements();
        nanopub = new NanopubImpl(statements, nsPrefixes, ns);
        if (removeUnusedPrefixesEnabled) {
            ((NanopubWithNs) nanopub).removeUnusedPrefixes();
        }
        finalized = true;
        return nanopub;
    }

    /**
     * Finalizes the nanopub and gives it a trusty URI. S<a href="ee">http://arxiv.org/abs/1401.5</a>775 and
     * <a href="https://github.com/trustyuri/trustyuri-java">...</a>
     * <p>
     * This method dynamically loads the TrustURI classes. Make sure you have the jar installed or
     * uncomment the entry in the pom file.
     *
     * @return the finalized nanopub with a trusty URI
     * @throws Exception if an error occurs during the transformation
     */
    public Nanopub finalizeTrustyNanopub() throws Exception {
        return finalizeTrustyNanopub(false);
    }

    /**
     * Finalizes the nanopub and gives it a trusty URI. S<a href="ee">http://arxiv.org/abs/1401.5</a>775 and
     * <a href="https://github.com/trustyuri/trustyuri-java">...</a>
     * <p>
     * This method dynamically loads the TrustURI classes. Make sure you have the jar installed or
     * uncomment the entry in the pom file.
     *
     * @param addTimestamp whether to add a timestamp to the nanopub
     * @return the finalized nanopub with a trusty URI
     * @throws Exception if an error occurs during the transformation
     */
    public Nanopub finalizeTrustyNanopub(boolean addTimestamp) throws Exception {
        Nanopub preNanopub = finalizeNanopub(addTimestamp);
        return MakeTrustyNanopub.transform(preNanopub);
    }

    private void collectStatements() {
        statements = new ArrayList<>();
        addStatement(nanopubUri, RDF.TYPE, NP.NANOPUBLICATION, headUri);
        addStatement(nanopubUri, NP.HAS_ASSERTION, assertionUri, headUri);
        addStatement(nanopubUri, NP.HAS_PROVENANCE, provenanceUri, headUri);
        addStatement(nanopubUri, NP.HAS_PUBINFO, pubinfoUri, headUri);
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

    /**
     * Returns the current assertion statements of the nanopublication.
     *
     * @return a list of current assertion statements
     */
    public List<Statement> getCurrentAssertionStatements() {
        return new ArrayList<>(assertion);
    }

    /**
     * Returns the current provenance statements of the nanopublication.
     *
     * @return a list of current provenance statements
     */
    public List<Statement> getCurrentProvenanceStatements() {
        return new ArrayList<>(provenance);
    }

    /**
     * Returns the current pubinfo statements of the nanopublication.
     *
     * @return a list of current pubinfo statements
     */
    public List<Statement> getCurrentPubinfoStatements() {
        return new ArrayList<>(pubinfo);
    }

}
