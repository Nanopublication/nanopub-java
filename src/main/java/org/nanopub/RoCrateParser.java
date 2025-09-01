package org.nanopub;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.jsonld.JSONLDSettings;
import org.nanopub.vocabulary.NPX;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * This class represents a parser for RO-Crate metadata files.
 */
public class RoCrateParser {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static HttpClient client = HttpClient.newHttpClient();

    public static InputStream downloadRoCreateMetadataFile(String uri) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder().GET().uri(new URI(uri)).build();
        HttpResponse<InputStream> httpResponse = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        return httpResponse.body();
    }

    /**
     * Parses a RO-Crate metadata file from a given URL.
     *
     * @param url          the url where the metadata file is published (including trailing "/")
     * @param roCrateMetadata the ro-create metadata.
     * @return a Nanopub object containing the parsed data.
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
        parser.parse(roCrateMetadata, url);

        // Create Nanopub
        NanopubCreator npCreator = new NanopubCreator(true);
        npCreator.addAssertionStatements(handler.getStatements());

        // we always use the specified name: "ro-crate-metadata.json"
        npCreator.addProvenanceStatement(PROV.WAS_DERIVED_FROM, vf.createIRI(url+ "ro-crate-metadata.json"));
        npCreator.addPubinfoStatement(RDF.TYPE, NPX.RO_CRATE_NANOPUB);

        return npCreator.finalizeNanopub(true);
    }

}
