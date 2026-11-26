package org.nanopub.extra.server;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that {@link GetNanopub} waits for registries that answer {@code 429 Too Many Requests},
 * instead of giving up on them at once.
 */
class GetNanopubRetryTest {

    private static final String CODE = "RAa4_F5rX1q3-E840jnabqGA9GuwTBa1hnJQPNC9fp_P0";
    private static final String A = "https://registry-a.example/";
    private static final String B = "https://registry-b.example/";

    private final List<Long> waits = new ArrayList<>();
    private final Map<String, Integer> requests = new HashMap<>();
    private GetNanopub.Sleeper originalSleeper;

    @BeforeEach
    void setUp() {
        originalSleeper = GetNanopub.sleeper;
        GetNanopub.sleeper = waits::add;
    }

    @AfterEach
    void tearDown() {
        GetNanopub.sleeper = originalSleeper;
    }

    private record Reply(int status, String retryAfter) {
    }

    private static Reply found() {
        return new Reply(200, null);
    }

    private static Reply notFound() {
        return new Reply(404, null);
    }

    private static Reply tooManyRequests() {
        return new Reply(429, null);
    }

    private static Reply tooManyRequests(String retryAfter) {
        return new Reply(429, retryAfter);
    }

    /**
     * A client for registries that answer with the given replies in turn, and with the last one
     * from then on.
     */
    private HttpClient client(Map<String, List<Reply>> replies) throws Exception {
        String trig = Files.readString(Path.of(getClass().getResource("/templates/" + CODE + ".trig").toURI()));
        HttpClient client = mock(HttpClient.class);
        when(client.execute(any(HttpUriRequest.class))).thenAnswer(call -> {
            URI uri = call.<HttpUriRequest>getArgument(0).getURI();
            String registry = uri.getScheme() + "://" + uri.getHost() + "/";
            int n = requests.merge(registry, 1, Integer::sum);
            List<Reply> answers = replies.get(registry);
            Reply reply = answers.get(Math.min(n, answers.size()) - 1);
            BasicHttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, reply.status(), null));
            if (reply.retryAfter() != null) {
                response.setHeader("Retry-After", reply.retryAfter());
            }
            response.setEntity(new StringEntity(reply.status() == 200 ? trig : "", StandardCharsets.UTF_8));
            return response;
        });
        return client;
    }

    private Nanopub get(HttpClient client, String... registryUrls) {
        List<RegistryInfo> registries = new ArrayList<>();
        for (String url : registryUrls) {
            RegistryInfo info = new RegistryInfo();
            info.url = url;
            info.status = "ready";
            registries.add(info);
        }
        return GetNanopub.getWithIterator(CODE, registries.iterator(), client);
    }

    @Test
    void busyRegistryIsAskedAgainAfterAWait() throws Exception {
        Nanopub np = get(client(Map.of(A, List.of(tooManyRequests(), found()))), A);
        assertNotNull(np);
        assertTrue(np.getUri().stringValue().endsWith(CODE));
        assertEquals(List.of(1000L), waits);
        assertEquals(2, requests.get(A));
    }

    @Test
    void otherRegistriesAreAskedBeforeWaiting() throws Exception {
        Nanopub np = get(client(Map.of(A, List.of(tooManyRequests()), B, List.of(found()))), A, B);
        assertNotNull(np);
        assertEquals(List.of(), waits);
        assertEquals(1, requests.get(A));
        assertEquals(1, requests.get(B));
    }

    @Test
    void registryThatLacksTheNanopubIsNotAskedAgain() throws Exception {
        Nanopub np = get(client(Map.of(A, List.of(notFound()), B, List.of(tooManyRequests(), tooManyRequests(), found()))), A, B);
        assertNotNull(np);
        assertEquals(List.of(1000L, 2000L), waits);
        assertEquals(1, requests.get(A));
        assertEquals(3, requests.get(B));
    }

    @Test
    void retryAfterIsWaitedFor() throws Exception {
        Nanopub np = get(client(Map.of(A, List.of(tooManyRequests("3"), found()), B, List.of(tooManyRequests(), found()))), A, B);
        assertNotNull(np);
        // Long enough for both: A asked for 3 seconds, B for nothing in particular.
        assertEquals(List.of(3000L), waits);
    }

    @Test
    void retryAfterBeyondTheLimitIsNotWaitedFor() throws Exception {
        assertNull(get(client(Map.of(A, List.of(tooManyRequests("60"), found()))), A));
        assertEquals(List.of(), waits);
        assertEquals(1, requests.get(A));
    }

    @Test
    void givesUpAfterThreeRounds() throws Exception {
        assertNull(get(client(Map.of(A, List.of(tooManyRequests()))), A));
        assertEquals(List.of(1000L, 2000L, 4000L), waits);
        assertEquals(4, requests.get(A));
    }

    @Test
    void noWaitWhenNoRegistryIsBusy() throws Exception {
        assertNull(get(client(Map.of(A, List.of(notFound()), B, List.of(new Reply(500, null)))), A, B));
        assertEquals(List.of(), waits);
        assertEquals(1, requests.get(A));
        assertEquals(1, requests.get(B));
    }

}
