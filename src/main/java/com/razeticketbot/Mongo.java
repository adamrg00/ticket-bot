package com.razeticketbot;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.javacord.api.entity.channel.ServerTextChannel;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
public class Mongo {
    static MongoClient mongoClient;
    static MongoDatabase ticketsDatabase;
    public static void ConnectToDatabase() {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        ticketsDatabase = mongoClient.getDatabase("tickets");
    }
    public static String getTicketName(String ticketType, String serverId, String ticketValue) {
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
        int amountOfZeros = 4 - amountOfTicketsString.length();
        return ticketValue + "-" + "0".repeat(amountOfZeros) + amountOfTicketsString;
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
        ticket.append("ticket-creator", creatorUserId);
        ticket.append("deleted", false);
        serverTicketsCollection.insertOne(ticket);
    }
    public static int getAmountOfTicketsOpenByUser(String serverId, String userId) {
        MongoCollection<Document> serverTicketsCollection = ticketsDatabase.getCollection(serverId);
        Document query = new Document().append("ticket-creator", userId).append("deleted", false);
        return (int) serverTicketsCollection.countDocuments(query);
    };
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
    public static void addUserToTicket(String channelId, String serverId, String userId) {
        MongoCollection<Document> serverTicketsCollection = ticketsDatabase.getCollection(serverId);
        Document query = new Document().append("channel-id", channelId);
        Bson updates = Updates.addToSet("added-users", userId);
        try {
            UpdateResult result = serverTicketsCollection.updateOne(query, updates);
        } catch (MongoException me) {
            System.err.println(me);
        }
    }
    public static void removeUserFromTicket(String channelId, String serverId, String userId) {
        MongoCollection<Document> serverTicketsCollection = ticketsDatabase.getCollection(serverId);
        Document currentTicket = serverTicketsCollection.find(eq("channel-id", channelId)).first();
        ArrayList<String> currentAddedUsers = (ArrayList<String>) currentTicket.get("added-users");
        currentAddedUsers.removeIf(user -> user.equals(userId));
        Document query = new Document().append("channel-id", channelId);
        Bson updates = Updates.set("added-users", currentAddedUsers);
        try {
            UpdateResult result = serverTicketsCollection.updateOne(query, updates);
        } catch (MongoException me) {
            System.err.println(me);
        }
    }
    public static void saveTranscriptOfTicket(URL transcriptUrl, String channelId, String serverId) {
        MongoCollection<Document> serverTicketsCollection = ticketsDatabase.getCollection(serverId);
        Document query = new Document().append("channel-id", channelId);
        Bson updates = Updates.combine(Updates.set("transcript", transcriptUrl.toString()),  Updates.set("deleted", true));
        try {
            UpdateResult result = serverTicketsCollection.updateOne(query, updates);
        } catch (MongoException me) {
            System.err.println(me);
        }
    }
}
