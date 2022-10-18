package com.razeticketbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelUpdater;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    public static void close(DiscordApi api, ArrayList<String> ticketUsers, ServerTextChannel channel, User commandAuthor) {
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
}
