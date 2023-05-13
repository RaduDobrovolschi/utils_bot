package com.utilsbot.keyboard;

import com.utilsbot.domain.enums.CallbackDataEnum;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Optional;

import static com.utilsbot.utils.AppUtils.getDataFromCallback;

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

    public static void restoreHourSelection(InlineKeyboardMarkup markup, int hour) {
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        List<InlineKeyboardButton> firstRow = keyboard.get(0);
        if (hour > 12) {
            hour -= 12;
            addSelection(firstRow.get(0));
        } else {
            addSelection(firstRow.get(1));
        }
        int row = hour/3;
        int rounded = row * 3;
        addSelection(keyboard.get(rounded == hour? row : 1 + row).get(rounded == hour? 2 : hour - rounded - 1));
    }

    public static Optional<Integer> getHour(InlineKeyboardMarkup markup) {
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        List<InlineKeyboardButton> firstRow = keyboard.get(0);
        boolean isAm;
        if (firstRow.get(0).getText().startsWith("-")) {
            isAm = true;
        } else if (firstRow.get(1).getText().startsWith("-")) {
            isAm = false;
        } else return Optional.empty();

        for (int i = 1; i < 5; ++i) {
            for (InlineKeyboardButton btn : keyboard.get(i)) {
                if (btn.getText().startsWith("-")) {
                    int hour = Integer.parseInt(btn.getText().replace("-", ""));
                    return Optional.of(isAm? hour + 12 : hour);
                }
            }
        }
        return Optional.empty();
    }

    public static void updateHour(InlineKeyboardMarkup markup, String callbackData) {
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        cleanFirstSelectedHour(keyboard);
        int hour = getDataFromCallback("H_", callbackData);
        int row = hour/3;
        int rounded = row * 3;
        addSelection(keyboard.get(rounded == hour? row : 1 + row).get(rounded == hour? 2 : hour - rounded - 1));
    }

    private static void cleanFirstSelectedHour(List<List<InlineKeyboardButton>> keyboard) {
        for (int i = 1; i < 5; ++i) {
            for (InlineKeyboardButton key : keyboard.get(i)) {
                String text = key.getText();
                if (text.startsWith("-")) {
                    key.setText(text.replace("-", ""));
                    return;
                }
            }
        }
    }

    public static void updateAmPm(InlineKeyboardMarkup markup, String callbackData) {
        if (callbackData.contains("AM")) {
            switchSelectByRow(0, markup, 0);
        } else if (callbackData.contains("PM")) {
            switchSelectByRow(0, markup, 1);
        }
    }

    public static void switchSelectByRow(int row, InlineKeyboardMarkup markup, int selectId) {
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        List<InlineKeyboardButton> inlineKeyboardButtons = keyboard.get(row);
        cleanMarkupSingle(inlineKeyboardButtons);
        InlineKeyboardButton inlineKeyboardButton = inlineKeyboardButtons.get(selectId);
        addSelection(inlineKeyboardButton);
    }

    public static void cleanMarkupSingle(List<InlineKeyboardButton> keys) {
        for (InlineKeyboardButton key : keys) {
            String text = key.getText();
            if (text.startsWith("-")) {
                key.setText(text.replace("-", ""));
                return;
            }
        }
    }

    public static void addSelection(InlineKeyboardButton btn) {
        String text = btn.getText();
        btn.setText("-" + text + "-");
    }
}
