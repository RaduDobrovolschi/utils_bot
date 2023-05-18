package com.utilsbot.utils;

import net.suuft.libretranslate.Language;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppUtils {

    public static final String PRIVATE = "private";
    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";

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

    public static int indexOfByRegex(CharSequence regex, CharSequence text) {
        return indexOfByRegex(Pattern.compile(regex.toString()), text);
    }

    public static int indexOfByRegex(Pattern pattern, CharSequence text) {
        Matcher m = indexOfByRegexToMatcher(pattern, text);
        if ( m != null ) {
            return m.start();
        }
        return -1;
    }

    public static Matcher indexOfByRegexToMatcher(CharSequence regex, CharSequence text) {
        return indexOfByRegexToMatcher(Pattern.compile(regex.toString()), text);
    }

    public static Matcher indexOfByRegexToMatcher(Pattern pattern, CharSequence text) {
        Matcher m = pattern.matcher(text);
        if ( m.find() ) {
            return m;
        }
        return null;
    }
}
