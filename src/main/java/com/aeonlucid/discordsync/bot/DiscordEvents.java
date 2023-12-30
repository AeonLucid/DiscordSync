package com.aeonlucid.discordsync.bot;

import com.aeonlucid.discordsync.config.Configuration;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionDisconnectEvent;
import net.dv8tion.jda.api.events.session.SessionRecreateEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class DiscordEvents extends ListenerAdapter {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Configuration config;

    private boolean isReady;

    public DiscordEvents(Configuration config) {

        this.config = config;
    }

    public boolean isReady() {
        return isReady;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        isReady = true;

        // Load members of target guild.
        final JDA jda = event.getJDA();
        final TextChannel channel = jda.getTextChannelById(config.botChannel);

        if (channel != null) {
            channel.getGuild().loadMembers().onSuccess(members -> {
                LOGGER.debug("Loaded {} guild members into cache", members.size());
            }).onError(throwable -> {
                LOGGER.error("Failed to load guild members", throwable);
            });
        } else {
            LOGGER.error("Failed to load guild members, text channel {} was not found", config.botChannel);
        }
    }

    @Override
    public void onSessionResume(@NotNull SessionResumeEvent event) {
        isReady = true;
    }

    @Override
    public void onSessionRecreate(@NotNull SessionRecreateEvent event) {
        isReady = true;
    }

    @Override
    public void onSessionDisconnect(@NotNull SessionDisconnectEvent event) {
        isReady = false;
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        isReady = false;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
            return;
        }

        // Get discord member.
        final Member member = event.getMember();

        if (member == null) {
            return;
        }

        // Get minecraft server.
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return;
        }

        final PlayerList playerList = server.getPlayerList();

        // Create message.
        final String sender = member.getEffectiveName();
        final String message = event.getMessage().getContentStripped();

        final Component textComponent = Component.literal("")
                        .append(Component.literal("[Discord @").withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(sender).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal("] ").withStyle(ChatFormatting.AQUA))
                        .append(message);

        // Send to all players.
        playerList.broadcastSystemMessage(textComponent, false);
    }
}
