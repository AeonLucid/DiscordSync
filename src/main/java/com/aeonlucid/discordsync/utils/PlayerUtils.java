package com.aeonlucid.discordsync.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;

public class PlayerUtils {

    public static String getName(PlayerEntity player) {
        String name = player.getDisplayName().getContents();

        if (name.isEmpty()) {
            name = player.getName().getContents();
        }

        return name;
    }

    public static String getAvatar(ServerPlayerEntity player) {
        return String.format("https://crafatar.com/avatars/%s?overlay=true&size=100", player.getUUID());
    }

}
