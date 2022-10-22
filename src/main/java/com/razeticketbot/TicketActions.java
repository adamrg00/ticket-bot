package com.razeticketbot;

import org.bson.Document;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelUpdater;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.Interaction;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.razeticketbot.Main.ticketHashTable;

public class TicketActions {
    public static void create(Server server, String ticketType, Interaction interaction) {
        List<ChannelCategory> categories = server.getChannelCategories();
        boolean categoryExists = false;
        ChannelCategory category = null;

        // check if the ticket category exists
        for (ChannelCategory channelCategory : categories) {
            if (Objects.equals(channelCategory.getName(), ticketType)) {
                categoryExists = true;
                category = channelCategory;
                break;
            }
        }
        // if ticket category does not exist, create it
        if (!categoryExists) {
            category = Create.category(server, ticketType);
        }
        // create the ticket in either the new category or existing one:
        String ticketName = Mongo.getTicketName(ticketType, server.getIdAsString());
        ServerTextChannel newTicket = Create.channel(server, ticketName, category);
        Mongo.createTicket(ticketType, newTicket, server.getIdAsString(), ticketName, interaction.getUser().getIdAsString());
        ServerTextChannelUpdater setPermissions = new ServerTextChannelUpdater(newTicket);
        setPermissions.addPermissionOverwrite(server.getEveryoneRole(),
                new PermissionsBuilder()
                        .setAllDenied()
                        .build());
        setPermissions.addPermissionOverwrite(interaction.getUser(),
                new PermissionsBuilder()
                        .setAllowed(PermissionType.SEND_MESSAGES)
                        .setAllowed(PermissionType.READ_MESSAGE_HISTORY)
                        .setAllowed(PermissionType.VIEW_CHANNEL)
                        .build());
        for(String roleId : ticketHashTable.get(ticketType).rolesThatCanSeeTicketsDefault) {
            Optional<Role> role = server.getRoleById(roleId);
            if (role.isPresent()) {
                Role trueRole = role.get();
                Permissions rolePerms = newTicket.getOverwrittenPermissions(trueRole);
                setPermissions.addPermissionOverwrite(trueRole,
                        new PermissionsBuilder(rolePerms)
                                .setAllAllowed()
                                .build());
                // PERMISSIONS SET DOES NOT WORK!! SOME PERMISSIONS GET RESET IMPROPERLY, WAIT ON JAVADOC DISCORD FOR SUPPORT HERE
            } else {
                System.out.println("Role ID " + role + " does not exist!!!");
            }
        }
        setPermissions.update();


        // respond to the client with a link to the ticket:
        interaction.respondLater(true).thenAccept(interactionOriginalResponseUpdater -> {
            interactionOriginalResponseUpdater.setContent("Ticket has been created : <#" + newTicket.getIdAsString() + ">").update();
        });
    }
    public static void open(DiscordApi api, ServerTextChannel channel, User commandAuthor, Server server) {
        ArrayList<String> ticketUsers = Mongo.getUsersOfAticket(channel.getIdAsString(), server.getIdAsString());
        if (ticketUsers.size() == 0) {return;}
        ServerTextChannelUpdater updatePerms = new ServerTextChannelUpdater(channel);
        for (String ticketUser : ticketUsers) {
            User user = api.getUserById(ticketUser).join();
            updatePerms.addPermissionOverwrite(user,
                    new PermissionsBuilder(channel.getOverwrittenPermissions(user))
                            .setAllowed(PermissionType.SEND_MESSAGES)
                            .build());
        }
        updatePerms.update();
        EmbedBuilder notification = new EmbedBuilder()
                //.setTitle("Ticket Action")
                .setDescription("Has Opened the Ticket")
                .setAuthor(commandAuthor)
                .setColor(Color.GREEN);
                //.setThumbnail("https://st3.depositphotos.com/8089676/33517/v/1600/depositphotos_335171522-stock-illustration-closed-icon-illustration-vector-sign.jpg");
        new MessageBuilder()
                .addEmbed(notification)
                .addComponents(
                        ActionRow.of(
                                org.javacord.api.entity.message.component.Button.success("close-ticket", "Close Ticket"),
                                Button.danger("delete-ticket", "Delete Ticket")
                        )
                )
                .send(channel);
    }
    public static void close(DiscordApi api, ServerTextChannel channel, User commandAuthor, Server server) {
        ArrayList<String> ticketUsers = Mongo.getUsersOfAticket(channel.getIdAsString(), server.getIdAsString());
        if(ticketUsers.size() == 0) {return;}
        // GET LIST OF ALL ADDED USER IDs, remove all permissions except view channel and view message history;
        ServerTextChannelUpdater updatePerms = new ServerTextChannelUpdater(channel);
        for(String ticketUser : ticketUsers) {
            User user = api.getUserById(ticketUser).join();
            updatePerms.addPermissionOverwrite(user,
                    new PermissionsBuilder(channel.getOverwrittenPermissions(user))
                            .setDenied(PermissionType.SEND_MESSAGES)
                            .build());
        }
        updatePerms.update();
        // send nicely formatted closed message with buttons
        EmbedBuilder notification = new EmbedBuilder()
                //.setTitle("Ticket Action")
                .setDescription("Has Closed the Ticket")
                .setAuthor(commandAuthor)
                .setColor(Color.CYAN)
                .setThumbnail("https://st3.depositphotos.com/8089676/33517/v/1600/depositphotos_335171522-stock-illustration-closed-icon-illustration-vector-sign.jpg");
        new MessageBuilder()
                .addEmbed(notification)
                .addComponents(
                        ActionRow.of(
                                org.javacord.api.entity.message.component.Button.success("open-ticket", "Unlock Ticket"),
                                Button.danger("delete-ticket", "Delete Ticket")
                        )
                )
                .send(channel);
        // LOG THIS ACTION
    }
    public static void delete(DiscordApi api, ServerTextChannel channel, Server server, User commandAuthor) {
        EmbedBuilder notification = new EmbedBuilder()
                .setDescription("Has Marked the ticket for deletion")
                .addField("", "ticket will be deleted momentarily")
                .setAuthor(commandAuthor)
                .setColor(Color.CYAN);
        new MessageBuilder().addEmbed(notification).send(channel);
        CompletableFuture<MessageSet> tempMessageSet = channel.getMessages(9999999);
        tempMessageSet.thenAccept(messages -> {
            // SAVE TICKET MESSAGE HISTORY TO DATABASE
            Iterator<Message> messageIterator = messages.stream().iterator();
            ArrayList<Document> messageSetArrayList = new ArrayList<>();
            while (messageIterator.hasNext()) {
                Message currentMessage = messageIterator.next();
                String messageContent = currentMessage.getContent();
                String messageAuthor = currentMessage.getAuthor().getIdAsString();
                Instant timestamp = currentMessage.getCreationTimestamp();
                if (!messageContent.equals("")) {
                    Document doc = new Document().append("author", messageAuthor)
                                    .append("content", messageContent)
                                            .append("timestamp", timestamp);
                    messageSetArrayList.add(doc);
                }
            }
            String channelId = channel.getIdAsString();
            String serverId = server.getIdAsString();
            Mongo.saveTranscriptOfTicket(messageSetArrayList, channelId, serverId);
            // LOG DELETION OF TICKET IN A CHANNEL
            // ACTUALLY DELETE CHANNEL
            channel.delete();
        });
    }
    public static void removeUserFromTicket(User user, ServerTextChannel channel, Server server, User commandAuthor) {
        Collection<PermissionType> userPermissions = channel.getOverwrittenPermissions(user).getAllowedPermission();
        if (userPermissions.contains(PermissionType.VIEW_CHANNEL)) {
            new ServerTextChannelUpdater(channel)
                    .addPermissionOverwrite(
                            user,
                            new PermissionsBuilder()
                                    .setAllDenied()
                                    .build()
                    )
                    .update();
            Mongo.removeUserFromTicket(channel.getIdAsString(), server.getIdAsString(), user.getIdAsString());
            EmbedBuilder notification = new EmbedBuilder()
                    .setDescription("Has removed " + user.getName() + " from the ticket")
                    .setAuthor(commandAuthor)
                    .setColor(Color.ORANGE);
            new MessageBuilder().addEmbed(notification).send(channel);
        } else {
            channel.sendMessage(user.getName() + " is not in the ticket!");
        }
    }
    public static void addUserToTicket(User user, ServerTextChannel channel, Server server, User commandAuthor) {
        Collection<PermissionType> userPermissions = channel.getOverwrittenPermissions(user).getAllowedPermission();
        if ( ! userPermissions.contains(PermissionType.VIEW_CHANNEL)) {
            new ServerTextChannelUpdater(channel)
                    .addPermissionOverwrite(
                            user,
                            new PermissionsBuilder()
                                    .setAllowed(PermissionType.SEND_MESSAGES)
                                    .setAllowed(PermissionType.VIEW_CHANNEL)
                                    .setAllowed(PermissionType.READ_MESSAGE_HISTORY)
                                    .build()
                    )
                    .update();
            Mongo.addUserToTicket(channel.getIdAsString(), server.getIdAsString(), user.getIdAsString());
            EmbedBuilder notification = new EmbedBuilder()
                    .setDescription("Has added " + user.getName() + " to the ticket")
                    .setAuthor(commandAuthor)
                    .setColor(Color.magenta);
            new MessageBuilder().addEmbed(notification).send(channel);
        } else {
            channel.sendMessage(user.getName() + " is already in the ticket!");
        }
    }
}
