package com.razeticketbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.awt.*;
import java.util.*;
import java.util.List;

import static com.razeticketbot.Main.*;

public class BotActions {
    // Sends the dropdowm menu to a channel (command only accessible by admins)
    public static void sendMenuToChannel(List<SelectMenuOption> options, TextChannel channel, Server server) {
        EmbedBuilder embed = new EmbedBuilder()
                .addField(":hammer: Player Reports", "Select the player report button in the drop down menu below to open a player report. You will be invited to a #report-(number) channel where staff will be with you shortly. We will respond as soon as possible.")
                .addField(":speech_balloon: General Support", "Select the general support button in the drop down menu below to open a general support ticket. You will be invited to a #support-(number) channel where staff will be with you shortly. We will respond as soon as possible.")
                .addField(":no_entry_sign: Ban Appeals", "If you're interested in appealing a ban, select the ban appeals button in the drop down menu below. Staff will be with you shortly. Do not ping staff.")
                .addField(":moneybag: Compensation Request", "Select the compensation request button in the drop down menu below to open a comp request ticket. You will be invited to a #comp-(number) channel where staff will be with you shortly. We will respond as soon as possible. Be ready to provide proof of the item(s) that you lost.")
                .setFooter("Raze Network")
                .setColor(Color.MAGENTA);
        new MessageBuilder()
                .addEmbed(embed)
                .addComponents(ActionRow.of(SelectMenu.create("Ticket Types",
                        "Click here to choose which ticket you want to open",
                        0,
                        1,
                        options)))
                .send(channel);
        // Auto-denies everyone permission to type in the channel
        new ServerTextChannelUpdater((ServerTextChannel) channel)
                .addPermissionOverwrite(server.getEveryoneRole()
                        , new PermissionsBuilder()
                                .setDenied(PermissionType.SEND_MESSAGES)
                                .build())
                .update();
    }
    public static void runCommand(String message, MessageCreateEvent event, DiscordApi api) {
        // Split the command message into each argument, make sure command is valid format, and sent in a server
        String[] args = message.split(" ");
        if(args.length < 1) {return;}
        if(args[0].length() == 0) {return;}
        char firstChar = args[0].charAt(0);
        if (! (firstChar == '$')) {return;}
        Optional<ServerTextChannel> optChannel = event.getServerTextChannel();
        Optional<Server> optServer = event.getServer();
        if(optChannel.isPresent() & optServer.isPresent()) {
            ServerTextChannel channel = optChannel.get();
            Server server = optServer.get();
            String channelId = channel.getIdAsString();
            String serverId = server.getIdAsString();
            // custom commands not for tickets
            if(args[0].equals("$build")) {
                if(event.getMessageAuthor().isServerAdmin()) {
                    BotActions.sendMenuToChannel(options, channel, server);
                }
                return;
            } else if(args[0].equals("$appeal")) {
                if(isUserTicketAdmin(event.getMessageAuthor().asUser().get(), server, "General Support")) {
                    sendBanAppealForm(channel);
                }
                return;
            }
            if(! isUserTicketAdmin(event.getMessageAuthor().asUser().get(), server, "General Support")) {
                channel.sendMessage("You do not have the permission to do this");
                return;
            }
            // Ticket command switch statement
            if (Mongo.checkChannelIsTicket(channelId, serverId)){
                switch(args[0]) {
                    case "$close":
                        try {
                            TicketActions.close(api, channel, event.getMessageAuthor().asUser().get(), server);
                        } catch (NoSuchElementException error) {
                            channel.sendMessage("Author of message unknown error");
                        }
                        break;
                    case "$delete":
                        try {
                            TicketActions.delete(api, channel, server, event.getMessageAuthor().asUser().get());
                        } catch (NoSuchElementException error) {
                            channel.sendMessage("Author of message unknown error");
                        };

                        break;
                    case "$add":
                        if(args.length < 2) {return;}
                        for(int i = 0; i < args.length - 1; i++) {
                            String user = args[i + 1].replaceAll("[^0-9]", "");
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
                    case "$remove":
                        if(args.length < 2) {return;}
                        for(int i = 0; i < args.length - 1; i++) {
                            String user = args[i + 1].replaceAll("[^0-9]", "");
                            api.getUserById(user).thenAccept(trueUser -> {
                                try {
                                    TicketActions.removeUserFromTicket(trueUser, channel, server, event.getMessageAuthor().asUser().get());
                                } catch (NoSuchElementException nsee) {
                                    channel.sendMessage("User ID :" + user + "Not Found");
                                }
                            });
                        }
                        break;
                    case "$open":
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

    // Function to check if a user is staff for a certain ticket type
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
    public static void sendBanAppealForm(ServerTextChannel channel) {
        EmbedBuilder banAppealFormEmbed = new EmbedBuilder()
                .setTitle("Ban Appeal Form")
                .setDescription("```Discord Name of your banned account:\n" +
                        "TXAdmin Ban ID (or N/A if you are not TXAdmin Banned):\n" +
                        "Reason for Ban:\n" +
                        "Length of Ban:\n" +
                        "Time since ban:\n" +
                        "How would the server benefit from you being unbanned:\n" +
                        "(For Permanent bans) If you're unbanned, explain the character you're going to play:```"
                        + "\n NOTE: IF YOUR BAN REASON IS RELATING TO, BUT NOT LIMITED TO:\n" +
                        "Exploiting, Hacking, Rape, harassment, etc.\n" +
                        "YOUR APPEAL WILL NOT BE LOOKED AT.\n \n"
                        + "\n NOTE 2: IF YOU HAVE BEEN BANNED BY THE ANTI CHEAT, PLEASE INCLUDE THE BAN ID IN TEXT FORM (NOT JUST THE PHOTO)\n");
        channel.sendMessage(banAppealFormEmbed);
    }

}
