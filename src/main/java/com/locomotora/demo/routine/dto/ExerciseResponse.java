package com.locomotora.demo.routine.dto;

/**
 * Ejercicio de un dia de rutina. {@code instructions} viene del catalogo: la app la muestra en
 * "Tecnica" mientras se entrena, aun sin conexion.
 */
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
        String notes,
        String equipment,
        String instructions
) {
    public ExerciseResponse(String id, String exerciseId, String name, int sets, int reps, Integer repsMin,
                            Integer repsMax, int restSeconds, String primaryMuscleGroup, String notes, String equipment) {
        this(id, exerciseId, name, sets, reps, repsMin, repsMax, restSeconds, primaryMuscleGroup, notes, equipment, null);
    }
}
