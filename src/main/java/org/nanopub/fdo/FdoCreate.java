package org.nanopub.fdo;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.CliRunner;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.SignatureException;

/**
 * Create and optionally publish a nanopub for a handle-based FDO.
 */
public class FdoCreate extends CliRunner {

    @com.beust.jcommander.Parameter(description = "handle-id-or-url", required = true)
    private String handleId;

    @com.beust.jcommander.Parameter(names = "-u", description = "Unsigned, the Nanopub is not signed. Do not use with -p.")
    private boolean unsigned;

    @com.beust.jcommander.Parameter(names = "-p", description = "Directly publish the Nanopub.")
    private boolean publish;

    @com.beust.jcommander.Parameter(names = {"-s", "--enrich-from-schema"}, description = "Resolve the FDO profile's type registry entry and use property handles as predicates (with rdfs:label in pubinfo).")
    private boolean enrichFromSchema;

    /**
     * Main method to run the FdoCreate command-line tool.
     *
     * @param args command line arguments, expects a handle identifier
     */
    public static void main(String[] args) {
        try {
            FdoCreate obj = CliRunner.initJc(new FdoCreate(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Resolves the handle and creates a nanopub from it, then either prints or publishes it.
     *
     * @throws org.nanopub.MalformedNanopubException        if the nanopub is malformed
     * @throws java.io.IOException                          if there is an I/O error
     * @throws java.net.URISyntaxException                  if the handle identifier is not a valid URI
     * @throws java.lang.InterruptedException               if the thread is interrupted while resolving the handle
     * @throws net.trustyuri.TrustyUriException             if there is an issue with Trusty URI
     * @throws java.security.SignatureException             if there is an issue with signing the nanopub
     * @throws java.security.InvalidKeyException            if the signing key is invalid
     * @throws org.nanopub.NanopubAlreadyFinalizedException if the nanopub has already been finalized
     */
    public void run() throws MalformedNanopubException, IOException, URISyntaxException, InterruptedException,
            TrustyUriException, SignatureException, InvalidKeyException, NanopubAlreadyFinalizedException {

        if (unsigned && publish) {
            System.err.println("Do not use -u and -p together.");
            throw new ParameterException("Cannot use -u and -p together.");
        }

        String resolvedId = FdoUtils.extractHandleId(handleId);
        if (resolvedId == null) {
            System.err.println("Not a recognisable handle or handle/DOI URL: " + handleId);
            throw new ParameterException("Not a recognisable handle or handle/DOI URL: " + handleId);
        }

        Nanopub np = FdoNanopubCreator.createFromHandleSystem(resolvedId, enrichFromSchema);

        if (unsigned) {
            NanopubUtils.writeToStream(np, System.out, RDFFormat.TRIG);
        } else if (publish) {
            Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
            PublishNanopub.publish(signedNp);
        } else {
            Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
            NanopubUtils.writeToStream(signedNp, System.out, RDFFormat.TRIG);
        }
    }

}
