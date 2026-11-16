package com.locomotora.demo.routine.dto;

import java.util.List;

/**
 * Ejercicio del catalogo. stressZones: zonas del cuerpo que carga de forma importante (KNEE,
 * LOWER_BACK, ...), para cuidar las lesiones que el usuario declara.
 */
public record ExerciseCatalogItemResponse(
        String id,
        String name,
        String category,
        String primaryMuscleGroup,
        String equipment,
        String difficulty,
        String description,
        String instructions,
        boolean custom,
        List<String> stressZones
) {
    public ExerciseCatalogItemResponse {
        stressZones = stressZones == null ? List.of() : List.copyOf(stressZones);
    }

    public ExerciseCatalogItemResponse(String id, String name, String category, String primaryMuscleGroup,
            String equipment, String difficulty, String description, String instructions, boolean custom) {
        this(id, name, category, primaryMuscleGroup, equipment, difficulty, description, instructions, custom, List.of());
    }
}
