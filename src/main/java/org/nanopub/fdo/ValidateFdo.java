package org.nanopub.fdo;

import com.google.gson.Gson;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.nanopub.fdo.rest.gson.ParsedSchemaResponse;
import org.nanopub.vocabulary.FDOF;
import org.nanopub.vocabulary.HDL;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.nanopub.fdo.FdoUtils.*;

/**
 * ValidateFdo provides methods to validate FDO records using SHACL shapes.
 */
// TODO class that provides the Op.Validate operations.
//      See https://fdo-connect.gitlab.io/ap1/architecture-documentation/main/operation-specification/
public class ValidateFdo {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String SHACL_PROPERTY_SHAPE = "propertyShape";

    private static HttpClient client = HttpClient.newHttpClient();

    private ValidateFdo() {
    }  // no instances allowed

    /**
     * Validate an FdoRecord against the shapes defined.
     *
     * @param fdoRecord the FdoRecord to validate
     * @return ValidationResult containing the result of the validation
     * @throws org.nanopub.fdo.FdoNotFoundException if the profile ID in the FdoRecord cannot be resolved
     * @throws java.net.URISyntaxException          if the schema URL is not a valid URI
     * @throws java.io.IOException                  if there is an error reading the schema
     * @throws java.lang.InterruptedException       if the HTTP request is interrupted
     */
    public static ValidationResult validate(FdoRecord fdoRecord) throws FdoNotFoundException, URISyntaxException, IOException, InterruptedException {

        String profileId = fdoRecord.getProfile();
        String schemaUrl = RetrieveFdo.resolveId(profileId).getSchemaUrl();

        HttpRequest req = HttpRequest.newBuilder().GET().uri(new URI(schemaUrl)).build();
        HttpResponse<String> httpResponse = client.send(req, HttpResponse.BodyHandlers.ofString());

        Set<Statement> shaclShape = createShaclValidationShapeFromJson(httpResponse);
        Set<Statement> data = addTypeStatement(fdoRecord);

        System.out.println("Validating FdoRecord " + fdoRecord.getId());
        System.out.println("Against Schema " + schemaUrl);

        return ShaclValidator.validateShacl(shaclShape, data);
    }

    private static Set<Statement> addTypeStatement(FdoRecord fdoRecord) {
        Set<Statement> data = fdoRecord.buildStatements();
        Statement first = data.toArray(new Statement[0])[0];
        data.add(vf.createStatement(first.getSubject(), RDF.TYPE, FDOF.FAIR_DIGITAL_OBJECT));
        return data;
    }

    /**
     * Read all the *property* fields from the profile.json in the httpResponse and convert them to a set of statements
     * for shacl validation. *required* fields have min-count 1, the others do not have a min-count. (We assume that
     * additionalProperties is always true.)
     * When we just want to validate a shape, this method is fine. If the profile should be published as a nanopub,
     * we want to specify the subject-prefix for the statements with the surrounding nanopub uri by using @createShaclValidationShapeFromJson(httpResponse, subjPrefix)
     *
     * @param httpResponse the HTTP response containing the profile JSON
     * @return a set of statements representing the SHACL validation shape
     */
    public static Set<Statement> createShaclValidationShapeFromJson(HttpResponse<String> httpResponse) {
        return createShaclValidationShapeFromJson(httpResponse, "https://w3id.org/kpxl/shacl/temp/");
    }

    /**
     * Read all the *property* fields from the profile.json in the httpResponse and convert them to a set of statements
     * for shacl validation. *required* fields have min-count 1, the others do not have a min-count. (We assume that
     * additionalProperties is always true.)
     * If the profile should be published as a nanopub, we want to specify the subject-prefix for the statements with the surrounding nanopub uri
     *
     * @param httpResponse the HTTP response containing the profile JSON
     * @param subjPrefix   the prefix to use for the subject of the statements
     * @return a set of statements representing the SHACL validation shape
     */
    public static Set<Statement> createShaclValidationShapeFromJson(HttpResponse<String> httpResponse, String subjPrefix) {
        ParsedSchemaResponse r = new Gson().fromJson(httpResponse.body(), ParsedSchemaResponse.class);

        Set<Statement> shaclShape = new HashSet<>();
        IRI nodeShape = vf.createIRI(subjPrefix + "nodeShape");
        List<String> reqired = Arrays.asList(r.required);
        int i = 0;
        for (String s : r.properties.keySet()) {
            i++;
            IRI propertyShape = vf.createIRI(subjPrefix + SHACL_PROPERTY_SHAPE + i);
            shaclShape.add(vf.createStatement(propertyShape, SHACL.MAX_COUNT, vf.createLiteral(1)));
            if (reqired.contains(s)) {
                shaclShape.add(vf.createStatement(propertyShape, SHACL.MIN_COUNT, vf.createLiteral(1)));
            }
            if (s.equals(PROFILE_HANDLE) || s.equals(PROFILE_HANDLE_1) || s.equals(PROFILE_HANDLE_2)) {
                shaclShape.add(vf.createStatement(propertyShape, SHACL.PATH, DCTERMS.CONFORMS_TO));
            } else {
                shaclShape.add(vf.createStatement(propertyShape, SHACL.PATH, vf.createIRI(HDL.NAMESPACE + s)));
            }
            shaclShape.add(vf.createStatement(nodeShape, SHACL.PROPERTY, propertyShape));
        }
        shaclShape.add(vf.createStatement(nodeShape, SHACL.TARGET_CLASS, FDOF.FAIR_DIGITAL_OBJECT));
        shaclShape.add(vf.createStatement(nodeShape, RDF.TYPE, SHACL.NODE_SHAPE));

        return shaclShape;
    }

}
