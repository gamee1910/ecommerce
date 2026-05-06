package com.ecommerce.service.product.common.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    private TimeUtils() {}

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // ── Formatters ────────────────────────────────────────────────────────────

    /** 2026-03-21 10:30:00 */
    public static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(VN_ZONE);

    /** Instant → "2026-03-21 10:30:00" (VN timezone) */
    public static String formatVn(Instant instant) {
        if (instant == null) return null;
        return DATE_TIME.format(instant);
    }
}
