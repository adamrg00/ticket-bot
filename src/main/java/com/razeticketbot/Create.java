package com.razeticketbot;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ChannelCategoryBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.server.Server;
public class Create {
    public static ServerTextChannel channel(Server server, String channelName, ChannelCategory category) {
        return new ServerTextChannelBuilder(server)
                .setName(channelName)
                .setCategory(category)
                .create()
                .join();
    }
    public static ChannelCategory category(Server server, String name) {
        return new ChannelCategoryBuilder(server)
                .setName(name)
                .create()
                .join();
    }
}
