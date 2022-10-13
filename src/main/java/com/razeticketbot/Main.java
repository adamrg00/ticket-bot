package com.razeticketbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.Interaction;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        DiscordApi api = new DiscordApiBuilder()
                .setToken("MTAyOTM3NTAxNDIxMTk2NDkzOA.G500Kp.KIjJE1tHtx80f6IvqGoVe40k3TqJVxMp0wx9sE")
                .login()
                .join();

        api.addMessageCreateListener(event -> {
            if (event.getMessageContent().equalsIgnoreCase("ticket buildMenu")) {
                TextChannel channel = event.getChannel();

                event.getChannel().sendMessage("building the dropdown");
                MessageBuilder makeMenu = new MessageBuilder().setContent("Select an option of this list!")
                        .addComponents(
                                ActionRow.of(SelectMenu.create("Ticket Types", "Click here to choose which ticket you want to open", 1, 1,
                                        Arrays.asList(SelectMenuOption.create("General Support", "general_support", "Click here to choose general support ticket"),
                                                SelectMenuOption.create("Player Report", "player_report", "Click here to choose player report ticket"))))
                        );
                makeMenu.send(channel);
            }
        });

        api.addSelectMenuChooseListener(event -> {
            List<SelectMenuOption> options = event.getSelectMenuInteraction().getChosenOptions();
            String ticketType = options.get(0).getLabel();
            Interaction interaction = event.getInteraction();

            Optional<Server> optionalServer = interaction.getServer();
            if (optionalServer.isPresent()) {
                Server server = optionalServer.get();
                ServerTextChannel newTicket = new ServerTextChannelBuilder(server)
                        .setName(ticketType)
                        .create()
                        .join();
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