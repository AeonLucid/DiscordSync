package com.aeonlucid.discordsync.bot;

import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DiscordEvents extends ListenerAdapter {

    private final DiscordClient client;

    private boolean isReady;

    public DiscordEvents(DiscordClient client) {
        this.client = client;
    }

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

}
