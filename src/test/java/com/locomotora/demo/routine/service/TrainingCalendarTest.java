package com.locomotora.demo.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TrainingCalendarTest {
    private final UUID upperA = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID lowerA = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private final UUID upperB = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private final List<UUID> days = List.of(upperA, lowerA, upperB);

    // Miercoles 11 de noviembre de 2026; la semana empieza el lunes 9.
    private final LocalDate today = LocalDate.of(2026, 11, 11);

    @Test
    void nextDayFollowsTheOrderAndARoundStartsAgainWhenAllAreDone() {
        assertEquals(Optional.of(lowerA), TrainingCalendar.cycle(days, sessions(upperA)).nextDayId());
        TrainingCalendar.Cycle complete = TrainingCalendar.cycle(days, sessions(upperA, lowerA, upperB));
        assertEquals(Optional.of(upperA), complete.nextDayId());
        assertTrue(complete.doneInRound().isEmpty());
        assertNull(complete.roundStartedAt());
    }

    @Test
    void neverTrainedStartsAtDayOneAndEmptyRoutineHasNoDay() {
        assertEquals(Optional.of(upperA), TrainingCalendar.cycle(days, List.of()).nextDayId());
        assertEquals(Optional.empty(), TrainingCalendar.cycle(List.of(), sessions(upperA)).nextDayId());
    }

    @Test
    void doingAnotherDayLeavesTheDueOnePendingAndItStaysNext() {
        // Tocaba upperA y se hizo upperB: upperA queda pendiente y sigue tocando.
        TrainingCalendar.Cycle cycle = TrainingCalendar.cycle(days, sessions(upperB));
        assertEquals(Optional.of(upperA), cycle.nextDayId());
        assertEquals(Set.of(upperB), cycle.doneInRound().keySet());
        assertEquals(Map.of(upperA, at(1)), cycle.pendingSince());
        assertEquals(at(1), cycle.roundStartedAt());

        // Al hacerlo deja de estar pendiente y toca lowerA, que nunca se salto.
        TrainingCalendar.Cycle next = TrainingCalendar.cycle(days, sessions(upperB, upperA));
        assertEquals(Optional.of(lowerA), next.nextDayId());
        assertTrue(next.pendingSince().isEmpty());
    }

    @Test
    void repeatingADayAlreadyDoneChangesNothing() {
        TrainingCalendar.Cycle cycle = TrainingCalendar.cycle(days, sessions(upperA, upperA));
        assertEquals(Optional.of(lowerA), cycle.nextDayId());
        assertEquals(Set.of(upperA), cycle.doneInRound().keySet());
        assertEquals(at(1), cycle.doneInRound().get(upperA));
        assertTrue(cycle.pendingSince().isEmpty());
    }

    @Test
    void deletedDaysAreIgnoredAndNewDaysCountAsMissing() {
        UUID deleted = UUID.randomUUID();
        TrainingCalendar.Cycle cycle = TrainingCalendar.cycle(days, sessions(deleted, upperA));
        assertEquals(Optional.of(lowerA), cycle.nextDayId());
        assertEquals(Set.of(upperA), cycle.doneInRound().keySet());

        UUID added = UUID.randomUUID();
        List<UUID> longer = List.of(upperA, lowerA, upperB, added);
        // Con un dia nuevo, la vuelta de 3 ya no esta completa: falta el nuevo.
        assertEquals(Optional.of(added), TrainingCalendar.cycle(longer, sessions(upperA, lowerA, upperB)).nextDayId());
    }

    @Test
    void weeksMetGoesFromOldestToCurrent() {
        List<LocalDate> trained = List.of(d(10, 26), d(10, 28), d(11, 2), d(11, 9), d(11, 10));
        assertEquals(List.of(true, false, true), TrainingCalendar.weeksMet(trained, today, 2, 3));
    }

    /** Un entrenamiento por dia, empezando el 1 de noviembre. */
    private static List<TrainingCalendar.DaySession> sessions(UUID... dayIds) {
        return IntStream.range(0, dayIds.length)
                .mapToObj(i -> new TrainingCalendar.DaySession(dayIds[i], at(i + 1)))
                .toList();
    }

    private static Instant at(int day) {
        return Instant.parse("2026-11-%02dT19:00:00Z".formatted(day));
    }

    @Test
    void weekStartsOnMonday() {
        assertEquals(LocalDate.of(2026, 11, 9), TrainingCalendar.weekStart(today));
        assertEquals(LocalDate.of(2026, 11, 9), TrainingCalendar.weekStart(LocalDate.of(2026, 11, 15)));
    }

    @Test
    void currentWeekInProgressDoesNotBreakTheStreak() {
        List<LocalDate> trained = List.of(
                // semana del 26 oct: 3 dias
                d(10, 26), d(10, 28), d(10, 30),
                // semana del 2 nov: 3 dias
                d(11, 2), d(11, 4), d(11, 6),
                // semana actual: 1 dia (todavia no cumple)
                d(11, 9));
        assertEquals(2, TrainingCalendar.weekStreak(trained, today, 3));
    }

    @Test
    void currentWeekCountsOnceTheTargetIsMet() {
        List<LocalDate> trained = List.of(d(11, 2), d(11, 4), d(11, 6), d(11, 9), d(11, 10), d(11, 11));
        assertEquals(2, TrainingCalendar.weekStreak(trained, today, 3));
    }

    @Test
    void aShortWeekBreaksTheStreak() {
        List<LocalDate> trained = List.of(
                d(10, 19), d(10, 21), d(10, 23),
                d(10, 26),                 // solo 1 dia: corta
                d(11, 2), d(11, 4), d(11, 6));
        assertEquals(1, TrainingCalendar.weekStreak(trained, today, 3));
    }

    @Test
    void twoSessionsTheSameDayCountAsOneDay() {
        List<LocalDate> trained = List.of(d(11, 2), d(11, 2), d(11, 4));
        assertEquals(0, TrainingCalendar.weekStreak(trained, today, 3));
        assertEquals(0, TrainingCalendar.weekStreak(List.of(), today, 3));
    }

    private static LocalDate d(int month, int day) {
        return LocalDate.of(2026, month, day);
    }
}
