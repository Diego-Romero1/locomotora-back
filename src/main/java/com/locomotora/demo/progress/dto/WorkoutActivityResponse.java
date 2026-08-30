package com.locomotora.demo.progress.dto;

import java.util.List;

public record WorkoutActivityResponse(
        String id,
        String routineId,
        String routineTitle,
        String routineDayId,
        String routineDayTitle,
        String completedAt,
        List<ExerciseActivityResponse> exercises
) {
}