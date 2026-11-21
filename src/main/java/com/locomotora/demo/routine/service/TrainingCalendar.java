package com.locomotora.demo.routine.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Reglas puras de calendario: que dia toca (vueltas), la semana y la racha. */
public final class TrainingCalendar {
    private TrainingCalendar() {
    }

    /** Un entrenamiento de un dia de la rutina. */
    public record DaySession(UUID dayId, Instant completedAt) {
    }

    /**
     * Estado de la vuelta actual. {@code doneInRound}: dias hechos en esta vuelta y cuando.
     * {@code pendingSince}: el dia que tocaba cuando se hizo otro en su lugar, y desde cuando quedo pendiente.
     * {@code roundStartedAt}: primer entrenamiento de la vuelta (null si todavia no empezo).
     */
    public record Cycle(
            Optional<UUID> nextDayId,
            Map<UUID, Instant> doneInRound,
            Map<UUID, Instant> pendingSince,
            Instant roundStartedAt
    ) {
    }

    /**
     * Vueltas (R5): cada dia de la rutina se hace una vez por vuelta y toca el primero que falta, en orden.
     * Si se hace otro, el que tocaba queda pendiente y sigue primero. Al completar todos empieza otra vuelta.
     * Los entrenamientos de dias que ya no existen se ignoran; un dia nuevo entra como faltante.
     *
     * @param orderedDayIds dias en el orden de la rutina
     * @param sessions      entrenamientos de la rutina, del mas viejo al mas nuevo
     */
    public static Cycle cycle(List<UUID> orderedDayIds, List<DaySession> sessions) {
        if (orderedDayIds.isEmpty()) {
            return new Cycle(Optional.empty(), Map.of(), Map.of(), null);
        }
        Set<UUID> known = Set.copyOf(orderedDayIds);
        Map<UUID, Instant> done = new LinkedHashMap<>();
        Map<UUID, Instant> skipped = new LinkedHashMap<>();
        Instant start = null;
        for (DaySession session : sessions) {
            if (session.dayId() == null || !known.contains(session.dayId())) {
                continue;
            }
            UUID due = firstMissing(orderedDayIds, done);
            if (!session.dayId().equals(due) && !done.containsKey(session.dayId())) {
                skipped.putIfAbsent(due, session.completedAt());
            }
            if (!done.containsKey(session.dayId())) {
                done.put(session.dayId(), session.completedAt());
                if (start == null) {
                    start = session.completedAt();
                }
            }
            if (done.size() == orderedDayIds.size()) {
                done.clear();
                skipped.clear();
                start = null;
            }
        }
        Map<UUID, Instant> pending = new LinkedHashMap<>(skipped);
        pending.keySet().removeIf(done::containsKey);
        return new Cycle(Optional.of(firstMissing(orderedDayIds, done)), Map.copyOf(done), Map.copyOf(pending), start);
    }

    private static UUID firstMissing(List<UUID> orderedDayIds, Map<UUID, Instant> done) {
        return orderedDayIds.stream().filter(id -> !done.containsKey(id)).findFirst().orElse(orderedDayIds.get(0));
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

    /** Si se cumplio el objetivo en cada una de las ultimas {@code count} semanas, de la mas vieja a la actual. */
    public static List<Boolean> weeksMet(Collection<LocalDate> trainedDates, LocalDate today, int targetDays, int count) {
        int target = Math.max(1, targetDays);
        Set<LocalDate> days = Set.copyOf(trainedDates);
        LocalDate current = weekStart(today);
        List<Boolean> result = new java.util.ArrayList<>(count);
        for (int i = count - 1; i >= 0; i--) {
            result.add(daysInWeek(days, current.minusWeeks(i)) >= target);
        }
        return result;
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
