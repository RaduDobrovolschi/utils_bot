package com.utilsbot.keyboard;

import com.utilsbot.domain.enums.MessagesEnum;
import net.suuft.libretranslate.Language;
import org.apache.commons.collections4.ListUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

import static com.utilsbot.domain.enums.CallbackDataEnum.*;
import static com.utilsbot.domain.enums.MessagesEnum.LANG_SELECT_MSG;
import static com.utilsbot.keyboard.KeyboardHelper.createBtn;

public class CustomKeyboards {

    private static final EnumMap<MessagesEnum, InlineKeyboardMarkup> keyboardsMap = new EnumMap<>(MessagesEnum.class);

    static {
        keyboardsMap.put(LANG_SELECT_MSG, languageSelectionKeyboard());
    }

    private CustomKeyboards() {}

    public static InlineKeyboardMarkup infoKeyboard(boolean dadBot, Language language) {
        return InlineKeyboardMarkup.builder()
                .keyboard(
                        List.of(
                                List.of(createBtn("Dad bot: " + (dadBot? "ON" : "OFF"), DAD_BOT)),
                                List.of(createBtn("Translation: " + language.getCode().toUpperCase(), TRANSLATION_SELECTOR)),
                                List.of(createBtn("Exit", EXIT))
                        )
                ).build();
    }

    public static InlineKeyboardMarkup languageSelectionKeyboard() {
        List<List<InlineKeyboardButton>> partition = new ArrayList<>(ListUtils.partition(
                Arrays.stream(Language.values())
                        .map(lang -> createBtn(lang.getCode().toUpperCase(), lang.toString()))
                        .toList(), 3));
        partition.add(List.of(createBtn("Back", MAIN_MENU)));
        return InlineKeyboardMarkup.builder()
                .keyboard(partition)
                .build();
    }

    public static Map<MessagesEnum, InlineKeyboardMarkup> getKeyboardsMap() {
        return keyboardsMap;
    }
}
