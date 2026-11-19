package org.nanopub.templates;

import com.beust.jcommander.ParameterException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.CliRunner;
import org.nanopub.Nanopub;
import org.nanopub.NanopubProfile;
import org.nanopub.NanopubUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line tool that makes nanopublications from templates and values, with a
 * {@link TemplateFiller}. The nanopublications are not signed.
 */
public class FillTemplate extends CliRunner {

    @com.beust.jcommander.Parameter(description = "requests", required = true)
    private List<String> requests = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-o", description = "Output file; standard output if not given")
    private File outputFile;

    @com.beust.jcommander.Parameter(names = "-t", description = "File with a template nanopub to use instead of retrieving it; can be given several times")
    private List<String> templateFiles = new ArrayList<>();

    @com.beust.jcommander.Parameter(names = "-c", description = "The IRI of the creator, typically an ORCID IRI; overrides the one of the request")
    private String creator;

    @com.beust.jcommander.Parameter(names = "--profile", description = "Profile file for the creator IRI if neither -c nor the request gives one, defaults to ~/.nanopub/profile.yaml")
    private File profileFile;

    /**
     * Runs the tool.
     *
     * @param args the requests, and options
     */
    public static void main(String[] args) {
        try {
            FillTemplate obj = CliRunner.initJc(new FillTemplate(), args);
            int failed = obj.run();
            if (failed > 0) System.exit(1);
        } catch (ParameterException ex) {
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private PrintStream err = System.err;
    private OutputStream out;
    private NanopubResolver networkResolver = NanopubResolver.DEFAULT;

    /**
     * Creates the tool without requests, for {@link #main(String[])}.
     */
    public FillTemplate() {
    }

    /**
     * Creates the tool for the given requests.
     *
     * @param requests      the requests: each a link to the publishing form of Nanodash, the
     *                      query string of one, or a JSON object, or the name of a file that
     *                      holds one of these
     * @param templateFiles files with template nanopublications to use instead of retrieving
     *                      them, for templates that are not published yet
     */
    public FillTemplate(List<String> requests, List<String> templateFiles) {
        this.requests = new ArrayList<>(requests);
        this.templateFiles = new ArrayList<>(templateFiles);
    }

    /**
     * Sets where the nanopublications are written to; the output file, or else standard
     * output, if not set.
     *
     * @param out the stream to write to
     */
    public void setOutput(OutputStream out) {
        this.out = out;
    }

    /**
     * Sets where the problems of a request are written to; standard error if not set.
     *
     * @param err the stream to write to
     */
    public void setErrorOutput(PrintStream err) {
        this.err = err;
    }

    /**
     * Sets how nanopublications are retrieved that are not among the given template files;
     * from the nanopublication network if not set.
     *
     * @param resolver the resolver for templates and for the nanopublication a new one is a
     *                 version of
     */
    public void setResolver(NanopubResolver resolver) {
        this.networkResolver = resolver;
    }

    /**
     * Sets the creator of the nanopublications, overriding the one of a request.
     *
     * @param creator the IRI of the creator, or null to take it from the request
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * Sets the profile file the creator is read from if nothing else gives one.
     *
     * @param profileFile the profile file, or null for the default one
     */
    public void setProfileFile(File profileFile) {
        this.profileFile = profileFile;
    }

    /**
     * Makes the nanopublications and writes them.
     *
     * @return the number of requests that no nanopublication could be made from
     * @throws IOException if a file cannot be read or written
     */
    public int run() throws IOException {
        TemplateFiller filler = new TemplateFiller(LocalNanopubs.resolver(templateFiles, networkResolver));
        List<Nanopub> nanopubs = new ArrayList<>();
        int failed = 0;
        for (String given : requests) {
            File file = new File(given);
            String text = file.isFile() ? Files.readString(file.toPath(), StandardCharsets.UTF_8) : given;
            try {
                FillRequest request = FillRequest.parse(text);
                IRI creatorIri = getCreator();
                if (creatorIri != null) {
                    request.setCreator(creatorIri);
                } else if (request.getCreator() == null) {
                    request.setCreator(getProfileCreator());
                }
                nanopubs.add(filler.fill(request));
            } catch (TemplateFillException ex) {
                failed++;
                err.println("CANNOT FILL: " + (file.isFile() ? given : "request " + (requests.indexOf(given) + 1)));
                for (String problem : ex.getProblems()) err.println("- " + problem);
            }
        }
        if (failed > 0) return failed;
        OutputStream target = out != null ? out : outputFile != null ? new FileOutputStream(outputFile) : System.out;
        try {
            for (Nanopub np : nanopubs) {
                NanopubUtils.writeToStream(np, target, RDFFormat.TRIG);
                target.write('\n');
            }
            target.flush();
        } finally {
            if (out == null && outputFile != null) target.close();
        }
        return 0;
    }

    private IRI getCreator() throws TemplateFillException {
        if (creator == null || creator.isBlank()) return null;
        try {
            return PlaceholderMatcher.createIri(creator);
        } catch (IllegalArgumentException ex) {
            throw new TemplateFillException("The creator is not an IRI: " + creator);
        }
    }

    private IRI getProfileCreator() {
        NanopubProfile profile = new NanopubProfile(profileFile != null ? profileFile.getPath() : NanopubProfile.IMPLICIT_PROFILE_FILE_NAME);
        String orcid = profile.getOrcidId();
        if (orcid == null || orcid.isBlank()) return null;
        try {
            return PlaceholderMatcher.createIri(orcid);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

}
