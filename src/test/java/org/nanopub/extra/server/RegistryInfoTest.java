package org.nanopub.extra.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.NanopubUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistryInfoTest {

    private static final long SETUP_ID = 4464410476474558003L;
    private static final int TRUST_STATE_COUNTER = 10;
    private static final String LAST_TRUST_STATE_UPDATE = "2025-07-16T14:46:59.217214952Z[Etc/UTC]";
    private static final String TRUST_STATE_HASH = "8e9a4d6aac892105d4b5ae7e89233c159fe175418ff607e133e980a83456d082";
    private static final String STATUS = "ready";
    private static final String COVERAGE_TYPES = "all";
    private static final String COVERAGE_AGENTS = "viaSetting";
    private static final String CURRENT_SETTING = "RA8DQemv3WwH2K_YON_fnyyVvWVnxaTFFU4YAFW6gFyc4";
    private static final String ORIGINAL_SETTING = "RA8DQemv3WwH2K_YON_fnyyVvWVnxaTFFU4YAFW6gFyc4";
    private static final int AGENT_COUNT = 447;
    private static final int ACCOUNT_COUNT = 513;
    private static final int NANOPUB_COUNT = 63963;
    private static final int LOAD_COUNTER = 63963;

    private static final String registryInfoJsonString = String.format(
            "{\"setupId\":%d,\"trustStateCounter\":%d,\"lastTrustStateUpdate\":\"%s\",\"trustStateHash\":\"%s\",\"status\":\"%s\",\"coverageTypes\":\"%s\",\"coverageAgents\":\"%s\",\"currentSetting\":\"%s\",\"originalSetting\":\"%s\",\"agentCount\":%d,\"accountCount\":%d,\"nanopubCount\":%d,\"loadCounter\":%d}",
            SETUP_ID, TRUST_STATE_COUNTER, LAST_TRUST_STATE_UPDATE, TRUST_STATE_HASH, STATUS, COVERAGE_TYPES, COVERAGE_AGENTS, CURRENT_SETTING, ORIGINAL_SETTING, AGENT_COUNT, ACCOUNT_COUNT, NANOPUB_COUNT, LOAD_COUNTER
    );

    private final String validUrl = "https://registry.np.trustyuri.net/";
    private final String invalidUrl = "https://invalid.registry.url/";

    MockedStatic<NanopubUtils> mockStatic = mockStatic(NanopubUtils.class);
    private RegistryInfo registryInfo;

    @BeforeEach
    void setUp() throws IOException, RegistryInfo.RegistryInfoException {
        InputStream mockInputStream = new ByteArrayInputStream(registryInfoJsonString.getBytes(StandardCharsets.UTF_8));

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(mockInputStream);
        mockStatic.when(NanopubUtils::getHttpClient).thenReturn(mockHttpClient);

        registryInfo = RegistryInfo.load(validUrl);
    }

    @AfterEach
    void tearDown() {
        mockStatic.close();
    }

    @Test
    void loadReturnsRegistryInfoForValidUrl() {
        assertNotNull(registryInfo);
        assertEquals(validUrl, registryInfo.getUrl());
    }

    @Test
    void loadThrowsExceptionForInvalidUrl() {
        assertThrows(RegistryInfo.RegistryInfoException.class, () -> RegistryInfo.load(invalidUrl));
    }

    @Test
    void getSetupId() {
        assertEquals(SETUP_ID, registryInfo.getSetupId());
    }

    @Test
    void getTrustStateCounter() {
        assertEquals(TRUST_STATE_COUNTER, registryInfo.getTrustStateCounter());
    }

    @Test
    void getCollectionUrl() {
        assertEquals(validUrl + "np/", registryInfo.getCollectionUrl());
    }

    @Test
    void getLastTrustStateUpdate() {
        assertEquals(LAST_TRUST_STATE_UPDATE, registryInfo.getLastTrustStateUpdate());
    }

    @Test
    void getTrustStateHash() {
        assertEquals(TRUST_STATE_HASH, registryInfo.getTrustStateHash());
    }

    @Test
    void getStatus() {
        assertEquals(STATUS, registryInfo.getStatus());
    }

    @Test
    void getCoverageTypes() {
        assertEquals(COVERAGE_TYPES, registryInfo.getCoverageTypes());
    }

    @Test
    void getCoverageAgents() {
        assertEquals(COVERAGE_AGENTS, registryInfo.getCoverageAgents());
    }

    @Test
    void getCurrentSetting() {
        assertEquals(CURRENT_SETTING, registryInfo.getCurrentSetting());
    }

    @Test
    void getOriginalSetting() {
        assertEquals(ORIGINAL_SETTING, registryInfo.getOriginalSetting());
    }

    @Test
    void getAgentCount() {
        assertEquals(AGENT_COUNT, registryInfo.getAgentCount());
    }

    @Test
    void getAccountCount() {
        assertEquals(ACCOUNT_COUNT, registryInfo.getAccountCount());
    }

    @Test
    void getNanopubCount() {
        assertEquals(NANOPUB_COUNT, registryInfo.getNanopubCount());
    }

    @Test
    void getLoadCounter() {
        assertEquals(LOAD_COUNTER, registryInfo.getLoadCounter());
    }

    @Test
    void toStringTest() {
        assertEquals(validUrl, registryInfo.toString());
    }

    @Test
    void asJson() {
        Gson gson = new Gson();
        JsonObject initialObject = gson.fromJson(registryInfoJsonString, JsonObject.class);
        initialObject.addProperty("url", validUrl);

        JsonObject expectedJson = gson.fromJson(registryInfo.asJson(), JsonObject.class);

        assertEquals(initialObject, expectedJson);
    }

}