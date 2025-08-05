package org.nanopub.extra.security;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.utils.TestUtils;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

class KeyDeclarationTest {

    private final String keyLocation = "https://nanodash.knowledgepixels.com/";
    private KeyDeclaration keyDeclaration;

    @BeforeEach
    void setUp() {
        keyDeclaration = new KeyDeclaration(iri(keyLocation));
    }

    @Test
    void getKeyLocationReturnsSetKeyLocation() {
        String anotherKeyLocation = "https://nanodash.knowledgepixels.com/newLocation";
        IRI anotherKeyLocationIri = iri(anotherKeyLocation);

        assertNotEquals(anotherKeyLocationIri, keyDeclaration.getKeyLocation());
        keyDeclaration.setKeyLocation(anotherKeyLocationIri);
        assertEquals(anotherKeyLocationIri, keyDeclaration.getKeyLocation());
    }

    @Test
    void getKeyLocationReturnsNullWhenKeyLocationNotSet() {
        assertNull(keyDeclaration.getKeyLocation());
        assertEquals(keyDeclaration.getUri(), iri(keyLocation));
    }

    @Test
    void addDeclarerAddsValidDeclarerToSet() {
        IRI declarer = iri(TestUtils.ORCID);

        keyDeclaration.addDeclarer(declarer);
        assertTrue(keyDeclaration.getDeclarers().contains(declarer));
    }

    @Test
    void addDeclarerDoesNotAddDuplicateDeclarer() {
        IRI declarer = iri(TestUtils.ORCID);

        keyDeclaration.addDeclarer(declarer);
        keyDeclaration.addDeclarer(declarer);

        assertEquals(1, keyDeclaration.getDeclarers().size());
    }


    @Test
    void getDeclarersReturnsEmptySetWhenNoDeclarersAdded() {
        assertTrue(keyDeclaration.getDeclarers().isEmpty());
    }

    @Test
    void getDeclarersReturnsSetWithAddedDeclarers() {
        IRI declarer1 = iri(TestUtils.ORCID);
        IRI declarer2 = iri("https://orcid.org/0000-0000-0000-0001");

        keyDeclaration.addDeclarer(declarer1);
        keyDeclaration.addDeclarer(declarer2);

        assertTrue(keyDeclaration.getDeclarers().contains(declarer1));
        assertTrue(keyDeclaration.getDeclarers().contains(declarer2));
        assertEquals(2, keyDeclaration.getDeclarers().size());
    }

    @Test
    void hasDeclarerReturnsTrueForExistingDeclarer() {
        IRI declarer = iri(TestUtils.ORCID);

        keyDeclaration.addDeclarer(declarer);
        assertTrue(keyDeclaration.hasDeclarer(declarer));
    }

}