package com.aeonlucid.discordsync.bot;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.aeonlucid.discordsync.config.Configuration;
import com.aeonlucid.discordsync.utils.MessageUtils;
import com.aeonlucid.discordsync.utils.StringUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiscordClient {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Configuration config;
    private final DiscordEvents events;
    private final DiscordThread thread;

    private boolean shuttingDown;
    private JDA bot;
    private JDAWebhookClient webhook;

    public DiscordClient(Configuration config) {
        this.config = config;
        this.events = new DiscordEvents(this.config);
        this.thread = new DiscordThread(this, this.events, this.config);
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
        builder.setActivity(Activity.playing("Server starting"));
        builder.addEventListeners(events);
        builder.setEnableShutdownHook(false);
        builder.setAutoReconnect(true);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setChunkingFilter(ChunkingFilter.ALL);

        try {
            bot = builder.build();

            thread.setKeepRunning(true);
            thread.start();
        } catch (Exception e) {
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
            thread.setKeepRunning(false);

            updatePresence("Server stopping");

            try {
                bot.shutdownNow();
            } catch (Exception | NoClassDefFoundError e) {
                LOGGER.error("Exception caught while stopping discord bot", e);
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

        channel.sendMessage(MessageUtils.replaceMentions(message, channel, false).getDiscordMessage()).submit();
    }

    /**
     * Send a message as a player to discord.
     * @param message The message to write
     * @param displayName Minecraft display name of the sender
     * @param avatarUrl Avatar URL of the sender
     * @return A replacement message for the chat, null if no replacement.
     */
    public String sendPlayerMessage(String message, String displayName, String avatarUrl) {
        if (webhook == null || shuttingDown) {
            return null;
        }

        TextChannel channel = null;

        if (bot != null && events.isReady()) {
            channel = bot.getTextChannelById(config.botChannel);
        }

        final WebhookMessageBuilder builder = new WebhookMessageBuilder();
        final MessageUtils.Result result = MessageUtils.replaceMentions(message, channel, true);

        builder.setContent(result.getDiscordMessage());
        builder.setUsername(displayName);
        builder.setAvatarUrl(avatarUrl);

        webhook.send(builder.build());

        return result.getMinecraftMessage();
    }

    public void updatePresence(String message) {
        if (bot == null || shuttingDown) {
            return;
        }

        final Presence presence = bot.getPresence();
        final Activity activity = presence.getActivity();

        // Only update if presence changed.
        if (activity == null || message.equals(activity.getName())) {
            return;
        }

        presence.setPresence(Activity.playing(message), false);
    }

}
