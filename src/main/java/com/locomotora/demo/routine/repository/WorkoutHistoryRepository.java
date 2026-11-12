package com.locomotora.demo.routine.repository;

import com.locomotora.demo.routine.dto.HistoryDtos.ExercisePoint;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionListItem;
import com.locomotora.demo.routine.dto.HistoryDtos.TrainedExercise;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Lecturas y correcciones del historial de entrenamientos. */
@Repository
public class WorkoutHistoryRepository {
    /** Fuerza estimada de una serie (Epley; con 1 rep, el peso). Igual que en la app. */
    static final String E1RM = "(CASE WHEN wel.reps = 1 THEN wel.weight_kg ELSE wel.weight_kg * (1 + wel.reps / 30.0) END)";
    /** Series que cuentan para records y fuerza estimada. */
    static final String MEASURABLE = "wel.completed AND wel.weight_kg > 0 AND wel.reps > 0";

    private static final String SESSION_COLUMNS = """
            ws.id, ws.kind, ws.routine_id,
            COALESCE(ws.routine_title, r.title) AS routine_title,
            COALESCE(ws.day_title, rd.title) AS day_title,
            ws.started_at, ws.completed_at, ws.perceived_exertion, ws.notes,
            (SELECT count(*) FROM workout_exercise_logs wel
              WHERE wel.workout_session_id = ws.id AND wel.completed) AS completed_sets,
            (SELECT count(DISTINCT (wel.exercise_id, COALESCE(wel.exercise_position, 0))) FROM workout_exercise_logs wel
              WHERE wel.workout_session_id = ws.id AND wel.completed) AS exercise_count,
            (SELECT COALESCE(sum(wel.reps * wel.weight_kg), 0) FROM workout_exercise_logs wel
              WHERE wel.workout_session_id = ws.id AND wel.completed) AS volume
            """;

    private final JdbcTemplate jdbcTemplate;

    public WorkoutHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record SessionRow(SessionListItem item, String notes, Instant completedAt) {
    }

