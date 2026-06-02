package com.locomotora.demo.user.repository;

import com.locomotora.demo.user.dto.GoalRequest;
import com.locomotora.demo.user.dto.GoalResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GoalRepository {
    private final JdbcTemplate jdbcTemplate;

    public GoalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GoalResponse> findByUserId(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id, goal_type, target_value, target_unit, target_date, priority, status, notes
                FROM user_goals
                WHERE user_id = ?
                ORDER BY priority, created_at DESC
                """,
                this::mapGoal,
                userId
        );
    }

    public String findPrimaryGoal(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT goal_type
                FROM user_goals
                WHERE user_id = ?
                ORDER BY priority, created_at DESC
                LIMIT 1
                """,
                rs -> rs.next() ? rs.getString("goal_type") : null,
                userId
        );
    }

    public void deleteByUserId(UUID userId) {
        jdbcTemplate.update("DELETE FROM user_goals WHERE user_id = ?", userId);
    }

    public void insert(UUID userId, GoalRequest goal) {
        jdbcTemplate.update(
                """
                INSERT INTO user_goals (user_id, goal_type, target_value, target_unit, target_date, priority, status, notes)
                VALUES (?, ?, ?, ?, ?, ?, COALESCE(?, 'ACTIVE'), ?)
                """,
                userId,
                goal.goalType(),
                goal.targetValue(),
                goal.targetUnit(),
                goal.targetDate(),
                goal.priority() == null ? 1 : goal.priority(),
                goal.status(),
                goal.notes()
        );
    }

    public void insertPrimaryActive(UUID userId, String goalType) {
        jdbcTemplate.update(
                """
                INSERT INTO user_goals (user_id, goal_type, priority, status)
                VALUES (?, ?, 1, 'ACTIVE')
                """,
                userId,
                goalType
        );
    }

    private GoalResponse mapGoal(ResultSet rs, int rowNum) throws SQLException {
        LocalDate targetDate = rs.getObject("target_date", LocalDate.class);
        return new GoalResponse(
                rs.getObject("id", UUID.class).toString(),
                rs.getString("goal_type"),
                rs.getBigDecimal("target_value"),
                rs.getString("target_unit"),
                targetDate == null ? null : targetDate.toString(),
                rs.getInt("priority"),
                rs.getString("status"),
                rs.getString("notes")
        );
    }
}
