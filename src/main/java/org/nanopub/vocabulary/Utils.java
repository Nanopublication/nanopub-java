package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.AbstractNamespace;
import org.eclipse.rdf4j.model.base.InternedIRI;

/**
 * Utility class for creating RDF4J namespaces and IRIs.
 * This class provides methods to create namespaces and IRIs used within the vocabulary.
 */
public class Utils {

    private Utils() {
    }

    static Namespace createNamespace(String prefix, String namespace) {
        return new VocabularyNamespace(prefix, namespace);
    }

    static IRI createIRI(String namespace, String localName) {
        return new InternedIRI(namespace, localName);
    }

    private static class VocabularyNamespace extends AbstractNamespace {

        private final String prefix;
        private final String namespace;

        /**
         * Constructs a new VocabularyNamespace with the given prefix and namespace.
         *
         * @param prefix    the prefix of the namespace
         * @param namespace the full namespace URI
         */
        public VocabularyNamespace(String prefix, String namespace) {
            this.prefix = prefix;
            this.namespace = namespace;
        }

        /**
         * <@inheritDoc>
         */
        @Override
        public String getPrefix() {
            return this.prefix;
        }

        /**
         * <@inheritDoc>
         */
        @Override
        public String getName() {
            return this.namespace;
        }

    }

}
