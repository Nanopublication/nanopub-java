package org.nanopub;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.jsonld.JSONLDSettings;
import org.jspecify.annotations.NonNull;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.SCHEMA;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class represents a parser for RO-Crate metadata files.
 */
public class RoCrateParser {

    private static final Log LOG = LogFactory.getLog(RoCrateParser.class);
    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final HttpClient client = HttpClient.newHttpClient();

    public static InputStream downloadRoCreateMetadataFile(String uri) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder().GET().uri(new URI(uri)).build();
        HttpResponse<InputStream> httpResponse = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        return httpResponse.body();
    }

    /**
     * Parses a RO-Crate metadata file from a given URL.
     *
     * @param url          the url where the metadata file is published (including trailing "/")
     * @param roCrateMetadata the ro-create metadata file name, may be the empty string
     * @return a signed Nanopub object containing the parsed data.
     * @throws org.nanopub.MalformedNanopubException if the parsed data does not conform to the expected structure.
     * @throws java.io.IOException                   if an I/O error occurs while reading the metadata file.
     * @throws java.lang.InterruptedException        if the operation is interrupted.
     * @throws java.net.URISyntaxException           if the URL is malformed.
     */
    public Nanopub parseRoCreate(String url, InputStream roCrateMetadata) throws MalformedNanopubException, IOException, NanopubAlreadyFinalizedException {
        RDFParser parser = Rio.createParser(RDFFormat.JSONLD);

        // Configure parser settings
        parser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);

        // We do not accept spaces in urls (@id elements)
        // parser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false);

        // Since JSONLDSettings.WHITELIST does not contain "https://w3id.org/ro/crate/1.0/context" we disable SECURE MODE
        parser.getParserConfig().set(JSONLDSettings.SECURE_MODE, false);

        Model model = new LinkedHashModel();
        StatementCollector handler = new StatementCollector(model);

        parser.setRDFHandler(handler);
        IRI globalRoCrateRef = constructRoCrateUrl(url, roCrateMetadata);
        parser.parse(roCrateMetadata, globalRoCrateRef.stringValue());

        // Create Nanopub
        NanopubCreator npCreator = new NanopubCreator(true);
        Collection<Statement> metadataStatements = handler.getStatements();
        npCreator.addAssertionStatements(metadataStatements);

        // Extract some special statements
        IRI identifier = extractToplevelIdentifierOrBackup(metadataStatements, globalRoCrateRef.stringValue(), null);
        String label = extractToplevelName(metadataStatements, identifier);

        // as provenance statement WAS_DERIVED_FROM we always use the specified name: "ro-crate-metadata.json"
        npCreator.addProvenanceStatement(PROV.WAS_DERIVED_FROM, vf.createIRI(url+ "ro-crate-metadata.json"));
        npCreator.addPubinfoStatement(NPX.INTRODUCES,  identifier);
        npCreator.addPubinfoStatement(RDFS.LABEL, vf.createLiteral(label));
        npCreator.addPubinfoStatement(RDF.TYPE, NPX.RO_CRATE_NANOPUB);

        return npCreator.finalizeNanopub(true);
    }

    /**
     * Find the ID of the RO-Crate.
     * @param url where we get the RO-crate
     * @param roCrateMetadata LATER not yet implemented
     * @return our current best guess for the ID_IRI
     */
    // default access for testing
    static IRI constructRoCrateUrl(String url, InputStream roCrateMetadata) {
        String id;
        final String BASE_ROCRATE_API_URL = "https://api.rohub.org/api/ros/";
        final String BASE_ROCRATE_API_URL_SUFFIX = "crate/download/";
        final String BASE_ROHUB_URL = "https://w3id.org/ro-id/";
        final String patternUrlUntilLastSlash = "(https?://.*/)(.*)";
        if (url.startsWith("http")) {
            if (url.startsWith("https://api.rohub.org/api/ros/")) {
                id = StringUtils.substringAfter(url, BASE_ROCRATE_API_URL);
                id = StringUtils.removeEnd(id, BASE_ROCRATE_API_URL_SUFFIX);
                return vf.createIRI(BASE_ROHUB_URL + id);
            } else if (url.endsWith("/")) {
                return vf.createIRI(url);
            } else if (url.matches(patternUrlUntilLastSlash)) {
                // probably ends in  ./metadata.json or something like that, we remove it anyway
                Pattern p = Pattern.compile(patternUrlUntilLastSlash);
                Matcher m = p.matcher(url); m.matches();
                String resultingUrl = m.group(1);
                if (LOG.isDebugEnabled()) {
                    try {
                        String filename = m.group(2);
                        if (filename.equals("ro-crate-metadata.json") || filename.equals("ro-crate-metadata.jsonld")) {
                            // standard case, no logging
                        } else {
                            LOG.debug("Unexpected filename for RO-Create Metadata: " + filename);
                            LOG.debug("Stripping the filename anyway and use '" + resultingUrl + "' as RO-Crate base.");
                        }
                    } catch (IllegalStateException | IndexOutOfBoundsException e) {
                      // there was no trailing filename, all good
                    }
                }
                if (resultingUrl == null) {
                    LOG.warn("Could not determine RO-Crate base URL with input url: " + url);
                }
                return vf.createIRI(resultingUrl);
            } else {
                // TODO extract from roCrateMetadata
                return vf.createIRI(url);
            }
        }
        return vf.createIRI(url);
    }

    /* @return jsonld graph -> top_level_name max 212 chars */
    @NonNull
    private String extractToplevelName(Collection<Statement> metadataStatements, IRI subj) {
        Collection<Statement> nameCandidates = metadataStatements.stream()
                .filter(st -> st.getSubject().equals(subj))
                .filter(st -> st.getPredicate().equals(SCHEMA.NAME))
                .collect(Collectors.toSet());
        if (nameCandidates.size() != 1) {
            LOG.info(String.format("This RO-Crate has an invalid number (%n) of names: %s", nameCandidates.size(), subj.stringValue()));
            nameCandidates.stream().forEach(possibleName -> LOG.debug(possibleName.toString()));
        }
        String name = nameCandidates.stream()
                .findFirst().get().getObject().stringValue();
        if (name == null) {
            name = metadataStatements.stream()
                    .filter(st -> st.getSubject().equals(subj)
                            && st.getPredicate().equals(SCHEMA.DESCRIPTION))
                    .findFirst().get().getObject().stringValue();
        }
        return StringUtils.substring(name, 0, 212); // 212 is just our convention;-) 222 was a good choice, too
    }

    @NonNull
    private IRI extractToplevelIdentifierOrBackup(Collection<Statement> metadataStatements, String bestGuess, String latestBackupIdentifier) {
        if (bestGuess != null) {
            return vf.createIRI(bestGuess);
        }
        // TODO verify if this is correct, and check if sometimes the backup is an even better choice
        IRI identifier = (IRI) metadataStatements.stream()
                .filter(st -> st.getPredicate().equals(SCHEMA.RO_CRATE_IDENTIFIER))
                .findFirst().get().getSubject(); // TODO or do we need the Object-Value???
        if (identifier == null) {
            identifier = vf.createIRI(latestBackupIdentifier);
            // TODO, probably the best first backup choice is the download url if available in the metadate,
            // the url from above is only the second backup, so we never have any null pointer issues.
        }
        return identifier;
    }

}
