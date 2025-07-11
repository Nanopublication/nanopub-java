package org.nanopub.extra.setting;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a nanopub setting, which includes various configurations.
 */
public class NanopubSetting implements Serializable {

    private static ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * The IRI for the agents relation.
     */
    public static final IRI HAS_AGENTS = vf.createIRI("http://purl.org/nanopub/x/hasAgents");

    /**
     * The IRI for the services relation.
     */
    public static final IRI HAS_SERVICES = vf.createIRI("http://purl.org/nanopub/x/hasServices");

    /**
     * The IRI for the bootstrap service relation.
     */
    public static final IRI HAS_BOOTSTRAP_SERVICE = vf.createIRI("http://purl.org/nanopub/x/hasBootstrapService");

    /**
     * The IRI for the trust range algorithm relation.
     */
    public static final IRI HAS_TRUST_RANGE_ALGORITHM = vf.createIRI("http://purl.org/nanopub/x/hasTrustRangeAlgorithm");

    /**
     * The IRI for the update strategy relation.
     */
    public static final IRI HAS_UPDATE_STRATEGY = vf.createIRI("http://purl.org/nanopub/x/hasUpdateStrategy");

    /**
     * Retrieves the default local nanopub setting.
     *
     * @return the NanopubSetting object for the default setting.
     * @throws RDF4JException            if there is an error with RDF4J operations.
     * @throws MalformedNanopubException if the nanopub is malformed.
     * @throws IOException               if there is an error reading the input stream.
     */
    public static NanopubSetting getLocalSetting() throws RDF4JException, MalformedNanopubException, IOException {
        return getLocalSetting(null);
    }

    /**
     * Retrieves the local nanopub setting by name.
     *
     * @param name the name of the setting, or "default" if null is provided.
     * @return the NanopubSetting object corresponding to the specified name.
     * @throws RDF4JException            if there is an error with RDF4J operations.
     * @throws MalformedNanopubException if the nanopub is malformed.
     * @throws IOException               if there is an error reading the input stream.
     */
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

    /**
     * Constructs a NanopubSetting from a Nanopub object.
     *
     * @param nanopub the Nanopub object to create the setting from.
     */
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
                if (agentIntroCollection != null)
                    throw new RuntimeException("Two agent intro collections found: " + nanopub.getUri());
                agentIntroCollection = obj;
            } else if (pred.equals(HAS_SERVICES)) {
                if (serviceIntroCollection != null)
                    throw new RuntimeException("Two service intro collections found: " + nanopub.getUri());
                serviceIntroCollection = obj;
            } else if (pred.equals(HAS_BOOTSTRAP_SERVICE)) {
                bootstrapServices.add(obj);
            } else if (pred.equals(HAS_TRUST_RANGE_ALGORITHM)) {
                if (trustRangeAlgorithm != null)
                    throw new RuntimeException("Two trust range algorithms found: " + nanopub.getUri());
                trustRangeAlgorithm = obj;
            } else if (pred.equals(HAS_UPDATE_STRATEGY)) {
                if (updateStrategy != null)
                    throw new RuntimeException("Two update strategies found: " + nanopub.getUri());
                updateStrategy = obj;
            }
        }
    }

    /**
     * Returns the Nanopub object associated with this setting.
     *
     * @return the Nanopub object representing the setting.
     */
    public Nanopub getNanopub() {
        return nanopub;
    }

    /**
     * Returns the name of the setting.
     *
     * @return the name of the setting, or null if not set.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the IRI of the agent intro collection.
     *
     * @return the IRI of the agent intro collection, or null if not set.
     */
    public IRI getAgentIntroCollection() {
        return agentIntroCollection;
    }

    /**
     * Returns the IRI of the service intro collection.
     *
     * @return the IRI of the service intro collection, or null if not set.
     */
    public IRI getServiceIntroCollection() {
        return serviceIntroCollection;
    }

    /**
     * Returns a set of IRIs representing the bootstrap services.
     *
     * @return a set of IRIs for bootstrap services, which may be empty if none are defined.
     */
    public Set<IRI> getBootstrapServices() {
        return bootstrapServices;
    }

    /**
     * Returns the IRI of the trust range algorithm.
     *
     * @return the IRI of the trust range algorithm, or null if not set.
     */
    public IRI getTrustRangeAlgorithm() {
        return trustRangeAlgorithm;
    }

    /**
     * Returns the IRI of the update strategy.
     *
     * @return the IRI of the update strategy, or null if not set.
     */
    public IRI getUpdateStrategy() {
        return updateStrategy;
    }

}
