package com.utilsbot.domain.enums;

public enum CallbackDataEnum {

    DAD_BOT("DAD_BOT"),
    TRANSLATION_SELECTOR("TRANSLATION_SELECTOR"),
    EXIT("EXIT"),
    MAIN_MENU("MAIN_MENU");

    private final String value;

    public String getValue() {
        return value;
    }

    CallbackDataEnum(String val) {
        this.value = val;
    }
}
