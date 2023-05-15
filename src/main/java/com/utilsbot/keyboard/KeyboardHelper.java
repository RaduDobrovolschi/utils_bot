package com.utilsbot.keyboard;

import com.utilsbot.domain.enums.CallbackDataEnum;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.function.IntUnaryOperator;

public class KeyboardHelper {

    private KeyboardHelper() {}

    public static final InlineKeyboardButton emptyBtn = InlineKeyboardButton.builder()
            .text(" ")
            .callbackData(CallbackDataEnum.IGNORE.toString())
            .build();

    public static InlineKeyboardButton createBtn(String text) {
        return createBtn(text, CallbackDataEnum.IGNORE);
    }

    public static InlineKeyboardButton createBtn(String text, CallbackDataEnum callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData.toString())
                .build();
    }

    public static InlineKeyboardButton createBtn(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    public static void updateHour(InlineKeyboardMarkup markup, IntUnaryOperator timeUpdate) {
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        InlineKeyboardButton inlineKeyboardButton = keyboard.get(0).get(1);
        String text = inlineKeyboardButton.getText();
        String[] split = text.split(":");
        int hour = Integer.parseInt(split[0]);
        hour = timeUpdate.applyAsInt(hour);
        inlineKeyboardButton.setText(hour < 23 && hour >= 0? hour + ":" + split[1] : text);
    }

    public static void updateMinute(InlineKeyboardMarkup markup, IntUnaryOperator timeUpdate) {
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        InlineKeyboardButton inlineKeyboardButton = keyboard.get(0).get(1);
        String text = inlineKeyboardButton.getText();
        String[] split = text.split(":");
        int minute = Integer.parseInt(split[1]);
        minute = timeUpdate.applyAsInt(minute);
        inlineKeyboardButton.setText(minute < 60 && minute >= 0? split[0] + ":" + (minute < 10 ? "0" + minute : minute) : text);
    }
}
