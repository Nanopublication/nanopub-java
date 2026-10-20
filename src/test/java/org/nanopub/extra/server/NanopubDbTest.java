package org.nanopub.extra.server;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.trustyuri.TrustyUriUtils;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteSubfolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NanopubDb reads nanopubs, stored as TriG under their artifact code, from a MongoDB database
 * through the current MongoDB driver.
 */
class NanopubDbTest {

    private static final String HOST = "db.example.org";
    private static final int PORT = 27018;
    private static final String DB_NAME = "nanopub-server";

    private final MongoClient client = mock(MongoClient.class);
    private final MongoDatabase database = mock(MongoDatabase.class);
    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final FindIterable<Document> found = mock(FindIterable.class);

    private MockedStatic<MongoClients> mongoClients;
    private MongoClientSettings settings;

    @BeforeEach
    void setUp() {
        mongoClients = mockStatic(MongoClients.class);
        mongoClients.when(() -> MongoClients.create(any(MongoClientSettings.class))).thenAnswer(invocation -> {
            settings = invocation.getArgument(0);
            return client;
        });
        when(client.getDatabase(DB_NAME)).thenReturn(database);
        when(database.getCollection("nanopubs")).thenReturn(collection);
        when(collection.find(any(Bson.class))).thenReturn(found);
    }

    @AfterEach
    void tearDown() {
        mongoClients.close();
    }

    private static Nanopub storedNanopub() throws Exception {
        return new NanopubImpl(NanopubTestSuite.getLatest().getValid(TestSuiteSubfolder.SIGNED).getFirst().toFile());
    }

    @Test
    void connectsToTheGivenServerWithoutCredentials() {
        new NanopubDb(HOST, PORT, DB_NAME, null, null);
        assertEquals(List.of(new ServerAddress(HOST, PORT)), settings.getClusterSettings().getHosts());
        assertNull(settings.getCredential());
    }

    @Test
    void authenticatesAgainstTheDatabase() {
        new NanopubDb(HOST, PORT, DB_NAME, "reader", "secret");
        MongoCredential credential = settings.getCredential();
        assertEquals("reader", credential.getUserName());
        assertEquals(DB_NAME, credential.getSource());
        assertArrayEquals("secret".toCharArray(), credential.getPassword());
    }

    @Test
    void returnsTheNanopubStoredUnderItsArtifactCode() throws Exception {
        Nanopub nanopub = storedNanopub();
        String artifactCode = TrustyUriUtils.getArtifactCode(nanopub.getUri().stringValue());
        when(found.first()).thenReturn(new Document("_id", artifactCode)
                .append("nanopub", NanopubUtils.writeToString(nanopub, RDFFormat.TRIG)));

        NanopubDb db = new NanopubDb(HOST, PORT, DB_NAME, null, null);

        assertEquals(nanopub.getUri(), db.getNanopub(artifactCode).getUri());
        assertTrue(db.hasNanopub(artifactCode));
        ArgumentCaptor<Bson> filter = ArgumentCaptor.forClass(Bson.class);
        verify(collection, atLeastOnce()).find(filter.capture());
        assertEquals(new BsonDocument("_id", new BsonString(artifactCode)), filter.getValue().toBsonDocument());
    }

    @Test
    void returnsNothingForAnUnknownArtifactCode() {
        when(found.first()).thenReturn(null);
        NanopubDb db = new NanopubDb(HOST, PORT, DB_NAME, null, null);
        assertNull(db.getNanopub("RAunknown"));
        assertFalse(db.hasNanopub("RAunknown"));
    }

    @Test
    void isAccessibleWhenThePingSucceeds() {
        when(database.runCommand(any(Bson.class))).thenReturn(new Document("ok", 1.0));
        assertTrue(new NanopubDb(HOST, PORT, DB_NAME, null, null).isAccessible());
    }

    @Test
    void isNotAccessibleWhenThePingFails() {
        when(database.runCommand(any(Bson.class))).thenThrow(new MongoTimeoutException("no server"));
        assertFalse(new NanopubDb(HOST, PORT, DB_NAME, null, null).isAccessible());
    }

}
