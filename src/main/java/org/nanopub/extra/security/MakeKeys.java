package org.nanopub.extra.security;

import com.beust.jcommander.ParameterException;
import jakarta.xml.bind.DatatypeConverter;
import org.nanopub.CliRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

/**
 * Command line tool to create RSA or DSA key files for signing nanopublications.
 */
public class MakeKeys extends CliRunner {

    /**
     * Default path and file name prefix for key files.
     */
    public static final String DEFAULT_KEYFILE_PREFIX = "~/.nanopub/id";

    @com.beust.jcommander.Parameter(names = "-f", description = "Path and file name prefix of key files")
    private String pathAndFilenamePrefix = DEFAULT_KEYFILE_PREFIX;

    @com.beust.jcommander.Parameter(names = "-a", description = "Signature algorithm: either RSA or DSA, default is RSA")
    private SignatureAlgorithm algorithm = SignatureAlgorithm.RSA;

    /**
     * Main method to run the MakeKeys command line tool.
     *
     * @param args Command line arguments
     * @throws IOException If an I/O error occurs while creating key files
     */
    public static void main(String[] args) throws IOException {
        try {
            MakeKeys obj = CliRunner.initJc(new MakeKeys(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private void run() throws IOException {
        make(pathAndFilenamePrefix, algorithm);
        System.out.println("Created " + algorithm + " key files at: " + pathAndFilenamePrefix);
    }

    /**
     * Creates RSA or DSA key files at the specified path and filename prefix.
     *
     * @param pathAndFilenamePrefix The path and filename prefix for the key files
     * @param algorithm             The signature algorithm to use (RSA or DSA)
     * @throws IOException If an I/O error occurs while creating key files
     */
    public static void make(String pathAndFilenamePrefix, SignatureAlgorithm algorithm) throws IOException {

        // Preparation:
        String pathAndFilename = SignatureUtils.getFullFilePath(pathAndFilenamePrefix) + "_" + algorithm.name().toLowerCase();
        File publicKeyFile = new File(pathAndFilename + ".pub");
        if (publicKeyFile.exists()) {
            throw new FileAlreadyExistsException("Key file already exists: " + publicKeyFile);
        }
        File privateKeyFile = new File(pathAndFilename);
        if (privateKeyFile.exists()) {
            throw new FileAlreadyExistsException("Key file already exists: " + privateKeyFile);
        }
        File parentDir = privateKeyFile.getParentFile();
        if (parentDir != null) parentDir.mkdir();
        publicKeyFile.createNewFile();
        publicKeyFile.setReadable(true, false);
        publicKeyFile.setWritable(false, false);
        publicKeyFile.setWritable(true, true);
        privateKeyFile.createNewFile();
        privateKeyFile.setReadable(false, false);
        privateKeyFile.setReadable(true, true);
        privateKeyFile.setWritable(false, false);
        privateKeyFile.setWritable(true, true);

        // Creating and writing keys
        KeyPairGenerator keyPairGenerator;
        SecureRandom random;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance(algorithm.name());
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
        keyPairGenerator.initialize(2048, random);
        KeyPair keyPair = keyPairGenerator.genKeyPair();

        try (FileOutputStream outPublic = new FileOutputStream(publicKeyFile)) {
            outPublic.write(DatatypeConverter.printBase64Binary(keyPair.getPublic().getEncoded()).getBytes());
        }
        try (FileOutputStream outPrivate = new FileOutputStream(privateKeyFile)) {
            outPrivate.write(DatatypeConverter.printBase64Binary(keyPair.getPrivate().getEncoded()).getBytes());
        }
    }

}
