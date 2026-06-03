package com.locomotora.demo.routine.dto;

import java.util.List;

public record RoutineDayResponse(
        String id,
        int dayIndex,
        String title,
        String focus,
        List<ExerciseResponse> exercises
) {
}
