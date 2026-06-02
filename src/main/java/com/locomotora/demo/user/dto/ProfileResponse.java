package com.locomotora.demo.user.dto;

import java.math.BigDecimal;

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
