package com.razeticketbot;
import org.bson.Document;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelUpdater;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
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
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.List;

import static com.razeticketbot.Main.MAXIMUM_OPEN_TICKETS_PER_USER;
import static com.razeticketbot.Main.ticketHashTable;
public class TicketActions {
    public static void create(Server server, String ticketType, Interaction interaction, String ticketValue) {
        User user = interaction.getUser();
        boolean isUserAdmin = BotActions.isUserTicketAdmin(user, server, ticketType);
        if (!isUserAdmin & Mongo.getAmountOfTicketsOpenByUser(server.getIdAsString(), user.getIdAsString()) >= MAXIMUM_OPEN_TICKETS_PER_USER) {
            interaction.respondLater(true).thenAccept(interactionOriginalResponseUpdater -> {
                interactionOriginalResponseUpdater.setContent("You already have the maximum amount of tickets open").update();
            });
            return;
        }
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
        String ticketName = Mongo.getTicketName(ticketType, server.getIdAsString(), ticketValue);
        ServerTextChannel newTicket = Create.channel(server, ticketName, category);
        Mongo.createTicket(ticketType, newTicket, server.getIdAsString(), ticketName, user.getIdAsString());
        ServerTextChannelUpdater setPermissions = new ServerTextChannelUpdater(newTicket);
        setPermissions.addPermissionOverwrite(server.getEveryoneRole(),
                new PermissionsBuilder()
                        .setAllDenied()
                        .build());
        setPermissions.addPermissionOverwrite(user,
                new PermissionsBuilder()
                        .setAllowed(PermissionType.SEND_MESSAGES)
                        .setAllowed(PermissionType.READ_MESSAGE_HISTORY)
                        .setAllowed(PermissionType.VIEW_CHANNEL)
                        .build());
        for (String roleId : ticketHashTable.get(ticketType).rolesThatCanSeeTicketsDefault) {
            Optional<Role> role = server.getRoleById(roleId);
            if (role.isPresent()) {
                Role trueRole = role.get();
                Permissions rolePerms = newTicket.getOverwrittenPermissions(trueRole);
                setPermissions.addPermissionOverwrite(trueRole,
                        new PermissionsBuilder(rolePerms)
                                .setAllAllowed()
                                .build());
            }
        }
        setPermissions.update();


        // respond to the client with a link to the ticket:
        interaction.respondLater(true).thenAccept(interactionOriginalResponseUpdater -> {
            interactionOriginalResponseUpdater.setContent("Ticket has been created : <#" + newTicket.getIdAsString() + ">").update();
        });
        String ticketMessageToEmbed = ticketHashTable.get(ticketType).ticketSpecificMessageOnOpen;
        EmbedBuilder ticketEmbed = new EmbedBuilder()
                .addField(ticketName, ticketMessageToEmbed)
                .setColor(Color.BLUE)
                .setFooter("Do not ping anyone as we will be with you shortly");
        new MessageBuilder()
                .setContent("<@" + user.getIdAsString() + "> Thank you for opening a ticket, please read the message below!")
                .addEmbed(ticketEmbed)
                .send(newTicket);
        if(ticketType.equals("Ban Appeal")) {
            BotActions.sendBanAppealForm(newTicket);

        }
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
                .setDescription("Has Opened the Ticket")
                .setAuthor(commandAuthor)
                .setColor(Color.GREEN);
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
                .setDescription("Has Closed the Ticket")
                .setAuthor(commandAuthor)
                .setColor(Color.CYAN);
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
                .addField("", "ticket will be deleted momentarily...")
                .setAuthor(commandAuthor)
                .setColor(Color.CYAN);
        new MessageBuilder().addEmbed(notification).send(channel);
            // SAVE TICKET MESSAGE HISTORY TO DATABASE
        Iterator<Message> messageIterator = channel.getMessagesAsStream().iterator();
        ArrayList<Document> messageSetArrayList = new ArrayList<>();
        String channelId = channel.getIdAsString();
        String serverId = server.getIdAsString();

        api.addMessageCreateListener(event -> {
            if(event.getMessage().getContent().equals("transcript") & event.getMessageAuthor().isBotUser()) {
                MessageAttachment transcript = event.getMessage().getAttachments().get(0);
                URL transcriptUrl = transcript.getUrl();
                Mongo.saveTranscriptOfTicket(transcriptUrl, channelId, serverId);
                try {
                    event.getChannel().asServerTextChannel().get().delete();
                } catch (NoSuchElementException nsee) {
                    System.out.println(nsee);
                }
            }
        });
        try {
            ProcessBuilder pb = new ProcessBuilder("node", "transcript.js", serverId, channelId);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
    };
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
