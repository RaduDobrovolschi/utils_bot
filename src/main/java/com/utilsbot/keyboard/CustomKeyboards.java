package com.utilsbot.keyboard;

import com.utilsbot.domain.enums.MessagesEnum;
import net.suuft.libretranslate.Language;
import org.apache.commons.collections4.ListUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

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

    static {
        keyboardsMap.put(LANG_SELECT_MSG, languageSelectionKeyboard());
        keyboardsMap.put(REQUEST_LOCATION, cancelRegionUpdate());
        keyboardsMap.put(NOTIFICATION_UPDATE, updateNotification());
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
                                List.of(createBtn("Cancel", CANCEL))
                        )
                )
                .build();
    }


    public static InlineKeyboardMarkup notificationConfig(List<InlineKeyboardButton> notifications) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>(List.of(
                List.of(
                        createBtn("Ping everyone now", PING_EVERYONE),
                        createBtn("toggle notifications", UPDATE_USER_GROUP)
                )
        ));
        notifications.forEach(i -> keyboard.add(List.of(i)));
        keyboard.add(
                List.of(
                    createBtn("Back", MAIN_MENU),
                    createBtn("Update time region", TIME_REGION_UPDATE_NAME)
            )
        );
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
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

    public static InlineKeyboardMarkup timeKeyboard(int h, int m) {
        return InlineKeyboardMarkup.builder()
                .keyboard(
                        List.of(
                                List.of(
                                        createBtn("--", T_DEC),
                                        createBtn( h + ":" + (m < 10? "0" : "") + m, T_MANUAL_INPUT),
                                        createBtn("++", T_INC)
                                ),
                                List.of(
                                        createBtn("-10", T_SUB_10),
                                        createBtn("-5", T_SUB_5),
                                        createBtn("+5", T_ADD_5),
                                        createBtn("+10", T_ADD_10)
                                ),
                                List.of(
                                        createBtn("Back", ADD_NOTIFICATION),
                                        createBtn("Next", NEXT_UPDATE_TIME)
                                )
                        )
                )
                .build();
    }

    public static InlineKeyboardMarkup updateNotification() {
        return InlineKeyboardMarkup.builder()
                .keyboard(
                        List.of(
                                List.of(
                                        createBtn("Add custom msg", ADD_NF_CUSTOM_MSG)
                                ),
                                List.of(
                                        createBtn("delete notification", DELETE_NF)
                                ),
                                List.of(
                                        createBtn("Back", NOTIFICATIONS_CONFIG)
                                )
                        )
                )
                .build();
    }

    public static InlineKeyboardMarkup getKeyboard(MessagesEnum enumVal) {
        return keyboardsMap.get(enumVal);
    }
}
