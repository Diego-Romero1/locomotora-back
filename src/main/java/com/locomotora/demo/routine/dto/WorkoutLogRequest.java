package com.locomotora.demo.routine.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public record WorkoutLogRequest(
        @NotBlank String routineId,
        List<String> completedExerciseIds,
        String completedAt
) {
    public WorkoutLogRequest {
        if (completedExerciseIds == null) {
            completedExerciseIds = new ArrayList<>();
        }
    }
}
