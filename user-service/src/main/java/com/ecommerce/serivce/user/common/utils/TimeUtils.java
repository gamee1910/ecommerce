package com.ecommerce.serivce.user.common.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    private TimeUtils() {}

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Format: 21-03-2026 10:32:03
     */
    public static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(VN_ZONE);

    /**
     * Format: 21-03-2026
     */
    public static final DateTimeFormatter DATE_ONLY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(VN_ZONE);

    /**
     * Format: 21-03-2026
     */
    public static final DateTimeFormatter TIME_ONLY =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(VN_ZONE);

    public static String formatVn(Instant instant) {
        if (instant == null) return null;
        return DATE_TIME.format(instant);
    }

    public static String formatDate(Instant instant) {
        if (instant == null) return null;
        return DATE_ONLY.format(instant);
    }

    public static String formatTime(Instant instant) {
        if (instant == null) return null;
        return TIME_ONLY.format(instant);
    }

    public static Instant parseVN(String text) {
        if (text == null || text.isBlank()) return null;
        return ZonedDateTime.parse(text, DATE_TIME).toInstant();
    }

    /**
     * Helper: Change Instant to timezone VN
     */
    public static ZonedDateTime nowVn() {
        return ZonedDateTime.now(VN_ZONE);
    }

    public static boolean isExpired(Instant instant) {
        return instant != null && instant.isBefore(Instant.now());
    }
}
