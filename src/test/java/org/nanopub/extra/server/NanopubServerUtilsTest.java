package org.nanopub.extra.server;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.services.ServiceLookup;
import org.nanopub.utils.TestUtils;
import org.nanopub.vocabulary.NPS;
import org.nanopub.vocabulary.NPX;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NanopubServerUtilsTest {

    private static String overridePropertyOld;

    @BeforeAll
    static void beforeAll() {
        overridePropertyOld = System.getProperty(NanopubServerUtils.REGISTRY_INSTANCES_PROPERTY);
    }

    @AfterAll
    static void afterAll() {
        if (overridePropertyOld == null) {
            System.clearProperty(NanopubServerUtils.REGISTRY_INSTANCES_PROPERTY);
        } else {
            System.setProperty(NanopubServerUtils.REGISTRY_INSTANCES_PROPERTY, overridePropertyOld);
        }
    }

    @BeforeEach
    @AfterEach
    void resetCaches() throws NoSuchFieldException, IllegalAccessException {
        System.clearProperty(NanopubServerUtils.REGISTRY_INSTANCES_PROPERTY);
        System.clearProperty(NanopubServerUtils.REGISTRY_EVICTION_COOLDOWN_PROPERTY);
        Field f = NanopubServerUtils.class.getDeclaredField("registryServerList");
        f.setAccessible(true);
        f.set(null, null);
        Field e = NanopubServerUtils.class.getDeclaredField("evictedRegistriesUntil");
        e.setAccessible(true);
        ((Map<?, ?>) e.get(null)).clear();
        ServiceLookup.clearCache();
    }

    @Test
    void isProtectedNanopubTrue() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(TestUtils.anyIri, TestUtils.anyIri, TestUtils.anyIri);
        creator.addProvenanceStatement(TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(RDF.TYPE, NPX.PROTECTED_NANOPUB);
        Nanopub nanopub = creator.finalizeNanopub();
        assertTrue(NanopubServerUtils.isProtectedNanopub(nanopub));
    }

    @Test
    void isProtectedNanopubFalse() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        assertFalse(NanopubServerUtils.isProtectedNanopub(nanopub));
    }

    @Test
    void getRegistryServerListUsesOverride() {
        System.setProperty(NanopubServerUtils.REGISTRY_INSTANCES_PROPERTY,
                "https://reg-a.example/  https://reg-b.example/");
        assertEquals(
                List.of("https://reg-a.example/", "https://reg-b.example/"),
                NanopubServerUtils.getRegistryServerList());
    }

    @Test
    void getRegistryServerListUnionsBootstrapAndDiscovered() throws Exception {
        List<String> bootstrap = NanopubServerUtils.getBootstrapServerList();
        // Pick one real bootstrap URL to verify dedupe, plus a fresh URL to verify appending.
        String duplicateOfBootstrap = bootstrap.get(0);
        String freshDiscovered = "https://discovered.example/";
        seedServiceLookupCache(NPS.NANOPUB_REGISTRY_1_0,
                List.of(duplicateOfBootstrap, freshDiscovered));

        List<String> result = NanopubServerUtils.getRegistryServerList();

        assertEquals(bootstrap.size() + 1, result.size(),
                "Discovered URL that duplicates a bootstrap entry should be deduped");
        assertEquals(bootstrap, result.subList(0, bootstrap.size()),
                "Bootstrap URLs should appear first and in original order");
        assertEquals(freshDiscovered, result.get(result.size() - 1),
                "Fresh discovered URL should be appended at the end");
        for (String b : bootstrap) {
            assertEquals(1, result.stream().filter(b::equals).count(),
                    "Bootstrap URL " + b + " should appear exactly once");
        }
    }

    @Test
    void getRegistryServerListIsCached() throws Exception {
        seedServiceLookupCache(NPS.NANOPUB_REGISTRY_1_0, List.of("https://x.example/"));
        List<String> first = NanopubServerUtils.getRegistryServerList();

        // Mutate the override after the first call. The cached result should be unaffected.
        System.setProperty(NanopubServerUtils.REGISTRY_INSTANCES_PROPERTY, "https://y.example/");
        List<String> second = NanopubServerUtils.getRegistryServerList();

        assertEquals(first, second);
    }

    @SuppressWarnings("unchecked")
    private static void seedServiceLookupCache(IRI typeIri, List<String> urls) throws Exception {
        Field f = ServiceLookup.class.getDeclaredField("cache");
        f.setAccessible(true);
        ((Map<IRI, List<String>>) f.get(null)).put(typeIri, urls);
    }

    // --- Registry status / eviction ---

    @Test
    void isReadyRegistryStatusByString() {
        for (String good : List.of("ready", "Ready", "updating", "UPDATING")) {
            assertTrue(NanopubServerUtils.isReadyRegistryStatus(good), good + " should be ready");
        }
        // coreReady is excluded: matches the registry's own stricter check in
        // RegistryPeerConnector / NanopubLoader (only the core nanopubs are
        // loaded at that stage; the full corpus is still being fetched).
        for (String bad : List.of("launching", "coreLoading", "coreReady", "Loading", "resetting", "unknown")) {
            assertFalse(NanopubServerUtils.isReadyRegistryStatus(bad), bad + " should not be ready");
        }
        // Backwards compatibility: missing/empty status is treated as ready.
        assertTrue(NanopubServerUtils.isReadyRegistryStatus((String) null));
        assertTrue(NanopubServerUtils.isReadyRegistryStatus(""));
    }

    @Test
    void isReadyRegistryStatusByResponse() {
        org.apache.http.HttpResponse resp = org.mockito.Mockito.mock(org.apache.http.HttpResponse.class);
        // Missing header => ready.
        org.mockito.Mockito.when(resp.getFirstHeader(NanopubServerUtils.REGISTRY_STATUS_HEADER)).thenReturn(null);
        assertTrue(NanopubServerUtils.isReadyRegistryStatus(resp));

        org.apache.http.Header h = org.mockito.Mockito.mock(org.apache.http.Header.class);
        org.mockito.Mockito.when(h.getValue()).thenReturn("ready");
        org.mockito.Mockito.when(resp.getFirstHeader(NanopubServerUtils.REGISTRY_STATUS_HEADER)).thenReturn(h);
        assertTrue(NanopubServerUtils.isReadyRegistryStatus(resp));

        org.mockito.Mockito.when(h.getValue()).thenReturn("coreLoading");
        assertFalse(NanopubServerUtils.isReadyRegistryStatus(resp));
    }

    @Test
    void getRegistryEvictionCooldownMillisDefault() {
        System.clearProperty(NanopubServerUtils.REGISTRY_EVICTION_COOLDOWN_PROPERTY);
        assertEquals(300_000L, NanopubServerUtils.getRegistryEvictionCooldownMillis());
    }

    @Test
    void getRegistryEvictionCooldownMillisFromProperty() {
        System.setProperty(NanopubServerUtils.REGISTRY_EVICTION_COOLDOWN_PROPERTY, "45");
        try {
            assertEquals(45_000L, NanopubServerUtils.getRegistryEvictionCooldownMillis());
        } finally {
            System.clearProperty(NanopubServerUtils.REGISTRY_EVICTION_COOLDOWN_PROPERTY);
        }
    }

    @Test
    void getRegistryEvictionCooldownMillisIgnoresInvalidValues() {
        System.setProperty(NanopubServerUtils.REGISTRY_EVICTION_COOLDOWN_PROPERTY, "-1");
        try {
            assertEquals(300_000L, NanopubServerUtils.getRegistryEvictionCooldownMillis());
        } finally {
            System.clearProperty(NanopubServerUtils.REGISTRY_EVICTION_COOLDOWN_PROPERTY);
        }
        System.setProperty(NanopubServerUtils.REGISTRY_EVICTION_COOLDOWN_PROPERTY, "carrot");
        try {
            assertEquals(300_000L, NanopubServerUtils.getRegistryEvictionCooldownMillis());
        } finally {
            System.clearProperty(NanopubServerUtils.REGISTRY_EVICTION_COOLDOWN_PROPERTY);
        }
    }

    @Test
    void evictRegistryAndCheck() {
        String url = "https://reg-x.example/";
        assertFalse(NanopubServerUtils.isRegistryEvicted(url));
        NanopubServerUtils.evictRegistry(url, "test");
        assertTrue(NanopubServerUtils.isRegistryEvicted(url));
    }

    @Test
    void evictionRespectsCooldown() throws Exception {
        // Cool-down of 0s — the eviction should immediately be considered expired.
        System.setProperty(NanopubServerUtils.REGISTRY_EVICTION_COOLDOWN_PROPERTY, "0");
        try {
            String url = "https://reg-y.example/";
            NanopubServerUtils.evictRegistry(url, "test");
            assertFalse(NanopubServerUtils.isRegistryEvicted(url),
                    "0-second cool-down should not keep the registry evicted");
        } finally {
            System.clearProperty(NanopubServerUtils.REGISTRY_EVICTION_COOLDOWN_PROPERTY);
        }
    }

}