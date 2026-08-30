package com.locomotora.demo.routine.dto;

import java.math.BigDecimal;

public record WorkoutExerciseLogRequest(
        String routineExerciseId,
        Integer reps,
        BigDecimal weightKg
) {
}