package com.locomotora.demo.progress.dto;

import java.math.BigDecimal;

public record ExerciseActivityResponse(
        String id,
        String exerciseId,
        String exerciseName,
        String primaryMuscleGroup,
        Integer reps,
        BigDecimal weightKg,
        String completedAt
) {
}