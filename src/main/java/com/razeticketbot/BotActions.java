package com.razeticketbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.razeticketbot.Main.MAKE_A_TICKET_CATEGORY;
import static com.razeticketbot.Main.MAKE_A_TICKET_CHANNEL;

public class BotActions {
    public static void sendMenuToChannel(List<SelectMenuOption> options, TextChannel channel, Server server) {
        new MessageBuilder()
                .setContent("Select an option of this list!")
                .addComponents(ActionRow.of(SelectMenu.create("Ticket Types",
                        "Click here to choose which ticket you want to open",
                        0,
                        1,
                        options)))
                .send(channel);
        new ServerTextChannelUpdater((ServerTextChannel) channel)
                .addPermissionOverwrite(server.getEveryoneRole()
                        , new PermissionsBuilder()
                                .setDenied(PermissionType.SEND_MESSAGES)
                                .build())
                .update();
    }
    public static void runTicketCommand(String message, MessageCreateEvent event, DiscordApi api) {
        String[] args = message.split(" ");
        if (!args[0].equals("ticket")) {return;}
        Optional<ServerTextChannel> optChannel = event.getServerTextChannel();
        Optional<Server> optServer = event.getServer();
        if(optChannel.isPresent() & optServer.isPresent()) {
            ServerTextChannel channel = optChannel.get();
            Server server = optServer.get();
            String channelId = channel.getIdAsString();
            String serverId = server.getIdAsString();
            if (Mongo.checkChannelIsTicket(channelId, serverId)){
                switch(args[1]) {
                    case "close":
                        try {
                            TicketActions.close(api, channel, event.getMessageAuthor().asUser().get(), server);
                        } catch (NoSuchElementException error) {
                            channel.sendMessage("Author of message unknown error");
                        }
                        break;
                    case "delete":
                        try {
                            TicketActions.delete(api, channel, server, event.getMessageAuthor().asUser().get());
                        } catch (NoSuchElementException error) {
                            channel.sendMessage("Author of message unknown error");
                        };

                        break;
                    case "save":
                        // NOT SURE IF WILL KEEP THIS FEATURE, BUT IF DO:
                        // SAVE TICKET MESSAGE HISTORY TO DATABASE
                        // LOG SAVE OF HISTORY
                        break;
                    case "lock":
                        // TAKE A THIRD ARGUMENT (ROLE)
                        // IF ROLE IS GREATER THAN CURRENT SCOPE OF TICKET, REMOVE TICKET PERMS FOR ALL LOWER STAFF ROLES
                        // LOG THIS ACTION
                        break;
                    case "add":
                        // TAKE A THIRD...Nth ARGUMENT (USER IDS)
                        // IF USER ID EXISTS IN SERVER....
                        // ADD THEM TO TICKET WITH DEFAULT USER PERMISSIONS
                        // ADD THEIR USER ID TO THE ARRAY STORED IN THE DATABASE
                        // LOG ACTION
                        break;
                    case "remove":
                        // ADD TO A SEPARATE ARRAY IN DATABASE MAYBE?
                        // REMOVE FROM ADDED USERS IN DB
                        // REMOVE ALL PERMISSIONS
                        // LOG ACTION
                        break;
                    case "open":
                        // GET LIST OF CURRENTLY ADDED TICKET USERS
                        // GIVE THEM BACK THE SEND MESSAGES PERMISSION
                        break;
                    default:
                        break;
                }
            } else {
                channel.sendMessage("Channel is not a ticket!!!");
            }

        }
        // add message author role check before switch statement!!!
        // add make sure this is a ticket check for channel!!!

    }
    public static void onJoinNewServer(DiscordApi api, List<SelectMenuOption> options) {
        Collection<Server> servers = api.getServers();
        for(Server server : servers) {
            Collection<ChannelCategory> categories = server.getChannelCategories();
            boolean doesCategoryExist = false;
            List<RegularServerChannel> channelsInMakeATicketCategory = null;
            ChannelCategory useCategory = null;
            for(ChannelCategory category : categories) {
                if(Objects.equals(category.getName(), MAKE_A_TICKET_CATEGORY)) {
                    useCategory = category;
                    doesCategoryExist = true;
                    channelsInMakeATicketCategory = category.getChannels();
                    break;
                }
            }
            if (!doesCategoryExist) {
                useCategory = Create.category(server, MAKE_A_TICKET_CATEGORY);
                channelsInMakeATicketCategory = useCategory.getChannels();
            }
            boolean doesChannelExist = false;
            RegularServerChannel useChannel = null;
            for(RegularServerChannel channel : channelsInMakeATicketCategory) {
                if(Objects.equals(channel.getName(), MAKE_A_TICKET_CHANNEL)) {
                    useChannel = channel;
                    doesChannelExist = true;
                    break;
                }
            }
            if(!doesChannelExist) {
                useChannel = Create.channel(server, MAKE_A_TICKET_CHANNEL, useCategory);
                BotActions.sendMenuToChannel(options, (TextChannel) useChannel, server);
            }

        }
    }
}
