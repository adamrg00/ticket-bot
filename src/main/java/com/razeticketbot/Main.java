package com.razeticketbot;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.Interaction;
import java.awt.*;
import java.util.*;
import java.util.List;
public class Main {
    final static String MAKE_A_TICKET_CATEGORY = "support";
    final static String MAKE_A_TICKET_CHANNEL = "make-a-ticket";
    final static Ticket general_support = new Ticket("General Support",
            "general_support",
            "Click here to choose general support ticket",
            new String[]{"1031292038265704600"});
    final static Ticket player_report =  new Ticket("Player Report",
            "player_report",
            "Click here to choose player report ticket",
            new String[]{"1031292038265704600"});
    static Hashtable<String, Ticket> ticketHashTable = new Hashtable<>(5);
    // Create data structure of tickets for dropdown
    final static Ticket[] tickets ={general_support, player_report};
    // Initialise the List of menu options
    static List<SelectMenuOption> options = new ArrayList<>();
    // function to run whenever the bot joins a new server:
    // function for ticket commands
    public static void main(String[] args) {
        ticketHashTable.put(general_support.name, general_support);
        ticketHashTable.put(player_report.name, player_report);
        // Login the bot
        DiscordApi api = new DiscordApiBuilder()
                .setToken("MTAyOTM3NTAxNDIxMTk2NDkzOA.G500Kp.KIjJE1tHtx80f6IvqGoVe40k3TqJVxMp0wx9sE")
                .login()
                .join();
        Mongo.ConnectToDatabase();
        // Print online + admin join link
        System.out.println("-- BOT ONLINE -- ");
        System.out.println("INVITE LINK : " + api.createBotInvite(Permissions.fromBitmask(8)));
        // Populate the tickets list based on Ticket objects defined above.
        for(Ticket ticket : tickets) {
            options.add(SelectMenuOption.create(ticket.name, ticket.value, ticket.description));
        }
        // Add event to add the category, channel and message whenever bot is added to a new server.
        api.addServerJoinListener(event -> {
            BotActions.onJoinNewServer(api, options);
        });
        //Handle eventualities needed when messages are sent!!!!
        api.addMessageCreateListener(event -> {
            String message = event.getMessage().getContent().toLowerCase();
           BotActions.runTicketCommand(message, event, api);
        });
        // Handle what happens on click of menu options ( the creation of tickets )
        api.addSelectMenuChooseListener(event -> {
            List<SelectMenuOption> options = event.getSelectMenuInteraction().getChosenOptions();
            Interaction interaction = event.getInteraction();
            // make sure that an option is selected before proceeding with creating a ticket
            if(options.size() == 0) {
                interaction.createImmediateResponder().respond();
                return;
            }
            // get the type of ticket and check for the server
            String ticketType = options.get(0).getLabel();
            Optional<Server> optionalServer = interaction.getServer();
            if (optionalServer.isPresent()) {
                Server server = optionalServer.get();
                TicketActions.create(server, ticketType, interaction);
                // handle very impossible occurence of the button being pressed in dms:
            } else {
                System.out.println("interaction did not take place in a server");
            }
            ;
        });
        api.addButtonClickListener(event -> {
            ButtonInteraction buttonInteraction = event.getButtonInteraction();
            String buttonEvent = buttonInteraction.getCustomId();
            Optional<TextChannel> optChannel = buttonInteraction.getChannel();
            if (optChannel.isPresent()) {
                ServerTextChannel channel = (ServerTextChannel) optChannel.get();

                switch (buttonEvent) {
                    case "open-ticket":
                        break;
                    case "delete-ticket":
                        TicketActions.delete(api, channel, channel.getServer(), buttonInteraction.getUser());
                }
            }
        });
    }
}