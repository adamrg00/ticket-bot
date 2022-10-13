package com.razeticketbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.Interaction;

import java.util.*;

public class Main {
    // Create data structure of tickets for dropdown
    final static Ticket[] tickets = {new Ticket("General Support", "general_support", "Click here to choose general support ticket"),
                                     new Ticket("Player Report", "player_report", "Click here to choose player report ticket"),
    };
    // Initialise the List of menu options
    static List<SelectMenuOption> options = new ArrayList<>();

    public static ServerTextChannel createChannel(Server server, String ticketType, ChannelCategory category) {
        return new ServerTextChannelBuilder(server)
                .setName(ticketType)
                .setCategory(category)
                .create()
                .join();
    }
    public static ChannelCategory createCategory(Server server, String name) {
        return new ChannelCategoryBuilder(server)
                .setName(name)
                .create()
                .join();
    }
    public static void main(String[] args) {

        // Login the bot
        DiscordApi api = new DiscordApiBuilder()
                .setToken("MTAyOTM3NTAxNDIxMTk2NDkzOA.G500Kp.KIjJE1tHtx80f6IvqGoVe40k3TqJVxMp0wx9sE")
                .login()
                .join();

        // Populate menu option list with tickets from data structure
        for(int i = 0; i < tickets.length; i++) {
            options.add(SelectMenuOption.create(tickets[i].name, tickets[i].value, tickets[i].description));
        }

        // Add command listener to build the menu -- REPLACE WITH MORE ELEGANT DEPLOYMENT OF MENU!!!
        api.addMessageCreateListener(event -> {
            if (event.getMessageContent().equalsIgnoreCase("ticket buildMenu")) {
                TextChannel channel = event.getChannel();
                event.getChannel().sendMessage("building the dropdown");
                new MessageBuilder()
                        .setContent("Select an option of this list!")
                        .addComponents(ActionRow.of(SelectMenu.create("Ticket Types", "Click here to choose which ticket you want to open", 0, 1, options)))
                        .send(channel);
            }
        });

        // Handle what happens on click of menu options ( the creation of tickets )
        api.addSelectMenuChooseListener(event -> {
            List<SelectMenuOption> options = event.getSelectMenuInteraction().getChosenOptions();
            Interaction interaction = event.getInteraction();
            if(options.size() == 0) {
                interaction.createImmediateResponder().respond();
                return;
            }
            String ticketType = options.get(0).getLabel();
            Optional<Server> optionalServer = interaction.getServer();
            if (optionalServer.isPresent()) {
                Server server = optionalServer.get();
                List<ChannelCategory> categories = server.getChannelCategories();
                boolean categoryExists = false;
                ChannelCategory category = null;
                for (ChannelCategory channelCategory : categories) {
                    if (Objects.equals(channelCategory.getName(), ticketType)) {
                        categoryExists = true;
                        category = channelCategory;
                        break;
                    }
                }
                if (!categoryExists) {
                    category = createCategory(server, ticketType);
                }

                ServerTextChannel newTicket = createChannel(server, ticketType, category);

                interaction.respondLater(true).thenAccept(interactionOriginalResponseUpdater -> {
                    interactionOriginalResponseUpdater.setContent("Ticket has been created : <#" + newTicket.getIdAsString() + ">").update();
                });
            } else {
                System.out.println("interaction did not take place in a server");
            }

            ;
        });
    }
}