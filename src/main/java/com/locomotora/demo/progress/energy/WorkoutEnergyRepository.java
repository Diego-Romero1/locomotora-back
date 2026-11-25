package com.locomotora.demo.progress.energy;

import static com.locomotora.demo.routine.repository.WorkoutHistoryRepository.E1RM;
import static com.locomotora.demo.routine.repository.WorkoutHistoryRepository.MEASURABLE;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Datos para calcular las kcal de una sesion y donde se guardan. */
@Repository
public class WorkoutEnergyRepository {
    private final JdbcTemplate jdbcTemplate;

    public WorkoutEnergyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** {@code bodyWeightKg}: peso guardado de un calculo anterior que no fue estimado (null si no hay). */
    public record SessionData(UUID userId, Instant startedAt, Instant completedAt, BigDecimal bodyWeightKg) {
    }

    public record LogData(
            UUID id,
            UUID exerciseId,
            int position,
            Integer reps,
            BigDecimal weightKg,
            Integer durationSeconds,
            Instant completedAt,
            String name,
            String nameEn,
            String primaryMuscleGroup,
            String mechanic,
            String equipment,
            String exerciseType
    ) {
    }

    public record ProfileData(String sex, LocalDate birthDate, BigDecimal heightCm, BigDecimal weightKg) {
    }

    public Optional<SessionData> findSession(UUID sessionId) {
        return jdbcTemplate.query(
                """
                SELECT user_id, started_at, completed_at,
                       CASE WHEN body_weight_estimated THEN NULL ELSE body_weight_kg END AS body_weight_kg
                FROM workout_sessions
                WHERE id = ? AND status = 'COMPLETED'
                """,
                (rs, rowNum) -> new SessionData(
                        rs.getObject("user_id", UUID.class),
                        instant(rs.getTimestamp("started_at")),
                        instant(rs.getTimestamp("completed_at")),
                        rs.getBigDecimal("body_weight_kg")
                ),
                sessionId
        ).stream().findFirst();
    }

    /** Series hechas de la sesion, en el orden en que se muestran. */
    public List<LogData> findCompletedLogs(UUID sessionId) {
        return jdbcTemplate.query(
                """
                SELECT wel.id, wel.exercise_id, COALESCE(wel.exercise_position, 0) AS position, wel.reps, wel.weight_kg,
                       wel.duration_seconds, wel.completed_at, e.name, e.name_en, e.primary_muscle_group,
                       e.mechanic, e.equipment, e.exercise_type
                FROM workout_exercise_logs wel
                JOIN exercises e ON e.id = wel.exercise_id
                WHERE wel.workout_session_id = ? AND wel.completed
                ORDER BY COALESCE(wel.exercise_position, 0), wel.created_at, wel.set_number
                """,
                (rs, rowNum) -> new LogData(
                        rs.getObject("id", UUID.class),
                        rs.getObject("exercise_id", UUID.class),
                        rs.getInt("position"),
                        (Integer) rs.getObject("reps"),
                        rs.getBigDecimal("weight_kg"),
                        (Integer) rs.getObject("duration_seconds"),
                        instant(rs.getTimestamp("completed_at")),
                        rs.getString("name"),
                        rs.getString("name_en"),
                        rs.getString("primary_muscle_group"),
                        rs.getString("mechanic"),
                        rs.getString("equipment"),
                        rs.getString("exercise_type")
                ),
                sessionId
        );
    }

    public ProfileData findProfile(UUID userId) {
        return jdbcTemplate.query(
                "SELECT sex, birth_date, height_cm, weight_kg FROM user_profiles WHERE user_id = ?",
                (rs, rowNum) -> new ProfileData(
                        rs.getString("sex"),
                        rs.getObject("birth_date", LocalDate.class),
                        rs.getBigDecimal("height_cm"),
                        rs.getBigDecimal("weight_kg")
                ),
                userId
        ).stream().findFirst().orElse(new ProfileData(null, null, null, null));
    }

