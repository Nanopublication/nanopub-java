package org.nanopub.templates;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
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


    final NanopubCreator creator;
    final IRI templateNode;
    private int statementCount = 0;

    TemplateBuilder(IRI templateType) throws Exception {
        creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        templateNode = creator.getAssertionUri();
        add(templateNode, RDF.TYPE, templateType);
    }

    TemplateBuilder() throws Exception {
        this(NTEMPLATE.ASSERTION_TEMPLATE);
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
        IRI st = local("st" + (++statementCount));
        add(templateNode, NTEMPLATE.HAS_STATEMENT, st);
        add(st, RDF.SUBJECT, subj).add(st, RDF.PREDICATE, pred).add(st, RDF.OBJECT, obj);
        for (IRI type : types) add(st, RDF.TYPE, type);
        return st;
    }

    // Declares a placeholder of the given types that is the object of a statement of its own.
    IRI placeholder(String name, IRI... types) throws Exception {
        IRI placeholder = local(name);
        for (IRI type : types) add(placeholder, RDF.TYPE, type);
        statement(SUBJECT, PREDICATE, placeholder);
        return placeholder;
    }

    Template build() throws Exception {
        return new Template(creator.finalizeNanopub(), NanopubResolver.NONE);
    }

}
