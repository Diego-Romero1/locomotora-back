package com.locomotora.demo.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Edicion parcial del perfil (cada seccion de Perfil manda solo lo suyo). null = sin cambios.
 * Para borrar el estilo preferido o las notas medicas se manda "". Las listas reemplazan a las
 * guardadas. Mismos rangos y vocabularios que el onboarding.
 */
public record ProfilePatchRequest(
        @Size(min = 1, max = 120) String name,
        String goalType,
        String experienceLevel,
        @Min(1) @Max(7) Integer trainingDaysPerWeek,
        @Min(15) @Max(180) Integer sessionDurationMinutes,
        @DecimalMin("120") @DecimalMax("230") BigDecimal heightCm,
        @DecimalMin("30") @DecimalMax("250") BigDecimal weightKg,
        String preferredTrainingStyle,
        @Size(min = 1, max = 10) List<String> equipment,
        @Size(max = 12) List<OnboardingRequest.@Valid LimitationItem> limitations,
        @Min(13) @Max(100) Integer age,
        String sex,
        String activityLevel,
        String trainingLocation,
        @Size(max = 10) List<String> healthFlags,
        @Size(max = 12) List<@Size(min = 1, max = 60) String> dietaryPreferences,
        @Size(max = 1000) String medicalNotes
) {
}
