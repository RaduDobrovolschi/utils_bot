package com.utilsbot.service.dto;

import com.utilsbot.domain.enums.InputType;
import com.utilsbot.domain.enums.TimeUnits;
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
        Optional<EnumMap<TimeUnits, Integer>> notificationTimeData
){
    public ExpectingInputDto(Long userId, Long chatId, InputType inputType, int year, int month) {
        this(Instant.now(), userId, chatId, inputType, Optional.empty(), Optional.of(new EnumMap<>(TimeUnits.class)));
        updateNotificationTimeData(TimeUnits.YEAR, year);
        updateNotificationTimeData(TimeUnits.MONTH, month);
    }

    public ExpectingInputDto(Long userId, Long chatId, InputType inputType, Message previousMsg){
        this(Instant.now(), userId, chatId, inputType, Optional.of(previousMsg), Optional.empty());
    }

    public boolean updateNotificationTimeData(TimeUnits timeUnits, int data) {
        if (notificationTimeData.isEmpty() || !inputType.equals(InputType.NOTIFICATION_BUILD)) {
            return false;
        }
        notificationTimeData.get().put(timeUnits, data);
        return true;
    }

    public boolean isAllTimeDataPresent() {
        if (notificationTimeData.isEmpty()) {
            return false;
        }
        return TimeUnits.values().length == notificationTimeData.get().values().size();
    }

    public LocalDateTime getTime() {
        if (notificationTimeData.isPresent() && isAllTimeDataPresent()) {
            EnumMap<TimeUnits, Integer> timeUnitsIntegerEnumMap = notificationTimeData.get();
             return LocalDateTime.of(
                    timeUnitsIntegerEnumMap.get(TimeUnits.YEAR),
                    timeUnitsIntegerEnumMap.get(TimeUnits.MONTH),
                    timeUnitsIntegerEnumMap.get(TimeUnits.DAY),
                    timeUnitsIntegerEnumMap.get(TimeUnits.HOUR),
                    timeUnitsIntegerEnumMap.get(TimeUnits.MINUTE)
             );
        }
        return null;
    }
}

