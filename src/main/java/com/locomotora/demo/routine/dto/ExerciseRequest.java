package com.locomotora.demo.routine.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ExerciseRequest(
        String id,
        @NotBlank String name,
        @Min(1) int sets,
        @Min(1) int reps,
        @Min(0) int restSeconds,
        String primaryMuscleGroup,
        String difficulty,
        String notes
) {
}
