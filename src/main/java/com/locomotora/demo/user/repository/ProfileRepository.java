package com.locomotora.demo.user.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locomotora.demo.common.JsonCodes;
import com.locomotora.demo.user.dto.OnboardingResponse;
import com.locomotora.demo.user.dto.ProfileRequest;
import com.locomotora.demo.user.dto.ProfileResponse;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {
    private final JdbcTemplate jdbcTemplate;

    public ProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureProfile(UUID userId) {
        jdbcTemplate.update(
                """
                INSERT INTO user_profiles (user_id)
                VALUES (?)
                ON CONFLICT (user_id) DO NOTHING
                """,
                userId
        );
    }

    public UserAccount findAccount(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT id, email, name, profile_photo_url FROM users WHERE id = ?",
                this::mapAccount,
                userId
        );
    }

    public ProfileResponse findProfile(UUID userId) {
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

    public void updateName(UUID userId, String name) {
        jdbcTemplate.update(
                """
                UPDATE users
                SET name = ?, updated_at = now()
                WHERE id = ?
                """,
                name,
                userId
        );
    }

    public void updateProfile(UUID userId, ProfileRequest request) {
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
    }

    public void updateProfilePhoto(UUID userId, String profilePhotoUrl) {
        jdbcTemplate.update(
                """
                UPDATE users
                SET profile_photo_url = ?, updated_at = now()
                WHERE id = ?
                """,
                profilePhotoUrl,
                userId
        );
    }

    public void deleteProfilePhoto(UUID userId) {
        jdbcTemplate.update(
                """
                UPDATE users
                SET profile_photo_url = NULL, updated_at = now()
                WHERE id = ?
                """,
                userId
        );
    }

    public OnboardingProfileSnapshot findOnboardingProfile(UUID userId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT height_cm, weight_kg, experience_level, training_days_per_week,
                       session_duration_minutes, onboarding_completed, birth_date, sex, activity_level,
                       training_location, health_flags::text AS health_flags, onboarding_skipped_at,
                       dietary_preferences::text AS dietary_preferences, medical_notes
                FROM user_profiles
                WHERE user_id = ?
                """,
                (rs, rowNum) -> new OnboardingProfileSnapshot(
                        rs.getBigDecimal("height_cm"),
                        rs.getBigDecimal("weight_kg"),
                        rs.getString("experience_level"),
                        (Integer) rs.getObject("training_days_per_week"),
                        (Integer) rs.getObject("session_duration_minutes"),
                        rs.getBoolean("onboarding_completed"),
                        rs.getObject("birth_date", LocalDate.class),
                        rs.getString("sex"),
                        rs.getString("activity_level"),
                        rs.getString("training_location"),
                        JsonCodes.parse(rs.getString("health_flags")),
                        rs.getObject("onboarding_skipped_at") != null,
                        readStringList(rs.getString("dietary_preferences")),
                        rs.getString("medical_notes")
                ),
                userId
        );
    }

    public void updateOnboardingProfile(UUID userId, OnboardingProfileUpdate update) {
        jdbcTemplate.update(
                """
                UPDATE user_profiles
                SET height_cm = ?, weight_kg = ?, experience_level = ?, training_days_per_week = ?,
                    session_duration_minutes = ?, birth_date = ?, sex = ?, activity_level = ?,
                    training_location = ?, health_flags = CAST(? AS jsonb),
                    onboarding_completed = true, updated_at = now()
                WHERE user_id = ?
                """,
                update.heightCm(),
                update.weightKg(),
                update.experienceLevel(),
                update.trainingDaysPerWeek(),
                update.sessionDurationMinutes(),
                update.birthDate(),
                update.sex(),
                update.activityLevel(),
                update.trainingLocation(),
                JsonCodes.toArray(update.healthFlags()),
                userId
        );
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Arreglo JSON de textos libres; cualquier otra cosa (como el {} inicial) es una lista vacia. */
    static List<String> readStringList(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            JsonNode node = JSON.readTree(json);
            if (!node.isArray()) {
                return List.of();
            }
            List<String> values = new java.util.ArrayList<>();
            node.forEach(item -> {
                if (item.isTextual() && !item.asText().isBlank()) {
                    values.add(item.asText());
                }
            });
            return values;
        } catch (JsonProcessingException invalid) {
            return List.of();
        }
    }

    public static String writeStringList(List<String> values) {
        try {
            return JSON.writeValueAsString(values);
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * Edicion parcial: solo las columnas presentes. Las claves son nombres de columna fijos del
     * codigo (no vienen del usuario), los valores van como parametros.
     */
    public void patchProfile(UUID userId, java.util.LinkedHashMap<String, Object> columns) {
        if (columns.isEmpty()) {
            return;
        }
        List<String> sets = new java.util.ArrayList<>();
        List<Object> args = new java.util.ArrayList<>();
        columns.forEach((column, value) -> {
            if (!PATCHABLE_COLUMNS.contains(column)) {
                throw new IllegalArgumentException("Column not patchable: " + column);
            }
            boolean json = column.equals("health_flags") || column.equals("dietary_preferences");
            sets.add(column + (json ? " = CAST(? AS jsonb)" : " = ?"));
            args.add(value);
        });
        args.add(userId);
        jdbcTemplate.update(
                "UPDATE user_profiles SET " + String.join(", ", sets) + ", updated_at = now() WHERE user_id = ?",
                args.toArray()
        );
    }

    private static final java.util.Set<String> PATCHABLE_COLUMNS = java.util.Set.of(
            "experience_level", "training_days_per_week", "session_duration_minutes", "height_cm", "weight_kg",
            "birth_date", "sex", "activity_level", "training_location", "health_flags", "dietary_preferences",
            "medical_notes");

    /** Nueva medicion de peso para Progreso, si cambio respecto de la ultima. */
    public void insertWeightIfChanged(UUID userId, BigDecimal weightKg) {
        jdbcTemplate.update(
                """
                INSERT INTO body_metrics (user_id, weight_kg, notes)
                SELECT ?, ?, 'Perfil'
                WHERE NOT EXISTS (
                    SELECT 1 FROM (
                        SELECT weight_kg FROM body_metrics
                        WHERE user_id = ? AND weight_kg IS NOT NULL
                        ORDER BY measured_at DESC LIMIT 1
                    ) latest
                    WHERE latest.weight_kg = ?
                )
                """,
                userId,
                weightKg,
                userId,
                weightKg
        );
    }

    public void markOnboardingCompleted(UUID userId) {
        jdbcTemplate.update(
                "UPDATE user_profiles SET onboarding_completed = true, updated_at = now() WHERE user_id = ?",
                userId
        );
    }

    /** El usuario salteo el onboarding: deja de mostrarse, pero el perfil sigue incompleto. */
    public void markOnboardingSkipped(UUID userId) {
        jdbcTemplate.update(
                """
                UPDATE user_profiles
                SET onboarding_skipped_at = COALESCE(onboarding_skipped_at, now()), updated_at = now()
                WHERE user_id = ?
                """,
                userId
        );
    }

    /** Primer registro de peso para Progreso: solo si el usuario todavia no tiene ninguno. */
    public void insertInitialWeight(UUID userId, BigDecimal weightKg) {
        jdbcTemplate.update(
                """
                INSERT INTO body_metrics (user_id, weight_kg, notes)
                SELECT ?, ?, 'Onboarding'
                WHERE NOT EXISTS (SELECT 1 FROM body_metrics WHERE user_id = ? AND weight_kg IS NOT NULL)
                """,
                userId,
                weightKg,
                userId
        );
    }

    public String findPreferredTrainingStyle(UUID userId) {
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

    public List<String> findEquipment(UUID userId) {
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

    public List<OnboardingResponse.LimitationItem> findLimitations(UUID userId) {
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

    public void replacePreference(UUID userId, String preferredTrainingStyle) {
        jdbcTemplate.update("DELETE FROM user_preferences WHERE user_id = ?", userId);
        jdbcTemplate.update(
                """
                INSERT INTO user_preferences (user_id, preferred_training_style)
                VALUES (?, ?)
                """,
                userId,
                preferredTrainingStyle
        );
    }

    public void deletePreference(UUID userId) {
        jdbcTemplate.update("DELETE FROM user_preferences WHERE user_id = ?", userId);
    }

    public void deleteEquipment(UUID userId) {
        jdbcTemplate.update("DELETE FROM user_equipment WHERE user_id = ?", userId);
    }

    public void insertEquipment(UUID userId, String equipmentName) {
        jdbcTemplate.update(
                """
                INSERT INTO user_equipment (user_id, equipment_name)
                VALUES (?, ?)
                """,
                userId,
                equipmentName
        );
    }

    public void deleteLimitations(UUID userId) {
        jdbcTemplate.update("DELETE FROM user_limitations WHERE user_id = ?", userId);
    }

    public void insertLimitation(UUID userId, String limitationType, String notes) {
        jdbcTemplate.update(
                """
                INSERT INTO user_limitations (user_id, limitation_type, notes)
                VALUES (?, ?, ?)
                """,
                userId,
                limitationType,
                notes
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
        return new ProfileResponse(
                birthDate == null ? null : birthDate.toString(),
                rs.getString("sex"),
                rs.getBigDecimal("height_cm"),
                rs.getBigDecimal("weight_kg"),
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

    public record UserAccount(UUID id, String name, String email, String profilePhotoUrl) {
    }

    public record OnboardingProfileSnapshot(
            BigDecimal heightCm,
            BigDecimal weightKg,
            String experienceLevel,
            Integer trainingDaysPerWeek,
            Integer sessionDurationMinutes,
            boolean onboardingCompleted,
            LocalDate birthDate,
            String sex,
            String activityLevel,
            String trainingLocation,
            List<String> healthFlags,
            boolean onboardingSkipped,
            List<String> dietaryPreferences,
            String medicalNotes
    ) {
    }

    public record OnboardingProfileUpdate(
            BigDecimal heightCm,
            BigDecimal weightKg,
            String experienceLevel,
            Integer trainingDaysPerWeek,
            Integer sessionDurationMinutes,
            LocalDate birthDate,
            String sex,
            String activityLevel,
            String trainingLocation,
            List<String> healthFlags
    ) {
    }
}
