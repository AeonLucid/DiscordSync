package com.aeonlucid.discordsync.utils;

public class MessageUtils {

    public static String sanitizeMentions(String message) {
        return message
                .replace("@here", "[here]")
                .replace("@everyone", "[everyone]");
    }

}
