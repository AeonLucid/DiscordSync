package com.aeonlucid.discordsync.bot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DiscordThread extends Thread {

    private static final Logger LOGGER = LogManager.getLogger();

    private final DiscordClient client;
    private final DiscordEvents events;

    private final ConcurrentLinkedQueue<String> serverMessages;

    private boolean isActive;

    public DiscordThread(DiscordClient client, DiscordEvents events) {
        this.client = client;
        this.events = events;
        this.serverMessages = new ConcurrentLinkedQueue<>();

        setName("DiscordSync thread");
        setDaemon(true);
    }

    @Override
    public void run() {
        while (isActive) {
            // Send queued server messages when bot is ready.
            sendQueuedServerMessages();

            // Sleep.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error(e);
            }
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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

}
