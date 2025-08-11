package org.nanopub.extra.setting;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPX;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

class NanopubSettingTest {

    @Test
    void getLocalDefaultSetting() throws MalformedNanopubException, IOException {
        NanopubSetting setting = NanopubSetting.getLocalSetting();
        assertNotNull(setting);
        assertEquals(setting.getNanopub().getUri().getLocalName(), "RAqzx-R_CV5VqJ2Ib_lRnhW0wTILBhSH8cg0xmZwCi-Ag");
    }

    @Test
    void getLocalSettingWithName() throws MalformedNanopubException, IOException {
        NanopubSetting setting = NanopubSetting.getLocalSetting(null);
        NanopubSetting settingWithName = NanopubSetting.getLocalSetting("default");
        assertEquals(setting, settingWithName);

        assertThrows(NullPointerException.class, () -> NanopubSetting.getLocalSetting("test"));
    }

    @Test
    void constructorThrowsRuntimeException() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(vf.createStatement(anyIri, anyIri, anyIri));
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        assertThrows(RuntimeException.class, () -> new NanopubSetting(nanopub));

        NanopubCreator creator2 = TestUtils.getNanopubCreator();
        creator2.addAssertionStatements(vf.createStatement(anyIri, RDF.TYPE, vf.createLiteral("Something that is not an IRI")));
        creator2.addProvenanceStatements(vf.createStatement(creator2.getAssertionUri(), anyIri, anyIri));
        creator2.addPubinfoStatements(vf.createStatement(creator2.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub2 = creator2.finalizeNanopub();
        assertThrows(RuntimeException.class, () -> new NanopubSetting(nanopub2));
    }

    @Test
    void constructorWithDoubleAgent() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri),
                vf.createStatement(anyIri, RDF.TYPE, vf.createLiteral("Something that is not an IRI")),
                vf.createStatement(vf.createIRI(anyIri.stringValue() + "/another"), anyIri, anyIri),
                vf.createStatement(anyIri, NPX.HAS_AGENTS, anyIri),
                vf.createStatement(anyIri, NPX.HAS_AGENTS, vf.createIRI("https://knowledgepixels.com/nanopubIri#anotherIri"))
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        assertThrows(RuntimeException.class, () -> new NanopubSetting(nanopub));
    }

    @Test
    void constructorWithDoubleServices() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri),
                vf.createStatement(anyIri, NPX.HAS_SERVICES, anyIri),
                vf.createStatement(anyIri, NPX.HAS_SERVICES, vf.createIRI("https://knowledgepixels.com/nanopubIri#anotherIri"))
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        assertThrows(RuntimeException.class, () -> new NanopubSetting(nanopub));
    }

    @Test
    void constructorWithDoubleTrustRangeAlgorithm() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri),
                vf.createStatement(anyIri, NPX.HAS_TRUST_RANGE_ALGORITHM, anyIri),
                vf.createStatement(anyIri, NPX.HAS_TRUST_RANGE_ALGORITHM, vf.createIRI("https://knowledgepixels.com/nanopubIri#anotherIri"))
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        assertThrows(RuntimeException.class, () -> new NanopubSetting(nanopub));
    }

    @Test
    void constructorWithDoubleUpdateStrategy() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri),
                vf.createStatement(anyIri, NPX.HAS_UPDATE_STRATEGY, anyIri),
                vf.createStatement(anyIri, NPX.HAS_UPDATE_STRATEGY, vf.createIRI("https://knowledgepixels.com/nanopubIri#anotherIri"))
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        assertThrows(RuntimeException.class, () -> new NanopubSetting(nanopub));
    }

    @Test
    void getNanopub() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(vf.createStatement(anyIri, RDF.TYPE, anyIri));
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        Nanopub retrievedNanopub = setting.getNanopub();
        assertEquals(nanopub, retrievedNanopub);
    }

    @Test
    void getNameWhenSet() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        String settingLabel = "Test Setting";
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri),
                vf.createStatement(anyIri, RDFS.LABEL, vf.createLiteral(settingLabel))
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        assertEquals(settingLabel, setting.getName());
    }

    @Test
    void getNameWhenNotSet() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        assertNull(setting.getName());
    }

    @Test
    void getUpdateStrategyWhenSet() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        IRI updateStrategyIri = vf.createIRI("https://knowledgepixels.com/nanopubIri#updateStrategy");
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri),
                vf.createStatement(anyIri, NPX.HAS_UPDATE_STRATEGY, updateStrategyIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        assertEquals(updateStrategyIri, setting.getUpdateStrategy());
    }

    @Test
    void getUpdateStrategyWhenNotSet() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        assertNull(setting.getUpdateStrategy());
    }

    @Test
    void getTrustRangeAlgorithmWhenSet() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        IRI trustRangeAlgorithmIri = vf.createIRI("https://knowledgepixels.com/nanopubIri#trustRangeAlgorithm");
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri),
                vf.createStatement(anyIri, NPX.HAS_TRUST_RANGE_ALGORITHM, trustRangeAlgorithmIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        assertEquals(trustRangeAlgorithmIri, setting.getTrustRangeAlgorithm());
    }

    @Test
    void getTrustRangeAlgorithmWhenNotSet() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        assertNull(setting.getTrustRangeAlgorithm());
    }

    @Test
    void getServiceIntroCollectionWhenSet() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        IRI serviceIntroCollectionIri = vf.createIRI("https://knowledgepixels.com/nanopubIri#serviceIntroCollection");
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri),
                vf.createStatement(anyIri, NPX.HAS_SERVICES, serviceIntroCollectionIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        assertEquals(serviceIntroCollectionIri, setting.getServiceIntroCollection());
    }

    @Test
    void getServiceIntroCollectionWhenNotSet() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        assertNull(setting.getServiceIntroCollection());
    }

    @Test
    void getAgentIntroCollectionWhenSet() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        IRI agentIntroCollectionIri = vf.createIRI("https://knowledgepixels.com/nanopubIri#agentIntroCollection");
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri),
                vf.createStatement(anyIri, NPX.HAS_AGENTS, agentIntroCollectionIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        assertEquals(agentIntroCollectionIri, setting.getAgentIntroCollection());
    }

    @Test
    void getAgentIntroCollectionWhenNotSet() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        assertNull(setting.getAgentIntroCollection());
    }

    @Test
    void getBootstrapServicesWhenSet() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        Set<IRI> bootstrapServices = new HashSet<>();
        IRI bootstrapService1 = vf.createIRI("https://knowledgepixels.com/nanopubIri#bootstrapService1");
        IRI bootstrapService2 = vf.createIRI("https://knowledgepixels.com/nanopubIri#bootstrapService2");
        bootstrapServices.add(bootstrapService1);
        bootstrapServices.add(bootstrapService2);
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri),
                vf.createStatement(anyIri, NPX.HAS_BOOTSTRAP_SERVICE, bootstrapService1),
                vf.createStatement(anyIri, NPX.HAS_BOOTSTRAP_SERVICE, bootstrapService2)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        assertEquals(bootstrapServices, setting.getBootstrapServices());
        assertEquals(2, setting.getBootstrapServices().size());
    }

    @Test
    void getBootstrapServicesWhenNotSet() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatements(
                vf.createStatement(anyIri, RDF.TYPE, anyIri)
        );
        creator.addProvenanceStatements(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();
        NanopubSetting setting = new NanopubSetting(nanopub);
        assertTrue(setting.getBootstrapServices().isEmpty());
    }

}