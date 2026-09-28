package org.nanopub.extra.services;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.server.NanopubServerUtils;
import org.nanopub.extra.setting.NanopubSetting;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPS;
import org.nanopub.vocabulary.NPX;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.nanopub.utils.TestUtils.vf;

class ServiceLookupTest {

    private static final IRI COLLECTION = vf.createIRI("https://example.org/serviceCollection");
    private static final IRI INTRO = vf.createIRI("https://example.org/serviceIntro");
    private static final IRI SERVICE_URL = vf.createIRI("https://query.example.org/");
    private static final List<String> BOOTSTRAP = List.of("https://server.example.org/");

    @BeforeEach
    @AfterEach
    void clearCache() {
        ServiceLookup.clearCache();
    }

    /**
     * An index nanopub that lists the given service intro nanopubs.
     */
    private static Nanopub collectionNanopub(IRI... elements) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator("https://example.org/collection#");
        for (IRI element : elements) {
            creator.addAssertionStatement(creator.getNanopubUri(), NPX.INCLUDES_ELEMENT, element);
        }
        creator.addProvenanceStatement(TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(RDF.TYPE, NPX.NANOPUB_INDEX);
        return creator.finalizeNanopub();
    }

    /**
     * A service intro nanopub that declares the given subject to be of the given types.
     */
    private static Nanopub introNanopub(IRI subject, IRI... types) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator("https://example.org/intro#");
        for (IRI type : types) {
            creator.addAssertionStatement(subject, RDF.TYPE, type);
        }
        creator.addProvenanceStatement(TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(TestUtils.anyIri, TestUtils.anyIri);
        return creator.finalizeNanopub();
    }

    private static NanopubSetting settingWithCollection(IRI collection) {
        NanopubSetting setting = mock(NanopubSetting.class);
        when(setting.getServiceIntroCollection()).thenReturn(collection);
        return setting;
    }

    /**
     * Runs the lookup with the whole retrieval chain stubbed out.
     */
    private static List<String> lookup(NanopubSetting setting, Nanopub collection, Nanopub intro) {
        try (MockedStatic<NanopubSetting> settings = mockStatic(NanopubSetting.class);
             MockedStatic<NanopubServerUtils> servers = mockStatic(NanopubServerUtils.class);
             MockedStatic<GetNanopub> getNanopub = mockStatic(GetNanopub.class)) {
            settings.when(NanopubSetting::getDefaultSetting).thenReturn(setting);
            servers.when(NanopubServerUtils::getBootstrapServerList).thenReturn(BOOTSTRAP);
            getNanopub.when(() -> GetNanopub.get(eq(COLLECTION.stringValue()), anyList())).thenReturn(collection);
            getNanopub.when(() -> GetNanopub.get(eq(INTRO.stringValue()), anyList())).thenReturn(intro);

            return ServiceLookup.getServices(NPS.NANOPUB_QUERY_1_1);
        }
    }

    @Test
    void findsTheServiceUrlOfTheRequestedType() throws Exception {
        List<String> services = lookup(settingWithCollection(COLLECTION),
                collectionNanopub(INTRO),
                introNanopub(SERVICE_URL, NPX.NANOPUB_SERVICE, NPS.NANOPUB_QUERY_1_1));

        assertEquals(List.of(SERVICE_URL.stringValue()), services);
    }

    @Test
    void ignoresServicesOfAnotherType() throws Exception {
        List<String> services = lookup(settingWithCollection(COLLECTION),
                collectionNanopub(INTRO),
                introNanopub(SERVICE_URL, NPX.NANOPUB_SERVICE, vf.createIRI("https://example.org/otherType")));

        assertTrue(services.isEmpty());
    }

    @Test
    void ignoresIntrosThatAreNotNanopubServices() throws Exception {
        List<String> services = lookup(settingWithCollection(COLLECTION),
                collectionNanopub(INTRO),
                introNanopub(SERVICE_URL, NPS.NANOPUB_QUERY_1_1));

        assertTrue(services.isEmpty());
    }

    @Test
    void ignoresIntrosWithSeveralTypedSubjects() throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator("https://example.org/intro#");
        creator.addAssertionStatement(SERVICE_URL, RDF.TYPE, NPX.NANOPUB_SERVICE);
        creator.addAssertionStatement(vf.createIRI("https://other.example.org/"), RDF.TYPE, NPS.NANOPUB_QUERY_1_1);
        creator.addProvenanceStatement(TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(TestUtils.anyIri, TestUtils.anyIri);

        List<String> services = lookup(settingWithCollection(COLLECTION),
                collectionNanopub(INTRO), creator.finalizeNanopub());

        assertTrue(services.isEmpty());
    }

    @Test
    void copesWithAnIntroThatCannotBeRetrieved() throws Exception {
        List<String> services = lookup(settingWithCollection(COLLECTION), collectionNanopub(INTRO), null);

        assertTrue(services.isEmpty());
    }

    @Test
    void copesWithAnEmptySettingCollection() throws Exception {
        List<String> services = lookup(settingWithCollection(null), collectionNanopub(INTRO), null);

        assertTrue(services.isEmpty());
    }

    @Test
    void copesWithACollectionThatCannotBeRetrieved() throws Exception {
        List<String> services = lookup(settingWithCollection(COLLECTION), null, null);

        assertTrue(services.isEmpty());
    }

    @Test
    void copesWithAFailingSetting() {
        try (MockedStatic<NanopubSetting> settings = mockStatic(NanopubSetting.class)) {
            settings.when(NanopubSetting::getDefaultSetting).thenThrow(new RuntimeException("no setting"));

            assertTrue(ServiceLookup.getServices(NPS.NANOPUB_QUERY_1_1).isEmpty());
        }
    }

    @Test
    void cachesTheResult() throws Exception {
        NanopubSetting setting = settingWithCollection(COLLECTION);
        Nanopub collection = collectionNanopub(INTRO);
        Nanopub intro = introNanopub(SERVICE_URL, NPX.NANOPUB_SERVICE, NPS.NANOPUB_QUERY_1_1);

        List<String> first = lookup(setting, collection, intro);
        // the second lookup runs without any of the retrieval stubs in place, so it can only
        // succeed if the answer was cached
        List<String> second = ServiceLookup.getServices(NPS.NANOPUB_QUERY_1_1);

        assertEquals(first, second);
        assertEquals(List.of(SERVICE_URL.stringValue()), second);
    }

    @Test
    void clearCacheForgetsWhatWasLookedUp() throws Exception {
        lookup(settingWithCollection(COLLECTION), collectionNanopub(INTRO),
                introNanopub(SERVICE_URL, NPX.NANOPUB_SERVICE, NPS.NANOPUB_QUERY_1_1));

        ServiceLookup.clearCache();

        // without the stubs, and with nothing cached, the lookup falls back to an empty list
        try (MockedStatic<NanopubSetting> settings = mockStatic(NanopubSetting.class)) {
            settings.when(NanopubSetting::getDefaultSetting).thenThrow(new RuntimeException("no setting"));
            assertTrue(ServiceLookup.getServices(NPS.NANOPUB_QUERY_1_1).isEmpty());
        }
    }

}
