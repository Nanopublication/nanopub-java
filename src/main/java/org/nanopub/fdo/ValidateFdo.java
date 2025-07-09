package org.nanopub.fdo;

import com.google.gson.Gson;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.fdo.rest.gson.ParsedSchemaResponse;

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

// TODO class that provides the Op.Validate operations.
//      See https://fdo-connect.gitlab.io/ap1/architecture-documentation/main/operation-specification/
public class ValidateFdo {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	private static final IRI SHACL_MAX_COUNT = vf.createIRI("http://www.w3.org/ns/shacl#maxCount");
	private static final IRI SHACL_MIN_COUNT = vf.createIRI("http://www.w3.org/ns/shacl#minCount");
	private static final IRI SHACL_PATH = vf.createIRI("http://www.w3.org/ns/shacl#path");
	private static final IRI SHACL_TARGET = vf.createIRI("http://www.w3.org/ns/shacl#targetClass");
	private static final IRI SHACL_PROPERTY = vf.createIRI("http://www.w3.org/ns/shacl#property");
	private static final IRI SHACL_NODE_SHAPE = vf.createIRI("http://www.w3.org/ns/shacl#NodeShape");
	private static final String SHACL_PROPERTY_SHAPE = "propertyShape";

	private static HttpClient client = HttpClient.newHttpClient();

	private ValidateFdo() {}  // no instances allowed

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
		data.add(vf.createStatement(first.getSubject(), RDF.TYPE, RDF_TYPE_FDO));
		return data;
	}

	/**
	 * Read all the *property* fields from the profile.json in the httpResponse and convert them to a set of statements
	 * for shacl validation. *required* fields have min-count 1, the others do not have a min-count. (We assume that
	 * additionalProperties is always true.)
	 * When we just want to validate a shape, this method is fine. If the profile should be published as a nanopub,
	 * we want to specify the subject-prefix for the statements with the surrounding nanopub uri by useing
	 * @createShaclValidationShapeFromJson(httpResponse, subjPrefix)
	 */
	public static Set<Statement> createShaclValidationShapeFromJson(HttpResponse<String> httpResponse) {
		return createShaclValidationShapeFromJson(httpResponse, "https://w3id.org/kpxl/shacl/temp/");
	}

	/**
	 * Read all the *property* fields from the profile.json in the httpResponse and convert them to a set of statements
	 * for shacl validation. *required* fields have min-count 1, the others do not have a min-count. (We assume that
	 * additionalProperties is always true.)
	 * If the profile should be published as a nanopub, we want to specify the subject-prefix for the statements with the surrounding nanopub uri
	 */
	public static Set<Statement> createShaclValidationShapeFromJson(HttpResponse<String> httpResponse, String subjPrefix) {
		ParsedSchemaResponse r = new Gson().fromJson(httpResponse.body(), ParsedSchemaResponse.class);

		Set<Statement> shaclShape = new HashSet<>();
		IRI nodeShape = vf.createIRI(subjPrefix + "nodeShape");
		List<String> reqired = Arrays.asList(r.required);
		int i = 0;
		for (String s: r.properties.keySet()) {
			i++;
			IRI propertyShape = vf.createIRI(subjPrefix + SHACL_PROPERTY_SHAPE + i);
			shaclShape.add(vf.createStatement(propertyShape, SHACL_MAX_COUNT, vf.createLiteral(1)));
			if (reqired.contains(s)) {
				shaclShape.add(vf.createStatement(propertyShape, SHACL_MIN_COUNT, vf.createLiteral(1)));
			}
			if (s.equals(PROFILE_HANDLE) || s.equals(PROFILE_HANDLE_1) || s.equals(PROFILE_HANDLE_2)) {
				shaclShape.add(vf.createStatement(propertyShape, SHACL_PATH, PROFILE_IRI));
			} else {
				shaclShape.add(vf.createStatement(propertyShape, SHACL_PATH, vf.createIRI(FDO_URI_PREFIX + s)));
			}
			shaclShape.add(vf.createStatement(nodeShape, SHACL_PROPERTY, propertyShape));
		}
		shaclShape.add(vf.createStatement(nodeShape, SHACL_TARGET, RDF_TYPE_FDO));
		shaclShape.add(vf.createStatement(nodeShape, RDF.TYPE, SHACL_NODE_SHAPE));

		return shaclShape;
	}

}
