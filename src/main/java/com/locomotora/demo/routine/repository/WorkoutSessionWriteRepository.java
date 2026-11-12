package com.locomotora.demo.routine.repository;

import com.locomotora.demo.routine.dto.WorkoutSessionResponse;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso a datos de {@code POST /workout-sessions}: una sesion por envio, una fila por serie.
 */
@Repository
public class WorkoutSessionWriteRepository {
    private final JdbcTemplate jdbcTemplate;

    public WorkoutSessionWriteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UUID> findSessionIdByClientId(UUID userId, UUID clientSessionId) {
        return jdbcTemplate.query(
                "SELECT id FROM workout_sessions WHERE user_id = ? AND client_session_id = ?",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                userId,
                clientSessionId
        ).stream().findFirst();
    }

    /**
     * Titulo de una rutina del usuario. No filtra por {@code is_active}: una sesion que quedo
     * en la cola del telefono tiene que poder guardarse aunque la rutina se haya borrado despues.
     */
    public Optional<String> findOwnedRoutineTitle(UUID routineId, UUID userId) {
        return jdbcTemplate.query(
                "SELECT title FROM routines WHERE id = ? AND user_id = ?",
                (rs, rowNum) -> rs.getString("title"),
                routineId,
                userId
        ).stream().findFirst();
    }

    public Optional<RoutineDayRef> findRoutineDay(UUID routineDayId, UUID routineId) {
        return jdbcTemplate.query(
                "SELECT id, title FROM routine_days WHERE id = ? AND routine_id = ?",
                (rs, rowNum) -> new RoutineDayRef(rs.getObject("id", UUID.class), rs.getString("title")),
                routineDayId,
                routineId
        ).stream().findFirst();
    }

    public Optional<RoutineDayRef> findFirstRoutineDay(UUID routineId) {
        return jdbcTemplate.query(
                "SELECT id, title FROM routine_days WHERE routine_id = ? ORDER BY day_index LIMIT 1",
                (rs, rowNum) -> new RoutineDayRef(rs.getObject("id", UUID.class), rs.getString("title")),
                routineId
        ).stream().findFirst();
    }

    /** Ejercicios planificados de toda la rutina, por id de {@code routine_exercises}. */
    public Map<UUID, PlannedExercise> findPlannedExercises(UUID routineId) {
        Map<UUID, PlannedExercise> planned = new HashMap<>();
        jdbcTemplate.query(
                """
                SELECT re.id, re.exercise_id, re.reps_min, re.reps_max, re.target_reps,
                       re.target_weight_kg, re.rest_seconds
                FROM routine_exercises re
                JOIN routine_days rd ON rd.id = re.routine_day_id
                WHERE rd.routine_id = ?
                """,
                rs -> {
                    Integer targetReps = (Integer) rs.getObject("target_reps");
                    Integer repsMin = (Integer) rs.getObject("reps_min");
                    Integer repsMax = (Integer) rs.getObject("reps_max");
                    PlannedExercise exercise = new PlannedExercise(
                            rs.getObject("id", UUID.class),
                            rs.getObject("exercise_id", UUID.class),
                            repsMin == null ? targetReps : repsMin,
                            repsMax == null ? targetReps : repsMax,
                            rs.getBigDecimal("target_weight_kg"),
                            (Integer) rs.getObject("rest_seconds")
                    );
                    planned.put(exercise.id(), exercise);
                },
                routineId
        );
        return planned;
    }

