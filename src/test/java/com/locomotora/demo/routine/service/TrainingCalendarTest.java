package com.locomotora.demo.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrainingCalendarTest {
    private final UUID upperA = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID lowerA = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private final UUID upperB = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private final List<UUID> days = List.of(upperA, lowerA, upperB);

    // Miercoles 11 de noviembre de 2026; la semana empieza el lunes 9.
    private final LocalDate today = LocalDate.of(2026, 11, 11);

    @Test
    void nextDayFollowsTheLastTrainedAndWrapsAround() {
        assertEquals(Optional.of(lowerA), TrainingCalendar.nextDayId(days, Optional.of(upperA)));
        assertEquals(Optional.of(upperA), TrainingCalendar.nextDayId(days, Optional.of(upperB)));
    }

    @Test
    void nextDayIsTheFirstWhenNeverTrainedOrDayWasDeleted() {
        assertEquals(Optional.of(upperA), TrainingCalendar.nextDayId(days, Optional.empty()));
        assertEquals(Optional.of(upperA), TrainingCalendar.nextDayId(days, Optional.of(UUID.randomUUID())));
        assertEquals(Optional.empty(), TrainingCalendar.nextDayId(List.of(), Optional.of(upperA)));
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
