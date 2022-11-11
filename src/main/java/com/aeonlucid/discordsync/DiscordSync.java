package com.aeonlucid.discordsync;

import com.aeonlucid.discordsync.bot.DiscordClient;
import com.aeonlucid.discordsync.config.Configuration;
import com.aeonlucid.discordsync.config.ConfigurationLoader;
import com.aeonlucid.discordsync.utils.PlayerUtils;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("discordsync")
public class DiscordSync {

    private final ConfigurationLoader configLoader;

    private Configuration config;
    private DiscordClient discordClient;

    public DiscordSync() {
        configLoader = new ConfigurationLoader();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onServerSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // Load configuration.
        config = configLoader.load();

        // Initialize discord client.
        discordClient = new DiscordClient(config);
        discordClient.initialize();
    }

    private void onServerSetup(FMLDedicatedServerSetupEvent event) {
        if (discordClient != null) {
            discordClient.sendServerMessage("**Server starting**");
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (discordClient != null) {
            discordClient.sendServerMessage("**Server stopping**");
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        if (discordClient != null) {
            discordClient.shutdown();
        }
    }

    @SubscribeEvent
    public void playerJoin(final PlayerEvent.PlayerLoggedInEvent e) {
        if (discordClient != null) {
            discordClient.sendServerMessage(String.format("**%s joined the server**", PlayerUtils.getName(e.getPlayer())));
        }
    }

    @SubscribeEvent
    public void playerLeft(final PlayerEvent.PlayerLoggedOutEvent e) {
        if (discordClient != null) {
            discordClient.sendServerMessage(String.format("**%s left the server**", PlayerUtils.getName(e.getPlayer())));
        }
    }

    // High priority because other mods are potentially replacing the TranslationTextComponent
    // with another type of component.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void playerChat(final ServerChatEvent e) {
        if (discordClient != null) {
            String replacement = discordClient.sendPlayerMessage(MarkdownSanitizer.sanitize(e.getMessage()),
                    PlayerUtils.getName(e.getPlayer()),
                    PlayerUtils.getAvatar(e.getPlayer()));

            if (replacement != null && e.getComponent() instanceof TextComponent) {
                //e.setComponent(new TextComponent(replacement));
                //final TranslationTextComponent component = (TranslationTextComponent)e.getComponent();
                //
                //component.getArgs()[1] = new TextComponent(replacement);
                // TODO: Debug.
            }
        }
    }

    @SubscribeEvent
    public void playerDeath(final LivingDeathEvent e) {
        final Entity entity = e.getEntity();

        if (!(entity instanceof Player)) {
            return;
        }

        final String deathMessage = e.getSource().getLocalizedDeathMessage(e.getEntityLiving()).getString();

        if (discordClient != null) {
            discordClient.sendServerMessage(String.format("**%s**", MarkdownSanitizer.sanitize(deathMessage)));
        }
    }

    @SubscribeEvent
    public void playerAdvancement(final AdvancementEvent e) {
        final MinecraftServer server = e.getPlayer().getServer();

        if (server == null) {
            return;
        }

        final PlayerAdvancements advancements = server.getPlayerList().getPlayerAdvancements((ServerPlayer) e.getPlayer());
        final Advancement advancement = e.getAdvancement();

        if (!advancements.getOrStartProgress(advancement).isDone()) {
            return;
        }

        final DisplayInfo titleDisplay = advancement.getDisplay();

        if (titleDisplay == null) {
            return;
        }

        if (discordClient != null) {
            discordClient.sendServerMessage(String.format("%s has made the advancement **[%s]**",
                    PlayerUtils.getName(e.getPlayer()),
                    MarkdownSanitizer.sanitize(titleDisplay.getTitle().getString())));
        }
    }
}
