package com.utilsbot.keyboard;

import com.utilsbot.domain.enums.CallbackDataEnum;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

public class KeyboardHelper {

    private KeyboardHelper() {}

    public static InlineKeyboardButton createBtn(String text, CallbackDataEnum callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData.getValue())
                .build();
    }

    public static InlineKeyboardButton createBtn(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }
}
