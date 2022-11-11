package com.aeonlucid.discordsync.utils;

public class StringUtils {

    public static boolean isNullOrEmpty(String str) {
        if (str == null) {
            return true;
        }

        return str.isEmpty();
    }

}
