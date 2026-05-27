package com.locomotora.demo.user;

import com.locomotora.demo.common.CurrentUser;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {
    private final JdbcTemplate jdbcTemplate;

    public ProfileController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/me")
    public MeResponse me() {
        UUID userId = CurrentUser.id();
        UserAccount account = jdbcTemplate.queryForObject(
                "SELECT id, email, name FROM users WHERE id = ?",
                this::mapAccount,
                userId
        );
        ProfileResponse profile = profile();
        return new MeResponse(account.id().toString(), account.name(), account.email(), profile);
    }

    @GetMapping("/profile")
    public ProfileResponse profile() {
        UUID userId = CurrentUser.id();
        ensureProfile(userId);
        return jdbcTemplate.queryForObject(
                """
                SELECT birth_date, sex, height_cm, activity_level, experience_level,
                       training_days_per_week, session_duration_minutes, medical_notes,
                       injuries::text AS injuries, dietary_preferences::text AS dietary_preferences
                FROM user_profiles
                WHERE user_id = ?
                """,
                this::mapProfile,
                userId
        );
    }

    @PutMapping("/profile")
    @Transactional
    public ProfileResponse updateProfile(@Valid @RequestBody ProfileRequest request) {
        UUID userId = CurrentUser.id();
        ensureProfile(userId);
        jdbcTemplate.update(
                """
                UPDATE user_profiles
                SET birth_date = ?, sex = ?, height_cm = ?, activity_level = ?, experience_level = ?,
                    training_days_per_week = ?, session_duration_minutes = ?, medical_notes = ?,
                    injuries = CAST(? AS jsonb), dietary_preferences = CAST(? AS jsonb), updated_at = now()
                WHERE user_id = ?
                """,
                request.birthDate(),
                request.sex(),
                request.heightCm(),
                request.activityLevel(),
                request.experienceLevel(),
                request.trainingDaysPerWeek(),
                request.sessionDurationMinutes(),
                request.medicalNotes(),
                request.injuriesJson() == null ? "[]" : request.injuriesJson(),
                request.dietaryPreferencesJson() == null ? "{}" : request.dietaryPreferencesJson(),
                userId
        );
        return profile();
    }

    @GetMapping("/goals")
    public List<GoalResponse> goals() {
        return jdbcTemplate.query(
                """
                SELECT id, goal_type, target_value, target_unit, target_date, priority, status, notes
                FROM user_goals
                WHERE user_id = ?
                ORDER BY priority, created_at DESC
                """,
                this::mapGoal,
                CurrentUser.id()
        );
    }

    @PutMapping("/goals")
    @Transactional
    public List<GoalResponse> replaceGoals(@RequestBody List<GoalRequest> goals) {
        UUID userId = CurrentUser.id();
        jdbcTemplate.update("DELETE FROM user_goals WHERE user_id = ?", userId);
        for (GoalRequest goal : goals) {
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
        return goals();
    }

    private void ensureProfile(UUID userId) {
        jdbcTemplate.update(
                """
                INSERT INTO user_profiles (user_id)
                VALUES (?)
                ON CONFLICT (user_id) DO NOTHING
                """,
                userId
        );
    }

    private UserAccount mapAccount(ResultSet rs, int rowNum) throws SQLException {
        return new UserAccount(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("email"));
    }

    private ProfileResponse mapProfile(ResultSet rs, int rowNum) throws SQLException {
        LocalDate birthDate = rs.getObject("birth_date", LocalDate.class);
        BigDecimal height = rs.getBigDecimal("height_cm");
        return new ProfileResponse(
                birthDate == null ? null : birthDate.toString(),
                rs.getString("sex"),
                height,
                rs.getString("activity_level"),
                rs.getString("experience_level"),
                (Integer) rs.getObject("training_days_per_week"),
                (Integer) rs.getObject("session_duration_minutes"),
                rs.getString("medical_notes"),
                rs.getString("injuries"),
                rs.getString("dietary_preferences")
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

    public record MeResponse(String id, String name, String email, ProfileResponse profile) {
    }

    public record ProfileRequest(
            LocalDate birthDate,
            String sex,
            BigDecimal heightCm,
            String activityLevel,
            String experienceLevel,
            Integer trainingDaysPerWeek,
            Integer sessionDurationMinutes,
            String medicalNotes,
            String injuriesJson,
            String dietaryPreferencesJson
    ) {
    }

    public record ProfileResponse(
            String birthDate,
            String sex,
            BigDecimal heightCm,
            String activityLevel,
            String experienceLevel,
            Integer trainingDaysPerWeek,
            Integer sessionDurationMinutes,
            String medicalNotes,
            String injuriesJson,
            String dietaryPreferencesJson
    ) {
    }

    public record GoalRequest(
            String goalType,
            BigDecimal targetValue,
            String targetUnit,
            LocalDate targetDate,
            Integer priority,
            String status,
            String notes
    ) {
    }

    public record GoalResponse(String id, String goalType, BigDecimal targetValue, String targetUnit,
                               String targetDate, int priority, String status, String notes) {
    }

    private record UserAccount(UUID id, String name, String email) {
    }
}
