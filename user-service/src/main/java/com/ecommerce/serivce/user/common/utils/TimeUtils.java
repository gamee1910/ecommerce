package com.ecommerce.serivce.user.common.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    private TimeUtils() {}

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // ── Formatters ────────────────────────────────────────────────────────────

    /**
     * 2026-03-21 10:30:00
     */
    public static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(VN_ZONE);

    /**
     * 2026-03-21
     */
    public static final DateTimeFormatter DATE_ONLY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(VN_ZONE);

    /**
     * 10:30:00
     */
    public static final DateTimeFormatter TIME_ONLY =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(VN_ZONE);

    /**
     * 21/03/2026 10:30 — dùng cho display UI
     */
    public static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(VN_ZONE);

    // ── DB bridge: Instant ↔ OffsetDateTime ───────────────────────────────────

    /**
     * Instant → OffsetDateTime (UTC) để pass vào JdbcClient param.
     * PostgreSQL JDBC driver không nhận Instant trực tiếp.
     * <p>
     * Dùng khi: .param("createdAt", TimeUtils.toDb(entity.getCreatedAt()))
     */
    public static OffsetDateTime toDb(Instant instant) {
        if (instant == null) return null;
        return instant.atOffset(ZoneOffset.UTC);
    }

    /**
     * ResultSet column → Instant.
     * Dùng thay rs.getTimestamp() vì getTimestamp() không handle timezone đúng.
     * <p>
     * Dùng khi: .createdAt(TimeUtils.fromDb(rs, "created_at"))
     */
    public static Instant fromDb(ResultSet rs, String column) throws SQLException {
        OffsetDateTime odt = rs.getObject(column, OffsetDateTime.class);
        return odt != null ? odt.toInstant() : null;
    }

    // ── Format: Instant → String ──────────────────────────────────────────────

    /**
     * Instant → "2026-03-21 10:30:00" (VN timezone)
     */
    public static String formatVn(Instant instant) {
        if (instant == null) return null;
        return DATE_TIME.format(instant);
    }

    /**
     * Instant → "21/03/2026 10:30" (display format)
     */
    public static String formatDisplay(Instant instant) {
        if (instant == null) return null;
        return DISPLAY.format(instant);
    }

    /**
     * Instant → "2026-03-21"
     */
    public static String formatDate(Instant instant) {
        if (instant == null) return null;
        return DATE_ONLY.format(instant);
    }

    /**
     * Instant → "10:30:00"
     */
    public static String formatTime(Instant instant) {
        if (instant == null) return null;
        return TIME_ONLY.format(instant);
    }

    /**
     * Instant → ISO 8601 UTC — dùng cho audit log, Kafka event
     */
    public static String formatIso(Instant instant) {
        if (instant == null) return null;
        return instant.toString(); // e.g. 2026-03-21T03:30:00Z
    }

    // ── Parse: String → Instant ───────────────────────────────────────────────

    /**
     * "2026-03-21 10:30:00" (VN) → Instant (UTC)
     */
    public static Instant parseVn(String text) {
        if (text == null || text.isBlank()) return null;
        return ZonedDateTime.parse(text, DATE_TIME).toInstant();
    }

    /**
     * "2026-03-21T03:30:00Z" ISO 8601 → Instant
     */
    public static Instant parseIso(String text) {
        if (text == null || text.isBlank()) return null;
        return Instant.parse(text);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Instant hiện tại theo VN timezone
     */
    public static ZonedDateTime nowVn() {
        return ZonedDateTime.now(VN_ZONE);
    }

    /**
     * Check Instant đã qua chưa
     */
    public static boolean isExpired(Instant instant) {
        return instant != null && instant.isBefore(Instant.now());
    }
}
