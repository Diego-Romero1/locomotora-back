package com.locomotora.demo.ai.dto;

import java.util.List;

/**
 * Borrador de rutina generado por Claude. No se guarda: la app lo abre en el asistente (paso de
 * revision) para que el usuario lo ajuste y lo guarde con POST /routines (source AI).
 * Todos los ejercicios existen en el catalogo visible del usuario.
 *
 * @param warnings avisos de lo que se corrigio (ejercicios descartados, valores fuera de rango).
 */
public record GeneratedRoutineResponse(
        String title,
        String description,
        String objective,
        String difficulty,
        int daysPerWeek,
        int estimatedDurationMinutes,
        List<Day> days,
        String model,
        List<String> warnings
) {
    /** @param focus grupos musculares separados por coma, como en routine_days.focus. */
    public record Day(int dayIndex, String title, String focus, List<Exercise> exercises) {
    }

    public record Exercise(
            String exerciseId,
            String name,
            String primaryMuscleGroup,
            String equipment,
            int sets,
            int repsMin,
            int repsMax,
            int restSeconds,
            String notes
    ) {
    }
}