    /** Pagina de sesiones (sin registros sueltos), de la mas nueva a la mas vieja. */
    public List<SessionRow> findSessions(UUID userId, Instant beforeAt, UUID beforeId, int limit) {
        String cursorFilter = beforeAt == null ? "" : "AND (ws.completed_at, ws.id) < (?, ?)";
        Object[] args = beforeAt == null
                ? new Object[] {userId, limit}
                : new Object[] {userId, Timestamp.from(beforeAt), beforeId, limit};
        return jdbcTemplate.query(
                "SELECT " + SESSION_COLUMNS + """
                FROM workout_sessions ws
                LEFT JOIN routines r ON r.id = ws.routine_id
                LEFT JOIN routine_days rd ON rd.id = ws.routine_day_id
                WHERE ws.user_id = ? AND ws.status = 'COMPLETED' AND ws.kind <> 'QUICK_LOG'
                """ + cursorFilter + """
                 ORDER BY ws.completed_at DESC, ws.id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> mapSession(rs, 0),
                args
        );
    }

    public Optional<SessionRow> findSession(UUID sessionId, UUID userId) {
        return jdbcTemplate.query(
                "SELECT " + SESSION_COLUMNS + """
                FROM workout_sessions ws
                LEFT JOIN routines r ON r.id = ws.routine_id
                LEFT JOIN routine_days rd ON rd.id = ws.routine_day_id
                WHERE ws.id = ? AND ws.user_id = ? AND ws.status = 'COMPLETED'
                """,
                (rs, rowNum) -> mapSession(rs, 0),
                sessionId,
                userId
        ).stream().findFirst();
    }

    /**
     * Cuantos ejercicios de cada sesion superaron su mejor fuerza estimada previa.
     * La primera vez que se hace un ejercicio no cuenta como record.
     */
    public Map<UUID, Integer> countRecords(UUID userId, Collection<UUID> sessionIds) {
        Map<UUID, Integer> result = new HashMap<>();
        if (sessionIds.isEmpty()) {
            return result;
        }
        jdbcTemplate.query(
                connection -> {
                    var statement = connection.prepareStatement(
                            """
                            SELECT b.session_id, count(*) AS records
                            FROM (
                                SELECT wel.workout_session_id AS session_id, wel.exercise_id, ws.completed_at,
                                       max(%1$s) AS best
                                FROM workout_exercise_logs wel
                                JOIN workout_sessions ws ON ws.id = wel.workout_session_id
                                WHERE ws.id = ANY (?) AND %2$s
                                GROUP BY wel.workout_session_id, wel.exercise_id, ws.completed_at
                            ) b
                            JOIN LATERAL (
                                SELECT max(%1$s) AS prior
                                FROM workout_exercise_logs wel
                                JOIN workout_sessions ps ON ps.id = wel.workout_session_id
                                WHERE ps.user_id = ? AND ps.status = 'COMPLETED' AND wel.exercise_id = b.exercise_id
                                  AND %2$s AND ps.completed_at < b.completed_at
                            ) p ON true
                            WHERE p.prior IS NOT NULL AND b.best > p.prior + 0.05
                            GROUP BY b.session_id
                            """.formatted(E1RM, MEASURABLE));
                    statement.setArray(1, connection.createArrayOf("uuid", sessionIds.toArray()));
                    statement.setObject(2, userId);
                    return statement;
                },
                rs -> {
                    result.put(rs.getObject("session_id", UUID.class), rs.getInt("records"));
                }
        );
        return result;
    }

    public record LogRow(
            UUID id,
            UUID exerciseId,
            String exerciseName,
            String primaryMuscleGroup,
            UUID routineExerciseId,
            int position,
            int setNumber,
            Integer reps,
            BigDecimal weightKg,
            boolean completed,
            String notes,
            Integer targetRepsMin,
            Integer targetRepsMax,
            BigDecimal targetWeightKg,
            Integer restSeconds
    ) {
    }

    public List<LogRow> findLogs(UUID sessionId) {
        return jdbcTemplate.query(
                """
                SELECT wel.id, wel.exercise_id, e.name, e.primary_muscle_group, wel.routine_exercise_id,
                       wel.exercise_position, wel.set_number, wel.reps, wel.weight_kg, wel.completed, wel.notes,
                       wel.target_reps_min, wel.target_reps_max, wel.target_weight_kg, wel.rest_seconds
                FROM workout_exercise_logs wel
                JOIN exercises e ON e.id = wel.exercise_id
                WHERE wel.workout_session_id = ?
                ORDER BY COALESCE(wel.exercise_position, 0), wel.created_at, wel.set_number
                """,
                (rs, rowNum) -> new LogRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("exercise_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("primary_muscle_group"),
                        rs.getObject("routine_exercise_id", UUID.class),
                        rs.getInt("exercise_position"),
                        rs.getInt("set_number"),
                        (Integer) rs.getObject("reps"),
                        rs.getBigDecimal("weight_kg"),
                        rs.getBoolean("completed"),
                        rs.getString("notes"),
                        (Integer) rs.getObject("target_reps_min"),
                        (Integer) rs.getObject("target_reps_max"),
                        rs.getBigDecimal("target_weight_kg"),
                        (Integer) rs.getObject("rest_seconds")
                ),
                sessionId
        );
    }

    /** Mejor fuerza estimada de cada ejercicio en las sesiones anteriores a esta. */
    public Map<UUID, BigDecimal> findPriorBest(UUID userId, UUID sessionId) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        jdbcTemplate.query(
                """
                SELECT wel.exercise_id, max(%1$s) AS best
                FROM workout_exercise_logs wel
                JOIN workout_sessions ps ON ps.id = wel.workout_session_id
                WHERE ps.user_id = ? AND ps.status = 'COMPLETED' AND %2$s
                  AND ps.completed_at < (SELECT completed_at FROM workout_sessions WHERE id = ?)
                  AND wel.exercise_id IN (SELECT exercise_id FROM workout_exercise_logs WHERE workout_session_id = ?)
                GROUP BY wel.exercise_id
                """.formatted(E1RM, MEASURABLE),
                rs -> {
                    result.put(rs.getObject("exercise_id", UUID.class), rs.getBigDecimal("best"));
                },
                userId,
                sessionId,
                sessionId
        );
        return result;
    }

    public void updateSessionInfo(UUID sessionId, Integer perceivedExertion, String notes) {
        jdbcTemplate.update(
                "UPDATE workout_sessions SET perceived_exertion = ?, notes = ?, updated_at = now() WHERE id = ?",
                perceivedExertion,
                notes,
                sessionId
        );
    }

    /** Actualiza una serie de la sesion. Devuelve false si la serie no es de esa sesion. */
    public boolean updateSet(UUID sessionId, UUID setId, Integer reps, BigDecimal weightKg) {
        return jdbcTemplate.update(
                "UPDATE workout_exercise_logs SET reps = ?, weight_kg = ? WHERE id = ? AND workout_session_id = ?",
                reps,
                weightKg,
                setId,
                sessionId
        ) == 1;
    }

    public boolean deleteSet(UUID sessionId, UUID setId) {
        return jdbcTemplate.update(
                "DELETE FROM workout_exercise_logs WHERE id = ? AND workout_session_id = ?",
                setId,
                sessionId
        ) == 1;
    }

    public int countCompletedSets(UUID sessionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM workout_exercise_logs WHERE workout_session_id = ? AND completed",
                Integer.class,
                sessionId
        );
        return count == null ? 0 : count;
    }

    public boolean deleteSession(UUID sessionId, UUID userId) {
        return jdbcTemplate.update("DELETE FROM workout_sessions WHERE id = ? AND user_id = ?", sessionId, userId) == 1;
    }

    /** Ejercicios con al menos una serie hecha, del ultimo entrenado al mas viejo. */
    public List<TrainedExercise> findTrainedExercises(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT e.id, e.name, e.primary_muscle_group,
                       count(DISTINCT ws.id) AS sessions,
                       max(ws.completed_at) AS last_completed_at,
                       max(%1$s) FILTER (WHERE %2$s) AS best
                FROM workout_exercise_logs wel
                JOIN workout_sessions ws ON ws.id = wel.workout_session_id
                JOIN exercises e ON e.id = wel.exercise_id
                WHERE ws.user_id = ? AND ws.status = 'COMPLETED' AND wel.completed
                GROUP BY e.id, e.name, e.primary_muscle_group
                ORDER BY max(ws.completed_at) DESC
                """.formatted(E1RM, MEASURABLE),
                (rs, rowNum) -> {
                    BigDecimal best = rs.getBigDecimal("best");
                    return new TrainedExercise(
                            rs.getObject("id", UUID.class).toString(),
                            rs.getString("name"),
                            rs.getString("primary_muscle_group"),
                            rs.getInt("sessions"),
                            rs.getTimestamp("last_completed_at").toInstant().toString(),
                            best == null ? null : best.setScale(1, java.math.RoundingMode.HALF_UP)
                    );
                },
                userId
        );
    }

