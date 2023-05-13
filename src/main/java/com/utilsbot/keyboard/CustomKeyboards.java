package com.utilsbot.keyboard;

import com.utilsbot.domain.enums.MessagesEnum;
import com.utilsbot.domain.enums.ReplyKeyboardMarkupData;
import net.suuft.libretranslate.Language;
import org.apache.commons.collections4.ListUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.*;

import static com.utilsbot.domain.enums.CallbackDataEnum.*;
import static com.utilsbot.domain.enums.MessagesEnum.*;
import static com.utilsbot.keyboard.KeyboardHelper.createBtn;
import static com.utilsbot.keyboard.KeyboardHelper.emptyBtn;

public class CustomKeyboards {

    private static final EnumMap<MessagesEnum, InlineKeyboardMarkup> keyboardsMap = new EnumMap<>(MessagesEnum.class);
//    public static final ReplyKeyboardMarkup requestLocationKeyboard = requestLocationKeyboard();
//    public static final ReplyKeyboardRemove replyKeyboardRemove = emptyKeyboard();

    static {
        keyboardsMap.put(LANG_SELECT_MSG, languageSelectionKeyboard());
        keyboardsMap.put(REQUEST_LOCATION, cancelRegionUpdate());
        keyboardsMap.put(SEL_NOTIFY_HOUR, hourKeyboard());
    }

    private CustomKeyboards() {}

    public static InlineKeyboardMarkup infoKeyboard(boolean dadBot, Language language) {
        return InlineKeyboardMarkup.builder()
                .keyboard(
                        List.of(
                                List.of(createBtn("Notifications and /everyone", NOTIFICATIONS_CONFIG)),
                                List.of(
                                        createBtn("Dad bot: " + (dadBot? "ON" : "OFF"), DAD_BOT),
                                        createBtn("Translation: " + language.getCode().toUpperCase(), TRANSLATION_SELECTOR)
                                        ),
                                List.of(createBtn("Exit", EXIT))
                        )
                ).build();
    }

    private static InlineKeyboardMarkup cancelRegionUpdate() {
        return InlineKeyboardMarkup.builder()
                .keyboard(
                        List.of(
                                List.of(createBtn("Cancel", CANCEL_REGION_UPDATE))
                        )
                )
                .build();
    }


    public static InlineKeyboardMarkup notificationConfig() {
        return InlineKeyboardMarkup.builder()
                .keyboard(
                        List.of(
                                List.of(
                                        createBtn("Ping everyone now", PING_EVERYONE),
                                        createBtn("toggle notifications", UPDATE_USER_GROUP)
                                ),
                                List.of(createBtn("+ New notification", ADD_NOTIFICATION)),
                                List.of(
                                        createBtn("Back", MAIN_MENU),
                                        createBtn("Update time region", TIME_REGION_UPDATE_NAME)
                                )
                        )
                )
                .build();
    }

    private static InlineKeyboardMarkup languageSelectionKeyboard() {
        List<List<InlineKeyboardButton>> partition = new ArrayList<>(ListUtils.partition(
                Arrays.stream(Language.values())
                        .map(lang -> createBtn(lang.getCode().toUpperCase(), lang.toString()))
                        .toList(), 3));
        partition.add(List.of(createBtn("Back", MAIN_MENU)));
        return InlineKeyboardMarkup.builder()
                .keyboard(partition)
                .build();
    }

    public static InlineKeyboardMarkup monthsKeyboard() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        int monthInt = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        ++monthInt;

        int foo = 0;
        List<InlineKeyboardButton> months = new ArrayList<>();
        for (int i = monthInt; i < monthInt + 12; ++i) {
            if (i == 13) {
                ++year;
                foo = 12;
            }
            if ((i - monthInt) % 3 == 0) {
                keyboard.add(months);
                months = new ArrayList<>();
            }
            months.add(
                    createBtn(Month.of(i - foo).name() + " " + year, "NF_Y_" + year + "M_" + (i - foo))
            );
        }

