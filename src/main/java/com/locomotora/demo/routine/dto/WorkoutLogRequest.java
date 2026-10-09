package com.locomotora.demo.routine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public record WorkoutLogRequest(
        @NotBlank String routineId,
        String routineDayId,
        List<String> completedExerciseIds,
        List<@Valid WorkoutExerciseLogRequest> completedExercises,
        String completedAt
) {
    public WorkoutLogRequest {
        if (completedExerciseIds == null) {
            completedExerciseIds = new ArrayList<>();
        }
        if (completedExercises == null) {
            completedExercises = new ArrayList<>();
        }
    }
}
