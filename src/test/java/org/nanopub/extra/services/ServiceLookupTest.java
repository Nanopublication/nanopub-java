package org.nanopub.extra.services;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.server.NanopubServerUtils;
import org.nanopub.extra.setting.NanopubSetting;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPS;
import org.nanopub.vocabulary.NPX;

import java.nio.file.Files;
import java.nio.file.Path;
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

    // A collection with a trusty URI, which is what makes its content worth keeping in a file.
    private static final IRI TRUSTY_COLLECTION = vf.createIRI("https://w3id.org/np/RAs1kSfFxDv8n9xfHH3ls18e5yO7w5E0vuAiI5K4yM1nY");
    private static final IRI REGISTRY_URL = vf.createIRI("https://registry.example.org/");

    @TempDir
    Path tempDir;

    private Path cacheFile;

    private String cacheFilePropertyBefore;

    @BeforeEach
    void keepServicesInATempFile() {
        cacheFilePropertyBefore = System.getProperty(ServiceLookup.CACHE_FILE_PROPERTY);
        cacheFile = tempDir.resolve("cachedservices");
        System.setProperty(ServiceLookup.CACHE_FILE_PROPERTY, cacheFile.toString());
        ServiceLookup.clearCache();
    }

    @AfterEach
    void clearCache() {
        // put back rather than cleared: cleared, the tests that follow would use the file in the
        // home directory
        if (cacheFilePropertyBefore == null) {
            System.clearProperty(ServiceLookup.CACHE_FILE_PROPERTY);
        } else {
            System.setProperty(ServiceLookup.CACHE_FILE_PROPERTY, cacheFilePropertyBefore);
        }
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

    /**
     * Runs the lookup for the trusty collection, and reports how often the intro was fetched.
     */
    private static int lookupCountingFetches(Nanopub collection, Nanopub intro, IRI... types) {
        try (MockedStatic<NanopubSetting> settings = mockStatic(NanopubSetting.class);
             MockedStatic<NanopubServerUtils> servers = mockStatic(NanopubServerUtils.class);
             MockedStatic<GetNanopub> getNanopub = mockStatic(GetNanopub.class)) {
            NanopubSetting setting = settingWithCollection(TRUSTY_COLLECTION);
            settings.when(NanopubSetting::getDefaultSetting).thenReturn(setting);
            servers.when(NanopubServerUtils::getBootstrapServerList).thenReturn(BOOTSTRAP);
            getNanopub.when(() -> GetNanopub.get(eq(TRUSTY_COLLECTION.stringValue()), anyList())).thenReturn(collection);
            getNanopub.when(() -> GetNanopub.get(eq(INTRO.stringValue()), anyList())).thenReturn(intro);

            for (IRI type : types) {
                ServiceLookup.getServices(type);
            }
            return (int) mockingDetails(GetNanopub.class).getInvocations().stream()
                    .filter(invocation -> INTRO.stringValue().equals(invocation.getArgument(0))).count();
        }
    }

    /**
     * Publishing needs both the query and the registry instances. The collection says what each
     * service is, so reading it once answers for every type (issue #208).
     */
    @Test
    void walksTheCollectionOnceForAllTypes() throws Exception {
        int fetches = lookupCountingFetches(collectionNanopub(INTRO),
                introNanopub(SERVICE_URL, NPX.NANOPUB_SERVICE, NPS.NANOPUB_QUERY_1_1, NPS.NANOPUB_REGISTRY_1_0),
                NPS.NANOPUB_QUERY_1_1, NPS.NANOPUB_REGISTRY_1_0);

        assertEquals(1, fetches);
        assertEquals(List.of(SERVICE_URL.stringValue()), ServiceLookup.getServices(NPS.NANOPUB_QUERY_1_1));
        assertEquals(List.of(SERVICE_URL.stringValue()), ServiceLookup.getServices(NPS.NANOPUB_REGISTRY_1_0));
    }

    /**
     * The command line starts a JVM per call, so what is only remembered in memory is looked up
     * again by every call. A JVM that starts afresh is what clearing the memory stands in for.
     */
    @Test
    void whatWasReadIsKeptForTheNextJvm() throws Exception {
        Nanopub collection = collectionNanopub(INTRO);
        Nanopub intro = introNanopub(SERVICE_URL, NPX.NANOPUB_SERVICE, NPS.NANOPUB_QUERY_1_1);
        assertEquals(1, lookupCountingFetches(collection, intro, NPS.NANOPUB_QUERY_1_1));
        assertTrue(Files.exists(cacheFile));

        ServiceLookup.clearCache();

        assertEquals(0, lookupCountingFetches(collection, intro, NPS.NANOPUB_QUERY_1_1));
        assertEquals(List.of(SERVICE_URL.stringValue()), ServiceLookup.getServices(NPS.NANOPUB_QUERY_1_1));
        assertTrue(ServiceLookup.getServices(NPS.NANOPUB_REGISTRY_1_0).isEmpty());
    }

    @Test
    void whatWasKeptAnswersForEveryType() throws Exception {
        Files.writeString(cacheFile, "# services of " + TRUSTY_COLLECTION + "\n"
                + SERVICE_URL + "\t" + NPS.NANOPUB_QUERY_1_1 + "\n"
                + REGISTRY_URL + "\t" + NPS.NANOPUB_REGISTRY_1_0 + " https://example.org/otherType\n");

        assertEquals(0, lookupCountingFetches(null, null, NPS.NANOPUB_QUERY_1_1, NPS.NANOPUB_REGISTRY_1_0));
        assertEquals(List.of(SERVICE_URL.stringValue()), ServiceLookup.getServices(NPS.NANOPUB_QUERY_1_1));
        assertEquals(List.of(REGISTRY_URL.stringValue()), ServiceLookup.getServices(NPS.NANOPUB_REGISTRY_1_0));
    }

    /**
     * A new setting names a new collection, and what an earlier one listed says nothing about it.
     */
    @Test
    void whatWasKeptForAnotherCollectionIsNotUsed() throws Exception {
        Files.writeString(cacheFile, "# services of https://w3id.org/np/RAanotherCollection0000000000000000000000000000\n"
                + "https://stale.example.org/\t" + NPS.NANOPUB_QUERY_1_1 + "\n");

        int fetches = lookupCountingFetches(collectionNanopub(INTRO),
                introNanopub(SERVICE_URL, NPX.NANOPUB_SERVICE, NPS.NANOPUB_QUERY_1_1), NPS.NANOPUB_QUERY_1_1);

        assertEquals(1, fetches);
        assertEquals(List.of(SERVICE_URL.stringValue()), ServiceLookup.getServices(NPS.NANOPUB_QUERY_1_1));
        assertTrue(Files.readString(cacheFile).startsWith("# services of " + TRUSTY_COLLECTION));
    }

    @Test
    void aFileThatCannotBeReadIsLookedPast() throws Exception {
        Files.writeString(cacheFile, "# services of " + TRUSTY_COLLECTION + "\nnot a line this class wrote\n");

        int fetches = lookupCountingFetches(collectionNanopub(INTRO),
                introNanopub(SERVICE_URL, NPX.NANOPUB_SERVICE, NPS.NANOPUB_QUERY_1_1), NPS.NANOPUB_QUERY_1_1);

        assertEquals(1, fetches);
        assertEquals(List.of(SERVICE_URL.stringValue()), ServiceLookup.getServices(NPS.NANOPUB_QUERY_1_1));
    }

    /**
     * An intro that could not be fetched may be there a minute later. Keeping the list without it
     * would hide that service from every later call.
     */
    @Test
    void aCollectionThatCouldNotBeReadToTheEndIsNotKept() throws Exception {
        lookupCountingFetches(collectionNanopub(INTRO), null, NPS.NANOPUB_QUERY_1_1);

        assertFalse(Files.exists(cacheFile));
    }

    @Test
    void aCollectionThatCouldNotBeRetrievedIsNotKept() {
        lookupCountingFetches(null, null, NPS.NANOPUB_QUERY_1_1);

        assertFalse(Files.exists(cacheFile));
    }

    /**
     * Only a trusty URI promises that what it names does not change.
     */
    @Test
    void aCollectionWithoutATrustyUriIsNotKept() throws Exception {
        lookup(settingWithCollection(COLLECTION), collectionNanopub(INTRO),
                introNanopub(SERVICE_URL, NPX.NANOPUB_SERVICE, NPS.NANOPUB_QUERY_1_1));

        assertFalse(Files.exists(cacheFile));
    }

    @Test
    void nothingIsKeptWhenNoFileIsNamed() throws Exception {
        System.setProperty(ServiceLookup.CACHE_FILE_PROPERTY, "");

        int fetches = lookupCountingFetches(collectionNanopub(INTRO),
                introNanopub(SERVICE_URL, NPX.NANOPUB_SERVICE, NPS.NANOPUB_QUERY_1_1), NPS.NANOPUB_QUERY_1_1);

        assertEquals(1, fetches);
        assertFalse(Files.exists(cacheFile));
    }

    /**
     * Not being able to keep the answer costs the next call a few seconds. It must not cost this one
     * its answer.
     */
    @Test
    void aFileThatCannotBeWrittenLeavesTheLookupAlone() throws Exception {
        Path inTheWay = Files.writeString(tempDir.resolve("a-file-not-a-directory"), "");
        System.setProperty(ServiceLookup.CACHE_FILE_PROPERTY, inTheWay.resolve("cachedservices").toString());

        lookupCountingFetches(collectionNanopub(INTRO),
                introNanopub(SERVICE_URL, NPX.NANOPUB_SERVICE, NPS.NANOPUB_QUERY_1_1), NPS.NANOPUB_QUERY_1_1);

        assertEquals(List.of(SERVICE_URL.stringValue()), ServiceLookup.getServices(NPS.NANOPUB_QUERY_1_1));
    }

}
