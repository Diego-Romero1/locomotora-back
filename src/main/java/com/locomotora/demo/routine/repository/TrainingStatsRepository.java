package com.locomotora.demo.routine.repository;

import com.locomotora.demo.routine.dto.LastPerformanceResponse.BestSet;
import com.locomotora.demo.routine.dto.LastPerformanceResponse.LastSession;
import com.locomotora.demo.routine.dto.LastPerformanceResponse.SetValue;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Lecturas sobre el historial de entrenamientos para Hoy y para la sesion en curso. */
@Repository
public class TrainingStatsRepository {
    private final JdbcTemplate jdbcTemplate;

    public TrainingStatsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Series de la ultima sesion en la que aparece cada ejercicio. */
    public Map<UUID, LastSession> findLastSessions(UUID userId, Collection<UUID> exerciseIds) {
        Map<UUID, LastSession> result = new LinkedHashMap<>();
        Map<UUID, List<SetValue>> sets = new HashMap<>();
        jdbcTemplate.query(
                connection -> {
                    var statement = connection.prepareStatement(
                            """
                            WITH last AS (
                                SELECT DISTINCT ON (wel.exercise_id)
                                       wel.exercise_id, ws.id AS session_id, ws.completed_at
                                FROM workout_exercise_logs wel
                                JOIN workout_sessions ws ON ws.id = wel.workout_session_id
                                WHERE ws.user_id = ? AND ws.status = 'COMPLETED' AND wel.completed
                                  AND wel.exercise_id = ANY (?)
                                ORDER BY wel.exercise_id, ws.completed_at DESC
                            )
                            SELECT l.exercise_id, l.session_id, l.completed_at,
                                   wel.set_number, wel.reps, wel.weight_kg
                            FROM last l
                            JOIN workout_exercise_logs wel
                              ON wel.workout_session_id = l.session_id
                             AND wel.exercise_id = l.exercise_id
                             AND wel.completed
                            ORDER BY l.exercise_id, wel.exercise_position NULLS FIRST, wel.set_number
                            """);
                    statement.setObject(1, userId);
                    statement.setArray(2, connection.createArrayOf("uuid", exerciseIds.toArray()));
                    return statement;
                },
                rs -> {
                    UUID exerciseId = rs.getObject("exercise_id", UUID.class);
                    List<SetValue> values = sets.computeIfAbsent(exerciseId, key -> new ArrayList<>());
                    if (!result.containsKey(exerciseId)) {
                        result.put(exerciseId, new LastSession(
                                rs.getObject("session_id", UUID.class).toString(),
                                rs.getTimestamp("completed_at").toInstant().toString(),
                                values
                        ));
                    }
                    values.add(new SetValue(
                            values.size() + 1,
                            (Integer) rs.getObject("reps"),
                            rs.getBigDecimal("weight_kg")
                    ));
                }
        );
        return result;
    }

    /** Mejor serie historica por fuerza estimada (Epley). */
    public Map<UUID, BestSet> findBestSets(UUID userId, Collection<UUID> exerciseIds) {
        Map<UUID, BestSet> result = new HashMap<>();
        jdbcTemplate.query(
                connection -> {
                    var statement = connection.prepareStatement(
                            """
                            SELECT DISTINCT ON (exercise_id) exercise_id, reps, weight_kg, completed_at, e1rm
                            FROM (
                                SELECT wel.exercise_id, wel.reps, wel.weight_kg, ws.completed_at,
                                       CASE WHEN wel.reps = 1 THEN wel.weight_kg
                                            ELSE wel.weight_kg * (1 + wel.reps / 30.0) END AS e1rm
                                FROM workout_exercise_logs wel
                                JOIN workout_sessions ws ON ws.id = wel.workout_session_id
                                WHERE ws.user_id = ? AND ws.status = 'COMPLETED' AND wel.completed
                                  AND wel.weight_kg > 0 AND wel.reps > 0
                                  AND wel.exercise_id = ANY (?)
                            ) candidates
                            ORDER BY exercise_id, e1rm DESC, completed_at
                            """);
                    statement.setObject(1, userId);
                    statement.setArray(2, connection.createArrayOf("uuid", exerciseIds.toArray()));
                    return statement;
                },
                rs -> {
                    result.put(rs.getObject("exercise_id", UUID.class), new BestSet(
                            (Integer) rs.getObject("reps"),
                            rs.getBigDecimal("weight_kg"),
                            rs.getBigDecimal("e1rm").setScale(1, java.math.RoundingMode.HALF_UP),
                            rs.getTimestamp("completed_at").toInstant().toString()
                    ));
                }
        );
        return result;
    }

    public record RoutineCandidate(UUID routineId, Instant at) {
    }

    /** Rutina activa del ultimo entrenamiento con rutina. */
    public Optional<RoutineCandidate> findLastTrainedActiveRoutine(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT ws.routine_id, ws.completed_at
                FROM workout_sessions ws
                JOIN routines r ON r.id = ws.routine_id AND r.user_id = ws.user_id AND r.is_active
                WHERE ws.user_id = ? AND ws.status = 'COMPLETED' AND ws.kind = 'ROUTINE'
                ORDER BY ws.completed_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new RoutineCandidate(
                        rs.getObject("routine_id", UUID.class),
                        rs.getTimestamp("completed_at").toInstant()
                ),
                userId
        ).stream().findFirst();
    }

    /** Rutina activa creada o editada mas recientemente. */
    public Optional<RoutineCandidate> findLatestUpdatedActiveRoutine(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id, updated_at FROM routines
                WHERE user_id = ? AND is_active
                ORDER BY updated_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new RoutineCandidate(
                        rs.getObject("id", UUID.class),
                        rs.getTimestamp("updated_at").toInstant()
                ),
                userId
        ).stream().findFirst();
    }

    /** Dia del ultimo entrenamiento de esa rutina (puede no existir o haberse borrado). */
    public Optional<UUID> findLastTrainedDayId(UUID userId, UUID routineId) {
        return jdbcTemplate.query(
                """
                SELECT routine_day_id FROM workout_sessions
                WHERE user_id = ? AND routine_id = ? AND status = 'COMPLETED' AND routine_day_id IS NOT NULL
                ORDER BY completed_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getObject("routine_day_id", UUID.class),
                userId,
                routineId
        ).stream().findFirst();
    }

    public Optional<UUID> findLastSessionId(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id FROM workout_sessions
                WHERE user_id = ? AND status = 'COMPLETED' AND kind <> 'QUICK_LOG'
                ORDER BY completed_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                userId
        ).stream().findFirst();
    }

    /** Fechas locales con al menos un entrenamiento (sin registros sueltos) desde {@code since}. */
    public List<LocalDate> findTrainedDates(UUID userId, String zoneId, Instant since) {
        return jdbcTemplate.query(
                """
                SELECT DISTINCT (completed_at AT TIME ZONE ?)::date AS day
                FROM workout_sessions
                WHERE user_id = ? AND status = 'COMPLETED' AND kind <> 'QUICK_LOG' AND completed_at >= ?
                ORDER BY day
                """,
                (rs, rowNum) -> rs.getObject("day", LocalDate.class),
                zoneId,
                userId,
                Timestamp.from(since)
        );
    }

    public Optional<Integer> findProfileTrainingDays(UUID userId) {
        return jdbcTemplate.query(
                "SELECT training_days_per_week FROM user_profiles WHERE user_id = ?",
                (rs, rowNum) -> (Integer) rs.getObject("training_days_per_week"),
                userId
        ).stream().filter(java.util.Objects::nonNull).findFirst();
    }
}
