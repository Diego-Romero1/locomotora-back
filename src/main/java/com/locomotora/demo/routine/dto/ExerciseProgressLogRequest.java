package com.locomotora.demo.routine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ExerciseProgressLogRequest(
        @NotNull @DecimalMin(value = "0.0") BigDecimal weightKg,
        @NotNull @Positive Integer reps,
        String completedAt
) {
}