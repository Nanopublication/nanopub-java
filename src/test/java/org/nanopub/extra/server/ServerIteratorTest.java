package org.nanopub.extra.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServerIteratorTest {

    private static final String OVERRIDE_URL = "https://local-registry.example/";

    @TempDir
    Path tempHome;

    private String oldUserHome;
    private String oldOverride;

    @BeforeEach
    void setUp() throws Exception {
        oldUserHome = System.getProperty("user.home");
        oldOverride = System.getProperty(NanopubServerUtils.REGISTRY_INSTANCES_PROPERTY);
        System.setProperty("user.home", tempHome.toString());
        System.clearProperty(NanopubServerUtils.REGISTRY_INSTANCES_PROPERTY);
        resetCaches();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (oldUserHome == null) {
            System.clearProperty("user.home");
        } else {
            System.setProperty("user.home", oldUserHome);
        }
        if (oldOverride == null) {
            System.clearProperty(NanopubServerUtils.REGISTRY_INSTANCES_PROPERTY);
        } else {
            System.setProperty(NanopubServerUtils.REGISTRY_INSTANCES_PROPERTY, oldOverride);
        }
        resetCaches();
    }

    private void resetCaches() throws Exception {
        Field registryServerList = NanopubServerUtils.class.getDeclaredField("registryServerList");
        registryServerList.setAccessible(true);
        registryServerList.set(null, null);
        Field serverInfos = ServerIterator.class.getDeclaredField("serverInfos");
        serverInfos.setAccessible(true);
        ((Map<?, ?>) serverInfos.get(null)).clear();
    }

    private File cacheFile() {
        return tempHome.resolve(".nanopub/cachedservers").toFile();
    }

    private List<RegistryInfo> registries(String... urls) {
        List<RegistryInfo> list = new ArrayList<>();
        for (String url : urls) {
            RegistryInfo info = new RegistryInfo();
            info.url = url;
            info.status = "ready";
            list.add(info);
        }
        return list;
    }

    private List<RegistryInfo> publicRegistries() {
        return registries("https://public-a.example/", "https://public-b.example/", "https://public-c.example/",
                "https://public-d.example/", "https://public-e.example/");
    }

    private void writeCacheFile(List<RegistryInfo> registries) throws Exception {
        File file = cacheFile();
        file.getParentFile().mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(registries);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> serversToContact(ServerIterator iterator) throws Exception {
        Field f = ServerIterator.class.getDeclaredField("serversToContact");
        f.setAccessible(true);
        return (List<String>) f.get(iterator);
    }

    private Object cachedServers(ServerIterator iterator) throws Exception {
        Field f = ServerIterator.class.getDeclaredField("cachedServers");
        f.setAccessible(true);
        return f.get(iterator);
    }

    @Test
    void cachedListIsUsedWhenNoOverrideIsSet() throws Exception {
        writeCacheFile(publicRegistries());

        ServerIterator iterator = new ServerIterator();

        assertNotNull(cachedServers(iterator), "A fresh cache file should be picked up");
        assertTrue(serversToContact(iterator).isEmpty(), "The bootstrap list should not be consulted");
        assertEquals("https://public-a.example/", iterator.next().getUrl());
    }

    @Test
    void overrideBeatsTheCachedList() throws Exception {
        writeCacheFile(publicRegistries());
        System.setProperty(NanopubServerUtils.REGISTRY_INSTANCES_PROPERTY, OVERRIDE_URL);

        ServerIterator iterator = new ServerIterator();

        assertNull(cachedServers(iterator), "The cache must be ignored when an override is configured");
        assertEquals(List.of(OVERRIDE_URL), serversToContact(iterator));
    }

    @Test
    void writeCachedServersPersistsTheListWithoutAnOverride() throws Exception {
        ServerIterator.writeCachedServers(publicRegistries());

        assertTrue(cacheFile().exists());
    }

    @Test
    void writeCachedServersRefusesToPersistUnderAnOverride() throws Exception {
        System.setProperty(NanopubServerUtils.REGISTRY_INSTANCES_PROPERTY, OVERRIDE_URL);

        ServerIterator.writeCachedServers(publicRegistries());

        assertFalse(cacheFile().exists(), "A list gathered under an override must not poison the shared cache");
    }

}
