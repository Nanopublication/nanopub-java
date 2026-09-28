package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VocabUtilsTest {

    @Test
    void createNamespaceReturnsCorrectNamespace() {
        Namespace namespace = VocabUtils.createNamespace("ex", "http://example.org/");
        assertEquals("ex", namespace.getPrefix());
        assertEquals("http://example.org/", namespace.getName());
    }

    @Test
    void createNamespaceWithEmptyPrefixThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> VocabUtils.createNamespace("", "http://example.org/"));
    }

    @Test
    void createNamespaceWithEmptyNamespaceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> VocabUtils.createNamespace("ex", ""));
    }

    @Test
    void createIRIReturnsCorrectIRI() {
        IRI iri = VocabUtils.createIRI("http://example.org/", "resource");
        assertEquals("http://example.org/resource", iri.toString());
    }

    @Test
    void createIRIWithEmptyNamespaceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> VocabUtils.createIRI("", "resource"));
    }

    @Test
    void createIRIWithEmptyLocalNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> VocabUtils.createIRI("http://example.org/", ""));
    }

    @Test
    void checkParameterWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> VocabUtils.createIRI(null, "resource"));
        assertThrows(IllegalArgumentException.class, () -> VocabUtils.createIRI("http://example.org/", null));
    }

    @Test
    void checkParameterWithEmptyThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> VocabUtils.createIRI("", "resource"));
        assertThrows(IllegalArgumentException.class, () -> VocabUtils.createIRI("http://example.org/", ""));
    }

}