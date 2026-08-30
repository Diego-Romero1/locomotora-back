package com.locomotora.demo.routine.repository;

import com.locomotora.demo.routine.dto.ExerciseRequest;
import com.locomotora.demo.routine.dto.RoutineRequest;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RoutineWriteRepository {
    private final JdbcTemplate jdbcTemplate;

    public RoutineWriteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID createRoutine(UUID userId, RoutineRequest request, String difficulty, UUID categoryId, boolean categoriesAvailable) {
        if (categoriesAvailable) {
            return jdbcTemplate.queryForObject(
                    """
                    INSERT INTO routines (user_id, title, description, objective, source, difficulty, estimated_duration_minutes, days_per_week, category_id)
                    VALUES (?, ?, ?, ?, 'MANUAL', ?, ?, ?, ?)
                    RETURNING id
                    """,
                    UUID.class,
                    userId,
                    request.title().trim(),
                    request.description(),
                    request.objective(),
                    difficulty,
                    request.estimatedDurationMinutes(),
                    request.daysPerWeek(),
                    categoryId
            );
        }

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO routines (user_id, title, description, objective, source, difficulty, estimated_duration_minutes, days_per_week)
                VALUES (?, ?, ?, ?, 'MANUAL', ?, ?, ?)
                RETURNING id
                """,
                UUID.class,
                userId,
                request.title().trim(),
                request.description(),
                request.objective(),
                difficulty,
                request.estimatedDurationMinutes(),
                request.daysPerWeek()
        );
    }

    public boolean isActiveGlobalTemplate(UUID routineId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM routines
                WHERE id = ? AND user_id IS NULL AND source = 'TEMPLATE' AND is_active = true
                """,
                Integer.class,
                routineId
        );
        return count != null && count > 0;
    }

    public int updateRoutine(UUID routineId, UUID userId, RoutineRequest request, String difficulty, UUID categoryId, boolean categoriesAvailable) {
        if (categoriesAvailable) {
            return jdbcTemplate.update(
                    """
                    UPDATE routines
                    SET title = ?, description = ?, objective = ?, difficulty = ?,
                        estimated_duration_minutes = ?, days_per_week = ?, category_id = ?, updated_at = now()
                    WHERE id = ? AND user_id = ? AND is_active = true
                    """,
                    request.title().trim(),
                    request.description(),
                    request.objective(),
                    difficulty,
                    request.estimatedDurationMinutes(),
                    request.daysPerWeek(),
                    categoryId,
                    routineId,
                    userId
            );
        }

        return jdbcTemplate.update(
                """
                UPDATE routines
                SET title = ?, description = ?, objective = ?, difficulty = ?,
                    estimated_duration_minutes = ?, days_per_week = ?, updated_at = now()
                WHERE id = ? AND user_id = ? AND is_active = true
                """,
                request.title().trim(),
                request.description(),
                request.objective(),
                difficulty,
                request.estimatedDurationMinutes(),
                request.daysPerWeek(),
                routineId,
                userId
        );
    }

    public int softDelete(UUID routineId, UUID userId) {
        return jdbcTemplate.update(
                "UPDATE routines SET is_active = false, updated_at = now() WHERE id = ? AND user_id = ?",
                routineId,
                userId
        );
    }

    public void deleteDays(UUID routineId) {
        jdbcTemplate.update("DELETE FROM routine_days WHERE routine_id = ?", routineId);
    }

    public UUID createDay(UUID routineId, int dayIndex, String title, String focus) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO routine_days (routine_id, day_index, title, focus) VALUES (?, ?, ?, ?) RETURNING id",
                UUID.class,
                routineId,
                dayIndex,
                title,
                focus
        );
    }

    public UUID findOrCreateExercise(ExerciseRequest exercise) {
        UUID requestedExerciseId = parseUuid(exercise.exerciseId());
        if (requestedExerciseId != null && exerciseExists(requestedExerciseId)) {
            return requestedExerciseId;
        }

        return jdbcTemplate.query(
                "SELECT id FROM exercises WHERE lower(name) = lower(?) ORDER BY created_at LIMIT 1",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                exercise.name().trim()
        ).stream().findFirst().orElseGet(() -> jdbcTemplate.queryForObject(
                """
                INSERT INTO exercises (name, primary_muscle_group, difficulty)
                VALUES (?, ?, ?)
                RETURNING id
                """,
                UUID.class,
                exercise.name().trim(),
                exercise.primaryMuscleGroup(),
                exercise.difficulty()
        ));
    }

    public void insertRoutineExercise(UUID routineDayId, UUID exerciseId, int position, ExerciseRequest exercise) {
        Integer targetReps = exercise.reps() != null
                ? exercise.reps()
                : (exercise.repsMax() != null ? exercise.repsMax() : exercise.repsMin());
        jdbcTemplate.update(
                """
                INSERT INTO routine_exercises
                    (routine_day_id, exercise_id, position, sets, reps_min, reps_max, target_reps, rest_seconds, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                routineDayId,
                exerciseId,
                position,
                exercise.sets(),
                exercise.repsMin(),
                exercise.repsMax(),
                targetReps,
                exercise.restSeconds(),
                exercise.notes()
        );
    }

    private boolean exerciseExists(UUID exerciseId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM exercises WHERE id = ? AND is_active = true",
                Integer.class,
                exerciseId
        );
        return count != null && count > 0;
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
