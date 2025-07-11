package org.nanopub.extra.setting;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;

import java.io.IOException;
import java.util.List;

/**
 * Command-line tool to retrieve the introduction nanopub for a user.
 */
public class GetIntroNanopub {

    @com.beust.jcommander.Parameter(description = "user-id", required = true)
    private List<String> userIds;

    /**
     * Main method to run the GetIntroNanopub tool.
     *
     * @param args Command-line arguments, expected to be user IDs.
     */
    public static void main(String[] args) {
        NanopubImpl.ensureLoaded();
        GetIntroNanopub obj = new GetIntroNanopub();
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

    private void run() throws IOException, RDF4JException {
        for (String userId : userIds) {
            NanopubUtils.writeToStream(IntroNanopub.get(userId).getNanopub(), System.out, RDFFormat.TRIG);
        }
    }

}
