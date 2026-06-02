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
        boolean onboardingCompleted
) {
    public record LimitationItem(String limitationType, String notes) {
    }
}
