package com.utilsbot.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

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

    public static Optional<Float> getOffsetFromText(String text) {
        if (text.matches("[-+]?\\d?\\d[,:\\.]?\\d?\\d?")) {
            String s = text.replaceFirst(":", ".")
                    .replaceFirst(",", ".");
            float offset;
            try {
                offset = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return Optional.empty();
            }
            float mins = offset - (int)offset;
            if (offset > -11 && offset < 14 &&
                (mins == 0.0f || mins == 0.30f || mins == 0.45f)) {
                return Optional.of(offset);
            }
        }
        return Optional.empty();
    }

    private TimeUtils() {}
}
