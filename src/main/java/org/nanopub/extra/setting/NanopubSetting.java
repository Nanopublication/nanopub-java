package org.nanopub.extra.setting;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

public class NanopubSetting implements Serializable {

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	public static final IRI HAS_AGENTS = vf.createIRI("http://purl.org/nanopub/x/hasAgents");
	public static final IRI HAS_SERVICES = vf.createIRI("http://purl.org/nanopub/x/hasServices");
	public static final IRI HAS_BOOTSTRAP_SERVICE = vf.createIRI("http://purl.org/nanopub/x/hasBootstrapService");
	public static final IRI HAS_TRUST_RANGE_ALGORITHM = vf.createIRI("http://purl.org/nanopub/x/hasTrustRangeAlgorithm");
	public static final IRI HAS_UPDATE_STRATEGY = vf.createIRI("http://purl.org/nanopub/x/hasUpdateStrategy");

	public static NanopubSetting getLocalSetting() throws RDF4JException, MalformedNanopubException, IOException {
		return getLocalSetting(null);
	}

	public static NanopubSetting getLocalSetting(String name) throws RDF4JException, MalformedNanopubException, IOException {
		if (name == null) name = "default";
		NanopubSetting setting = null;
		try (InputStream in = NanopubSetting.class.getResourceAsStream("/settings/" + name + ".trig")) {
			setting = new NanopubSetting(new NanopubImpl(in, RDFFormat.TRIG));
		}
		return setting;
	}

	private Nanopub nanopub;
	private IRI settingIri;
	private String name;
	private IRI agentIntroCollection;
	private IRI serviceIntroCollection;
	private Set<IRI> bootstrapServices = new HashSet<>();
	private IRI trustRangeAlgorithm;
	private IRI updateStrategy;

	public NanopubSetting(Nanopub nanopub) {
		this.nanopub = nanopub;
		for (Statement st : nanopub.getAssertion()) {
			if (st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof IRI) {
				settingIri = (IRI) st.getSubject();
				break;
			}
		}
		if (settingIri == null) {
			throw new RuntimeException("No setting IRI found: " + nanopub.getUri());
		}
		for (Statement st : nanopub.getAssertion()) {
			if (!st.getSubject().equals(settingIri)) continue;
			IRI pred = st.getPredicate();
			if (pred.equals(RDFS.LABEL)) {
				name = st.getObject().stringValue();
				continue;
			}
			if (!(st.getObject() instanceof IRI obj)) continue;
            if (pred.equals(HAS_AGENTS)) {
				if (agentIntroCollection != null) throw new RuntimeException("Two agent intro collections found: " + nanopub.getUri());
				agentIntroCollection = obj;
			} else if (pred.equals(HAS_SERVICES)) {
				if (serviceIntroCollection != null) throw new RuntimeException("Two service intro collections found: " + nanopub.getUri());
				serviceIntroCollection = obj;
			} else if (pred.equals(HAS_BOOTSTRAP_SERVICE)) {
				bootstrapServices.add(obj);
			} else if (pred.equals(HAS_TRUST_RANGE_ALGORITHM)) {
				if (trustRangeAlgorithm != null) throw new RuntimeException("Two trust range algorithms found: " + nanopub.getUri());
				trustRangeAlgorithm = obj;
			} else if (pred.equals(HAS_UPDATE_STRATEGY)) {
				if (updateStrategy != null) throw new RuntimeException("Two update strategies found: " + nanopub.getUri());
				updateStrategy = obj;
			}
		}
	}

	public Nanopub getNanopub() {
		return nanopub;
	}

	public String getName() {
		return name;
	}

	public IRI getAgentIntroCollection() {
		return agentIntroCollection;
	}

	public IRI getServiceIntroCollection() {
		return serviceIntroCollection;
	}

	public Set<IRI> getBootstrapServices() {
		return bootstrapServices;
	}

	public IRI getTrustRangeAlgorithm() {
		return trustRangeAlgorithm;
	}

	public IRI getUpdateStrategy() {
		return updateStrategy;
	}

}