        keyboard.add(months);
        keyboard.add(List.of(createBtn("Cancel", NOTIFICATIONS_CONFIG)));

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }

    public static InlineKeyboardMarkup dayOfMonthKeyboard(int year, int month) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(List.of(createBtn(Month.of(month).name() + " " + year, SELECT_MONTH)));
        keyboard.add(
                List.of(
                        createBtn("Sun"),
                        createBtn("Mon"),
                        createBtn("Tue"),
                        createBtn("Wed"),
                        createBtn("Thu"),
                        createBtn("Fri"),
                        createBtn("Sat"))
            );
        int dayOfWeek = LocalDate.of(year, month, 1).getDayOfWeek().getValue();
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        String callbackStrPrefix = "NF_Y_" + year + "_M_" + month + "D_";

        List<InlineKeyboardButton> week = new ArrayList<>();

        //first week
        if (dayOfWeek != 7)
            for (int i = 0; i < dayOfWeek; ++i) {
                week.add(emptyBtn);
            }
        for (int i = dayOfWeek + 1; i <= 7; ++i) {
            week.add(
                    createBtn(String.valueOf(i - dayOfWeek), callbackStrPrefix + (i - dayOfWeek))
            );
        }
        keyboard.add(week);

        //weeks in the middle
        week = new ArrayList<>();
        int startDay = 8 - dayOfWeek;
        int midDayLimit = daysInMonth + dayOfWeek;
        for (int i = startDay; i < midDayLimit; ++i) {
            if ((i - startDay) % 7 == 0 && !week.isEmpty()) {
                keyboard.add(week);
                week = new ArrayList<>();
                if (daysInMonth - i < 7) break;
            }
            week.add(createBtn(String.valueOf(i), callbackStrPrefix + i));
        }

        //last week
        for (int i = (keyboard.size() - 2) * 7 - dayOfWeek + 1; i <= daysInMonth; ++i) {
            week.add(
                    createBtn(String.valueOf(i), callbackStrPrefix + i)
            );
        }
        for (int i = week.size(); i < 7; ++i) {
            week.add(emptyBtn);
        }
        keyboard.add(week);
        keyboard.add(List.of(createBtn("Cancel", NOTIFICATIONS_CONFIG)));

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }

    private static InlineKeyboardMarkup hourKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboard(
                        List.of(
                                List.of(
                                        createBtn("AM", NF_P_AM),
                                        createBtn("PM", NF_P_PM)
                                ),
                                List.of(
                                        createBtn("1", NF_H_1),
                                        createBtn("2", NF_H_2),
                                        createBtn("3", NF_H_3)
                                ),
                                List.of(
                                        createBtn("4", NF_H_4),
                                        createBtn("5", NF_H_5),
                                        createBtn("6", NF_H_6)
                                ),
                                List.of(
                                        createBtn("7", NF_H_7),
                                        createBtn("8", NF_H_8),
                                        createBtn("9", NF_H_9)
                                ),
                                List.of(
                                        createBtn("10", NF_H_10),
                                        createBtn("11", NF_H_11),
                                        createBtn("12", NF_H_12)
                                ),
                                List.of(
                                        createBtn("Back", ADD_NOTIFICATION),
                                        createBtn("Next", SELECT_MIN)
                                )
                        )
                )
                .build();
    }

    public static InlineKeyboardMarkup minuteKeyboard(int h, int m) {
        return InlineKeyboardMarkup.builder()
                .keyboard(
                        List.of(
                                List.of(
                                        createBtn("--", MI_DEC),
                                        createBtn(h + ":" + (m < 10? "0" : "") + m + (h > 12? " am" : " pm"), MI_MANUAL_INPUT),
                                        createBtn("++", MI_INC)
                                ),
                                List.of(
                                        createBtn("-10", MI_SUB_10),
                                        createBtn("-5", MI_SUB_5),
                                        createBtn("+5", MI_ADD_5),
                                        createBtn("+10", MI_ADD_10)
                                ),
                                List.of(
                                        createBtn("Back", UPDATE_HOUR),
                                        createBtn("Next", UPDATE_NOTIFICATION)
                                )
                        )
                )
                .build();
    }

    private static ReplyKeyboardMarkup requestLocationKeyboard() {
        return ReplyKeyboardMarkup.builder()
                .keyboard(
                        List.of(
                                new KeyboardRow(
                                        List.of(
                                                KeyboardButton.builder()
                                                        .text(ReplyKeyboardMarkupData.SET_TIME_ZONE.getValue())
                                                        .requestLocation(true)
                                                        .build()
                                        )
                                )
                        )
                )
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .selective(true)
                .build();
    }

    private static ReplyKeyboardRemove emptyKeyboard() {
        return ReplyKeyboardRemove.builder()
                .removeKeyboard(true)
                .selective(true)
                .build();
    }

    public static InlineKeyboardMarkup getKeyboard(MessagesEnum enumVal) {
        return keyboardsMap.get(enumVal);
    }
}
