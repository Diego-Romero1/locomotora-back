package com.locomotora.demo.progress.repository;

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
}
