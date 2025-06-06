package org.nanopub.fdo;

import com.google.gson.Gson;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.MalformedNanopubException;
import org.nanopub.fdo.rest.gson.ParsedSchemaResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;

import static org.nanopub.fdo.FdoNanopubCreator.FDO_TYPE_PREFIX;
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

	private static final IRI TEMP_TYPE = vf.createIRI("https://w3id.org/kpxl/temptype");

	private static HttpClient client = HttpClient.newHttpClient();

	private ValidateFdo() {}  // no instances allowed

	// TODO Just a boolean as return value. Later probably an object that also includes errors/warnings.
	public static boolean isValid(FdoRecord fdoRecord) throws MalformedNanopubException, URISyntaxException, IOException, InterruptedException {

		String profileId = fdoRecord.getProfile();
		String schemaUrl = RetrieveFdo.retrieveRecordFromHandle(profileId).getSchemaUrl();

		HttpRequest req = HttpRequest.newBuilder().GET().uri(new URI(schemaUrl)).build();
		HttpResponse<String> httpResponse = client.send(req, HttpResponse.BodyHandlers.ofString());

		Set<Statement> shaclShape = createShaclShapeFromJson(httpResponse);
		Set<Statement> data = addTypeStatement(fdoRecord);

		System.out.println("Validating FdoRecord " + fdoRecord.getId());
		System.out.println("Against Schema " + schemaUrl);

		return ShaclValidator.validateShacl(shaclShape, data);
	}

	private static Set<Statement> addTypeStatement(FdoRecord fdoRecord) {
		Set<Statement> data = fdoRecord.getStatements();
		Statement first = data.toArray(new Statement[0])[0];
		data.add(vf.createStatement(first.getSubject(), RDF.TYPE, TEMP_TYPE));
		return data;
	}

	public static Set<Statement> createShaclShapeFromJson(HttpResponse<String> httpResponse) {
		ParsedSchemaResponse r = new Gson().fromJson(httpResponse.body(), ParsedSchemaResponse.class);

		Set<Statement> shaclShape = new HashSet<>();
		String SUBJ_PREFIX = "https://w3id.org/kpxl/shacl/temp/";
		int i = 0;
		for (String s: r.required) {
			i++;
			shaclShape.add(vf.createStatement(vf.createIRI(SUBJ_PREFIX+i), SHACL_MAX_COUNT, vf.createLiteral(1)));
			shaclShape.add(vf.createStatement(vf.createIRI(SUBJ_PREFIX+i), SHACL_MIN_COUNT, vf.createLiteral(1)));
			if (s.equals(PROFILE_HANDLE) || s.equals(PROFILE_HANDLE_1) || s.equals(PROFILE_HANDLE_2)) {
				shaclShape.add(vf.createStatement(vf.createIRI(SUBJ_PREFIX + i), SHACL_PATH, PROFILE_IRI));
			} else {
				shaclShape.add(vf.createStatement(vf.createIRI(SUBJ_PREFIX + i), SHACL_PATH, vf.createIRI(FDO_TYPE_PREFIX + s)));
			}
			shaclShape.add(vf.createStatement(vf.createIRI(SUBJ_PREFIX), SHACL_PROPERTY, vf.createIRI(SUBJ_PREFIX+i)));
		}
		shaclShape.add(vf.createStatement(FdoUtils.createIri(SUBJ_PREFIX), SHACL_TARGET, TEMP_TYPE));
		shaclShape.add(vf.createStatement(FdoUtils.createIri(SUBJ_PREFIX), RDF.TYPE, SHACL_NODE_SHAPE));

		return shaclShape;
	}

}