    public Set<UUID> findExistingExerciseIds(Collection<UUID> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Set.of();
        }
        UUID[] ids = exerciseIds.toArray(UUID[]::new);
        return new HashSet<>(jdbcTemplate.query(
                connection -> {
                    var statement = connection.prepareStatement("SELECT id FROM exercises WHERE id = ANY (?)");
                    statement.setArray(1, connection.createArrayOf("uuid", ids));
                    return statement;
                },
                (rs, rowNum) -> rs.getObject("id", UUID.class)
        ));
    }

    /**
     * Inserta la sesion. Si otra peticion con el mismo {@code client_session_id} ya la guardo,
     * no inserta nada y devuelve vacio.
     */
    public Optional<UUID> insertSession(NewSession session) {
        return jdbcTemplate.query(
                """
                INSERT INTO workout_sessions
                    (user_id, client_session_id, kind, routine_id, routine_day_id, routine_title, day_title,
                     started_at, completed_at, status, perceived_exertion, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'COMPLETED', ?, ?)
                ON CONFLICT (user_id, client_session_id) DO NOTHING
                RETURNING id
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                session.userId(),
                session.clientSessionId(),
                session.kind(),
                session.routineId(),
                session.routineDayId(),
                session.routineTitle(),
                session.dayTitle(),
                session.startedAt(),
                session.completedAt(),
                session.perceivedExertion(),
                session.notes()
        ).stream().findFirst();
    }

    public void insertSetLogs(UUID sessionId, List<SetLog> sets) {
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO workout_exercise_logs
                    (workout_session_id, routine_exercise_id, exercise_id, exercise_position, set_number,
                     reps, weight_kg, duration_seconds, completed, notes,
                     target_reps_min, target_reps_max, target_weight_kg, rest_seconds)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                sets,
                sets.size(),
                (statement, set) -> {
                    statement.setObject(1, sessionId);
                    statement.setObject(2, set.routineExerciseId(), Types.OTHER);
                    statement.setObject(3, set.exerciseId());
                    statement.setInt(4, set.exercisePosition());
                    statement.setInt(5, set.setNumber());
                    statement.setObject(6, set.reps(), Types.INTEGER);
                    statement.setBigDecimal(7, set.weightKg());
                    statement.setObject(8, set.durationSeconds(), Types.INTEGER);
                    statement.setBoolean(9, set.completed());
                    statement.setString(10, set.notes());
                    statement.setObject(11, set.targetRepsMin(), Types.INTEGER);
                    statement.setObject(12, set.targetRepsMax(), Types.INTEGER);
                    statement.setBigDecimal(13, set.targetWeightKg());
                    statement.setObject(14, set.restSeconds(), Types.INTEGER);
                }
        );
    }

    public Optional<WorkoutSessionResponse> findSessionSummary(UUID sessionId, UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT ws.id, ws.client_session_id, ws.kind, ws.routine_id, ws.routine_day_id,
                       ws.routine_title, ws.day_title, ws.started_at, ws.completed_at,
                       count(wel.id) FILTER (WHERE wel.completed) AS completed_sets,
                       COALESCE(sum(wel.reps * wel.weight_kg) FILTER (WHERE wel.completed), 0) AS volume
                FROM workout_sessions ws
                LEFT JOIN workout_exercise_logs wel ON wel.workout_session_id = ws.id
                WHERE ws.id = ? AND ws.user_id = ?
                GROUP BY ws.id
                """,
                (rs, rowNum) -> {
                    UUID clientSessionId = rs.getObject("client_session_id", UUID.class);
                    UUID routineId = rs.getObject("routine_id", UUID.class);
                    UUID routineDayId = rs.getObject("routine_day_id", UUID.class);
                    Timestamp startedAt = rs.getTimestamp("started_at");
                    Timestamp completedAt = rs.getTimestamp("completed_at");
                    long duration = startedAt == null || completedAt == null
                            ? 0
                            : Math.max(0, (completedAt.getTime() - startedAt.getTime()) / 1000);
                    BigDecimal volume = rs.getBigDecimal("volume");
                    return new WorkoutSessionResponse(
                            rs.getObject("id", UUID.class).toString(),
                            clientSessionId == null ? null : clientSessionId.toString(),
                            rs.getString("kind"),
                            routineId == null ? null : routineId.toString(),
                            routineDayId == null ? null : routineDayId.toString(),
                            rs.getString("routine_title"),
                            rs.getString("day_title"),
                            startedAt == null ? null : startedAt.toInstant().toString(),
                            completedAt == null ? null : completedAt.toInstant().toString(),
                            duration,
                            rs.getInt("completed_sets"),
                            volume == null ? BigDecimal.ZERO : volume
                    );
                },
                sessionId,
                userId
        ).stream().findFirst();
    }

    public record RoutineDayRef(UUID id, String title) {
    }

    public record PlannedExercise(
            UUID id,
            UUID exerciseId,
            Integer targetRepsMin,
            Integer targetRepsMax,
            BigDecimal targetWeightKg,
            Integer restSeconds
    ) {
    }

    public record NewSession(
            UUID userId,
            UUID clientSessionId,
            String kind,
            UUID routineId,
            UUID routineDayId,
            String routineTitle,
            String dayTitle,
            Timestamp startedAt,
            Timestamp completedAt,
            Integer perceivedExertion,
            String notes
    ) {
    }

    public record SetLog(
            UUID routineExerciseId,
            UUID exerciseId,
            int exercisePosition,
            int setNumber,
            Integer reps,
            BigDecimal weightKg,
            Integer durationSeconds,
            boolean completed,
            String notes,
            Integer targetRepsMin,
            Integer targetRepsMax,
            BigDecimal targetWeightKg,
            Integer restSeconds
    ) {
    }
}
