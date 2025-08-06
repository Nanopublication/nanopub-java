package org.nanopub.extra.security;

import com.beust.jcommander.ParameterException;
import org.junit.jupiter.api.Test;
import org.nanopub.CliRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MakeKeysTest {

    @Test
    void initWithoutParams() {
        CliRunner.initJc(new MakeKeys(), new String[0]);
        // LATER we may test if keys at default location don't get overwritten
    }

    @Test
    void initInvalidParams() {
        assertThrows(ParameterException.class, () -> CliRunner.initJc(new MakeKeys(), new String[]{"AnyWrong"}));
    }

    @Test
    void initValidParams() {

        // sig alg specified
        CliRunner.initJc(new MakeKeys(), new String[]{"-a", "RSA"});
        CliRunner.initJc(new MakeKeys(), new String[]{"-a", "DSA"});

        // path specified
        final String homeFolder = System.getProperty("user.home"); // Windows compatible since Java 8
        CliRunner.initJc(new MakeKeys(), new String[]{"-f", homeFolder});

        // both specified
        CliRunner.initJc(new MakeKeys(), new String[]{"-a", "DSA", "-f", homeFolder});
    }

    @Test
    void makeCreatesKeyFiles() throws IOException {
        String path = Objects.requireNonNull(this.getClass().getResource("/")).getPath();
        String prefix = "id";
        String pathAndFileName = path + "/" + prefix;
        String algorithmName = SignatureAlgorithm.RSA.name().toLowerCase();
        File privateKey = new File(pathAndFileName + "_" + algorithmName);
        File publicKey = new File(pathAndFileName + "_" + algorithmName + ".pub");
        if (privateKey.exists()) {
            assertTrue(privateKey.delete(), "Failed to delete existing private key file");
        }
        if (publicKey.exists()) {
            assertTrue(publicKey.delete(), "Failed to delete existing public key file");
        }
        MakeKeys.make(pathAndFileName, SignatureAlgorithm.RSA);

        assertTrue(privateKey.exists());
        assertTrue(publicKey.exists());
        assertTrue(privateKey.length() > 0);
        assertTrue(publicKey.length() > 0);

        assertTrue(privateKey.delete(), "Failed to delete private key file");
        assertTrue(publicKey.delete(), "Failed to delete public key file");
    }

    @Test
    void makeThrowsExceptionForExistingPrivateKeyFile() throws IOException {
        String path = Objects.requireNonNull(this.getClass().getResource("/")).getPath();
        String prefix = "id";
        String pathAndFileName = path + "/" + prefix;
        String algorithmName = SignatureAlgorithm.RSA.name().toLowerCase();
        File privateKey = new File(pathAndFileName + "_" + algorithmName);
        File publicKey = new File(pathAndFileName + "_" + algorithmName + ".pub");
        if (privateKey.exists()) {
            assertTrue(privateKey.delete(), "Failed to delete existing private key file");
        }
        if (publicKey.exists()) {
            assertTrue(publicKey.delete(), "Failed to delete existing public key file");
        }
        MakeKeys.make(pathAndFileName, SignatureAlgorithm.RSA);

        publicKey.delete();
        assertThrows(FileAlreadyExistsException.class, () -> MakeKeys.make(pathAndFileName, SignatureAlgorithm.RSA));

        privateKey.delete();
    }

    @Test
    void makeThrowsExceptionForExistingPublicKeyFile() throws IOException {
        String path = Objects.requireNonNull(this.getClass().getResource("/")).getPath();
        String prefix = "id";
        String pathAndFileName = path + "/" + prefix;
        String algorithmName = SignatureAlgorithm.RSA.name().toLowerCase();
        File privateKey = new File(pathAndFileName + "_" + algorithmName);
        File publicKey = new File(pathAndFileName + "_" + algorithmName + ".pub");
        if (privateKey.exists()) {
            assertTrue(privateKey.delete(), "Failed to delete existing private key file");
        }
        if (publicKey.exists()) {
            assertTrue(publicKey.delete(), "Failed to delete existing public key file");
        }
        MakeKeys.make(pathAndFileName, SignatureAlgorithm.RSA);

        privateKey.delete();
        assertThrows(FileAlreadyExistsException.class, () -> MakeKeys.make(pathAndFileName, SignatureAlgorithm.RSA));

        publicKey.delete();
    }

}