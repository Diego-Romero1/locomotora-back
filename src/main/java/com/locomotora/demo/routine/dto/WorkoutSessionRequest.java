package com.locomotora.demo.routine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Sesion completa enviada de una sola vez al terminar de entrenar.
 * {@code clientSessionId} lo genera el telefono: reenviar la misma sesion no la duplica.
 */
public record WorkoutSessionRequest(
        @NotNull UUID clientSessionId,
        UUID routineId,
        UUID routineDayId,
        @NotBlank String startedAt,
        @NotBlank String completedAt,
        @Min(1) @Max(10) Integer perceivedExertion,
        @Size(max = 2000) String notes,
        @NotEmpty @Size(max = 60) List<@Valid @NotNull WorkoutSessionExerciseRequest> exercises
) {
}
