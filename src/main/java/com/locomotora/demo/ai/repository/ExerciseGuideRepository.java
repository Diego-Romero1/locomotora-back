package com.locomotora.demo.ai.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Guia de tecnica guardada por ejercicio (tabla exercise_guides): consejos, video y alternativas. */
@Repository
public class ExerciseGuideRepository {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;

    public ExerciseGuideRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Un video guardado. Titulo, canal y duracion pueden faltar hasta el proximo refresco. */
    public record StoredVideo(String id, String title, String channel, Integer durationSeconds) {
    }

    /** tipsJson: el objeto de consejos tal como se guarda; null si todavia no hay. */
    public record Stored(
            String tipsJson,
            String videoStatus,
            String videoId,
            String videoTitle,
            String videoChannel,
            Integer videoDurationSeconds,
            Instant videoCheckedAt,
            List<StoredVideo> alternatives,
            List<String> failedIds,
            String videoSource,
            Instant videoRefreshedAt
    ) {
        public Stored {
            alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
            failedIds = failedIds == null ? List.of() : List.copyOf(failedIds);
        }

        public Stored(String tipsJson, String videoStatus, String videoId, String videoTitle, String videoChannel,
                Integer videoDurationSeconds, Instant videoCheckedAt) {
            this(tipsJson, videoStatus, videoId, videoTitle, videoChannel, videoDurationSeconds, videoCheckedAt,
                    List.of(), List.of(), null, null);
        }

        public StoredVideo primary() {
            return videoId == null ? null : new StoredVideo(videoId, videoTitle, videoChannel, videoDurationSeconds);
        }
    }

    public Optional<Stored> find(UUID exerciseId) {
        return jdbcTemplate.query(
                """
                SELECT tips::text AS tips, video_status, video_youtube_id, video_title, video_channel,
                       video_duration_seconds, video_checked_at, video_alternatives::text AS video_alternatives,
                       video_failed_ids::text AS video_failed_ids, video_source, video_refreshed_at
                FROM exercise_guides
                WHERE exercise_id = ?
                """,
                this::map,
                exerciseId
        ).stream().findFirst();
    }

    public void saveTips(UUID exerciseId, String tipsJson, String model) {
        jdbcTemplate.update(
                """
                INSERT INTO exercise_guides (exercise_id, tips, tips_model, tips_generated_at)
                VALUES (?, ?::jsonb, ?, now())
                ON CONFLICT (exercise_id) DO UPDATE
                SET tips = EXCLUDED.tips, tips_model = EXCLUDED.tips_model,
                    tips_generated_at = now(), updated_at = now()
                """,
                exerciseId, tipsJson, model
        );
    }

    /**
     * Guarda el estado del video. FOUND con el principal y sus alternativas; NOT_FOUND o PENDING sin video.
     * failedIds reemplaza la lista de videos que no se deben volver a elegir.
     */
    public void saveVideo(UUID exerciseId, String status, StoredVideo primary, List<StoredVideo> alternatives,
            List<String> failedIds, String source) {
        jdbcTemplate.update(
                """
                INSERT INTO exercise_guides (exercise_id, video_status, video_youtube_id, video_title, video_channel,
                                             video_duration_seconds, video_alternatives, video_failed_ids, video_source,
                                             video_checked_at, video_refreshed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, now(), now())
                ON CONFLICT (exercise_id) DO UPDATE
                SET video_status = EXCLUDED.video_status, video_youtube_id = EXCLUDED.video_youtube_id,
                    video_title = EXCLUDED.video_title, video_channel = EXCLUDED.video_channel,
                    video_duration_seconds = EXCLUDED.video_duration_seconds,
                    video_alternatives = EXCLUDED.video_alternatives, video_failed_ids = EXCLUDED.video_failed_ids,
                    video_source = EXCLUDED.video_source, video_checked_at = now(), video_refreshed_at = now(),
                    updated_at = now()
                """,
                exerciseId,
                status,
                primary == null ? null : primary.id(),
                primary == null ? null : primary.title(),
                primary == null ? null : primary.channel(),
                primary == null ? null : primary.durationSeconds(),
                json(alternatives == null ? List.of() : alternatives),
                json(failedIds == null ? List.of() : failedIds),
                source
        );
    }

