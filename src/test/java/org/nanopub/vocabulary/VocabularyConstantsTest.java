package org.nanopub.vocabulary;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks the vocabulary classes, which do nothing but hold constants. Rather
 * than restating every single IRI, this pins the namespace and prefix of each
 * vocabulary and then asserts the invariants that all of their IRI constants
 * have to satisfy.
 */
class VocabularyConstantsTest {

    /**
     * A vocabulary class together with the namespace and prefix it is expected
     * to declare. {@code prefix} is null for the vocabularies that do not
     * declare one.
     */
    private record Vocabulary(Class<?> type, String namespace, String prefix) {

        @Override
        public String toString() {
            return type.getSimpleName();
        }

    }

    static Stream<Vocabulary> vocabularies() {
        return Stream.of(
                new Vocabulary(FDOC.class, "https://w3id.org/fdoc/o/terms/", "fdoc"),
                new Vocabulary(FDOF.class, "https://w3id.org/fdof/ontology#", "fdof"),
                new Vocabulary(FIP.class, "https://w3id.org/fair/fip/terms/", "fip"),
                new Vocabulary(HDL.class, "https://hdl.handle.net/", "hdl"),
                new Vocabulary(KPXL.class, "https://w3id.org/kpxl/gen/terms/", null),
                new Vocabulary(KPXL_GRLC.class, "https://w3id.org/kpxl/grlc/", "kpxl_grlc"),
                new Vocabulary(NP.class, "http://www.nanopub.org/nschema#", "np"),
                new Vocabulary(NPA.class, "http://purl.org/nanopub/admin/", "npa"),
                new Vocabulary(NPS.class, "https://w3id.org/np/o/service/terms/", "nps"),
                new Vocabulary(NPX.class, "http://purl.org/nanopub/x/", "npx"),
                new Vocabulary(NTEMPLATE.class, "https://w3id.org/np/o/ntemplate/", "ntemplate"),
                new Vocabulary(PAV.class, "http://purl.org/pav/", "pav"),
                new Vocabulary(RDFG.class, "http://www.w3.org/2004/03/trix/rdfg-1/", "rdfg"),
                new Vocabulary(SCHEMA.class, "http://schema.org/", "schema")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vocabularies")
    void declaresItsNamespace(Vocabulary vocabulary) throws Exception {
        assertEquals(vocabulary.namespace(), constant(vocabulary.type(), "NAMESPACE"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vocabularies")
    void declaresAPrefixAndAMatchingNamespaceObject(Vocabulary vocabulary) throws Exception {
        if (vocabulary.prefix() == null) {
            assertFalse(hasConstant(vocabulary.type(), "PREFIX"),
                    vocabulary + " unexpectedly declares a PREFIX");
            assertFalse(hasConstant(vocabulary.type(), "NS"),
                    vocabulary + " unexpectedly declares an NS");
            return;
        }
        assertEquals(vocabulary.prefix(), constant(vocabulary.type(), "PREFIX"));

        Namespace ns = (Namespace) constant(vocabulary.type(), "NS");
        assertEquals(vocabulary.prefix(), ns.getPrefix());
        assertEquals(vocabulary.namespace(), ns.getName());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vocabularies")
    void iriConstantsAreInsideTheirOwnNamespace(Vocabulary vocabulary) throws Exception {
        for (Field field : iriFields(vocabulary.type())) {
            IRI iri = (IRI) field.get(null);
            assertNotNull(iri, vocabulary + "." + field.getName() + " is null");
            assertTrue(iri.stringValue().startsWith(vocabulary.namespace()),
                    vocabulary + "." + field.getName() + " is outside its namespace: " + iri);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vocabularies")
    void iriConstantsAreDistinct(Vocabulary vocabulary) throws Exception {
        Set<String> seen = new HashSet<>();
        for (Field field : iriFields(vocabulary.type())) {
            String iri = ((IRI) field.get(null)).stringValue();
            assertTrue(seen.add(iri), vocabulary + "." + field.getName() + " duplicates " + iri);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vocabularies")
    void holdsNothingButConstants(Vocabulary vocabulary) {
        for (Field field : vocabulary.type().getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            assertTrue(Modifier.isStatic(field.getModifiers()),
                    vocabulary + "." + field.getName() + " is not static");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vocabularies")
    void cannotBeInstantiated(Vocabulary vocabulary) throws Exception {
        // a vocabulary only holds constants, so it must declare a private constructor rather than
        // leaving the implicit public one in its API
        Constructor<?>[] constructors = vocabulary.type().getDeclaredConstructors();
        assertEquals(1, constructors.length, vocabulary + " should declare exactly one constructor");
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()),
                vocabulary + " must declare a private constructor");

        assertThrows(IllegalAccessException.class, () -> vocabulary.type().getDeclaredConstructor().newInstance());
    }

    private static Object constant(Class<?> type, String name) throws Exception {
        return type.getField(name).get(null);
    }

    private static boolean hasConstant(Class<?> type, String name) {
        try {
            type.getField(name);
            return true;
        } catch (NoSuchFieldException ex) {
            return false;
        }
    }

    private static List<Field> iriFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == IRI.class) {
                fields.add(field);
            }
        }
        return fields;
    }

}
