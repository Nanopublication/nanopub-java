package org.nanopub;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NanopubProfileTest {

    @Test
    void constructorWithInvalidProfileFile() {
        String profileFileName = Objects.requireNonNull(this.getClass().getResource("/")).getPath() + "profile.yml";
        File profileFile = new File(profileFileName);
        Assertions.assertFalse(profileFile.exists());

        NanopubProfile profile = new NanopubProfile(profileFileName);
        Assertions.assertNotNull(profile);
    }

    @Test
    void constructorWithInvalidYamlFile() {
        String profileFileName = Objects.requireNonNull(this.getClass().getResource("/invalid-profile.yaml")).getPath();
        assertThrows(RuntimeException.class, () -> new NanopubProfile(profileFileName));
    }

    @Test
    void constructorWithValidProfileFile() {
        String profileFileName = Objects.requireNonNull(this.getClass().getResource("/testsuite/transform/profile.yaml")).getPath();
        File profileFile = new File(profileFileName);
        Assertions.assertTrue(profileFile.exists());

        NanopubProfile profile = new NanopubProfile(profileFileName);
        Assertions.assertNotNull(profile);
        Assertions.assertNotNull(profile.getPrivateKeyPath());
        Assertions.assertNotNull(profile.getOrcidId());
    }

    @Test
    void getPrivateKeyPath() {
        String profileFileName = Objects.requireNonNull(this.getClass().getResource("/testsuite/transform/profile.yaml")).getPath();
        NanopubProfile profile = new NanopubProfile(profileFileName);
        assertEquals("src/test/resources/testsuite/transform/signed/rsa-key2/key/id_rsa", profile.getPrivateKeyPath());
    }

    @Test
    void getOrcid() {
        String profileFileName = Objects.requireNonNull(this.getClass().getResource("/testsuite/transform/profile.yaml")).getPath();
        NanopubProfile profile = new NanopubProfile(profileFileName);
        assertEquals("https://orcid.org/0000-0000-0000-0000", profile.getOrcidId());
    }

}