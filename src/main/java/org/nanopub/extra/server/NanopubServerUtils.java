package org.nanopub.extra.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.extra.setting.NanopubSetting;

public class NanopubServerUtils {

	protected NanopubServerUtils() {
		throw new RuntimeException("no instances allowed");
	}

	private static final List<String> bootstrapServerList = new ArrayList<>();

	public static List<String> getBootstrapServerList() {
		if (bootstrapServerList.isEmpty()) {
			try {
				for (IRI iri : NanopubSetting.getLocalSetting().getBootstrapServices()) {
					bootstrapServerList.add(iri.stringValue());
				}
			} catch (RDF4JException | MalformedNanopubException | IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		return bootstrapServerList;
	}

	public static final IRI PROTECTED_NANOPUB = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/ProtectedNanopub");

	public static boolean isProtectedNanopub(Nanopub np) {
		for (Statement st : np.getPubinfo()) {
			if (!st.getSubject().equals(np.getUri())) continue;
			if (!st.getPredicate().equals(RDF.TYPE)) continue;
			if (st.getObject().equals(PROTECTED_NANOPUB)) return true;
		}
		return false;
	}

}
