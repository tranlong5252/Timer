package com.leontg77.timer.util;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {


    public static String colour(final String s) {
        return (s != null) ? ChatColor.translateAlternateColorCodes('&', s) : "";
    }

    public static boolean isPlaceholderInMessage(String message) {
        Pattern pattern = Pattern.compile("\\{0}");
        Matcher matcher = pattern.matcher(message);
        return matcher.find();

    }


    public static String replace(String message, String... replaces) {
        for (int i = 0; i < replaces.length; i++) {
            message = message.replaceAll("\\{" + i + "}", replaces[i]);
        }
        return message;
    }
}
