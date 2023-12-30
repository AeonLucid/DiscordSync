package com.aeonlucid.discordsync.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PlayerUtils {

    public static String getName(Player player) {
        String name = player.getDisplayName().getString();

        if (name.isEmpty()) {
            name = player.getName().getString();
        }

        return name;
    }

    public static String getAvatar(ServerPlayer player) {
        return String.format("https://mc-heads.net/avatar/%s/100", player.getUUID());
    }

}
