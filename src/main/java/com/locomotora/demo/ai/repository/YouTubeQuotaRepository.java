package com.locomotora.demo.ai.repository;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Cuota de la YouTube Data API usada en el dia de YouTube (se renueva a medianoche del Pacifico). La comparten
 * el llenado diario, los reemplazos y el refresco, para no pasarse del limite del proyecto.
 */
@Repository
public class YouTubeQuotaRepository {
    static final ZoneId YOUTUBE_ZONE = ZoneId.of("America/Los_Angeles");

    private final JdbcTemplate jdbcTemplate;

    public YouTubeQuotaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static LocalDate today() {
        return LocalDate.now(YOUTUBE_ZONE);
    }

    public void record(int units, int searches) {
        jdbcTemplate.update(
                """
                INSERT INTO youtube_quota_usage (day, units, searches) VALUES (?, ?, ?)
                ON CONFLICT (day) DO UPDATE
                SET units = youtube_quota_usage.units + EXCLUDED.units,
                    searches = youtube_quota_usage.searches + EXCLUDED.searches
                """,
                today(), units, searches
        );
    }

    public int searchesToday() {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COALESCE((SELECT searches FROM youtube_quota_usage WHERE day = ?), 0)",
                Integer.class,
                today()
        );
        return value == null ? 0 : value;
    }

    public int unitsToday() {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COALESCE((SELECT units FROM youtube_quota_usage WHERE day = ?), 0)",
                Integer.class,
                today()
        );
        return value == null ? 0 : value;
    }
}
