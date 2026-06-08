package com.locomotora.demo.routine.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WorkoutSessionRepository {
    private final JdbcTemplate jdbcTemplate;

    public WorkoutSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean activeRoutineExists(UUID routineId, UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM routines WHERE id = ? AND user_id = ? AND is_active = true",
                Integer.class,
                routineId,
                userId
        );
        return count != null && count > 0;
    }

    public UUID findFirstRoutineDayId(UUID routineId) {
        return jdbcTemplate.query(
                "SELECT id FROM routine_days WHERE routine_id = ? ORDER BY day_index LIMIT 1",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                routineId
        ).stream().findFirst().orElse(null);
    }

    public UUID createCompletedSession(UUID userId, UUID routineId, UUID routineDayId, Timestamp completedAt) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO workout_sessions (user_id, routine_id, routine_day_id, started_at, completed_at, status)
                VALUES (?, ?, ?, ?, ?, 'COMPLETED')
                RETURNING id
                """,
                UUID.class,
                userId,
                routineId,
                routineDayId,
                completedAt,
                completedAt
        );
    }

    public Optional<ExerciseLookup> findExerciseForRoutineLog(UUID routineExerciseId, UUID routineId) {
        return jdbcTemplate.query(
                """
                SELECT re.exercise_id, re.target_reps, re.target_weight_kg
                FROM routine_exercises re
                JOIN routine_days rd ON rd.id = re.routine_day_id
                WHERE re.id = ? AND rd.routine_id = ?
                """,
                (rs, rowNum) -> new ExerciseLookup(
                        rs.getObject("exercise_id", UUID.class),
                        (Integer) rs.getObject("target_reps"),
                        rs.getBigDecimal("target_weight_kg")
                ),
                routineExerciseId,
                routineId
        ).stream().findFirst();
    }

    public boolean activeExerciseExists(UUID exerciseId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM exercises WHERE id = ? AND is_active = true",
                Integer.class,
                exerciseId
        );
        return count != null && count > 0;
    }

    public void insertExerciseLog(UUID sessionId, UUID routineExerciseId, ExerciseLookup exercise) {
        jdbcTemplate.update(
                """
                INSERT INTO workout_exercise_logs
                    (workout_session_id, routine_exercise_id, exercise_id, set_number, reps, weight_kg, completed)
                VALUES (?, ?, ?, 1, ?, ?, true)
                """,
                sessionId,
                routineExerciseId,
                exercise.exerciseId(),
                exercise.targetReps(),
                exercise.targetWeightKg()
        );
    }

    public void insertStandaloneExerciseLog(UUID sessionId, UUID exerciseId, Integer reps, BigDecimal weightKg) {
        jdbcTemplate.update(
                """
                INSERT INTO workout_exercise_logs
                    (workout_session_id, routine_exercise_id, exercise_id, set_number, reps, weight_kg, completed)
                VALUES (?, NULL, ?, 1, ?, ?, true)
                """,
                sessionId,
                exerciseId,
                reps,
                weightKg
        );
    }

    public record ExerciseLookup(UUID exerciseId, Integer targetReps, BigDecimal targetWeightKg) {
    }
}
