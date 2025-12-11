package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.AbstractNamespace;
import org.eclipse.rdf4j.model.base.InternedIRI;

/**
 * Utility class for creating RDF4J namespaces and IRIs.
 * This class provides methods to create namespaces and IRIs used within the vocabulary.
 */
public class VocabUtils {

    private VocabUtils() {
    }

    /**
     * Creates a new Namespace with the given prefix and namespace URI.
     *
     * @param prefix    the prefix of the namespace
     * @param namespace the full namespace URI
     * @return a Namespace object
     */
    public static Namespace createNamespace(String prefix, String namespace) {
        return new VocabularyNamespace(prefix, namespace);
    }

    /**
     * Creates a new IRI by combining the given namespace and local name.
     *
     * @param namespace the namespace URI
     * @param localName the local name
     * @return an IRI object
     */
    public static IRI createIRI(String namespace, String localName) {
        checkParameter(namespace, "Namespace");
        checkParameter(localName, "Local Name");
        return new InternedIRI(namespace, localName);
    }

    /**
     * Checks if the given parameter is null or empty.
     *
     * @param parameter     the parameter to check
     * @param parameterName the name of the parameter (for error messages)
     * @throws IllegalArgumentException if the parameter is null or empty
     */
    private static void checkParameter(String parameter, String parameterName) {
        if (parameter == null || parameter.isEmpty()) {
            throw new IllegalArgumentException(parameterName + " cannot be null or empty");
        }
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
            checkParameter(prefix, "Prefix");
            checkParameter(namespace, "Namespace");
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
