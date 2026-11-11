package com.locomotora.demo.routine.dto;

import java.math.BigDecimal;

public record ExerciseProgressLogResponse(
        String sessionId,
        String exerciseId,
        BigDecimal weightKg,
        Integer reps,
        String completedAt
) {
}