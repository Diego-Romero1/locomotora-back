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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
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
        ensureProfile(userId);
        UserAccount account = jdbcTemplate.queryForObject(
            "SELECT id, email, name, profile_photo_url FROM users WHERE id = ?",
                this::mapAccount,
                userId
        );
        return new MeResponse(
            account.id().toString(),
            account.name(),
            account.email(),
            account.profilePhotoUrl(),
            fetchProfile(userId),
            fetchGoals(userId),
            findPreference(userId),
            findLimitations(userId),
            findEquipment(userId)
        );
    }

    @GetMapping("/profile")
    public ProfileResponse profile() {
        UUID userId = CurrentUser.id();
        ensureProfile(userId);
        return fetchProfile(userId);
    }

    private ProfileResponse fetchProfile(UUID userId) {
        return jdbcTemplate.queryForObject(
                """
                  SELECT birth_date, sex, height_cm, weight_kg, activity_level, experience_level,
                      training_days_per_week, session_duration_minutes, onboarding_completed, medical_notes,
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
        String normalizedName = normalizeValue(request.name());
        if (normalizedName != null) {
            jdbcTemplate.update(
                    """
                    UPDATE users
                    SET name = ?, updated_at = now()
                    WHERE id = ?
                    """,
                    normalizedName,
                    userId
            );
        }
        jdbcTemplate.update(
                """
                UPDATE user_profiles
                SET birth_date = ?, sex = ?, height_cm = ?, weight_kg = ?, activity_level = ?, experience_level = ?,
                    training_days_per_week = ?, session_duration_minutes = ?, medical_notes = ?,
                    injuries = CAST(? AS jsonb), dietary_preferences = CAST(? AS jsonb), updated_at = now()
                WHERE user_id = ?
                """,
                request.birthDate(),
                request.sex(),
                request.heightCm(),
                request.weightKg(),
                request.activityLevel(),
                request.experienceLevel(),
                request.trainingDaysPerWeek(),
                request.sessionDurationMinutes(),
                request.medicalNotes(),
                request.injuriesJson() == null ? "[]" : request.injuriesJson(),
                request.dietaryPreferencesJson() == null ? "{}" : request.dietaryPreferencesJson(),
                userId
        );
        if (request.goalType() != null) {
            replaceGoalsForOnboarding(userId, request.goalType());
        }
        if (request.preferredTrainingStyle() != null) {
            replacePreference(userId, request.preferredTrainingStyle());
        }
        if (request.equipment() != null) {
            replaceEquipment(userId, request.equipment());
        }
        if (request.limitations() != null) {
            replaceLimitations(userId, request.limitations());
        }
        return profile();
    }

    @PutMapping("/profile/photo")
    public PhotoResponse updateProfilePhoto(@Valid @RequestBody PhotoRequest request) {
        UUID userId = CurrentUser.id();
        String profilePhotoUrl = normalizeValue(request.profilePhotoUrl());
        jdbcTemplate.update(
                """
                UPDATE users
                SET profile_photo_url = ?, updated_at = now()
                WHERE id = ?
                """,
                profilePhotoUrl,
                userId
        );
        return new PhotoResponse(profilePhotoUrl);
    }

    @DeleteMapping("/profile/photo")
    public PhotoResponse deleteProfilePhoto() {
        UUID userId = CurrentUser.id();
        jdbcTemplate.update(
                """
                UPDATE users
                SET profile_photo_url = NULL, updated_at = now()
                WHERE id = ?
                """,
                userId
        );
        return new PhotoResponse(null);
    }

    @GetMapping("/profile/onboarding")
    public OnboardingResponse onboarding() {
        UUID userId = CurrentUser.id();
        ensureProfile(userId);
        return buildOnboardingResponse(userId);
    }

    @PutMapping("/profile/onboarding")
    @Transactional
    public OnboardingResponse updateOnboarding(@Valid @RequestBody OnboardingRequest request) {
        UUID userId = CurrentUser.id();
        ensureProfile(userId);
        jdbcTemplate.update(
                """
                UPDATE user_profiles
                SET height_cm = ?, weight_kg = ?, experience_level = ?, training_days_per_week = ?,
                    session_duration_minutes = ?, onboarding_completed = true, updated_at = now()
                WHERE user_id = ?
                """,
                request.heightCm(),
                request.weightKg(),
                normalizeValue(request.experienceLevel()),
                request.trainingDaysPerWeek(),
                request.sessionDurationMinutes(),
                userId
        );
        replaceGoalsForOnboarding(userId, request.goalType());
        replacePreference(userId, request.preferredTrainingStyle());
        replaceEquipment(userId, request.equipment());
        replaceLimitations(userId, request.limitations());
        return buildOnboardingResponse(userId);
    }

    @GetMapping("/goals")
    public List<GoalResponse> goals() {
        return fetchGoals(CurrentUser.id());
    }

    private List<GoalResponse> fetchGoals(UUID userId) {
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
        return new UserAccount(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("profile_photo_url")
        );
    }

    private ProfileResponse mapProfile(ResultSet rs, int rowNum) throws SQLException {
        LocalDate birthDate = rs.getObject("birth_date", LocalDate.class);
        BigDecimal height = rs.getBigDecimal("height_cm");
        BigDecimal weight = rs.getBigDecimal("weight_kg");
        return new ProfileResponse(
                birthDate == null ? null : birthDate.toString(),
                rs.getString("sex"),
                height,
                weight,
                rs.getString("activity_level"),
                rs.getString("experience_level"),
                (Integer) rs.getObject("training_days_per_week"),
                (Integer) rs.getObject("session_duration_minutes"),
                rs.getBoolean("onboarding_completed"),
                rs.getString("medical_notes"),
                rs.getString("injuries"),
                rs.getString("dietary_preferences")
        );
    }

    private OnboardingResponse buildOnboardingResponse(UUID userId) {
        OnboardingProfileSnapshot profile = jdbcTemplate.queryForObject(
                """
                SELECT height_cm, weight_kg, experience_level, training_days_per_week,
                       session_duration_minutes, onboarding_completed
                FROM user_profiles
                WHERE user_id = ?
                """,
                (rs, rowNum) -> new OnboardingProfileSnapshot(
                        rs.getBigDecimal("height_cm"),
                        rs.getBigDecimal("weight_kg"),
                        rs.getString("experience_level"),
                        (Integer) rs.getObject("training_days_per_week"),
                        (Integer) rs.getObject("session_duration_minutes"),
                        rs.getBoolean("onboarding_completed")
                ),
                userId
        );
        return new OnboardingResponse(
                findPrimaryGoal(userId),
                profile.experienceLevel(),
                profile.trainingDaysPerWeek(),
                profile.sessionDurationMinutes(),
                profile.heightCm(),
                profile.weightKg(),
                findPreferredTrainingStyle(userId),
                findEquipment(userId),
                findLimitations(userId),
                profile.onboardingCompleted()
        );
    }

    private String findPrimaryGoal(UUID userId) {
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

    private String findPreferredTrainingStyle(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT preferred_training_style
                FROM user_preferences
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT 1
                """,
                rs -> rs.next() ? rs.getString("preferred_training_style") : null,
                userId
        );
    }

    private PreferenceResponse findPreference(UUID userId) {
        String preferredTrainingStyle = findPreferredTrainingStyle(userId);
        if (preferredTrainingStyle == null) {
            return null;
        }
        return new PreferenceResponse(preferredTrainingStyle);
    }

    private List<String> findEquipment(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT equipment_name
                FROM user_equipment
                WHERE user_id = ?
                ORDER BY created_at, equipment_name
                """,
                (rs, rowNum) -> rs.getString("equipment_name"),
                userId
        );
    }

    private List<OnboardingResponse.LimitationItem> findLimitations(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT limitation_type, notes
                FROM user_limitations
                WHERE user_id = ?
                ORDER BY created_at, limitation_type
                """,
                (rs, rowNum) -> new OnboardingResponse.LimitationItem(
                        rs.getString("limitation_type"),
                        rs.getString("notes")
                ),
                userId
        );
    }

    private void replaceGoalsForOnboarding(UUID userId, String goalType) {
        jdbcTemplate.update("DELETE FROM user_goals WHERE user_id = ?", userId);
        String normalizedGoalType = normalizeValue(goalType);
        if (!StringUtils.hasText(normalizedGoalType)) {
            return;
        }
        jdbcTemplate.update(
                """
                INSERT INTO user_goals (user_id, goal_type, priority, status)
                VALUES (?, ?, 1, 'ACTIVE')
                """,
                userId,
                normalizedGoalType
        );
    }

    private void replacePreference(UUID userId, String preferredTrainingStyle) {
        jdbcTemplate.update("DELETE FROM user_preferences WHERE user_id = ?", userId);
        String normalizedTrainingStyle = normalizeValue(preferredTrainingStyle);
        if (!StringUtils.hasText(normalizedTrainingStyle)) {
            return;
        }
        jdbcTemplate.update(
                """
                INSERT INTO user_preferences (user_id, preferred_training_style)
                VALUES (?, ?)
                """,
                userId,
                normalizedTrainingStyle
        );
    }

    private void replaceEquipment(UUID userId, List<String> equipment) {
        jdbcTemplate.update("DELETE FROM user_equipment WHERE user_id = ?", userId);
        if (equipment == null) {
            return;
        }
        for (String entry : equipment) {
            String normalizedEquipment = normalizeValue(entry);
            if (!StringUtils.hasText(normalizedEquipment)) {
                continue;
            }
            jdbcTemplate.update(
                    """
                    INSERT INTO user_equipment (user_id, equipment_name)
                    VALUES (?, ?)
                    """,
                    userId,
                    normalizedEquipment
            );
        }
    }

    private void replaceLimitations(UUID userId, List<OnboardingRequest.LimitationItem> limitations) {
        jdbcTemplate.update("DELETE FROM user_limitations WHERE user_id = ?", userId);
        if (limitations == null) {
            return;
        }
        for (OnboardingRequest.LimitationItem limitation : limitations) {
            if (limitation == null) {
                continue;
            }
            String limitationType = normalizeValue(limitation.limitationType());
            if (!StringUtils.hasText(limitationType)) {
                continue;
            }
            jdbcTemplate.update(
                    """
                    INSERT INTO user_limitations (user_id, limitation_type, notes)
                    VALUES (?, ?, ?)
                    """,
                    userId,
                    limitationType,
                    normalizeValue(limitation.notes())
            );
        }
    }

    private String normalizeValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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

    public record MeResponse(
            String id,
            String name,
            String email,
            String profilePhotoUrl,
            ProfileResponse profile,
            List<GoalResponse> goals,
            PreferenceResponse preferences,
            List<OnboardingResponse.LimitationItem> limitations,
            List<String> equipment
    ) {
    }

    public record ProfileRequest(
            String name,
            LocalDate birthDate,
            String sex,
            BigDecimal heightCm,
            BigDecimal weightKg,
            String activityLevel,
            String experienceLevel,
            Integer trainingDaysPerWeek,
            Integer sessionDurationMinutes,
            String medicalNotes,
            String injuriesJson,
            String dietaryPreferencesJson,
            String goalType,
            String preferredTrainingStyle,
            List<String> equipment,
            List<OnboardingRequest.LimitationItem> limitations
    ) {
    }

    public record ProfileResponse(
            String birthDate,
            String sex,
            BigDecimal heightCm,
            BigDecimal weightKg,
            String activityLevel,
            String experienceLevel,
            Integer trainingDaysPerWeek,
            Integer sessionDurationMinutes,
            boolean onboardingCompleted,
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

    public record PreferenceResponse(String preferredTrainingStyle) {
    }

    public record PhotoRequest(String profilePhotoUrl) {
    }

    public record PhotoResponse(String profilePhotoUrl) {
    }

    private record UserAccount(UUID id, String name, String email, String profilePhotoUrl) {
    }

    private record OnboardingProfileSnapshot(
            BigDecimal heightCm,
            BigDecimal weightKg,
            String experienceLevel,
            Integer trainingDaysPerWeek,
            Integer sessionDurationMinutes,
            boolean onboardingCompleted
    ) {
    }
}
