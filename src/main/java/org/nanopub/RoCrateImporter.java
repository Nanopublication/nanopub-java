package org.nanopub;

import com.beust.jcommander.ParameterException;
import net.trustyuri.TrustyUriException;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.SignatureException;

/**
 * Create and publish nanopub out of a RoCrate metadata file.
 */
public class RoCrateImporter extends CliRunner {

    // The complete url of the ro-crate-metadata.json, including the filename.
    @com.beust.jcommander.Parameter(description = "Url of RoCrate metadata", required = true)
    private String metadataUrl;

    @com.beust.jcommander.Parameter(names = "-l", description = "write to std.out, no publishing")
    private boolean createLocally;

    @com.beust.jcommander.Parameter(names = "-f", description = "Use this local file for a ro-crate-metadata.json, " +
            "instead of downloading from metadataUrl.")
    private String localFileName;


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
     * @throws org.nanopub.MalformedNanopubException if the nanopub is malformed
     * @throws java.io.IOException                   if there is an I/O error
     * @throws java.net.URISyntaxException           if the metadata URL is malformed
     * @throws java.lang.InterruptedException        if the thread is interrupted
     * @throws net.trustyuri.TrustyUriException      if there is an issue with Trusty URI
     * @throws java.security.SignatureException      if there is an issue with signing the nanopub
     * @throws java.security.InvalidKeyException     if the signing key is invalid
     */
    public void run() throws MalformedNanopubException, IOException, URISyntaxException, InterruptedException, TrustyUriException, SignatureException, InvalidKeyException {

        InputStream roCreateMetadata = null;
        if (localFileName != null) {
            roCreateMetadata = new FileInputStream(localFileName);
        } else {
            roCreateMetadata = RoCrateParser.downloadRoCreateMetadataFile(metadataUrl);
        }
        String metadataPath = metadataUrl.substring(0, metadataUrl.lastIndexOf('/'));

        RoCrateParser parser = new RoCrateParser();
        Nanopub np = parser.parseRoCreate(metadataPath, roCreateMetadata);

        if (createLocally) {
            NanopubUtils.writeToStream(np, System.out, RDFFormat.TRIG);
        } else {
            Nanopub signedNp = SignNanopub.signAndTransform(np, TransformContext.makeDefault());
            PublishNanopub.publish(signedNp);
        }
    }

}
