package com.locomotora.demo.routine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ejercicio propio: solo lo ve quien lo crea. */
public record OwnExerciseRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @Size(max = 80) String primaryMuscleGroup,
        @Size(max = 40) String equipment
) {
}
