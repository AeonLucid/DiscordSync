package com.aeonlucid.discordsync.utils;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    private static final Pattern MentionPattern = Pattern.compile("@[a-zA-Z0-9 ]{1,32}");

    private static final String ColorYellow = "\247e";
    private static final String ColorLightPurple = "\247d";
    private static final String ColorDarkPurple = "\2475";
    private static final String ColorReset = "\247r";

    /**
     * Replace discord mentions in a minecraft chat message with proper discord mentions.
     * @param message The message in which to replace.
     * @param channel The channel
     * @param userMentions Whether user mentions should be replaced
     */
    public static Result replaceMentions(String message, TextChannel channel, boolean userMentions) {
        // Replace dangerous mentions.
        String messageDiscord = message
                .replace("@here", "[here]")
                .replace("@everyone", "[everyone]");

        // Check if we should do user mentions at all.
        if (!userMentions) {
            return new Result(messageDiscord, null);
        }

        // Check if a channel is set and there are any @ signs left.
        if (channel == null || !messageDiscord.contains("@")) {
            return new Result(messageDiscord, null);
        }

        final List<Member> members = channel.getMembers();
        final Matcher matcher = MentionPattern.matcher(message);

        String messageMinecraft = messageDiscord;

        while (matcher.find()) {
            Member result = null;
            String target = null;

            // Split by spaces.
            final String[] targetParts = matcher.group().substring(1).split(" ");

            for (int i = targetParts.length; i > 0; i--) {
                // Create possible username match.
                final StringBuilder targetBuild = new StringBuilder();

                for (int j = 0; j < i; j++) {
                    targetBuild.append(targetParts[j]);

                    if (j + 1 != i) {
                        targetBuild.append(" ");
                    }
                }

                // Search for target in member list.
                target = targetBuild.toString();

                for (Member member : members) {
                    if (target.equalsIgnoreCase(member.getNickname())) {
                        result = member;
                        break;
                    }

                    if (target.equalsIgnoreCase(member.getEffectiveName())) {
                        result = member;
                        break;
                    }

                    if (target.equalsIgnoreCase(member.getUser().getName())) {
                        result = member;
                        break;
                    }
                }

                // Exit early if found.
                if (result != null) {
                    break;
                }
            }

            // Check if result is found.
            if (result != null) {
                final String replacement = "@" + target;

                messageDiscord = messageDiscord.replace(replacement, result.getAsMention());
                messageMinecraft = messageMinecraft.replace(replacement, ColorYellow + replacement + ColorReset);
            }
        }

        return new Result(messageDiscord, messageMinecraft);
    }

    public static class Result {

        private final String discordMessage;
        private final String minecraftMessage;

        public Result(String discordMessage, String minecraftMessage) {
            this.discordMessage = discordMessage;
            this.minecraftMessage = minecraftMessage;
        }

        /**
         * The modified message meant for Discord.
         */
        public String getDiscordMessage() {
            return discordMessage;
        }

        /**
         * The modified message meant for Minecraft.
         * @return null if no replacement.
         */
        public String getMinecraftMessage() {
            return minecraftMessage;
        }

    }

}
