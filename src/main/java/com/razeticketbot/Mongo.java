package com.razeticketbot;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class Mongo {
    static MongoClient mongoClient;
    static MongoDatabase ticketsDatabase;
    static MongoCollection<Document> ticketsCollection;
    public static void Connect() {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        ticketsDatabase = mongoClient.getDatabase("tickets");
        // Adjust this bit to have a different colelction for every server!!!!!!!!!
        ticketsCollection = ticketsDatabase.getCollection("tickets");
    }
    public static void Test() {
        Document test = new Document("_id", new ObjectId());
        test.append("test_key", 9999);
        ticketsCollection.insertOne(test);
    }
}
