package com.aeonlucid.discordsync.utils;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    private static final Pattern MentionPattern = Pattern.compile("@[a-zA-Z0-9 ]{1,32}");

    public static String replaceMentions(String message, TextChannel channel) {
        // Replace dangerous mentions.
        final String messageMinecraft = message
                .replace("@here", "[here]")
                .replace("@everyone", "[everyone]");

        // Check if a channel is set and there are any @ signs left.
        if (channel == null || !messageMinecraft.contains("@")) {
            return message;
        }

        final List<Member> members = channel.getMembers();
        final Matcher matcher = MentionPattern.matcher(message);

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
                message = message.replace("@" + target, result.getAsMention());
            }
        }

        return message;
    }

}
