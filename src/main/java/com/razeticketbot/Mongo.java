package com.razeticketbot;

import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.javacord.api.entity.channel.ServerTextChannel;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

public class Mongo {
    static MongoClient mongoClient;
    static MongoDatabase ticketsDatabase;
    public static void ConnectToDatabase() {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        ticketsDatabase = mongoClient.getDatabase("tickets");
    }
    public static String getTicketName(String ticketType, String serverId) {
        MongoCollection<Document> serverTicketsCollection = ticketsDatabase.getCollection(serverId);
        MongoCursor<Document> iterator = serverTicketsCollection.find(eq("ticket-type", ticketType)).iterator();
        int amountOfTickets = 1;
        try {
            while (iterator.hasNext()) {
                amountOfTickets++;
                iterator.next();
            }
        } finally {
            iterator.close();
        }
        String amountOfTicketsString = Integer.toString(amountOfTickets);
        int amountOfZeros = 5 - amountOfTicketsString.length();
        return ticketType.replaceAll(" ", "-").toLowerCase() + "-" + "0".repeat(amountOfZeros) + amountOfTicketsString;
    }
    public static void createTicket(String ticketType, ServerTextChannel channel, String serverId, String ticketName, String creatorUserId) {
        MongoCollection<Document> serverTicketsCollection = ticketsDatabase.getCollection(serverId);;
        Document ticket = new Document("_id", new ObjectId());
        List addedUsersList = new ArrayList<>();
        addedUsersList.add(creatorUserId);
        ticket.append("ticket-type", ticketType);
        ticket.append("ticket-name", ticketName);
        ticket.append("channel-id", channel.getIdAsString());
        ticket.append("added-users", addedUsersList);
        serverTicketsCollection.insertOne(ticket);
    }
    public static boolean checkChannelIsTicket(String channelId, String serverId) {
        MongoCollection<Document> serverTicketsCollection = ticketsDatabase.getCollection(serverId);
        Document ticketDoc = serverTicketsCollection.find(eq("channel-id",channelId)).first();
        return (ticketDoc != null);
    }
    public static ArrayList<String> getUsersOfAticket(String channelId, String serverId) {
        MongoCollection<Document> serverTicketsCollection = ticketsDatabase.getCollection(serverId);
        Document currentTicket = serverTicketsCollection.find(eq("channel-id", channelId)).first();
        if(currentTicket != null) {
            try {
                return (ArrayList<String>) currentTicket.get("added-users");
            }catch (ClassCastException exception) {
                return new ArrayList<String>();
            }
        } else {
            return new ArrayList<String>();
        }
    }
    public static void saveTranscriptOfTicket(ArrayList<Map<String, String>> arrayListOfMessages, String channelId, String serverId) {
        MongoCollection<Document> serverTicketsCollection = ticketsDatabase.getCollection(serverId);
        Document query = new Document().append("channel-id", channelId);
        Bson updates = Updates.addToSet("transcript", arrayListOfMessages);
        UpdateOptions options = new UpdateOptions().upsert(true);
        try {
            UpdateResult result = serverTicketsCollection.updateOne(query, updates, options);
        } catch (MongoException me) {
            System.err.println(me);
        }
    }
}
