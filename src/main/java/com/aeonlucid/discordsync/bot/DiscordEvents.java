package com.aeonlucid.discordsync.bot;

import com.aeonlucid.discordsync.config.Configuration;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
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
    public void onResumed(@NotNull ResumedEvent event) {
        isReady = true;
    }

    @Override
    public void onReconnected(@NotNull ReconnectedEvent event) {
        isReady = true;
    }

    @Override
    public void onDisconnect(@NotNull DisconnectEvent event) {
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

        final IFormattableTextComponent textComponent = new StringTextComponent("")
                        .append(new StringTextComponent("[Discord @").withStyle(TextFormatting.AQUA))
                        .append(new StringTextComponent(sender).withStyle(TextFormatting.GREEN))
                        .append(new StringTextComponent("] ").withStyle(TextFormatting.AQUA))
                        .append(message);

        // Send to all players.
        playerList.broadcastMessage(textComponent, ChatType.SYSTEM, Util.NIL_UUID);
    }
}
