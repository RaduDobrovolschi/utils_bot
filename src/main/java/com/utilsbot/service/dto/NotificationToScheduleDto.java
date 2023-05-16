package com.utilsbot.service.dto;

import java.time.Instant;

public record NotificationToScheduleDto(
        Long chatId,
        Long msgId,
        Instant triggerTime
) {
    public NotificationToScheduleDto(Long chatId, Instant triggerTime) {
        this(chatId, null, triggerTime);
    }

}
