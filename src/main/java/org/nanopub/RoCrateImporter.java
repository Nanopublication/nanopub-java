package org.nanopub;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.SignatureException;

/**
 * Create and publish nanopub out of a RoCrate metadata file.
 */
public class RoCrateImporter extends CliRunner {

    @com.beust.jcommander.Parameter(description = "Url of RoCrate metadata", required = true)
    private String metadataUrl;

    @com.beust.jcommander.Parameter(names = "-l", description = "write to std.out, no publishing")
    private boolean createLocally;

    private ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * Main method to run the RoCrateImporter.
     *
     * @param args command line arguments, expects the RoCrate metadata URL
     */
    public static void main(String[] args) {
        try {
            RoCrateImporter obj = CliRunner.initJc(new RoCrateImporter(), args);
            obj.run();
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Runs the RoCrateImporter to parse the RoCrate metadata and either create a local nanopub or publish it.
     *
     * @throws MalformedNanopubException if the nanopub is malformed
     * @throws IOException               if there is an I/O error
     * @throws URISyntaxException        if the metadata URL is malformed
     * @throws InterruptedException      if the thread is interrupted
     * @throws TrustyUriException        if there is an issue with Trusty URI
     * @throws SignatureException        if there is an issue with signing the nanopub
     * @throws InvalidKeyException       if the signing key is invalid
     */
    public void run() throws MalformedNanopubException, IOException, URISyntaxException, InterruptedException, TrustyUriException, SignatureException, InvalidKeyException {
        String metadataFilename = metadataUrl.substring(metadataUrl.lastIndexOf('/'));
        String metadataPath = metadataUrl.substring(0, metadataUrl.lastIndexOf('/'));

        RoCrateParser parser = new RoCrateParser();
        Nanopub np = parser.parseRoCreate(metadataPath, metadataFilename);

        if (createLocally) {
            NanopubUtils.writeToStream(np, System.out, RDFFormat.TRIG);
        } else {
            Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
            PublishNanopub.publish(signedNp);
        }
    }

}
