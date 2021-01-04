package org.nanopub.extra.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

import javax.xml.bind.DatatypeConverter;

import org.nanopub.NanopubImpl;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class MakeKeys {

	@com.beust.jcommander.Parameter(names = "-f", description = "Path and file name prefix of key files")
	private String pathAndFilenamePrefix = "~/.nanopub/id";

	@com.beust.jcommander.Parameter(names = "-a", description = "Signature algorithm: either RSA or DSA")
	private SignatureAlgorithm algorithm = SignatureAlgorithm.DSA;

	public static void main(String[] args) throws IOException {
		NanopubImpl.ensureLoaded();
		MakeKeys obj = new MakeKeys();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		try {
			obj.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private MakeKeys() {
	}

	private void run() throws IOException {
		make(pathAndFilenamePrefix, algorithm);
	}

	public static void make(String pathAndFilenamePrefix, SignatureAlgorithm algorithm) throws IOException {

		// Preparation:
		String pathAndFilename = System.getProperty("user.home") + "/" + pathAndFilenamePrefix.replaceFirst("^~", "") + "_" + algorithm.name().toLowerCase();
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
		keyPairGenerator.initialize(1024, random);
		KeyPair keyPair = keyPairGenerator.genKeyPair();

		FileOutputStream outPublic = new FileOutputStream(publicKeyFile);
		outPublic.write(DatatypeConverter.printBase64Binary(keyPair.getPublic().getEncoded()).getBytes());
		outPublic.close();
		FileOutputStream outPrivate = new FileOutputStream(privateKeyFile);
		outPrivate.write(DatatypeConverter.printBase64Binary(keyPair.getPrivate().getEncoded()).getBytes());
		outPrivate.close();
	}

}
