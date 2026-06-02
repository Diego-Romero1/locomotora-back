package com.locomotora.demo.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
