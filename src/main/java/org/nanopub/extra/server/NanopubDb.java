package org.nanopub.extra.server;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A class to manage a Nanopublication database.
 * <p>
 * It needs the MongoDB Java driver ({@code org.mongodb:mongodb-driver-sync}), which is an
 * optional dependency of nanopub-java: a project that uses this class has to declare the driver
 * itself.
 */
// This code is partly copied from ch.tkuhn.nanopub.server.NanopubDb
public class NanopubDb {

    // Use trig internally to keep namespaces:
    private static RDFFormat internalFormat = RDFFormat.TRIG;

    private static final Document PING_COMMAND = new Document("ping", 1);

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private MongoClient mongo;
    private MongoDatabase db;

    /**
     * Constructor to initialize the NanopubDb with MongoDB connection parameters.
     *
     * @param mongoDbHost     the host of the MongoDB server
     * @param mongoDbPort     the port of the MongoDB server
     * @param mongoDbName     the name of the MongoDB database
     * @param mongoDbUsername the username for MongoDB authentication (can be null)
     * @param mongoDbPw       the password for MongoDB authentication (can be null)
     */
    public NanopubDb(String mongoDbHost, int mongoDbPort, String mongoDbName, String mongoDbUsername, String mongoDbPw) {
        logger.debug("Initializing nanopub DB for mongodb://{}:{}/{}", mongoDbHost, mongoDbPort, mongoDbName);
        MongoClientSettings.Builder settings = MongoClientSettings.builder()
                .applyToClusterSettings(cluster -> cluster.hosts(List.of(new ServerAddress(mongoDbHost, mongoDbPort))));
        if (mongoDbUsername != null) {
            settings.credential(MongoCredential.createCredential(mongoDbUsername, mongoDbName, mongoDbPw.toCharArray()));
        }
        mongo = MongoClients.create(settings.build());
        db = mongo.getDatabase(mongoDbName);
    }

    /**
     * Returns the MongoDB client object.
     *
     * @return the MongoDB client object
     */
    public MongoClient getMongoClient() {
        return mongo;
    }

    /**
     * Checks if the database is accessible.
     * This method sends a ping command to the database and returns true if it succeeds.
     * If the command fails, it returns false.
     *
     * @return true if the database is accessible, false otherwise
     */
    public boolean isAccessible() {
        try {
            db.runCommand(PING_COMMAND);
        } catch (MongoException ex) {
            return false;
        }
        return true;
    }

    private MongoCollection<Document> getNanopubCollection() {
        return db.getCollection("nanopubs");
    }

    private Document findNanopubDocument(String artifactCode) {
        return getNanopubCollection().find(Filters.eq("_id", artifactCode)).first();
    }

    /**
     * Returns a Nanopub object for the given artifact code.
     *
     * @param artifactCode the artifact code of the nanopub to retrieve
     * @return the Nanopub object, or null if no nanopub with the given artifact code exists
     */
    public Nanopub getNanopub(String artifactCode) {
        Document document = findNanopubDocument(artifactCode);
        if (document == null) {
            return null;
        }
        String nanopubString = document.get("nanopub").toString();
        try {
            return new NanopubImpl(nanopubString, internalFormat);
        } catch (MalformedNanopubException ex) {
            throw new RuntimeException("Stored nanopub is not well-formed (this shouldn't happen)", ex);
        } catch (RDF4JException ex) {
            throw new RuntimeException("Stored nanopub is corrupted (this shouldn't happen)", ex);
        }
    }

    /**
     * Checks if a nanopub with the given artifact code exists in the database.
     *
     * @param artifactCode the artifact code of the nanopub to check
     * @return true if a nanopub with the given artifact code exists, false otherwise
     */
    public boolean hasNanopub(String artifactCode) {
        return findNanopubDocument(artifactCode) != null;
    }

}
