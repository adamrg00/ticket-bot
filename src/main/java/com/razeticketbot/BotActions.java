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
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.razeticketbot.Main.*;

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
        // add message author role check before switch statement!!!
        // add make sure this is a ticket check for channel!!!
        String[] args = message.split(" ");
        if(args.length < 2) {return;}
        if (!args[0].equals("ticket")) {return;}
        Optional<ServerTextChannel> optChannel = event.getServerTextChannel();
        Optional<Server> optServer = event.getServer();
        if(optChannel.isPresent() & optServer.isPresent()) {
            ServerTextChannel channel = optChannel.get();
            Server server = optServer.get();
            String channelId = channel.getIdAsString();
            String serverId = server.getIdAsString();
            if(! isUserTicketAdmin(event.getMessageAuthor().asUser().get(), server, "General Support")) {
                channel.sendMessage("You do not have the permission to do this");
                return;
            }
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
                        if(args.length < 3) {return;}
                        for(int i = 0; i < args.length - 2; i++) {
                            String user = args[i + 2].replaceAll("[^0-9]", "");
                            api.getUserById(user).thenAccept(trueUser -> {
                                try {
                                    TicketActions.addUserToTicket(trueUser, channel, server, event.getMessageAuthor().asUser().get());
                                } catch (NoSuchElementException nsee) {
                                    channel.sendMessage("User ID :" + user + "Not Found");
                                }
                            });
                        }
                        // LOG ACTION
                        break;
                    case "remove":
                        if(args.length < 3) {return;}
                        for(int i = 0; i < args.length - 2; i++) {
                            String user = args[i + 2].replaceAll("[^0-9]", "");
                            api.getUserById(user).thenAccept(trueUser -> {
                                try {
                                    TicketActions.removeUserFromTicket(trueUser, channel, server, event.getMessageAuthor().asUser().get());
                                } catch (NoSuchElementException nsee) {
                                    channel.sendMessage("User ID :" + user + "Not Found");
                                }
                            });
                        }
                        break;
                    case "open":
                        try {
                            TicketActions.open(api, channel, event.getMessageAuthor().asUser().get(), server);
                        } catch (NoSuchElementException error) {
                            channel.sendMessage("Author of message unknown error");
                        }
                        break;
                    default:
                        break;
                }
            } else {
                channel.sendMessage("This is not a ticket!");
            }

        }


    }
//    public static void onJoinNewServer(DiscordApi api, List<SelectMenuOption> options) {
//        Collection<Server> servers = api.getServers();
//        for(Server server : servers) {
//            Collection<ChannelCategory> categories = server.getChannelCategories();
//            boolean doesCategoryExist = false;
//            List<RegularServerChannel> channelsInMakeATicketCategory = null;
//            ChannelCategory useCategory = null;
//            for(ChannelCategory category : categories) {
//                if(Objects.equals(category.getName(), MAKE_A_TICKET_CATEGORY)) {
//                    useCategory = category;
//                    doesCategoryExist = true;
//                    channelsInMakeATicketCategory = category.getChannels();
//                    break;
//                }
//            }
//            if (!doesCategoryExist) {
//                useCategory = Create.category(server, MAKE_A_TICKET_CATEGORY);
//                channelsInMakeATicketCategory = useCategory.getChannels();
//            }
//            boolean doesChannelExist = false;
//            RegularServerChannel useChannel = null;
//            for(RegularServerChannel channel : channelsInMakeATicketCategory) {
//                if(Objects.equals(channel.getName(), MAKE_A_TICKET_CHANNEL)) {
//                    useChannel = channel;
//                    doesChannelExist = true;
//                    break;
//                }
//            }
//            if(!doesChannelExist) {
//                useChannel = Create.channel(server, MAKE_A_TICKET_CHANNEL, useCategory);
//                BotActions.sendMenuToChannel(options, (TextChannel) useChannel, server);
//            }
//
//        }
//    }
    public static boolean isUserTicketAdmin(User user, Server server, String ticketType) {
        Ticket typeOfTicket = ticketHashTable.get(ticketType);
        String[] adminRolesOfTicket = typeOfTicket.rolesThatCanSeeTicketsDefault;
        List<Role> rolesThatUserHas = user.getRoles(server);
        for(Role userRole : rolesThatUserHas) {
            if(Arrays.stream(adminRolesOfTicket).anyMatch(userRole.getIdAsString()::equals)) {
                return true;
            }
        }
        return false;
    };
}
