package com.locomotora.demo.routine.dto;

public record ExerciseResponse(
        String id,
        String exerciseId,
        String name,
        int sets,
        int reps,
        Integer repsMin,
        Integer repsMax,
        int restSeconds,
        String primaryMuscleGroup,
        String notes
) {
}
