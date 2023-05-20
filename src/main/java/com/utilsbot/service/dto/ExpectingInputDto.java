package com.utilsbot.service.dto;

import com.utilsbot.domain.enums.InputType;
import com.utilsbot.domain.enums.MessagesEnum;
import com.utilsbot.domain.enums.TimeUnits;
import jakarta.validation.constraints.NotNull;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Optional;

public record ExpectingInputDTO(
        Instant createTime,
        Long userId,
        Long chatId,
        InputType inputType,

        Optional<EnumMap<MessagesEnum, Message>> previousMsg,
        Optional<EnumMap<TimeUnits, Integer>> notificationTimeData,
        Optional<Long> notificationId
){
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private Long chatId;
        private InputType inputType;

        private final EnumMap<MessagesEnum, Message> previousMsg = new EnumMap<>(MessagesEnum.class);
        private EnumMap<TimeUnits, Integer> notificationTimeData = new EnumMap<>(TimeUnits.class);
        private Long notificationId;

        public Builder userIdAndChatId(@NotNull CallbackQuery callbackQuery) {
            this.userId = callbackQuery.getFrom().getId();
            this.chatId = callbackQuery.getMessage().getChatId();
            if (userId == null) {
                throw new NullPointerException("userId is marked non-null but is null");
            } else if (chatId == null) {
                throw new NullPointerException("chatId is marked non-null but is null");
            }
            return this;
        }

        public Builder inputType(@NotNull InputType inputType) {
            this.inputType = inputType;
            return this;
        }

        public Builder previousMsg(@NotNull MessagesEnum msgEnum, @NotNull Message msg) {
            this.previousMsg.put(msgEnum, msg);
            return this;
        }

        public Builder notificationTimeData(@NotNull TimeUnits timeUnits,@NotNull Integer timeVal) {
            this.notificationTimeData.put(timeUnits, timeVal);
            return this;
        }

        public Builder notificationTimeData(@NotNull EnumMap<TimeUnits, Integer> notificationTimeData) {
            this.notificationTimeData = notificationTimeData;
            return this;
        }

        public Builder notificationId(@NotNull Long notificationId) {
            this.notificationId = notificationId;
            return this;
        }

        public ExpectingInputDTO build() {
            if (userId == null) {
                throw new NullPointerException("userId is marked non-null but is null");
            } else if (chatId == null) {
                throw new NullPointerException("chatId is marked non-null but is null");
            } else if (inputType == null) {
                throw new NullPointerException("inputType is marked non-null but is null");
            }
            return new ExpectingInputDTO(
                            Instant.now(),
                            userId,
                            chatId,
                            inputType,
                            previousMsg.isEmpty()? Optional.empty() : Optional.of(previousMsg),
                            inputType.equals(InputType.NF_BUILD) || !notificationTimeData.isEmpty()? Optional.of(notificationTimeData) : Optional.empty(),
                            notificationId == null? Optional.empty() : Optional.of(notificationId)
                    );
        }
    }
}

