package com.aeonlucid.discordsync.bot;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.aeonlucid.discordsync.config.Configuration;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.minecraft.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;

public class DiscordClient extends ListenerAdapter {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Configuration config;
    private final DiscordEvents events;
    private final DiscordThread thread;

    private boolean shuttingDown;
    private JDA bot;
    private JDAWebhookClient webhook;

    public DiscordClient(Configuration config) {
        this.config = config;
        this.events = new DiscordEvents(this);
        this.thread = new DiscordThread(this, this.events);
    }

    public void initialize() {
        initializeBot();
        initializeWebhook();
    }

    private void initializeBot() {
        if (StringUtils.isNullOrEmpty(config.botToken) || StringUtils.isNullOrEmpty(config.botChannel)) {
            LOGGER.warn("Disabling bot features, the values botToken and/or botChannel are not set in the configuration");
            return;
        }

        JDABuilder builder = JDABuilder.createDefault(config.botToken);

        builder.disableCache(CacheFlag.ACTIVITY);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setCompression(Compression.ZLIB);
        builder.setActivity(Activity.playing("Test"));
        builder.addEventListeners(events);
        builder.setEnableShutdownHook(false);
        builder.setAutoReconnect(true);

        try {
            bot = builder.build();

            thread.setActive(true);
            thread.start();
        } catch (LoginException e) {
            LOGGER.error("Failed to authenticate with discord using the provider bot token", e);
        }
    }

    private void initializeWebhook() {
        if (StringUtils.isNullOrEmpty(config.channelWebhook)) {
            LOGGER.warn("Disabling webhook features, the value channelWebhook is not set in the configuration");
            return;
        }

        WebhookClientBuilder builder = new WebhookClientBuilder(config.channelWebhook);

        builder.setAllowedMentions(AllowedMentions.none());
        builder.setDaemon(true);
        builder.setWait(false);

        webhook = builder.buildJDA();
    }

    public void shutdown() {
        shuttingDown = true;

        if (bot != null) {
            try {
                bot.shutdownNow();
            } catch (Exception e) {
                LOGGER.error("Exception caught while stopping discord bot", e);
            } finally {
                thread.setActive(false);
            }
        }

        if (webhook != null) {
            try {
                webhook.close();
            } catch (Exception e) {
                LOGGER.error("Exception caught while stopping discord webhook", e);
            }
        }
    }

    /**
     * Send a message as the server to discord.
     * @param message The message to write
     */
    public void sendServerMessage(String message) {
        if (bot == null || shuttingDown) {
            return;
        }

        if (!events.isReady()) {
            thread.enqueueServerMessage(message);
            return;
        }

        TextChannel channel = bot.getTextChannelById(config.botChannel);

        if (channel == null) {
            LOGGER.warn("Failed to find botChannel with id {}", config.botChannel);
            return;
        }

        channel.sendMessage(message).submit();
    }

    /**
     * Send a message as a player to discord.
     * @param message The message to write
     * @param displayName Minecraft display name of the sender
     * @param avatarUrl Avatar URL of the sender
     */
    public void sendPlayerMessage(String message, String displayName, String avatarUrl) {
        if (webhook == null || shuttingDown) {
            return;
        }

        WebhookMessageBuilder builder = new WebhookMessageBuilder();

        builder.setContent(message);
        builder.setUsername(displayName);
        builder.setAvatarUrl(avatarUrl);

        webhook.send(builder.build());
    }
}
