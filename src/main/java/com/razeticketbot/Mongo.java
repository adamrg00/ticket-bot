package com.razeticketbot;

import com.mongodb.client.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.javacord.api.entity.channel.ServerTextChannel;

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
        ticket.append("transcript", null);
        serverTicketsCollection.insertOne(ticket);
    }
    public static boolean checkChannelIsTicket(String channelId, String serverId) {
        MongoCollection<Document> serverTicketsCollection = ticketsDatabase.getCollection(serverId);
        Document ticketDoc = serverTicketsCollection.find(eq("channel-id",channelId)).first();
        return (ticketDoc != null);
    }
}
