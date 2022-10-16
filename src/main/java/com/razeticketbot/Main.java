package com.razeticketbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.Interaction;

import java.util.*;

public class Main {
    final static String MAKE_A_TICKET_CATEGORY = "support";
    final static String MAKE_A_TICKET_CHANNEL = "make-a-ticket";
    // Create data structure of tickets for dropdown
    final static Ticket[] tickets =
            {new Ticket("General Support", "general_support", "Click here to choose general support ticket"),
                    new Ticket("Player Report", "player_report", "Click here to choose player report ticket"),
    };
    // Initialise the List of menu options
    static List<SelectMenuOption> options = new ArrayList<>();

    // function to create a channel
    public static ServerTextChannel createChannel(Server server, String channelName, ChannelCategory category) {
        return new ServerTextChannelBuilder(server)
                .setName(channelName)
                .setCategory(category)
                .create()
                .join();
    }

    // function to create a category
    public static ChannelCategory createCategory(Server server, String name) {
        return new ChannelCategoryBuilder(server)
                .setName(name)
                .create()
                .join();
    }
    // function to send the ticket select menu to a channel;
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


    // function to run whenever the bot joins a new server:
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
                useCategory = createCategory(server, MAKE_A_TICKET_CATEGORY);
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
                useChannel = createChannel(server, MAKE_A_TICKET_CHANNEL, useCategory);
                sendMenuToChannel(options, (TextChannel) useChannel, server);
            }

        }
    }
    public static void main(String[] args) {
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
            onJoinNewServer(api, options);
        });
        api.addMessageCreateListener(event -> {
            String message = event.getMessage().getContent().toLowerCase();
            // add message author role check before switch statement!!!
            // add make sure this is a ticket check for channel!!!
            switch(message) {
                case "ticket close":
                    break;
                case "ticket delete":
                    break;
                case "ticket save":
                     break;
                case "ticket lock":
                    break;
                case "ticket add":
                    break;
                case "ticket remove":
                    break;
            }
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
                    category = createCategory(server, ticketType);
                }
                // create the ticket in either the new category or existing one:
                String ticketName = Mongo.getTicketName(ticketType, server.getIdAsString());
                ServerTextChannel newTicket = createChannel(server, ticketName, category);
                Mongo.createTicket(ticketType, newTicket, server.getIdAsString(), ticketName);

                // respond to the client with a link to the ticket:
                interaction.respondLater(true).thenAccept(interactionOriginalResponseUpdater -> {
                    interactionOriginalResponseUpdater.setContent("Ticket has been created : <#" + newTicket.getIdAsString() + ">").update();
                });

                // handle very impossible occurence of the button being pressed in dms:
            } else {
                System.out.println("interaction did not take place in a server");
            }

            ;
        });
    }
}