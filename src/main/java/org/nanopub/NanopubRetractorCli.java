package org.nanopub;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.server.PublishNanopub;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

/**
 * <p>Command line interface for retraction of nanopublications.</p>
 * <p>Parameters:</p>
 * <ol>
 *      <li>-i: Nanopub to be retracted. If it starts with "http(s)://" it's considered to be the nanopub's url, else
 *              it's the local path to a nanopub file.</li>
 *      <li>-p: optional - publish the created retraction (without -p the retraction nanopub will only be written to stdout)</li>
 * </ol>
 * <p>For signing, use -k to specify a custom key file path and -s to specify the signer's orcid IRI.
 * Otherwise the key at "~/.nanopub/id_rsa" and the orcid from "~/.nanopub/profile.yaml" are used.</p>
 *
 * @since 1.80
 */
public class NanopubRetractorCli extends CliRunner {

    @com.beust.jcommander.Parameter(names = "-i", description = "Nanopub to be retracted, url or local path.", required = true)
    private String nanopubUrl;

    @com.beust.jcommander.Parameter(names = "-k", description = "Path and file name of key files")
    private String keyFilename;

    @com.beust.jcommander.Parameter(names = "-s", description = "The orcid IRI of the signer")
    private String signer;

    @com.beust.jcommander.Parameter(names = "-p", description = "directly publish the retraction")
    private boolean publish = false;

    /**
     * Main method for running
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            NanopubRetractorCli obj = CliRunner.initJc(new NanopubRetractorCli(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Runs the retractor.
     *
     * @throws MalformedNanopubException        if any.
     * @throws IOException                      if any.
     * @throws TrustyUriException               if any.
     * @throws GeneralSecurityException         if any.
     * @throws MalformedCryptoElementException  if any.
     * @throws NanopubAlreadyFinalizedException if the nanopub is already finalized.
     */
    public void run() throws MalformedNanopubException, IOException, TrustyUriException, GeneralSecurityException, MalformedCryptoElementException, NanopubAlreadyFinalizedException {
        Nanopub orig = getNanopubToRetract();
        TransformContext tc = createTransformContext();
        Nanopub retraction = NanopubRetractor.createRetraction(orig, tc);
        if (publish) {
            System.out.println("Publishing Retraction Nanopub.");
            PublishNanopub.publish(retraction);
        }
        // Write to System.out
        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        NanopubUtils.propagateToHandler(retraction, w);
    }

    private TransformContext createTransformContext() {
        if (keyFilename == null && signer == null) {
            return TransformContext.makeDefault();
        }
        ValueFactory vf = SimpleValueFactory.getInstance();
        NanopubProfile profile = new NanopubProfile(NanopubProfile.IMPLICIT_PROFILE_FILE_NAME);
        IRI signerIri = null;
        if (signer != null) {
            signerIri = vf.createIRI(signer);
        } else if (profile.getOrcidId() != null) {
            signerIri = vf.createIRI(profile.getOrcidId());
        }
        if (keyFilename == null) {
            keyFilename = profile.getPrivateKeyPath();
        }
        if (keyFilename == null) {
            keyFilename = TransformContext.DEFAULT_KEY_PATH;
        }
        SignatureAlgorithm algorithm = keyFilename.endsWith("_dsa") ? SignatureAlgorithm.DSA : SignatureAlgorithm.RSA;
        KeyPair key;
        try {
            key = SignNanopub.loadKey(keyFilename, algorithm);
        } catch (Exception ex) {
            throw new RuntimeException("Could not load key: " + ex.getMessage(), ex);
        }
        return new TransformContext(algorithm, key, signerIri, false, false, false);
    }

    private Nanopub getNanopubToRetract() throws MalformedNanopubException, IOException {
        if (nanopubUrl.startsWith("http://") || nanopubUrl.startsWith("https://")) {
            return GetNanopub.get(nanopubUrl);
        }
        return new NanopubImpl(new File(nanopubUrl));
    }

}
