package com.locomotora.demo.routine;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoutineController {
    private final JdbcTemplate jdbcTemplate;

    public RoutineController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/routine")
    public RoutineResponse todayRoutine() {
        UUID userId = CurrentUser.id();
        UUID routineId = jdbcTemplate.query(
                """
                SELECT id FROM routines
                WHERE user_id = ? AND is_active = true
                ORDER BY updated_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                userId
        ).stream().findFirst().orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No active routine found"));
        return getRoutine(routineId);
    }

    @GetMapping("/routines")
    public List<RoutineResponse> routines() {
        UUID userId = CurrentUser.id();
        List<UUID> ids = jdbcTemplate.query(
                """
                SELECT id FROM routines
                WHERE user_id = ? AND is_active = true
                ORDER BY updated_at DESC
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                userId
        );
        return ids.stream().map(this::getRoutine).toList();
    }

    @GetMapping("/routines/{id}")
    public RoutineResponse getRoutine(@PathVariable UUID id) {
        UUID userId = CurrentUser.id();
        RoutineHeader header = jdbcTemplate.query(
                """
                SELECT id, title, description, objective, difficulty, estimated_duration_minutes, days_per_week
                FROM routines
                WHERE id = ? AND user_id = ? AND is_active = true
                """,
                this::mapHeader,
                id,
                userId
        ).stream().findFirst().orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Routine not found"));
        return new RoutineResponse(
                header.id().toString(),
                header.title(),
                header.description(),
                header.objective(),
                header.difficulty(),
                header.estimatedDurationMinutes(),
                header.daysPerWeek(),
                exercisesForRoutine(header.id())
        );
    }

    @PostMapping("/routines")
    @Transactional
    public RoutineResponse createRoutine(@Valid @RequestBody RoutineRequest request) {
        UUID userId = CurrentUser.id();
        UUID routineId = jdbcTemplate.queryForObject(
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
                request.difficulty(),
                request.estimatedDurationMinutes(),
                request.daysPerWeek()
        );
        replaceExercises(routineId, request.exercises());
        return getRoutine(routineId);
    }

    @PutMapping("/routines/{id}")
    @Transactional
    public RoutineResponse updateRoutine(@PathVariable UUID id, @Valid @RequestBody RoutineRequest request) {
        UUID userId = CurrentUser.id();
        int updated = jdbcTemplate.update(
                """
                UPDATE routines
                SET title = ?, description = ?, objective = ?, difficulty = ?,
                    estimated_duration_minutes = ?, days_per_week = ?, updated_at = now()
                WHERE id = ? AND user_id = ? AND is_active = true
                """,
                request.title().trim(),
                request.description(),
                request.objective(),
                request.difficulty(),
                request.estimatedDurationMinutes(),
                request.daysPerWeek(),
                id,
                userId
        );
        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Routine not found");
        }
        replaceExercises(id, request.exercises());
        return getRoutine(id);
    }

    @DeleteMapping("/routines/{id}")
    public void deleteRoutine(@PathVariable UUID id) {
        UUID userId = CurrentUser.id();
        int updated = jdbcTemplate.update(
                "UPDATE routines SET is_active = false, updated_at = now() WHERE id = ? AND user_id = ?",
                id,
                userId
        );
        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Routine not found");
        }
    }

    @PostMapping("/workout-log")
    @Transactional
    public WorkoutLogResponse logWorkout(@Valid @RequestBody WorkoutLogRequest request) {
        UUID userId = CurrentUser.id();
        UUID routineId = UUID.fromString(request.routineId());
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM routines WHERE id = ? AND user_id = ? AND is_active = true",
                Integer.class,
                routineId,
                userId
        );
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Routine not found");
        }

        UUID dayId = jdbcTemplate.query(
                "SELECT id FROM routine_days WHERE routine_id = ? ORDER BY day_index LIMIT 1",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                routineId
        ).stream().findFirst().orElse(null);

        Instant completedAt = request.completedAt() == null ? Instant.now() : Instant.parse(request.completedAt());
        UUID sessionId = jdbcTemplate.queryForObject(
                """
                INSERT INTO workout_sessions (user_id, routine_id, routine_day_id, started_at, completed_at, status)
                VALUES (?, ?, ?, ?, ?, 'COMPLETED')
                RETURNING id
                """,
                UUID.class,
                userId,
                routineId,
                dayId,
                Timestamp.from(completedAt),
                Timestamp.from(completedAt)
        );

        for (String completedId : request.completedExerciseIds()) {
            UUID routineExerciseId = UUID.fromString(completedId);
            ExerciseLookup exercise = jdbcTemplate.query(
                    """
                    SELECT re.exercise_id, re.target_reps, re.target_weight_kg
                    FROM routine_exercises re
                    JOIN routine_days rd ON rd.id = re.routine_day_id
                    WHERE re.id = ? AND rd.routine_id = ?
                    """,
                    this::mapExerciseLookup,
                    routineExerciseId,
                    routineId
            ).stream().findFirst().orElse(null);
            if (exercise != null) {
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
        }

        return new WorkoutLogResponse(sessionId.toString(), request.completedExerciseIds().size(), completedAt.toString());
    }

    private void replaceExercises(UUID routineId, List<ExerciseRequest> exercises) {
        jdbcTemplate.update("DELETE FROM routine_days WHERE routine_id = ?", routineId);
        UUID routineDayId = jdbcTemplate.queryForObject(
                "INSERT INTO routine_days (routine_id, day_index, title, focus) VALUES (?, 1, 'Dia 1', 'General') RETURNING id",
                UUID.class,
                routineId
        );
        int position = 1;
        for (ExerciseRequest exercise : exercises) {
            UUID exerciseId = jdbcTemplate.queryForObject(
                    """
                    INSERT INTO exercises (name, primary_muscle_group, difficulty)
                    VALUES (?, ?, ?)
                    RETURNING id
                    """,
                    UUID.class,
                    exercise.name().trim(),
                    exercise.primaryMuscleGroup(),
                    exercise.difficulty()
            );
            jdbcTemplate.update(
                    """
                    INSERT INTO routine_exercises
                        (routine_day_id, exercise_id, position, sets, target_reps, rest_seconds, notes)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    routineDayId,
                    exerciseId,
                    position++,
                    exercise.sets(),
                    exercise.reps(),
                    exercise.restSeconds(),
                    exercise.notes()
            );
        }
    }

    private List<ExerciseResponse> exercisesForRoutine(UUID routineId) {
        return jdbcTemplate.query(
                """
                SELECT re.id, e.name, re.sets, re.target_reps, re.rest_seconds, re.notes
                FROM routine_exercises re
                JOIN exercises e ON e.id = re.exercise_id
                JOIN routine_days rd ON rd.id = re.routine_day_id
                WHERE rd.routine_id = ?
                ORDER BY rd.day_index, re.position
                """,
                this::mapExercise,
                routineId
        );
    }

    private RoutineHeader mapHeader(ResultSet rs, int rowNum) throws SQLException {
        return new RoutineHeader(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("objective"),
                rs.getString("difficulty"),
                (Integer) rs.getObject("estimated_duration_minutes"),
                (Integer) rs.getObject("days_per_week")
        );
    }

    private ExerciseResponse mapExercise(ResultSet rs, int rowNum) throws SQLException {
        return new ExerciseResponse(
                rs.getObject("id", UUID.class).toString(),
                rs.getString("name"),
                rs.getInt("sets"),
                rs.getInt("target_reps"),
                rs.getInt("rest_seconds"),
                rs.getString("notes")
        );
    }

    private ExerciseLookup mapExerciseLookup(ResultSet rs, int rowNum) throws SQLException {
        return new ExerciseLookup(
                rs.getObject("exercise_id", UUID.class),
                (Integer) rs.getObject("target_reps"),
                rs.getBigDecimal("target_weight_kg")
        );
    }

    public record RoutineRequest(
            @NotBlank String title,
            String description,
            String objective,
            String difficulty,
            Integer estimatedDurationMinutes,
            Integer daysPerWeek,
            @NotEmpty List<@Valid ExerciseRequest> exercises
    ) {
    }

    public record ExerciseRequest(
            String id,
            @NotBlank String name,
            @Min(1) int sets,
            @Min(1) int reps,
            @Min(0) int restSeconds,
            String primaryMuscleGroup,
            String difficulty,
            String notes
    ) {
    }

    public record RoutineResponse(
            String id,
            String title,
            String description,
            String objective,
            String difficulty,
            Integer estimatedDurationMinutes,
            Integer daysPerWeek,
            List<ExerciseResponse> exercises
    ) {
    }

    public record ExerciseResponse(String id, String name, int sets, int reps, int restSeconds, String notes) {
    }

    public record WorkoutLogRequest(
            @NotBlank String routineId,
            List<String> completedExerciseIds,
            String completedAt
    ) {
        public WorkoutLogRequest {
            if (completedExerciseIds == null) {
                completedExerciseIds = new ArrayList<>();
            }
        }
    }

    public record WorkoutLogResponse(String sessionId, int completedExercises, String completedAt) {
    }

    private record RoutineHeader(UUID id, String title, String description, String objective, String difficulty,
                                 Integer estimatedDurationMinutes, Integer daysPerWeek) {
    }

    private record ExerciseLookup(UUID exerciseId, Integer targetReps, java.math.BigDecimal targetWeightKg) {
    }
}
