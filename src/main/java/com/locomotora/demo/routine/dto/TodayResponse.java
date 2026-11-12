package com.locomotora.demo.routine.dto;

import java.util.List;

/**
 * Pantalla Hoy: que rutina y que dia toca, como va la semana y el ultimo entrenamiento.
 */
public record TodayResponse(
        RoutineResponse routine,
        String nextDayId,
        Week week,
        int weekStreak,
        WorkoutSessionResponse lastSession
) {
    /** {@code trainedDates} en la zona horaria pedida, formato yyyy-MM-dd. */
    public record Week(String weekStart, List<String> trainedDates, int targetDays) {
    }
}
