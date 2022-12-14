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
    // Until implementation of new method, if roles need adding etc, just add to system here
    static final String TEST_SERVER_ROLE = "1031292038265704600";
    static final String RAZE_SUPPORT = "827098973491560448";
    static final String RAZE_MOD = "776567102080548874";
    static final String RAZE_ADMIN = "776567056397107200";
    static final String RAZE_MGMT = "776570559752175617";
    final static Ticket generalSupport = new Ticket("General Support",
            "support",
            "Ask general questions or receive help for an unlisted issue",
            new String[]{TEST_SERVER_ROLE, RAZE_SUPPORT, RAZE_MOD, RAZE_ADMIN, RAZE_MGMT},
            "Please explain the support you're in need of in detail so that we can assist you. Do not ping anyone as we'll be with you shortly.",
            "\uD83D\uDCAC");
    final static Ticket playerReport =  new Ticket("Player Report",
            "report",
            "Used to report a player for in-game or OOC actions",
            new String[]{TEST_SERVER_ROLE, RAZE_MOD, RAZE_ADMIN, RAZE_MGMT},
            "Please explain your player report in detail so that we can assist you. Try and give us the PayPal or in-game ID of the user you're reporting. Do not ping anyone as we'll be with you shortly.",
            "\uD83D\uDD28");
    final static Ticket banAppeal = new Ticket("Ban Appeal",
            "appeal",
            "Be ready to list your Ban ID (displayed upon connection)",
                new String[]{TEST_SERVER_ROLE, RAZE_MOD, RAZE_ADMIN, RAZE_MGMT},
            "Please fill out the following ban appeal form and wait patiently for a response.",
            "\uD83D\uDEAB");
    final static Ticket compensationRequest = new Ticket("Compensation Request",
            "comp",
            "Request compensation for lost items ($5k+)",
            new String[]{TEST_SERVER_ROLE, RAZE_SUPPORT, RAZE_MOD, RAZE_ADMIN, RAZE_MGMT},
            "Please explain the problem that occurred that caused you to lose your item(s). Do not ping staff as we'll get back to you as soon as we can. *This is not a guaranteed compensation, some form of proof is required.*",
            "\uD83E\uDD11");
    final static Ticket[] tickets ={generalSupport, playerReport, banAppeal, compensationRequest};
    // Initialise the List of menu options
    static List<SelectMenuOption> options = new ArrayList<>();
    static Hashtable<String, Ticket> ticketHashTable = new Hashtable<>(5);
    public static void main(String[] args) {
        ticketHashTable.put(generalSupport.name, generalSupport);
        ticketHashTable.put(playerReport.name, playerReport);
        ticketHashTable.put(banAppeal.name, banAppeal);
        ticketHashTable.put(compensationRequest.name, compensationRequest);
        // Login the bot
        DiscordApi api = new DiscordApiBuilder()
                .setToken(System.getenv("BOTTOKEN"))
                .setAllIntents()
                .login()
                .join();
        Mongo.ConnectToDatabase();
        // Print online + admin join link
        System.out.println("-- BOT ONLINE -- ");
        System.out.println("INVITE LINK : " + api.createBotInvite(Permissions.fromBitmask(8)));
        // Populate the tickets list based on Ticket objects defined above.
        for(Ticket ticket : tickets) {
            options.add(SelectMenuOption.create(ticket.name, ticket.value, ticket.description, ticket.unicodeEmoji));
        }
        // Add event to add the category, channel and message whenever bot is added to a new server.

        //Handle eventualities needed when messages are sent!!!!
        api.addMessageCreateListener(event -> {
            String message = event.getMessage().getContent().toLowerCase();
           BotActions.runCommand(message, event, api);
        });

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