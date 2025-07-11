package org.nanopub.extra.server;

import com.mongodb.*;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to manage a Nanopublication database.
 */
// This code is partly copied from ch.tkuhn.nanopub.server.NanopubDb
public class NanopubDb {

    // Use trig internally to keep namespaces:
    private static RDFFormat internalFormat = RDFFormat.TRIG;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private MongoClient mongo;
    private DB db;

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
        logger.info("Initialize new DB object");
        ServerAddress serverAddress = new ServerAddress(mongoDbHost, mongoDbPort);

        if (mongoDbUsername != null) {
            MongoCredential credential = MongoCredential.createCredential(
                    mongoDbUsername,
                    mongoDbName,
                    mongoDbPw.toCharArray());
            mongo = new MongoClient(serverAddress, credential, MongoClientOptions.builder().build());
        } else {
            mongo = new MongoClient(serverAddress);
        }
        db = mongo.getDB(mongoDbName);
    }


    /**
     * Returns the MongoDB client object.
     *
     * @return the MongoDB client object
     */
    public MongoClient getMongoClient() {
        return mongo;
    }

    private static DBObject pingCommand = new BasicDBObject("ping", "1");

    /**
     * Checks if the database is accessible.
     * This method sends a ping command to the database and returns true if it succeeds.
     * If the command fails, it returns false.
     *
     * @return true if the database is accessible, false otherwise
     */
    public boolean isAccessible() {
        try {
            db.command(pingCommand);
        } catch (MongoException ex) {
            return false;
        }
        return true;
    }

    private DBCollection getNanopubCollection() {
        return db.getCollection("nanopubs");
    }

    /**
     * Returs a Nanopub object for the given artifact code.
     *
     * @param artifactCode the artifact code of the nanopub to retrieve
     * @return the Nanopub object, or null if no nanopub with the given artifact code exists
     */
    public Nanopub getNanopub(String artifactCode) {
        BasicDBObject query = new BasicDBObject("_id", artifactCode);
        DBCursor cursor = getNanopubCollection().find(query);
        if (!cursor.hasNext()) {
            return null;
        }
        String nanopubString = cursor.next().get("nanopub").toString();
        Nanopub np = null;
        try {
            np = new NanopubImpl(nanopubString, internalFormat);
        } catch (MalformedNanopubException ex) {
            throw new RuntimeException("Stored nanopub is not wellformed (this shouldn't happen)", ex);
        } catch (RDF4JException ex) {
            throw new RuntimeException("Stored nanopub is corrupted (this shouldn't happen)", ex);
        }
        return np;
    }

    /**
     * Checks if a nanopub with the given artifact code exists in the database.
     *
     * @param artifactCode the artifact code of the nanopub to check
     * @return true if a nanopub with the given artifact code exists, false otherwise
     */
    public boolean hasNanopub(String artifactCode) {
        BasicDBObject query = new BasicDBObject("_id", artifactCode);
        return getNanopubCollection().find(query).hasNext();
    }

}
