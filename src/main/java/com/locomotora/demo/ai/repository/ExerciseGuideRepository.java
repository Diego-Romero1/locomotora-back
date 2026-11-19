package com.locomotora.demo.ai.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Guia de tecnica guardada por ejercicio (tabla exercise_guides). */
@Repository
public class ExerciseGuideRepository {
    private final JdbcTemplate jdbcTemplate;

    public ExerciseGuideRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** tipsJson: el objeto de consejos tal como se guarda; null si todavia no hay. */
    public record Stored(
            String tipsJson,
            String videoStatus,
            String videoId,
            String videoTitle,
            String videoChannel,
            Integer videoDurationSeconds,
            Instant videoCheckedAt
    ) {
    }

    public Optional<Stored> find(UUID exerciseId) {
        return jdbcTemplate.query(
                """
                SELECT tips::text AS tips, video_status, video_youtube_id, video_title, video_channel,
                       video_duration_seconds, video_checked_at
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

    /** status FOUND con los datos del video, o NOT_FOUND con los datos en null. */
    public void saveVideo(UUID exerciseId, String status, String videoId, String title, String channel, Integer durationSeconds) {
        jdbcTemplate.update(
                """
                INSERT INTO exercise_guides (exercise_id, video_status, video_youtube_id, video_title, video_channel,
                                             video_duration_seconds, video_checked_at)
                VALUES (?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (exercise_id) DO UPDATE
                SET video_status = EXCLUDED.video_status, video_youtube_id = EXCLUDED.video_youtube_id,
                    video_title = EXCLUDED.video_title, video_channel = EXCLUDED.video_channel,
                    video_duration_seconds = EXCLUDED.video_duration_seconds, video_checked_at = now(),
                    updated_at = now()
                """,
                exerciseId, status, videoId, title, channel, durationSeconds
        );
    }

    private Stored map(ResultSet rs, int rowNum) throws SQLException {
        Timestamp checked = rs.getTimestamp("video_checked_at");
        return new Stored(
                rs.getString("tips"),
                rs.getString("video_status"),
                rs.getString("video_youtube_id"),
                rs.getString("video_title"),
                rs.getString("video_channel"),
                (Integer) rs.getObject("video_duration_seconds"),
                checked == null ? null : checked.toInstant()
        );
    }
}
