package com.utilsbot.service.dto;

import java.time.Instant;

public record NotificationToScheduleDTO(
        Long notificationId,
        Instant triggerTime
) {
}
