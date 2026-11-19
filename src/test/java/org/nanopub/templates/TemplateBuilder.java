package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

/**
 * Builds legacy-shape templates (the template node is the assertion graph URI) one
 * statement at a time, for tests.
 */
class TemplateBuilder {

    static final ValueFactory vf = SimpleValueFactory.getInstance();

    // A syntactically valid trusty URI shape (RA + 43 chars), so sub-IRIs strip correctly:
    static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";

    static final IRI SUBJECT = vf.createIRI("http://example.com/subject");
    static final IRI PREDICATE = vf.createIRI("http://example.com/predicate");

    static IRI local(String name) {
        return vf.createIRI(NP_URI + "/" + name);
    }


    final String npUri;
    final NanopubCreator creator;
    final IRI templateNode;
    private int statementCount = 0;

    TemplateBuilder(String npUri, IRI templateType) throws Exception {
        this.npUri = npUri;
        creator = new NanopubCreator(npUri);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        templateNode = creator.getAssertionUri();
        add(templateNode, RDF.TYPE, templateType);
    }

    TemplateBuilder(IRI templateType) throws Exception {
        this(NP_URI, templateType);
    }

    TemplateBuilder() throws Exception {
        this(NTEMPLATE.ASSERTION_TEMPLATE);
    }

    // An IRI in the namespace of this template.
    IRI iri(String name) {
        return vf.createIRI(npUri + "/" + name);
    }

    TemplateBuilder add(IRI subj, IRI pred, Value obj) throws Exception {
        creator.addAssertionStatement(subj, pred, obj);
        return this;
    }

    TemplateBuilder add(IRI subj, IRI pred, String literal) throws Exception {
        return add(subj, pred, vf.createLiteral(literal));
    }

    // Adds a top-level statement and returns its IRI.
    IRI statement(IRI subj, IRI pred, Value obj, IRI... types) throws Exception {
        IRI st = iri("st" + (++statementCount));
        add(templateNode, NTEMPLATE.HAS_STATEMENT, st);
        add(st, RDF.SUBJECT, subj).add(st, RDF.PREDICATE, pred).add(st, RDF.OBJECT, obj);
        for (IRI type : types) add(st, RDF.TYPE, type);
        return st;
    }

    // Declares a placeholder of the given types that is the object of a statement of its own.
    IRI placeholder(String name, IRI... types) throws Exception {
        IRI placeholder = iri(name);
        for (IRI type : types) add(placeholder, RDF.TYPE, type);
        statement(SUBJECT, PREDICATE, placeholder);
        return placeholder;
    }

    // Adds a grouped statement with the given members and returns its IRI. A member is
    // given as subject, predicate, object and whether it is optional.
    IRI group(String name, Object[][] members, IRI... types) throws Exception {
        IRI group = iri(name);
        add(templateNode, NTEMPLATE.HAS_STATEMENT, group);
        add(group, RDF.TYPE, NTEMPLATE.GROUPED_STATEMENT);
        for (IRI type : types) add(group, RDF.TYPE, type);
        int i = 0;
        for (Object[] m : members) {
            IRI member = iri(name + "-" + (char) ('a' + i++));
            add(group, NTEMPLATE.HAS_STATEMENT, member);
            add(member, RDF.SUBJECT, (IRI) m[0]).add(member, RDF.PREDICATE, (IRI) m[1]).add(member, RDF.OBJECT, (Value) m[2]);
            if (m.length > 3 && Boolean.TRUE.equals(m[3])) add(member, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        }
        return group;
    }

    // Declares a placeholder of the given types without a statement of its own.
    IRI declare(String name, IRI... types) throws Exception {
        IRI placeholder = iri(name);
        for (IRI type : types) add(placeholder, RDF.TYPE, type);
        return placeholder;
    }

    Nanopub buildNanopub() throws Exception {
        return creator.finalizeNanopub();
    }

    Template build() throws Exception {
        return new Template(creator.finalizeNanopub(), NanopubResolver.NONE);
    }

}
