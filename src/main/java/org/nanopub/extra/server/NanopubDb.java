package org.nanopub.extra.server;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;


// This code is partly copied from ch.tkuhn.nanopub.server.NanopubDb
public class NanopubDb {

	// Use trig internally to keep namespaces:
	private static RDFFormat internalFormat = RDFFormat.TRIG;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private MongoClient mongo;
	private DB db;

	public NanopubDb(String mongoDbHost, int mongoDbPort, String mongoDbName, String mongoDbUsername, String mongoDbPw) {
		logger.info("Initialize new DB object");
		ServerAddress serverAddress = new ServerAddress(mongoDbHost, mongoDbPort);
		List<MongoCredential> credentials = new ArrayList<>();
		if (mongoDbUsername != null) {
			credentials.add(MongoCredential.createMongoCRCredential(
					mongoDbUsername,
					mongoDbName,
					mongoDbPw.toCharArray()));
		}
		mongo = new MongoClient(serverAddress, credentials);
		db = mongo.getDB(mongoDbName);
	}

	public MongoClient getMongoClient() {
		return mongo;
	}

	private static DBObject pingCommand = new BasicDBObject("ping", "1");

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

	public boolean hasNanopub(String artifactCode) {
		BasicDBObject query = new BasicDBObject("_id", artifactCode);
		return getNanopubCollection().find(query).hasNext();
	}

}
