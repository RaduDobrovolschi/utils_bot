package com.utilsbot.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TimeUtils {

    public static final DateTimeFormatter defaultDTFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static LocalDateTime getOffsetDateTime(float offset) {
        return OffsetDateTime.now(ZoneOffset.ofHoursMinutes((int)offset, (int)((offset - (int)offset) * 100))).toLocalDateTime();
    }

    public static LocalDate getOffsetDate(float offset) {
        return OffsetDateTime.now(ZoneOffset.ofHoursMinutes((int)offset, (int)((offset - (int)offset) * 100))).toLocalDate();
    }

    public static LocalDateTime applyOffset(float gmtOffset, LocalDateTime time) {
        if (gmtOffset < 0) {
            return time.minusHours((int)gmtOffset).minusMinutes((int)((gmtOffset - (int)gmtOffset) * 100));
        } else {
            return time.plusHours((int)gmtOffset).plusMinutes((int)((gmtOffset - (int)gmtOffset) * 100));
        }
    }

    public static LocalDateTime removeOffset(float gmtOffset, LocalDateTime time) {
        if (gmtOffset > 0) {
            return time.minusHours((int)gmtOffset).minusMinutes((int)((gmtOffset - (int)gmtOffset) * 100));
        } else {
            return time.plusHours((int)gmtOffset).plusMinutes((int)((gmtOffset - (int)gmtOffset) * 100));
        }
    }

    public static float getOffsetFromText(String text) {
        if (text.matches("[-+]?\\d?\\d[,:\\.]\\d?\\d")) {
        }
        return 0;
    }

    private TimeUtils() {}
}
