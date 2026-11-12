package com.locomotora.demo.progress.repository;

import com.locomotora.demo.progress.dto.ExerciseActivityResponse;
import com.locomotora.demo.progress.dto.WorkoutActivityResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProgressRepository {
    private final JdbcTemplate jdbcTemplate;

    public ProgressRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countCompletedWorkouts(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM workout_sessions WHERE user_id = ? AND status = 'COMPLETED' AND kind <> 'QUICK_LOG'",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public int countActiveRoutines(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM routines WHERE user_id = ? AND is_active = true",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public int countActiveDaysLast30(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(DISTINCT completed_at::date)
                FROM workout_sessions
                WHERE user_id = ? AND status = 'COMPLETED' AND kind <> 'QUICK_LOG'
                  AND completed_at >= now() - interval '30 days'
                """,
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public List<WorkoutActivityResponse> findRecentWorkoutActivity(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT ws.id,
                       ws.routine_id,
                       COALESCE(r.title, ws.routine_title, 'Rutina') AS routine_title,
                       ws.routine_day_id,
                       COALESCE(rd.title, ws.day_title) AS routine_day_title,
                       ws.completed_at
                FROM workout_sessions ws
                LEFT JOIN routines r ON r.id = ws.routine_id
                LEFT JOIN routine_days rd ON rd.id = ws.routine_day_id
                WHERE ws.user_id = ?
                  AND ws.status = 'COMPLETED'
                ORDER BY ws.completed_at DESC
                LIMIT 10
                """,
                (rs, rowNum) -> {
                    UUID sessionId = rs.getObject("id", UUID.class);
                    UUID routineId = rs.getObject("routine_id", UUID.class);
                    UUID routineDayId = rs.getObject("routine_day_id", UUID.class);
                    return new WorkoutActivityResponse(
                            sessionId.toString(),
                            routineId == null ? null : routineId.toString(),
                            rs.getString("routine_title"),
                            routineDayId == null ? null : routineDayId.toString(),
                            rs.getString("routine_day_title"),
                            rs.getTimestamp("completed_at").toInstant().toString(),
                            findExercisesForSession(sessionId)
                    );
                },
                userId
        );
    }

    private List<ExerciseActivityResponse> findExercisesForSession(UUID sessionId) {
        return jdbcTemplate.query(
                """
                SELECT wel.id,
                       wel.exercise_id,
                       e.name AS exercise_name,
                       e.primary_muscle_group,
                       wel.reps,
                       wel.weight_kg,
                       COALESCE(ws.completed_at, wel.created_at) AS completed_at
                FROM workout_exercise_logs wel
                JOIN workout_sessions ws ON ws.id = wel.workout_session_id
                JOIN exercises e ON e.id = wel.exercise_id
                WHERE ws.id = ?
                  AND wel.completed = true
                ORDER BY COALESCE(wel.exercise_position, 0), wel.created_at, wel.set_number, e.name
                """,
                this::mapExerciseActivity,
                sessionId
        );
    }

    private ExerciseActivityResponse mapExerciseActivity(ResultSet rs, int rowNum) throws SQLException {
        return new ExerciseActivityResponse(
                rs.getObject("id", UUID.class).toString(),
                rs.getObject("exercise_id", UUID.class).toString(),
                rs.getString("exercise_name"),
                rs.getString("primary_muscle_group"),
                (Integer) rs.getObject("reps"),
                rs.getBigDecimal("weight_kg"),
                rs.getTimestamp("completed_at").toInstant().toString()
        );
    }
}
