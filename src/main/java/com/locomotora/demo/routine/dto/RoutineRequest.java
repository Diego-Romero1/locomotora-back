package com.locomotora.demo.routine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record RoutineRequest(
        @NotBlank String title,
        UUID categoryId,
        String description,
        String objective,
        String difficulty,
        Integer estimatedDurationMinutes,
        Integer daysPerWeek,
        List<@Valid RoutineDayRequest> days,
        List<@Valid ExerciseRequest> exercises,
        /** Origen al crear: MANUAL (por defecto), AI o TEMPLATE. Al editar se ignora. */
        String source
) {
    /** Compatibilidad con clientes que no mandan el origen. */
    public RoutineRequest(String title, UUID categoryId, String description, String objective, String difficulty,
                          Integer estimatedDurationMinutes, Integer daysPerWeek, List<RoutineDayRequest> days,
                          List<ExerciseRequest> exercises) {
        this(title, categoryId, description, objective, difficulty, estimatedDurationMinutes, daysPerWeek, days, exercises, null);
    }
}
