package org.nanopub.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilsTest {

    @Test
    void createNamespaceReturnsCorrectNamespace() {
        Namespace namespace = Utils.createNamespace("ex", "http://example.org/");
        assertEquals("ex", namespace.getPrefix());
        assertEquals("http://example.org/", namespace.getName());
    }

    @Test
    void createNamespaceWithEmptyPrefixThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Utils.createNamespace("", "http://example.org/"));
    }

    @Test
    void createNamespaceWithEmptyNamespaceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Utils.createNamespace("ex", ""));
    }

    @Test
    void createIRIReturnsCorrectIRI() {
        IRI iri = Utils.createIRI("http://example.org/", "resource");
        assertEquals("http://example.org/resource", iri.toString());
    }

    @Test
    void createIRIWithEmptyNamespaceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Utils.createIRI("", "resource"));
    }

    @Test
    void createIRIWithEmptyLocalNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Utils.createIRI("http://example.org/", ""));
    }

    @Test
    void checkParameterWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Utils.createIRI(null, "resource"));
        assertThrows(IllegalArgumentException.class, () -> Utils.createIRI("http://example.org/", null));
    }

    @Test
    void checkParameterWithEmptyThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Utils.createIRI("", "resource"));
        assertThrows(IllegalArgumentException.class, () -> Utils.createIRI("http://example.org/", ""));
    }

}