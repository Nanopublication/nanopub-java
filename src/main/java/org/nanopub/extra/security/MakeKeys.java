package org.nanopub.extra.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

import sun.misc.BASE64Encoder;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class MakeKeys {

	@com.beust.jcommander.Parameter(names = "-f", description = "Path and file name of key files")
	private String keyFilename = "~/.nanopub/id_dsa";

	public static void main(String[] args) throws IOException {
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

	private void run() throws Exception {

		// Preparation:
		keyFilename = keyFilename.replaceFirst("^~", System.getProperty("user.home"));
		File publicKeyFile = new File(keyFilename + ".pub");
		if (publicKeyFile.exists()) {
			throw new RuntimeException("Key file already exists: " + publicKeyFile);
		}
		File privateKeyFile = new File(keyFilename);
		if (privateKeyFile.exists()) {
			throw new RuntimeException("Key file already exists: " + privateKeyFile);
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
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DSA", "SUN");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		keyPairGenerator.initialize(1024, random);
		KeyPair keyPair = keyPairGenerator.genKeyPair();
		BASE64Encoder encoder = new BASE64Encoder();
		FileOutputStream outPublic = new FileOutputStream(publicKeyFile);
		outPublic.write((encoder.encode(keyPair.getPublic().getEncoded()) + "\n").getBytes());
		outPublic.close();
		FileOutputStream outPrivate = new FileOutputStream(privateKeyFile);
		outPrivate.write((encoder.encode(keyPair.getPrivate().getEncoded()) + "\n").getBytes());
		outPrivate.close();
	}

}
