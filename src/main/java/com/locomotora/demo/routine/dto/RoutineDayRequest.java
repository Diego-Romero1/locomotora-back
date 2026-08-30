package com.locomotora.demo.routine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;

public record RoutineDayRequest(
        String id,
        @Min(1) Integer dayIndex,
        String title,
        String focus,
        List<@Valid ExerciseRequest> exercises
) {
}