    public record ExerciseInfo(UUID id, String name, String primaryMuscleGroup) {
    }

    public Optional<ExerciseInfo> findExercise(UUID exerciseId) {
        return jdbcTemplate.query(
                "SELECT id, name, primary_muscle_group FROM exercises WHERE id = ?",
                (rs, rowNum) -> new ExerciseInfo(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("primary_muscle_group")
                ),
                exerciseId
        ).stream().findFirst();
    }

    /**
     * Las ultimas {@code limit} sesiones con este ejercicio, de la mas vieja a la mas nueva.
     * Por sesion: la serie de mayor fuerza estimada y el volumen del ejercicio.
     */
    public List<ExercisePoint> findExercisePoints(UUID userId, UUID exerciseId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT * FROM (
                    SELECT DISTINCT ON (ws.id)
                           ws.id AS session_id, ws.completed_at,
                           wel.weight_kg AS best_weight, wel.reps AS best_reps,
                           CASE WHEN %2$s THEN %1$s END AS e1rm,
                           sum(wel.reps * wel.weight_kg) FILTER (WHERE wel.completed) OVER (PARTITION BY ws.id) AS volume,
                           count(*) FILTER (WHERE wel.completed) OVER (PARTITION BY ws.id) AS set_count
                    FROM workout_exercise_logs wel
                    JOIN workout_sessions ws ON ws.id = wel.workout_session_id
                    WHERE ws.user_id = ? AND ws.status = 'COMPLETED' AND wel.exercise_id = ? AND wel.completed
                    ORDER BY ws.id, (CASE WHEN %2$s THEN %1$s END) DESC NULLS LAST, wel.reps DESC NULLS LAST
                ) per_session
                ORDER BY completed_at DESC
                LIMIT ?
                """.formatted(E1RM, MEASURABLE),
                (rs, rowNum) -> {
                    BigDecimal e1rm = rs.getBigDecimal("e1rm");
                    BigDecimal volume = rs.getBigDecimal("volume");
                    return new ExercisePoint(
                            rs.getObject("session_id", UUID.class).toString(),
                            rs.getTimestamp("completed_at").toInstant().toString(),
                            rs.getBigDecimal("best_weight"),
                            (Integer) rs.getObject("best_reps"),
                            e1rm == null ? null : e1rm.setScale(1, java.math.RoundingMode.HALF_UP),
                            volume == null ? BigDecimal.ZERO : volume,
                            rs.getInt("set_count")
                    );
                },
                userId,
                exerciseId,
                limit
        ).reversed();
    }

    /** Records de todo el historial del ejercicio: mejor fuerza estimada, mas peso y mas volumen en una sesion. */
    public com.locomotora.demo.routine.dto.HistoryDtos.ExerciseRecords findExerciseRecords(UUID userId, UUID exerciseId) {
        var bestE1rm = jdbcTemplate.query(
                """
                SELECT %1$s AS value, wel.weight_kg, wel.reps, ws.completed_at, ws.id
                FROM workout_exercise_logs wel
                JOIN workout_sessions ws ON ws.id = wel.workout_session_id
                WHERE ws.user_id = ? AND ws.status = 'COMPLETED' AND wel.exercise_id = ? AND %2$s
                ORDER BY value DESC, ws.completed_at
                LIMIT 1
                """.formatted(E1RM, MEASURABLE),
                (rs, rowNum) -> mapRecord(rs, 1),
                userId,
                exerciseId
        ).stream().findFirst().orElse(null);
        var heaviest = jdbcTemplate.query(
                """
                SELECT wel.weight_kg AS value, wel.weight_kg, wel.reps, ws.completed_at, ws.id
                FROM workout_exercise_logs wel
                JOIN workout_sessions ws ON ws.id = wel.workout_session_id
                WHERE ws.user_id = ? AND ws.status = 'COMPLETED' AND wel.exercise_id = ? AND %s
                ORDER BY wel.weight_kg DESC, wel.reps DESC, ws.completed_at
                LIMIT 1
                """.formatted(MEASURABLE),
                (rs, rowNum) -> mapRecord(rs, 2),
                userId,
                exerciseId
        ).stream().findFirst().orElse(null);
        var mostVolume = jdbcTemplate.query(
                """
                SELECT sum(wel.reps * wel.weight_kg) AS value, NULL::numeric AS weight_kg, NULL::int AS reps,
                       ws.completed_at, ws.id
                FROM workout_exercise_logs wel
                JOIN workout_sessions ws ON ws.id = wel.workout_session_id
                WHERE ws.user_id = ? AND ws.status = 'COMPLETED' AND wel.exercise_id = ? AND %s
                GROUP BY ws.id, ws.completed_at
                ORDER BY value DESC, ws.completed_at
                LIMIT 1
                """.formatted(MEASURABLE),
                (rs, rowNum) -> mapRecord(rs, 2),
                userId,
                exerciseId
        ).stream().findFirst().orElse(null);
        return new com.locomotora.demo.routine.dto.HistoryDtos.ExerciseRecords(bestE1rm, heaviest, mostVolume);
    }

    private com.locomotora.demo.routine.dto.HistoryDtos.RecordValue mapRecord(ResultSet rs, int scale) throws SQLException {
        return new com.locomotora.demo.routine.dto.HistoryDtos.RecordValue(
                rs.getBigDecimal("value").setScale(scale, java.math.RoundingMode.HALF_UP),
                rs.getBigDecimal("weight_kg"),
                (Integer) rs.getObject("reps"),
                rs.getTimestamp("completed_at").toInstant().toString(),
                rs.getObject("id", UUID.class).toString()
        );
    }

    public record WeekRow(LocalDate weekStart, int sessions, int trainedDays, int completedSets, BigDecimal volume) {
    }

    /** Totales por semana (lunes a domingo en {@code zoneId}) desde {@code since}, sin registros sueltos. */
    public List<WeekRow> findWeeklyTotals(UUID userId, String zoneId, Instant since) {
        return jdbcTemplate.query(
                """
                SELECT date_trunc('week', ws.completed_at AT TIME ZONE ?)::date AS week_start,
                       count(DISTINCT ws.id) AS sessions,
                       count(DISTINCT (ws.completed_at AT TIME ZONE ?)::date) AS trained_days,
                       count(wel.id) FILTER (WHERE wel.completed) AS completed_sets,
                       COALESCE(sum(wel.reps * wel.weight_kg) FILTER (WHERE wel.completed), 0) AS volume
                FROM workout_sessions ws
                LEFT JOIN workout_exercise_logs wel ON wel.workout_session_id = ws.id
                WHERE ws.user_id = ? AND ws.status = 'COMPLETED' AND ws.kind <> 'QUICK_LOG' AND ws.completed_at >= ?
                GROUP BY 1
                ORDER BY 1
                """,
                (rs, rowNum) -> new WeekRow(
                        rs.getObject("week_start", LocalDate.class),
                        rs.getInt("sessions"),
                        rs.getInt("trained_days"),
                        rs.getInt("completed_sets"),
                        rs.getBigDecimal("volume")
                ),
                zoneId,
                zoneId,
                userId,
                Timestamp.from(since)
        );
    }

    private SessionRow mapSession(ResultSet rs, int recordCount) throws SQLException {
        UUID routineId = rs.getObject("routine_id", UUID.class);
        Timestamp startedAt = rs.getTimestamp("started_at");
        Timestamp completedAt = rs.getTimestamp("completed_at");
        long duration = startedAt == null || completedAt == null
                ? 0
                : Math.max(0, (completedAt.getTime() - startedAt.getTime()) / 1000);
        SessionListItem item = new SessionListItem(
                rs.getObject("id", UUID.class).toString(),
                rs.getString("kind"),
                routineId == null ? null : routineId.toString(),
                rs.getString("routine_title"),
                rs.getString("day_title"),
                startedAt == null ? null : startedAt.toInstant().toString(),
                completedAt == null ? null : completedAt.toInstant().toString(),
                duration,
                rs.getInt("completed_sets"),
                rs.getInt("exercise_count"),
                rs.getBigDecimal("volume"),
                (Integer) rs.getObject("perceived_exertion"),
                recordCount
        );
        return new SessionRow(item, rs.getString("notes"), completedAt == null ? null : completedAt.toInstant());
    }
}
