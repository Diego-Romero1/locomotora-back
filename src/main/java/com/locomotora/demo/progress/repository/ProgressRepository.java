package com.locomotora.demo.progress.repository;

import com.locomotora.demo.progress.dto.ExerciseActivityResponse;
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
                "SELECT count(*) FROM workout_sessions WHERE user_id = ? AND status = 'COMPLETED'",
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
                WHERE user_id = ? AND status = 'COMPLETED' AND completed_at >= now() - interval '30 days'
                """,
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public List<ExerciseActivityResponse> findRecentExerciseActivity(UUID userId) {
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
                WHERE ws.user_id = ?
                  AND ws.status = 'COMPLETED'
                  AND wel.completed = true
                ORDER BY COALESCE(ws.completed_at, wel.created_at) DESC, wel.created_at DESC
                LIMIT 20
                """,
                this::mapExerciseActivity,
                userId
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
