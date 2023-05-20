package com.utilsbot.domain.enums;

public enum MessagesEnum {

    START_MESSAGE("""
                      This is a Telegram utils bot
                      
                      Here is what it can do:
                      
                      /DadBot - enable/disable dad bot
                      
                      /VmToText - enable/disable voice message to text. Transcripts all VMs in this chat if enabled
                      
                      /translate <language code> - sets translation target language
                      will automatically translate all new messages into the target lang
                      
                      /everyone - pings all users that enabled notifications with /notifications
                      
                      send a image with /ocr <language code optional> caption to identify the image text
                      
                      github page: https://github.com/RaduDobrovolschi/utils_bot
                      """),
    EVERYONE_CONFIG("""
                        Notifications config:
                        
                        Use /notifications to join/leave the notification group
                        
                        All users that joined will be included in /everyone list and the scheduled group notifications
                        
                        Users subscribed to notifications:
                        %s
                        """),
    LANG_SELECT_MSG("current language: %s\n\nplease select a target language"),
    SHARE_LOCATION("Please use the inline keyboard to share ur location"),
    REQUEST_MSG_UPDATE("please provide %s"),
    LOCATION_UPDATED("Your time zone was set to: %s %s %s"),
    LOCATION_FAIL("Failed to update time zone"),
    SEL_NOTIFY_MONTH("Please select notification month"),
    SEL_NOTIFY_DAY("Please select notification day"),
    SEL_NOTIFY_HOUR("Please select notification HOUR"),
    SEL_NOTIFY_MIN("Please select notification time in MINUTES"),
    NOTIFICATION_UPDATE("Notification scheduled for %s\n\nWill run in %d hours and %d minutes"),
    OCR_HELP("Send image with this caption to identify the text. If no language code is specified will default to en. Supported languages: ar, bg, zh, hr, cs, da, nl, fi, fr, de, el, hu, ko, it, ja, pl, pt, ru, sl, es, sv, tr, hi, kn, fa, te, ta, th, vi");

    private final String value;

    public String getValue() {
        return value;
    }

    MessagesEnum(String val) {
        this.value = val;
    }
}
