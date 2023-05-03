package com.utilsbot.utils;

import net.suuft.libretranslate.Language;

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
}
