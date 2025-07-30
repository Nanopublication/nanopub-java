package org.nanopub;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.trusty.MakeTrustyNanopub;
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
     */
    public NanopubCreator(boolean initWithTempNanopubIris) {
        this();
        if (initWithTempNanopubIris) {
            setNanopubUri(NanopubUtils.createTempNanopubIri());
        }
    }

    /**
     * Creates a new NanopubCreator with a specified nanopub URI.
     *
     * @param nanopubUri the nanopublication URI
     */
    public NanopubCreator(IRI nanopubUri) {
        this();
        setNanopubUri(nanopubUri);
    }

    /**
     * Creates a new NanopubCreator with a specified nanopub URI.
     *
     * @param nanopubUri the nanopublication URI as a string
     */
    public NanopubCreator(String nanopubUri) {
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
     */
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

    /**
     * Sets the nanopublication URI.
     *
     * @param nanopubUri the nanopublication URI as a string
     */
    public void setNanopubUri(String nanopubUri) {
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
     */
    public void setAssertionUri(IRI assertionUri) {
        if (finalized) throw new RuntimeException("Already finalized");
        if (assertionUriFixed) {
            throw new RuntimeException("Cannot change assertion URI anymore: has already been used");
        }
        this.assertionUri = assertionUri;
    }

    /**
     * Sets the assertion URI of the nanopublication.
     *
     * @param assertionUri the assertion URI as a string
     */
    public void setAssertionUri(String assertionUri) {
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
     */
    public void setProvenanceUri(IRI provenanceUri) {
        if (finalized) throw new RuntimeException("Already finalized");
        this.provenanceUri = provenanceUri;
    }

    /**
     * Sets the provenance URI of the nanopublication.
     *
     * @param provenanceUri the head URI as a string
     */
    public void setProvenanceUri(String provenanceUri) {
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
     */
    public void setPubinfoUri(IRI pubinfoUri) {
        if (finalized) throw new RuntimeException("Already finalized");
        this.pubinfoUri = pubinfoUri;
    }

    /**
     * Sets the pubinfo URI of the nanopublication.
     *
     * @param pubinfoUri the pubinfo URI as a string
     */
    public void setPubinfoUri(String pubinfoUri) {
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
     */
    public void addAssertionStatements(Statement... statements) {
        if (finalized) throw new RuntimeException("Already finalized");
        assertion.addAll(Arrays.asList(statements));
    }

    /**
     * Adds statements to the assertion part of the nanopublication.
     *
     * @param statements the statements to add
     */
    public void addAssertionStatements(Iterable<Statement> statements) {
        if (finalized) throw new RuntimeException("Already finalized");
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
     */
    public void addAssertionStatement(Resource subj, IRI pred, Value obj) {
        addAssertionStatements(vf.createStatement(subj, pred, obj));
    }

    /**
     * Adds a statement to the assertion part of the nanopublication.
     *
     * @param statement the predicate of the statement
     */
    public void addAssertionStatement(Statement statement) {
        if (finalized) throw new RuntimeException("Already finalized");
        assertion.add(statement);
    }

    /**
     * Adds statements to the provenance part of the nanopublication.
     *
     * @param statements the statements to add
     */
    public void addProvenanceStatements(Statement... statements) {
        if (finalized) throw new RuntimeException("Already finalized");
        provenance.addAll(Arrays.asList(statements));
    }

    /**
     * Adds statements to the provenance part of the nanopublication.
     *
     * @param statements the statements to add
     */
    public void addProvenanceStatements(Iterable<Statement> statements) {
        if (finalized) throw new RuntimeException("Already finalized");
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
     */
    public void addProvenanceStatement(Resource subj, IRI pred, Value obj) {
        addProvenanceStatements(vf.createStatement(subj, pred, obj));
    }

    /**
     * Adds a statement to the provenance part of the nanopublication.
     *
     * @param pred the predicate of the statement
     * @param obj  the object of the statement
     */
    public void addProvenanceStatement(IRI pred, Value obj) {
        if (assertionUri == null) throw new RuntimeException("Assertion URI not yet set");
        addProvenanceStatement(assertionUri, pred, obj);
        assertionUriFixed = true;
    }

    /**
     * Adds a statement to the provenance part of the nanopublication.
     *
     * @param statement the statement to add
     */
    public void addProvenanceStatement(Statement statement) {
        if (finalized) throw new RuntimeException("Already finalized");
        provenance.add(statement);
    }

    /**
     * Adds statements to the pubinfo part of the nanopublication.
     *
     * @param statements the statements to add
     */
    public void addPubinfoStatements(Statement... statements) {
        if (finalized) throw new RuntimeException("Already finalized");
        pubinfo.addAll(Arrays.asList(statements));
    }

    /**
     * Adds statements to the pubinfo part of the nanopublication.
     *
     * @param statements the statements to add
     */
    public void addPubinfoStatements(Iterable<Statement> statements) {
        if (finalized) throw new RuntimeException("Already finalized");
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
     */
    public void addPubinfoStatement(Resource subj, IRI pred, Value obj) {
        addPubinfoStatements(vf.createStatement(subj, pred, obj));
    }

    /**
     * Adds a statement to the pubinfo part of the nanopublication.
     *
     * @param pred the predicate of the statement
     * @param obj  the object of the statement
     */
    public void addPubinfoStatement(IRI pred, Value obj) {
        if (nanopubUri == null) throw new RuntimeException("Nanopublication URI not yet set");
        addPubinfoStatement(nanopubUri, pred, obj);
        nanopubUriFixed = true;
    }

    /**
     * Adds a statement to the pubinfo part of the nanopublication.
     *
     * @param statement the statement to add
     */
    public void addPubinfoStatement(Statement statement) {
        if (finalized) throw new RuntimeException("Already finalized");
        pubinfo.add(statement);
    }

    /**
     * Adds a timestamp to the pubinfo part of the nanopublication.
     *
     * @param date the date to add
     */
    public void addTimestamp(Date date) {
        addPubinfoStatement(DCTERMS.CREATED, vf.createLiteral(date));
    }

    /**
     * Adds a timestamp to the pubinfo part of the nanopublication using the current time.
     */
    public void addTimestampNow() {
        addPubinfoStatement(DCTERMS.CREATED, TimestampNow.getTimestamp());
    }

    /**
     * Adds a creator to the pubinfo part of the nanopublication.
     *
     * @param creator the creator IRI
     */
    public void addCreator(IRI creator) {
        addPubinfoStatement(DCTERMS.CREATOR, creator);
    }

    /**
     * Adds a creator to the pubinfo part of the nanopublication.
     *
     * @param orcidIdentifier the ORCID identifier of the creator
     */
    public void addCreator(String orcidIdentifier) {
        addCreator(getOrcidUri(orcidIdentifier));
    }

    /**
     * Adds an author to the pubinfo part of the nanopublication.
     *
     * @param author the author IRI
     */
    public void addAuthor(IRI author) {
        addPubinfoStatement(PAV.AUTHORED_BY, author);
    }

    /**
     * Adds an author to the pubinfo part of the nanopublication.
     *
     * @param orcidIdentifier the ORCID identifier of the author
     */
    public void addAuthor(String orcidIdentifier) {
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
     */
    public void addNamespace(String prefix, String namespace) {
        if (finalized) throw new RuntimeException("Already finalized");
        nsPrefixes.add(prefix);
        ns.put(prefix, namespace);
    }

    /**
     * Adds a namespace to the nanopub.
     *
     * @param prefix    the prefix of the namespace
     * @param namespace the namespace URI
     */
    public void addNamespace(String prefix, IRI namespace) {
        addNamespace(prefix, namespace.toString());
    }

    /**
     * Adds a namespace to the nanopub.
     *
     * @param namespace the namespace to add
     */
    public void addNamespace(Namespace namespace) {
        addNamespace(namespace.getPrefix(), namespace.getName());
    }

    /**
     * Adds multiple namespaces to the nanopub.
     *
     * @param namespaces the namespaces to add
     */
    public void addNamespaces(Namespace... namespaces) {
        if (finalized) throw new RuntimeException("Already finalized");
        for (Namespace namespace : namespaces) {
            nsPrefixes.add(namespace.getPrefix());
            ns.put(namespace.getPrefix(), namespace.getName());
        }
    }

    /**
     * Adds multiple namespaces to the nanopub.
     *
     * @param namespaces the namespaces to add
     */
    public void addNamespaces(Iterable<Namespace> namespaces) {
        if (finalized) throw new RuntimeException("Already finalized");
        for (Namespace namespace : namespaces) {
            nsPrefixes.add(namespace.getPrefix());
            ns.put(namespace.getPrefix(), namespace.getName());
        }
    }

    /**
     * Adds the default namespaces to the nanopub.
     * <p>
     * The default namespaces are defined in {@link org.nanopub.NanopubUtils#getDefaultNamespaces()}.
     */
    public void addDefaultNamespaces() {
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
     * @throws org.nanopub.MalformedNanopubException if the nanopub is malformed
     */
    public Nanopub finalizeNanopub() throws MalformedNanopubException {
        return finalizeNanopub(false);
    }

    /**
     * Finalizes the nanopub and returns it.
     * <p>
     * This method adds a timestamp to the nanopub if {@code addTimestamp} is true.
     *
     * @param addTimestamp whether to add a timestamp to the nanopub
     * @return the finalized nanopub
     * @throws org.nanopub.MalformedNanopubException if the nanopub is malformed
     */
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
     * @throws java.lang.Exception if an error occurs during the transformation
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
     * @throws java.lang.Exception if an error occurs during the transformation
     */
    public Nanopub finalizeTrustyNanopub(boolean addTimestamp) throws Exception {
        Nanopub preNanopub = finalizeNanopub(addTimestamp);
        return MakeTrustyNanopub.transform(preNanopub);
    }

    private void collectStatements() {
        statements = new ArrayList<>();
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
