package com.locomotora.demo.progress.dto;

public record ProgressSummaryResponse(
        int completedWorkouts,
        int activeRoutines,
        int activeDaysLast30,
        BodyMetricResponse latestMetric
) {
}
