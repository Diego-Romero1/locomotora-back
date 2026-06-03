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

    public int countRoutineDays(UUID routineId, UUID userId) {
        Integer dayCount = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM routine_days rd
                JOIN routines r ON r.id = rd.routine_id
                WHERE r.id = ? AND r.user_id = ? AND r.is_active = true
                """,
                Integer.class,
                routineId,
                userId
        );
        return dayCount == null ? 0 : dayCount;
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

    public UUID createDefaultDay(UUID routineId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO routine_days (routine_id, day_index, title, focus) VALUES (?, 1, 'Dia 1', 'General') RETURNING id",
                UUID.class,
                routineId
        );
    }

    public UUID findOrCreateExercise(ExerciseRequest exercise, String exerciseDifficulty) {
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
                exerciseDifficulty
        ));
    }

    public void insertRoutineExercise(UUID routineDayId, UUID exerciseId, int position, ExerciseRequest exercise) {
        jdbcTemplate.update(
                """
                INSERT INTO routine_exercises
                    (routine_day_id, exercise_id, position, sets, target_reps, rest_seconds, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                routineDayId,
                exerciseId,
                position,
                exercise.sets(),
                exercise.reps(),
                exercise.restSeconds(),
                exercise.notes()
        );
    }
}
