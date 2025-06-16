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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RoCrateParser {

    private HttpClient client = HttpClient.newHttpClient();
    private final ValueFactory vf = SimpleValueFactory.getInstance();
    private boolean verbose = false;

    /**
     * @param url the url where the metadata file is published (including trailing "/")
     * @param metadataFile the name of the metadata file.
     */
    public Nanopub parseRoCreate(String url, String metadataFile) throws MalformedNanopubException, IOException, InterruptedException, URISyntaxException {
        HttpRequest req = HttpRequest.newBuilder().GET().uri(new URI(url + metadataFile)).build();
        HttpResponse<InputStream> httpResponse = client.send(req, HttpResponse.BodyHandlers.ofInputStream());


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
        parser.parse(httpResponse.body(), url);

        // The 'model' now contains the parsed RDF data
        if (verbose) {
            System.out.println("Parsed " + model.size() + " statements.");
        }

        // Create Nanopub
        NanopubCreator npCreator = new NanopubCreator(true);
        npCreator.addAssertionStatements(handler.getStatements());

        npCreator.addProvenanceStatement(PROV.WAS_DERIVED_FROM, vf.createIRI(url+ metadataFile));
        npCreator.addPubinfoStatement(RDF.TYPE, vf.createIRI("http://purl.org/nanopub/x/ExampleRoCreateNanopub"));

        return npCreator.finalizeNanopub(true);
    }

}
