package com.utilsbot.utils;

import net.suuft.libretranslate.Language;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppUtils {

    private AppUtils() {}

    public static Language fromCode(String code) {
        for (Language language : Language.values()) {
            if (language.getCode().equalsIgnoreCase(code)) {
                return language;
            }
        }
        throw new IllegalArgumentException("Invalid language code: " + code);
    }

    public static int getDataFromCallback(String prefix, String callbackData) {
        Pattern pattern = Pattern.compile(prefix + "(\\d+)");
        Matcher matcher = pattern.matcher(callbackData);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    public static long getIdFromCallback(String prefix, String callbackData) {
        Pattern pattern = Pattern.compile(prefix + "(\\d+)");
        Matcher matcher = pattern.matcher(callbackData);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return 0;
    }
}
