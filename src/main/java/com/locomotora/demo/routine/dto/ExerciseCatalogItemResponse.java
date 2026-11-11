package com.locomotora.demo.routine.dto;

public record ExerciseCatalogItemResponse(
        String id,
        String name,
        String category,
        String primaryMuscleGroup,
        String equipment,
        String difficulty,
        String description,
        String instructions
) {
}