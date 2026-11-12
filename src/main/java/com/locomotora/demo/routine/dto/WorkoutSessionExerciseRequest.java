package com.locomotora.demo.routine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Un ejercicio dentro de la sesion. {@code routineExerciseId} es opcional: un ejercicio
 * agregado o reemplazado durante la sesion no pertenece a la rutina.
 */
public record WorkoutSessionExerciseRequest(
        UUID routineExerciseId,
        @NotNull UUID exerciseId,
        @Min(1) Integer position,
        @Size(max = 1000) String notes,
        @NotEmpty @Size(max = 50) List<@Valid @NotNull WorkoutSetRequest> sets
) {
}
