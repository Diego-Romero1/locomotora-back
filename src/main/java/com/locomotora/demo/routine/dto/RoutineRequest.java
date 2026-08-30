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
        List<@Valid ExerciseRequest> exercises
) {
}
