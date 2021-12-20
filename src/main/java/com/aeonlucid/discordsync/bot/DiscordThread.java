package com.aeonlucid.discordsync.bot;

import com.aeonlucid.discordsync.config.Configuration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DiscordThread extends Thread {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DiscordClient client;
    private final DiscordEvents events;
    private final Configuration config;

    private final ConcurrentLinkedQueue<String> serverMessages;

    private boolean keepRunning;

    public DiscordThread(DiscordClient client, DiscordEvents events, Configuration config) {
        this.client = client;
        this.events = events;
        this.config = config;
        this.serverMessages = new ConcurrentLinkedQueue<>();

        setName("DiscordSync thread");
        setDaemon(true);
    }

    @Override
    public void run() {
        while (keepRunning) {
            try {
                sendQueuedServerMessages();

                updatePresence();
            } catch (Exception e) {
                LOGGER.error("Exception caught in DiscordThread", e);
            }

            // Sleep.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error(e);
            }
        }
    }

    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }

    /**
     * Enqueue server messages to be sent when the bot is ready.
     */
    public void enqueueServerMessage(String message) {
        serverMessages.add(message);
    }

    /**
     * Send all enqueued server messages while.
     */
    private void sendQueuedServerMessages() {
        if (events.isReady()) {
            while (!serverMessages.isEmpty()) {
                client.sendServerMessage(serverMessages.remove());
            }
        }
    }

    /**
     * Update presence with current online players.
     */
    private void updatePresence() {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return;
        }

        final PlayerList playerList = server.getPlayerList();

        final int current = playerList.getPlayerCount();
        final int max = playerList.getMaxPlayers();

        final String message = config.botStatus
                .replace("%online%", String.valueOf(current))
                .replace("%max%", String.valueOf(max))
                .replace("%plural%", current != 1 ? "s" : "");

        client.updatePresence(message);
    }

}
