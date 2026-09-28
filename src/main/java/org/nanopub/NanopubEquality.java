package org.nanopub;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;

import java.util.Collection;
import java.util.Objects;

/**
 * Utility for checking whether two nanopubs have the same statements, ignoring differences in their temporary head IRIs and {@code dct:created} timestamps.
 * This is useful for checking that two unsigned nanopubs are the same, even when they are finalized.
 */
public final class NanopubEquality {

    /**
     * Sentinel used to replace each nanopub's own temp IRI before comparison.
     */
    private static final IRI SENTINEL = Values.iri("urn:nanopub:equality:sentinel");

    private NanopubEquality() {
    }

    /**
     * Returns {@code true} when {@code a} and {@code b} contain the same triples
     * apart from their temporary head IRIs and {@code dct:created} timestamps.
     *
     * @param a first unsigned nanopub
     * @param b second unsigned nanopub
     * @return {@code true} iff the two nanopubs are equal apart from their temp IRIs and creation timestamps
     */
    public static boolean unsignedNanopubsAreEqual(Nanopub a, Nanopub b) {
        Objects.requireNonNull(a, "first nanopub must not be null");
        Objects.requireNonNull(b, "second nanopub must not be null");
        return normalize(a).equals(normalize(b));
    }

    /**
     * Produces a {@link Model} of all triples in the nanopub after:
     * <ul>
     *   <li>replacing every occurrence of the nanopub's own IRI with {@link #SENTINEL}</li>
     *   <li>dropping the {@code dct:created} statement</li>
     * </ul>
     * Graph context IRIs (named graphs) are preserved as part of each statement's
     * context so that differences in graph membership are still detected.
     */
    private static Model normalize(Nanopub np) {
        IRI npIri = np.getUri();
        Model model = new LinkedHashModel();
        collectGraph(np.getHead(), npIri, model);
        collectGraph(np.getAssertion(), npIri, model);
        collectGraph(np.getProvenance(), npIri, model);
        collectPubinfoWithoutCreated(np.getPubinfo(), npIri, model);
        return model;
    }

    private static void collectGraph(Collection<Statement> statements, IRI npIri, Model target) {
        for (Statement st : statements) {
            target.add(rewrite(st, npIri));
        }
    }

    private static void collectPubinfoWithoutCreated(Collection<Statement> statements, IRI npIri, Model target) {
        for (Statement st : statements) {
            if (st.getPredicate().equals(DCTERMS.CREATED)) {
                continue; // drop the timestamp — it differs between instances
            }
            target.add(rewrite(st, npIri));
        }
    }

    /**
     * Returns a copy of {@code st} with every occurrence of {@code npIri}
     * (in subject, predicate, object, or context) replaced by {@link #SENTINEL}.
     */
    private static Statement rewrite(Statement st, IRI npIri) {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();

        Resource subject = (Resource) replaceIfMatch(st.getSubject(), npIri);
        IRI predicate = (IRI) replaceIfMatch(st.getPredicate(), npIri);
        Value object = replaceIfMatch(st.getObject(), npIri);
        Resource context = (Resource) replaceIfMatch(st.getContext(), npIri);

        return vf.createStatement(subject, predicate, object, context);
    }

    private static Value replaceIfMatch(Value value, IRI npIri) {
        if (value == null) {
            return SENTINEL; // treat a null context as the sentinel too
        }
        if (!(value instanceof IRI candidate)) {
            return value;
        }
        String candidateStr = candidate.stringValue();
        String npIriStr = npIri.stringValue();
        if (candidateStr.equals(npIriStr) || candidateStr.startsWith(npIriStr)) {
            return SENTINEL;
        }
        return value;
    }

}
