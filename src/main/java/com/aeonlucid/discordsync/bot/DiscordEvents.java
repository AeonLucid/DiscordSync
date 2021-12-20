package com.aeonlucid.discordsync.bot;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

public class DiscordEvents extends ListenerAdapter {

    private boolean isReady;

    public boolean isReady() {
        return isReady;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        isReady = true;
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

        // Send to server console.
        server.sendMessage(textComponent, Util.NIL_UUID);
    }
}
