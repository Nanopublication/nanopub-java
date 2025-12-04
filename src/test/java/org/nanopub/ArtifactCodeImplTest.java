package org.nanopub;

import net.trustyuri.rdf.RdfModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactCodeImplTest {

    private final String validArtifactCode = "RAQkRgam5soAC8p2audYEK88QTJjhxLqrDWP6siwwkr5c";

    @Test
    void constructWithValidCode() {
        ArtifactCodeImpl artifactCode = new ArtifactCodeImpl(validArtifactCode);
        assertEquals(validArtifactCode, artifactCode.getCode());
    }

    @Test
    void constructWithInvalidCode() {
        assertThrows(IllegalArgumentException.class, () -> new ArtifactCodeImpl("INVALID_CODE"));
    }

    @Test
    void testEquals() {
        ArtifactCodeImpl code1 = new ArtifactCodeImpl(validArtifactCode);
        ArtifactCodeImpl code2 = new ArtifactCodeImpl("RAoYy0bW33mUXlf6MOe3Q09AMualJr4D99z9vtEHFVsgE");
        ArtifactCodeImpl code3 = new ArtifactCodeImpl(validArtifactCode);
        assertEquals(code1, code3);
        assertNotEquals(code1, code2);
    }

    @Test
    void testHashCode() {
        ArtifactCodeImpl code1 = new ArtifactCodeImpl(validArtifactCode);
        ArtifactCodeImpl code2 = new ArtifactCodeImpl(validArtifactCode);
        assertEquals(code1.hashCode(), code2.hashCode());
    }

    @Test
    void testToString() {
        ArtifactCodeImpl artifactCode = new ArtifactCodeImpl(validArtifactCode);
        assertEquals("ArtifactCode{code='" + validArtifactCode + "'}", artifactCode.toString());
    }

    @Test
    void testGetModule() {
        ArtifactCodeImpl artifactCode = new ArtifactCodeImpl(validArtifactCode);
        assertInstanceOf(RdfModule.class, artifactCode.getModule());
        assertEquals(RdfModule.MODULE_ID, artifactCode.getModule().getModuleId());
    }

}