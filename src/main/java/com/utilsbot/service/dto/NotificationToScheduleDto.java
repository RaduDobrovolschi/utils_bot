package com.utilsbot.service.dto;

import java.time.Instant;

public record NotificationToScheduleDto(
        Long notificationId,
        Instant triggerTime
) {
}
