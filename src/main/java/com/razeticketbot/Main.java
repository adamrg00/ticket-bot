package com.razeticketbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.Interaction;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
public class Main {
    final static int MAXIMUM_OPEN_TICKETS_PER_USER = 5;
    final static String MAKE_A_TICKET_CATEGORY = "support";
    final static String MAKE_A_TICKET_CHANNEL = "make-a-ticket";
    final static Ticket general_support = new Ticket("General Support",
            "support",
            "Click here to choose general support ticket",
            new String[]{"1031292038265704600"},
            "Please explain the support you're in need of in detail so that we can assist you. Do not ping anyone as we'll be with you shortly.");
    final static Ticket player_report =  new Ticket("Player Report",
            "report",
            "Click here to choose player report ticket",
            new String[]{"1031292038265704600"},
            "Please explain your player report in detail so that we can assist you. Try and give us the PayPal or in-game ID of the user you're reporting. Do not ping anyone as we'll be with you shortly.");
    final static Ticket[] tickets ={general_support, player_report};
    // Initialise the List of menu options
    static List<SelectMenuOption> options = new ArrayList<>();
    static Hashtable<String, Ticket> ticketHashTable = new Hashtable<>(5);
    public static void main(String[] args) {
        ticketHashTable.put(general_support.name, general_support);
        ticketHashTable.put(player_report.name, player_report);
        // Login the bot
        DiscordApi api = new DiscordApiBuilder()
                .setToken("MTAyOTM3NTAxNDIxMTk2NDkzOA.Gr3Py3.3R81VnH_o4rGMo6juda5Q8dM1kxpMD4fLfG-gQ")
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
            //BotActions.onJoinNewServer(api, options);
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
            String ticketValue = options.get(0).getValue();
            Optional<Server> optionalServer = interaction.getServer();
            if (optionalServer.isPresent()) {
                Server server = optionalServer.get();
                TicketActions.create(server, ticketType, interaction, ticketValue);
                // handle very impossible occurence of the button being pressed in dms:
            } else {
                System.out.println("interaction did not take place in a server");
            }
            ;
        });
        api.addButtonClickListener(event -> {
            ButtonInteraction buttonInteraction = event.getButtonInteraction();
            Interaction interaction = event.getInteraction();
            String buttonEvent = buttonInteraction.getCustomId();
            Optional<TextChannel> optChannel = buttonInteraction.getChannel();
            if (optChannel.isPresent()) {
                ServerTextChannel channel = (ServerTextChannel) optChannel.get();

                switch (buttonEvent) {
                    case "open-ticket":
                        TicketActions.open(api, channel,  buttonInteraction.getUser(), channel.getServer());
                        interaction.createImmediateResponder().setContent("Ticket has been opened successfully").respond();
                        break;
                    case "delete-ticket":
                        TicketActions.delete(api, channel, channel.getServer(), buttonInteraction.getUser());
                        interaction.createImmediateResponder().setContent("Ticket will be deleted momentarily...").respond();
                        break;
                    case "close-ticket":
                        TicketActions.close(api, channel,  buttonInteraction.getUser(), channel.getServer());
                        interaction.createImmediateResponder().setContent("Ticket has been closed successfully").respond();
                        break;

                }
            }
        });
    }
}