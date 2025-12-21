package org.nanopub;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.server.PublishNanopub;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * <p>Command line interface for retraction of nanopublications.</p>
 * <p>Parameters:</p>
 * <ol>
 *      <li>-i: Nanopub to be retracted. If it starts with "http(s)://" it's considered to be the nanopub's url, else
 *              it's the local path to a nanopub file.</li>
 *      <li>-p: optional - publish the created retraction (without -p the retraction nanopub will only be written to stdout)</li>
 * </ol>
 * <p>For signing the key at "~/.nanopub/id_rsa" is used and the orcid from "~/.nanopub/profile.yaml</p>
 *
 * @since 1.80
 */
public class NanopubRetractorCli extends CliRunner {

    @com.beust.jcommander.Parameter(names = "-i", description = "Nanopub to be retracted, url or local path.", required = true)
    private String nanopubUrl;

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
        Nanopub retraction = NanopubRetractor.createRetraction(orig, TransformContext.makeDefault());
        if (publish) {
            System.out.println("Publishing Retraction Nanopub.");
            PublishNanopub.publish(retraction);
        }
        // Write to System.out
        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        NanopubUtils.propagateToHandler(retraction, w);
    }

    private Nanopub getNanopubToRetract() throws MalformedNanopubException, IOException {
        if (nanopubUrl.startsWith("http://") || nanopubUrl.startsWith("https://")) {
            return GetNanopub.get(nanopubUrl);
        }
        return new NanopubImpl(new File(nanopubUrl));
    }

}
