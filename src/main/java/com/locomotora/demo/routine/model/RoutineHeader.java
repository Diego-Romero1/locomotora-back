package com.locomotora.demo.routine.model;

import java.util.UUID;

public record RoutineHeader(
        UUID id,
        String title,
        String description,
        String objective,
        String difficulty,
        Integer estimatedDurationMinutes,
        Integer daysPerWeek,
        UUID categoryId,
        String categoryName,
        String source,
        String templateKey,
        String routineSplit
) {
}
