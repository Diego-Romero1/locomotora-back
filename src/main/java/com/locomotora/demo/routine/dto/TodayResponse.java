package com.locomotora.demo.routine.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pantalla Hoy: que rutina y que dia toca, como va la semana, el ultimo entrenamiento y el progreso (R5).
 */
public record TodayResponse(
        RoutineResponse routine,
        String nextDayId,
        Week week,
        int weekStreak,
        WorkoutSessionResponse lastSession,
        Progress progress
) {
    /**
     * {@code trainedDates} en la zona horaria pedida, formato yyyy-MM-dd. {@code sessions}: cada entrenamiento
     * de la semana con su fecha y el dia de rutina, para nombrar los dias del calendario.
     */
    public record Week(String weekStart, List<String> trainedDates, int targetDays, List<WeekSession> sessions) {
    }

    public record WeekSession(String date, String routineDayId, String dayTitle) {
    }

    /**
     * Tarjetas de progreso de Hoy. {@code weeksMet} y {@code weeklyVolumeKg}: ultimas 8 semanas, de la mas vieja
     * a la actual. Los meses son calendario en la zona horaria pedida.
     */
    public record Progress(
            List<Boolean> weeksMet,
            int monthWorkouts,
            int previousMonthWorkouts,
            List<BigDecimal> weeklyVolumeKg,
            LastRecord lastRecord
    ) {
    }

    /** La serie mas reciente que supero la mejor marca estimada anterior de su ejercicio. */
    public record LastRecord(String exerciseId, String exerciseName, BigDecimal weightKg, int reps, String completedAt) {
    }
}
