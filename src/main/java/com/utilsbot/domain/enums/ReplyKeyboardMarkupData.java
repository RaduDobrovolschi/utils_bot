package com.utilsbot.domain.enums;

public enum ReplyKeyboardMarkupData {
    SET_TIME_ZONE("Set time zone");

    private final String value;

    public String getValue() {
        return value;
    }

    ReplyKeyboardMarkupData(String val) {
        this.value = val;
    }
}
