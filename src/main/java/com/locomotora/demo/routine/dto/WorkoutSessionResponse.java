package com.locomotora.demo.routine.dto;

import java.math.BigDecimal;

public record WorkoutSessionResponse(
        String id,
        String clientSessionId,
        String kind,
        String routineId,
        String routineDayId,
        String routineTitle,
        String dayTitle,
        String startedAt,
        String completedAt,
        long durationSeconds,
        int completedSets,
        BigDecimal totalVolumeKg
) {
}
