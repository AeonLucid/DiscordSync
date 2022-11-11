package com.aeonlucid.discordsync.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PlayerUtils {

    public static String getName(Player player) {
        String name = player.getDisplayName().getContents();

        if (name.isEmpty()) {
            name = player.getName().getContents();
        }

        return name;
    }

    public static String getAvatar(ServerPlayer player) {
        return String.format("https://crafatar.com/avatars/%s?overlay=true&size=100", player.getUUID());
    }

}