    /** Ultimo peso medido hasta {@code at}; si no hay, el primero medido despues. */
    public Optional<BigDecimal> findBodyWeightAt(UUID userId, Instant at) {
        return jdbcTemplate.query(
                """
                SELECT weight_kg FROM body_metrics
                WHERE user_id = ? AND weight_kg > 0
                ORDER BY (measured_at <= ?) DESC,
                         CASE WHEN measured_at <= ? THEN measured_at END DESC,
                         measured_at
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getBigDecimal("weight_kg"),
                userId,
                Timestamp.from(at),
                Timestamp.from(at)
        ).stream().findFirst();
    }

    /** Mejor fuerza estimada de cada ejercicio de la sesion en las sesiones de {@code since} a antes de esta. */
    public Map<UUID, BigDecimal> findPriorBest(UUID userId, UUID sessionId, Instant before, Instant since) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        jdbcTemplate.query(
                """
                SELECT wel.exercise_id, max(%1$s) AS best
                FROM workout_exercise_logs wel
                JOIN workout_sessions ps ON ps.id = wel.workout_session_id
                WHERE ps.user_id = ? AND ps.status = 'COMPLETED' AND ps.id <> ? AND %2$s
                  AND ps.completed_at < ? AND ps.completed_at >= ?
                  AND wel.exercise_id IN (SELECT exercise_id FROM workout_exercise_logs WHERE workout_session_id = ?)
                GROUP BY wel.exercise_id
                """.formatted(E1RM, MEASURABLE),
                rs -> {
                    result.put(rs.getObject("exercise_id", UUID.class), rs.getBigDecimal("best"));
                },
                userId,
                sessionId,
                Timestamp.from(before),
                Timestamp.from(since),
                sessionId
        );
        return result;
    }

    public record LogEnergy(UUID logId, BigDecimal kcal) {
    }

    public record SessionEnergyRow(
            BigDecimal kcal,
            BigDecimal workKcal,
            BigDecimal bodyWeightKg,
            boolean bodyWeightEstimated,
            boolean personalized,
            int modelVersion
    ) {
    }

    public void saveEnergy(UUID sessionId, List<LogEnergy> logs, SessionEnergyRow session) {
        jdbcTemplate.update("UPDATE workout_exercise_logs SET energy_kcal = NULL WHERE workout_session_id = ?", sessionId);
        jdbcTemplate.batchUpdate(
                "UPDATE workout_exercise_logs SET energy_kcal = ? WHERE id = ?",
                logs,
                logs.size(),
                (statement, log) -> {
                    statement.setBigDecimal(1, log.kcal());
                    statement.setObject(2, log.logId());
                }
        );
        jdbcTemplate.update(
                """
                UPDATE workout_sessions
                SET energy_kcal = ?, energy_work_kcal = ?, body_weight_kg = ?, body_weight_estimated = ?,
                    energy_personalized = ?, energy_model_version = ?
                WHERE id = ?
                """,
                session.kcal(),
                session.workKcal(),
                session.bodyWeightKg(),
                session.bodyWeightEstimated(),
                session.personalized(),
                session.modelVersion(),
                sessionId
        );
    }

    /** Sesiones sin calcular o calculadas con otro modelo (sin registros sueltos). */
    public List<UUID> findPendingSessionIds(int modelVersion, int limit) {
        return jdbcTemplate.query(
                """
                SELECT id FROM workout_sessions
                WHERE status = 'COMPLETED' AND kind <> 'QUICK_LOG'
                  AND energy_model_version IS DISTINCT FROM ?
                ORDER BY completed_at
                LIMIT ?
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                modelVersion,
                limit
        );
    }

    /** Sesiones del usuario calculadas sin su peso o sin su perfil completo. */
    public List<UUID> findIncompleteSessionIds(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id FROM workout_sessions
                WHERE user_id = ? AND status = 'COMPLETED' AND kind <> 'QUICK_LOG'
                  AND (body_weight_estimated IS NOT FALSE OR energy_personalized IS NOT TRUE)
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                userId
        );
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
