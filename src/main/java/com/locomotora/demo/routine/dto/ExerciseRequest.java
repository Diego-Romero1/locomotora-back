package com.locomotora.demo.routine.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ExerciseRequest(
        String id,
        String exerciseId,
        @NotBlank String name,
        @Min(1) Integer sets,
        @Min(1) Integer reps,
        @Min(1) Integer repsMin,
        @Min(1) Integer repsMax,
        @Min(0) Integer restSeconds,
        String primaryMuscleGroup,
        String difficulty,
        String notes
) {
}
