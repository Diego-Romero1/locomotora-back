package com.locomotora.demo.user.service;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.service.Objectives;
import com.locomotora.demo.user.dto.OnboardingRequest;
import com.locomotora.demo.user.dto.ProfilePatchRequest;
import com.locomotora.demo.user.repository.ProfileRepository;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;

/** Vocabularios del onboarding y reglas que no caben en anotaciones. */
public final class OnboardingRules {
    public static final Set<String> LEVELS = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED");
    public static final Set<String> SEXES = Set.of("MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY");
    public static final Set<String> ACTIVITY_LEVELS = Set.of("SEDENTARY", "LIGHT", "MODERATE", "ACTIVE", "VERY_ACTIVE");
    public static final Set<String> LOCATIONS = Set.of("FULL_GYM", "BASIC_GYM", "HOME_EQUIPPED", "HOME_NONE");
    public static final Set<String> EQUIPMENT = Set.of(
            "BODYWEIGHT", "DUMBBELLS", "BARBELL", "KETTLEBELLS", "RESISTANCE_BANDS", "MACHINES");
    /** Zonas del cuerpo (mismos codigos que exercises.stress_zones) y OTHER para notas libres. */
    public static final Set<String> BODY_ZONES = Set.of(
            "NECK", "SHOULDER", "ELBOW", "WRIST", "LOWER_BACK", "HIP", "KNEE", "ANKLE");
    public static final String OTHER_LIMITATION = "OTHER";
    public static final Set<String> HEALTH_FLAGS = Set.of(
            "HEART_CONDITION", "CHEST_PAIN", "DIZZINESS", "HIGH_BLOOD_PRESSURE", "PREGNANCY");
    public static final Set<String> STYLES = Set.of(
            "FULL_BODY", "UPPER_LOWER", "PUSH_PULL_LEGS", "PPL_UPPER_LOWER", "BRO_SPLIT");

    private OnboardingRules() {
    }

    /** Revisa los vocabularios; lanza 400 con el primer campo invalido. */
    public static void validate(OnboardingRequest request) {
        String goal = Objectives.canonicalOrRaw(request.goalType());
        require(Objectives.CANONICAL.contains(goal), "goalType");
        require(LEVELS.contains(upper(request.experienceLevel())), "experienceLevel");
        checkOptional(request.sex(), SEXES, "sex");
        checkOptional(request.activityLevel(), ACTIVITY_LEVELS, "activityLevel");
        checkOptional(request.trainingLocation(), LOCATIONS, "trainingLocation");
        checkStyle(request.preferredTrainingStyle());
        checkCodes(request.equipment(), EQUIPMENT, "equipment");
        checkCodes(request.healthFlags(), HEALTH_FLAGS, "healthFlags");
        checkLimitations(request.limitations());
    }

    /** Lo mismo para la edicion parcial: solo se revisa lo que vino. */
    public static void validate(ProfilePatchRequest request) {
        if (request.name() != null) {
            require(!request.name().isBlank(), "name");
        }
        if (request.goalType() != null) {
            require(Objectives.CANONICAL.contains(Objectives.canonicalOrRaw(request.goalType())), "goalType");
        }
        checkOptional(request.experienceLevel(), LEVELS, "experienceLevel");
        checkOptional(request.sex(), SEXES, "sex");
        checkOptional(request.activityLevel(), ACTIVITY_LEVELS, "activityLevel");
        checkOptional(request.trainingLocation(), LOCATIONS, "trainingLocation");
        checkStyle(request.preferredTrainingStyle());
        checkCodes(request.equipment(), EQUIPMENT, "equipment");
        checkCodes(request.healthFlags(), HEALTH_FLAGS, "healthFlags");
        checkLimitations(request.limitations());
    }

    /**
     * El perfil queda completo cuando tiene todo lo que el onboarding pide como obligatorio. Asi,
     * quien lo salteo lo completa tambien seccion por seccion desde Perfil.
     */
    public static boolean isComplete(ProfileRepository.OnboardingProfileSnapshot profile, boolean hasGoal, boolean hasEquipment) {
        return hasGoal && hasEquipment
                && profile.experienceLevel() != null
                && profile.trainingDaysPerWeek() != null
                && profile.sessionDurationMinutes() != null
                && profile.heightCm() != null
                && profile.weightKg() != null
                && profile.birthDate() != null
                && profile.healthFlags() != null;
    }

    private static void checkOptional(String value, Set<String> allowed, String field) {
        require(value == null || allowed.contains(upper(value)), field);
    }

    private static void checkStyle(String style) {
        require(style == null || style.isBlank() || STYLES.contains(upper(style)), "preferredTrainingStyle");
    }

    private static void checkCodes(List<String> values, Set<String> allowed, String field) {
        if (values != null) {
            values.forEach(value -> require(value != null && allowed.contains(upper(value)), field));
        }
    }

    private static void checkLimitations(List<OnboardingRequest.LimitationItem> limitations) {
        if (limitations == null) {
            return;
        }
        for (OnboardingRequest.LimitationItem item : limitations) {
            require(item != null && item.limitationType() != null, "limitations");
            String type = upper(item.limitationType());
            require(BODY_ZONES.contains(type) || OTHER_LIMITATION.equals(type), "limitations");
            require(!OTHER_LIMITATION.equals(type) || (item.notes() != null && !item.notes().isBlank()), "limitations");
        }
    }

    /** Foto de perfil: imagen en base64 (data URI) o una URL publica; nunca una ruta local del telefono. */
    public static void validatePhoto(String photo) {
        require(photo != null && (photo.startsWith("data:image/jpeg;base64,") || photo.startsWith("data:image/png;base64,")
                || photo.startsWith("https://")), "profilePhotoUrl");
    }

    /**
     * Fecha de nacimiento aproximada a partir de la edad (mitad del ano). Si la fecha guardada ya
     * da esa edad, se conserva para no pisar una fecha real.
     */
    public static LocalDate birthDateFor(int age, LocalDate current, LocalDate today) {
        if (current != null && Period.between(current, today).getYears() == age) {
            return current;
        }
        return today.minusYears(age).minusMonths(6);
    }

    public static Integer ageOf(LocalDate birthDate, LocalDate today) {
        return birthDate == null ? null : Period.between(birthDate, today).getYears();
    }

    public static List<String> normalizedCodes(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(OnboardingRules::upper)
                .distinct()
                .toList();
    }

    static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static void require(boolean condition, String field) {
        if (!condition) {
            throw new ApiException(HttpStatus.BAD_REQUEST, field + ": invalid value");
        }
    }
}
