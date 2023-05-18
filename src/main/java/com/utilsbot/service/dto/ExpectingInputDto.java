package com.utilsbot.service.dto;

import com.utilsbot.domain.enums.InputType;
import com.utilsbot.domain.enums.TimeUnits;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Optional;

public record ExpectingInputDto(
        Instant createTime,
        Long userId,
        Long chatId,
        InputType inputType,

        Optional<Message> previousMsg,
        Optional<EnumMap<TimeUnits, Integer>> notificationTimeData,
        Optional<Long> notificationId
){
    public ExpectingInputDto(CallbackQuery callbackQuery, InputType inputType, int year, int month) {
        this(Instant.now(), callbackQuery.getFrom().getId(), callbackQuery.getMessage().getChatId(), inputType, Optional.empty(), Optional.of(new EnumMap<>(TimeUnits.class)), Optional.empty());
        updateNotificationTimeData(TimeUnits.YEAR, year);
        updateNotificationTimeData(TimeUnits.MONTH, month);
    }

    public ExpectingInputDto(CallbackQuery callbackQuery, InputType inputType, Message previousMsg) {
        this(Instant.now(), callbackQuery.getFrom().getId(), callbackQuery.getMessage().getChatId(), inputType, Optional.of(previousMsg), Optional.empty(), Optional.empty());
    }

    public ExpectingInputDto(CallbackQuery callbackQuery, InputType inputType, Message previousMsg, Long nfId) {
        this(Instant.now(), callbackQuery.getFrom().getId(), callbackQuery.getMessage().getChatId(), inputType, Optional.of(previousMsg), Optional.empty(), Optional.of(nfId));
    }

    public ExpectingInputDto(CallbackQuery callbackQuery, InputType inputType, Long notificationId) {
        this(Instant.now(), callbackQuery.getFrom().getId(), callbackQuery.getMessage().getChatId(), inputType, Optional.empty(), Optional.empty(), Optional.of(notificationId));
    }

    public ExpectingInputDto(CallbackQuery callbackQuery, InputType inputType, Message previousMsg, EnumMap<TimeUnits, Integer> notificationTimeData) {
        this(Instant.now(), callbackQuery.getFrom().getId(), callbackQuery.getMessage().getChatId(), inputType, Optional.of(previousMsg), Optional.of(notificationTimeData), Optional.empty());
    }

    public boolean updateNotificationTimeData(TimeUnits timeUnits, int data) {
        if (notificationTimeData.isEmpty() || !inputType.equals(InputType.NF_BUILD)) {
            return false;
        }
        notificationTimeData.get().put(timeUnits, data);
        return true;
    }

    public LocalDateTime createDateTime(int h, int m) {
        if (notificationTimeData.isPresent()) {
            EnumMap<TimeUnits, Integer> timeUnitsIntegerEnumMap = notificationTimeData.get();
            return LocalDateTime.of(
                    timeUnitsIntegerEnumMap.get(TimeUnits.YEAR),
                    timeUnitsIntegerEnumMap.get(TimeUnits.MONTH),
                    timeUnitsIntegerEnumMap.get(TimeUnits.DAY),
                    h, m
            );
        }
        return null;
    }
}

