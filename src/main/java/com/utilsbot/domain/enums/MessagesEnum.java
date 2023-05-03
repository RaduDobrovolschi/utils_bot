package com.utilsbot.domain.enums;

public enum MessagesEnum {

    START_MESSAGE("""
                      This is a Telegram utils bot
                      
                      Here is what it can do:
                      
                      /everyone - pings everyone in a group chat
                      
                      /DadBot - enable/disable dad bot
                      
                      /translate <language code> - sets translation target language(if no language is specified translation will be disabled)
                      will automatically translate all new messages into the target lang
                      
                      send a image with /ocr caption to identify the image text
                      
                      github page:
                      """),
    LANG_SELECT_MSG("current language: %s\n\nplease select a target language");

    private final String value;

    public String getValue() {
        return value;
    }

    MessagesEnum(String val) {
        this.value = val;
    }
}
