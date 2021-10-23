package com.javmarina.client.services.bot;

import com.javmarina.client.Client;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.GuildMessageChannel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;


/**
 * Subclass of {@link BotService} that takes input from a Discord bot.
 */
public class DiscordService extends BotService {

    private final DiscordClient client;
    private MessageChannel channel;

    public DiscordService(@NotNull final String token) {
        client = DiscordClientBuilder.create(token).build();
        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(ready -> System.out.println("Logged in as " + ready.getSelf().getUsername()));
    }

    @Override
    public void onStart() {
        super.onStart();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                // subscribe is like block, in that it will *request* for action
                // to be done, but instead of blocking the thread, waiting for it
                // to finish, it will just execute the results asynchronously.
                .subscribe(event -> {
                    final Message message = event.getMessage();
                    notifyMessageReceived(message.getContent().orElse(null));

                    channel = message.getChannel().block();
                    if (message.getContent().map(s -> "ping".equals(s.toLowerCase(Locale.getDefault()))).orElse(false)) {
                        channel.createMessage("Pong!").block();
                    }
                    if (message.getContent().map(s -> "!clean".equals(s.toLowerCase(Locale.getDefault()))).orElse(false)) {
                        removeChannelMessages(channel);
                    }
                });
        new Thread(() -> client.login().block()).start();
    }

    @Override
    public void onFinish() {
        super.onFinish();
        removeChannelMessages(channel);
        client.logout().block();
    }

    /**
     * Attempt to remove most recent messages in the channel. Up to 200 messages can be removed (it's
     * an API limitation).
     * @param messageChannel the text channel whose messages are going to be removed.
     */
    private void removeChannelMessages(@Nullable final MessageChannel messageChannel) {
        if (messageChannel == null) {
            return;
        }
        final GuildMessageChannel guildMessageChannel
                = client.getChannelById(messageChannel.getId()).ofType(GuildMessageChannel.class).block();
        if (guildMessageChannel != null) {
            guildMessageChannel.getLastMessageId().ifPresent(lastMessageId -> guildMessageChannel.bulkDelete(
                    guildMessageChannel.getMessagesBefore(lastMessageId).map(Message::getId)
            ).subscribe(System.out::println));
        }
    }

    @Override
    public String toString() {
        return Client.RESOURCE_BUNDLE.getString("client.discord");
    }
}
