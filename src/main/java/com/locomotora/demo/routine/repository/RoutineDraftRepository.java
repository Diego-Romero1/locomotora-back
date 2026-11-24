package com.locomotora.demo.routine.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Borradores de rutinas de cada usuario. El payload es el JSON del borrador de la app, sin interpretar. */
@Repository
public class RoutineDraftRepository {
    private final JdbcTemplate jdbcTemplate;

    public RoutineDraftRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record DraftRow(
            UUID id,
            UUID userId,
            String kind,
            UUID routineId,
            String title,
            int step,
            int dayCount,
            int exerciseCount,
            String payload,
            Instant updatedAt
    ) {
    }

    private static final String COLUMNS =
            "d.id, d.user_id, d.kind, d.routine_id, d.title, d.step, d.day_count, d.exercise_count, d.payload::text AS payload, d.client_updated_at";

    /**
     * Borradores del usuario, el mas nuevo primero. Los de editar una rutina que ya no esta activa
     * (borrada o archivada) no se listan.
     */
    public List<DraftRow> findByUser(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT %s
                FROM routine_drafts d
                LEFT JOIN routines r ON r.id = d.routine_id
                WHERE d.user_id = ? AND (d.routine_id IS NULL OR r.is_active)
                ORDER BY d.client_updated_at DESC
                """.formatted(COLUMNS),
                this::map,
                userId
        );
    }

    public Optional<DraftRow> findById(UUID id) {
        return jdbcTemplate.query("SELECT %s FROM routine_drafts d WHERE d.id = ?".formatted(COLUMNS), this::map, id)
                .stream()
                .findFirst();
    }

    public int countByUser(UUID userId) {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM routine_drafts WHERE user_id = ?", Integer.class, userId);
        return count == null ? 0 : count;
    }

    public boolean isActiveRoutineOf(UUID routineId, UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM routines WHERE id = ? AND user_id = ? AND is_active",
                Integer.class,
                routineId,
                userId);
        return count != null && count > 0;
    }

    /**
     * Crea o actualiza el borrador si el que llega es igual o mas nuevo que el guardado (y es del mismo
     * usuario). Devuelve si se escribio.
     */
    public boolean upsert(DraftRow draft) {
        int changed = jdbcTemplate.update(
                """
                INSERT INTO routine_drafts
                    (id, user_id, kind, routine_id, title, step, day_count, exercise_count, payload, client_updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
                ON CONFLICT (id) DO UPDATE SET
                    kind = EXCLUDED.kind,
                    routine_id = EXCLUDED.routine_id,
                    title = EXCLUDED.title,
                    step = EXCLUDED.step,
                    day_count = EXCLUDED.day_count,
                    exercise_count = EXCLUDED.exercise_count,
                    payload = EXCLUDED.payload,
                    client_updated_at = EXCLUDED.client_updated_at,
                    updated_at = now()
                WHERE routine_drafts.user_id = EXCLUDED.user_id
                  AND EXCLUDED.client_updated_at >= routine_drafts.client_updated_at
                """,
                draft.id(),
                draft.userId(),
                draft.kind(),
                draft.routineId(),
                draft.title(),
                draft.step(),
                draft.dayCount(),
                draft.exerciseCount(),
                draft.payload(),
                Timestamp.from(draft.updatedAt())
        );
        return changed > 0;
    }

    public boolean delete(UUID id, UUID userId) {
        return jdbcTemplate.update("DELETE FROM routine_drafts WHERE id = ? AND user_id = ?", id, userId) > 0;
    }

    private DraftRow map(ResultSet rs, int rowNum) throws SQLException {
        return new DraftRow(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("kind"),
                rs.getObject("routine_id", UUID.class),
                rs.getString("title"),
                rs.getInt("step"),
                rs.getInt("day_count"),
                rs.getInt("exercise_count"),
                rs.getString("payload"),
                rs.getTimestamp("client_updated_at").toInstant()
        );
    }
}
