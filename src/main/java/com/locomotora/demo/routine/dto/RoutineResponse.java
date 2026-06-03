package com.locomotora.demo.routine.dto;

import java.util.List;

public record RoutineResponse(
        String id,
        String title,
        String description,
        String objective,
        String difficulty,
        Integer estimatedDurationMinutes,
        Integer daysPerWeek,
        String categoryId,
        String categoryName,
        String routineSplit,
        String source,
        boolean isTemplate,
        String templateKey,
        List<RoutineDayResponse> days,
        List<ExerciseResponse> exercises
) {
}