    /** Refresco: datos al dia del principal y de las alternativas que siguen disponibles. No cambia el estado. */
    public void saveRefresh(UUID exerciseId, StoredVideo primary, List<StoredVideo> alternatives, List<String> failedIds) {
        jdbcTemplate.update(
                """
                UPDATE exercise_guides
                SET video_title = ?, video_channel = ?, video_duration_seconds = ?, video_alternatives = ?::jsonb,
                    video_failed_ids = ?::jsonb, video_refreshed_at = now(), updated_at = now()
                WHERE exercise_id = ?
                """,
                primary.title(), primary.channel(), primary.durationSeconds(), json(alternatives), json(failedIds), exerciseId
        );
    }

    /**
     * Ejercicios del catalogo global que todavia no tienen video, por prioridad: los habituales, despues los que
     * la gente tiene en sus rutinas, despues fuerza antes que estiramientos y cardio. Incluye los que no tuvieron
     * video hace mas de retryDays dias.
     */
    public List<UUID> nextToFill(int limit, int retryDays) {
        return jdbcTemplate.query(
                """
                SELECT e.id
                FROM exercises e
                LEFT JOIN exercise_guides g ON g.exercise_id = e.id
                WHERE e.created_by_user_id IS NULL AND e.is_active
                  AND (g.exercise_id IS NULL OR g.video_status = 'PENDING'
                       OR (g.video_status = 'NOT_FOUND' AND g.video_checked_at < now() - make_interval(days => ?)))
                ORDER BY e.is_core DESC,
                         (SELECT count(*) FROM routine_exercises re WHERE re.exercise_id = e.id) DESC,
                         CASE e.exercise_type WHEN 'STRENGTH' THEN 0 WHEN 'POWERLIFTING' THEN 1
                                              WHEN 'STRETCHING' THEN 3 ELSE 2 END,
                         e.name
                LIMIT ?
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                retryDays,
                limit
        );
    }

    /** Ejercicios con video cuyo ultimo refresco tiene mas de olderThanDays dias (o nunca se refresco). */
    public List<UUID> dueForRefresh(int olderThanDays, int limit) {
        return jdbcTemplate.query(
                """
                SELECT exercise_id
                FROM exercise_guides
                WHERE video_status = 'FOUND'
                  AND (video_refreshed_at IS NULL OR video_refreshed_at < now() - make_interval(days => ?))
                ORDER BY video_refreshed_at NULLS FIRST
                LIMIT ?
                """,
                (rs, rowNum) -> rs.getObject("exercise_id", UUID.class),
                olderThanDays,
                limit
        );
    }

    private Stored map(ResultSet rs, int rowNum) throws SQLException {
        Timestamp checked = rs.getTimestamp("video_checked_at");
        Timestamp refreshed = rs.getTimestamp("video_refreshed_at");
        return new Stored(
                rs.getString("tips"),
                rs.getString("video_status"),
                rs.getString("video_youtube_id"),
                rs.getString("video_title"),
                rs.getString("video_channel"),
                (Integer) rs.getObject("video_duration_seconds"),
                checked == null ? null : checked.toInstant(),
                read(rs.getString("video_alternatives"), new TypeReference<List<StoredVideo>>() { }),
                read(rs.getString("video_failed_ids"), new TypeReference<List<String>>() { }),
                rs.getString("video_source"),
                refreshed == null ? null : refreshed.toInstant()
        );
    }

    private static <T> List<T> read(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(json, type);
        } catch (Exception broken) {
            return List.of();
        }
    }

    private static String json(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception impossible) {
            return "[]";
        }
    }
}
