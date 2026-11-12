package com.locomotora.demo.routine.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WorkoutSetRequest(
        @NotNull @Min(1) @Max(50) Integer setNumber,
        @Min(0) @Max(1000) Integer reps,
        @DecimalMin("0.0") @DecimalMax("2000.0") BigDecimal weightKg,
        @Min(0) @Max(86400) Integer durationSeconds,
        Boolean completed
) {
    public boolean isCompleted() {
        return completed == null || completed;
    }
}
