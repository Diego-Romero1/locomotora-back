package com.locomotora.demo.routine.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Reglas puras de calendario: que dia toca y la racha semanal. */
public final class TrainingCalendar {
    private TrainingCalendar() {
    }

    /**
     * Dia siguiente al ultimo entrenado, en el orden de la rutina. Vuelve al primero al
     * terminar la semana. Si nunca se entreno o el dia ya no existe, el primero.
     */
    public static Optional<UUID> nextDayId(List<UUID> orderedDayIds, Optional<UUID> lastTrainedDayId) {
        if (orderedDayIds.isEmpty()) {
            return Optional.empty();
        }
        int last = lastTrainedDayId.map(orderedDayIds::indexOf).orElse(-1);
        if (last < 0) {
            return Optional.of(orderedDayIds.get(0));
        }
        return Optional.of(orderedDayIds.get((last + 1) % orderedDayIds.size()));
    }

    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Semanas seguidas (lunes a domingo) con al menos {@code targetDays} dias entrenados.
     * La semana actual suma si ya se cumplio; si todavia no, no corta la racha.
     */
    public static int weekStreak(Collection<LocalDate> trainedDates, LocalDate today, int targetDays) {
        int target = Math.max(1, targetDays);
        Set<LocalDate> weeks = trainedDates.stream().map(TrainingCalendar::weekStart).collect(Collectors.toSet());
        if (weeks.isEmpty()) {
            return 0;
        }
        Set<LocalDate> days = Set.copyOf(trainedDates);

        LocalDate week = weekStart(today);
        int streak = 0;
        if (daysInWeek(days, week) >= target) {
            streak = 1;
        }
        week = week.minusWeeks(1);
        while (daysInWeek(days, week) >= target) {
            streak += 1;
            week = week.minusWeeks(1);
        }
        return streak;
    }

    static int daysInWeek(Set<LocalDate> days, LocalDate weekStart) {
        int count = 0;
        for (int i = 0; i < 7; i++) {
            if (days.contains(weekStart.plusDays(i))) {
                count += 1;
            }
        }
        return count;
    }
}
