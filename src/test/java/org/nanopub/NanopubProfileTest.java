package org.nanopub;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nanopub.testsuite.NanopubTestSuite;

class NanopubProfileTest {

    @Test
    void constructorWithInvalidProfileFile() {
        String profileFileName = Objects.requireNonNull(this.getClass().getResource("/")).getPath() + "profile.yml";
        File profileFile = new File(profileFileName);
        assertFalse(profileFile.exists());

        NanopubProfile profile = new NanopubProfile(profileFileName);
        assertNotNull(profile);
    }

    @Test
    void constructorWithInvalidYamlFile() {
        String profileFileName = Objects.requireNonNull(this.getClass().getResource("/invalid-profile.yaml")).getPath();
        assertThrows(RuntimeException.class, () -> new NanopubProfile(profileFileName));
    }

    @Test
    void constructorWithUnreadableProfileFile(@TempDir File tempDir) {
        // A directory passes the exists() check but cannot be opened as a stream,
        // so the IOException branch of the constructor is taken.
        File directoryPosingAsProfile = new File(tempDir, "profile.yaml");
        assertTrue(directoryPosingAsProfile.mkdir());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new NanopubProfile(directoryPosingAsProfile.getPath()));
        assertInstanceOf(IOException.class, ex.getCause());
    }

    @Test
    void constructorWithValidProfileFile() {
        File profileFile = NanopubTestSuite.getLatest().getTransformProfile();
        assertTrue(profileFile.exists());

        NanopubProfile profile = new NanopubProfile(profileFile.getPath());
        assertNotNull(profile);
        assertNotNull(profile.getPrivateKeyPath());
        assertNotNull(profile.getOrcidId());
    }

    @Test
    void getPrivateKeyPath() {
        String profileFileName = NanopubTestSuite.getLatest().getTransformProfile().getPath();
        NanopubProfile profile = new NanopubProfile(profileFileName);
        assertEquals("src/test/resources/testsuite/transform/signed/rsa-key2/key/id_rsa", profile.getPrivateKeyPath());
    }

    @Test
    void getOrcid() {
        String profileFileName = NanopubTestSuite.getLatest().getTransformProfile().getPath();
        NanopubProfile profile = new NanopubProfile(profileFileName);
        assertEquals("https://orcid.org/0000-0000-0000-0000", profile.getOrcidId());
    }

}
