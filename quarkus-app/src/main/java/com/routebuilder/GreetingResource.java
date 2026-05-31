package com.routebuilder;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.inject.Inject;
import com.mongodb.client.MongoClient;
import org.bson.Document;
import java.util.HashMap;
import java.util.Map;

@Path("/hello")
public class GreetingResource {

    @Inject
    MongoClient mongoClient;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }

    @GET
    @Path("/mongo")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> testMongo() {
        Map<String, Object> result = new HashMap<>();
        try {
            var database = mongoClient.getDatabase("testdb");
            var collection = database.getCollection("test_collection");
            
            // Clean old data for the test run
            collection.deleteMany(new Document());
            
            // Insert a test document
            var doc = new Document("testKey", "testValue-" + System.currentTimeMillis());
            collection.insertOne(doc);
            
            // Read back one document
            var retrieved = collection.find().first();
            
            result.put("status", "success");
            result.put("message", "Successfully connected to MongoDB with TLS!");
            result.put("insertedDocument", doc.toJson());
            result.put("retrievedDocument", retrieved != null ? retrieved.toJson() : null);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}

