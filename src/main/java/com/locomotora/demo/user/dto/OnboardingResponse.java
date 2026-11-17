package com.locomotora.demo.user.dto;

import java.math.BigDecimal;
import java.util.List;

public record OnboardingResponse(
        String goalType,
        String experienceLevel,
        Integer trainingDaysPerWeek,
        Integer sessionDurationMinutes,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String preferredTrainingStyle,
        List<String> equipment,
        List<LimitationItem> limitations,
        boolean onboardingCompleted,
        /* Fecha de nacimiento (yyyy-MM-dd) y la edad calculada a partir de ella. */
        String birthDate,
        Integer age,
        String sex,
        String activityLevel,
        String trainingLocation,
        /* null = el chequeo de salud no se respondio todavia. */
        List<String> healthFlags,
        /* Lo salteo: la app no lo vuelve a mostrar al entrar, pero puede completarlo desde Perfil. */
        boolean onboardingSkipped,
        List<String> dietaryPreferences,
        String medicalNotes
) {
    public record LimitationItem(String limitationType, String notes) {
    }
}
