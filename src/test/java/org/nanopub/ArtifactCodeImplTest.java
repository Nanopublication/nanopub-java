package org.nanopub;

import net.trustyuri.rdf.RdfModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactCodeImplTest {

    private final String validArtifactCode = "RAQkRgam5soAC8p2audYEK88QTJjhxLqrDWP6siwwkr5c";

    @Test
    void constructWithValidCode() {
        ArtifactCode artifactCode = ArtifactCode.of(validArtifactCode);
        assertEquals(validArtifactCode, artifactCode.getCode());
    }

    @Test
    void constructWithInvalidCode() {
        assertNull(ArtifactCode.of("INVALID_CODE"));
    }

    @Test
    void testEquals() {
        ArtifactCode code1 = ArtifactCode.of(validArtifactCode);
        ArtifactCode code2 = ArtifactCode.of("RAoYy0bW33mUXlf6MOe3Q09AMualJr4D99z9vtEHFVsgE");
        ArtifactCode code3 = ArtifactCode.of(validArtifactCode);
        assertEquals(code1, code3);
        assertNotEquals(code1, code2);
    }

    @Test
    void testHashCode() {
        ArtifactCode code1 = ArtifactCode.of(validArtifactCode);
        ArtifactCode code2 = ArtifactCode.of(validArtifactCode);
        assertEquals(code1.hashCode(), code2.hashCode());
    }

    @Test
    void testToString() {
        ArtifactCode artifactCode = ArtifactCode.of(validArtifactCode);
        assertEquals("ArtifactCode{code='" + validArtifactCode + "'}", artifactCode.toString());
    }

    @Test
    void testGetModule() {
        ArtifactCode artifactCode = ArtifactCode.of(validArtifactCode);
        assertInstanceOf(RdfModule.class, artifactCode.getModule());
        assertEquals(RdfModule.MODULE_ID, artifactCode.getModule().getModuleId());
    }

}