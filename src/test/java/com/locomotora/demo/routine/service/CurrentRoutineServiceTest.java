package com.locomotora.demo.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.RoutineDayResponse;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.repository.RoutineWriteRepository;
import com.locomotora.demo.routine.repository.TrainingStatsRepository;
import com.locomotora.demo.routine.repository.TrainingStatsRepository.RoutineCandidate;
import com.locomotora.demo.routine.service.TrainingCalendar.DaySession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CurrentRoutineServiceTest {
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID chosen = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private final UUID trained = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private final UUID edited = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private final UUID dayA = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID dayB = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private final UUID dayC = UUID.fromString("00000000-0000-0000-0000-000000000203");

    @Mock
    private TrainingStatsRepository statsRepository;
    @Mock
    private RoutineReadService routineReadService;
    @Mock
    private RoutineWriteRepository routineWriteRepository;

    private CurrentRoutineService service;

    @BeforeEach
    void setUp() {
        service = new CurrentRoutineService(statsRepository, routineReadService, routineWriteRepository);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void theChosenRoutineWinsEvenIfAnotherWasEditedLater() {
        when(statsRepository.findChosenCurrentRoutine(userId)).thenReturn(Optional.of(chosen));

        assertEquals(Optional.of(chosen), service.resolve(userId));
        verify(statsRepository, never()).findLatestUpdatedActiveRoutine(any());
    }

    @Test
    void withoutAChoiceItFallsBackToTheMostRecentTrainedOrEdited() {
        when(statsRepository.findLastTrainedActiveRoutine(userId))
                .thenReturn(Optional.of(new RoutineCandidate(trained, Instant.parse("2026-11-10T19:00:00Z"))));
        when(statsRepository.findLatestUpdatedActiveRoutine(userId))
                .thenReturn(Optional.of(new RoutineCandidate(edited, Instant.parse("2026-11-01T10:00:00Z"))));

        assertEquals(Optional.of(trained), service.resolve(userId));
    }

    @Test
    void decorateMarksEachDayTheRoundAndTheDayThatComesNext() {
        // A y B hechos en orden: toca C.
        when(statsRepository.findDaySessions(userId, chosen)).thenReturn(List.of(
                new DaySession(dayA, Instant.parse("2026-11-09T19:00:00Z")),
                new DaySession(dayB, Instant.parse("2026-11-11T19:00:00Z"))));

        RoutineResponse result = service.decorate(routine(chosen, false), userId, Optional.of(chosen));

        assertTrue(result.current());
        assertEquals(dayC.toString(), result.nextDayId());
        assertEquals("2026-11-11T19:00:00Z", result.lastTrainedAt());
        assertEquals("2026-11-09T19:00:00Z", result.roundStartedAt());
        assertEquals("2026-11-09T19:00:00Z", day(result, dayA).lastTrainedAt());
        assertTrue(day(result, dayA).doneInRound());
        assertTrue(day(result, dayB).doneInRound());
        assertFalse(day(result, dayC).doneInRound());
        assertNull(day(result, dayC).lastTrainedAt());
    }

    @Test
    void doingAnotherDayKeepsTheDueOnePendingAndNext() {
        // Tocaba A y se hizo C: A sigue tocando, marcada como pendiente desde ese dia.
        when(statsRepository.findDaySessions(userId, chosen)).thenReturn(List.of(
                new DaySession(dayC, Instant.parse("2026-11-16T19:00:00Z"))));

        RoutineResponse result = service.decorate(routine(chosen, false), userId, Optional.of(chosen));

        assertEquals(dayA.toString(), result.nextDayId());
        assertEquals("2026-11-16T19:00:00Z", day(result, dayA).pendingSince());
        assertNull(day(result, dayB).pendingSince());
        assertTrue(day(result, dayC).doneInRound());
    }

    @Test
    void afterTheLastDayTheCycleStartsAgainAndANeverTrainedRoutineStartsAtDayOne() {
        when(statsRepository.findDaySessions(userId, chosen)).thenReturn(List.of(
                new DaySession(dayA, Instant.parse("2026-11-09T19:00:00Z")),
                new DaySession(dayB, Instant.parse("2026-11-10T19:00:00Z")),
                new DaySession(dayC, Instant.parse("2026-11-11T19:00:00Z"))));
        RoutineResponse wrapped = service.decorate(routine(chosen, false), userId, Optional.empty());
        assertEquals(dayA.toString(), wrapped.nextDayId());
        assertNull(wrapped.roundStartedAt());
        assertFalse(day(wrapped, dayA).doneInRound());

        when(statsRepository.findDaySessions(userId, trained)).thenReturn(List.of());
        RoutineResponse fresh = service.decorate(routine(trained, false), userId, Optional.of(chosen));
        assertEquals(dayA.toString(), fresh.nextDayId());
        assertNull(fresh.lastTrainedAt());
        assertFalse(fresh.current());
    }

    @Test
    void templatesAreReturnedAsTheyAre() {
        RoutineResponse template = routine(chosen, true);
        assertSame(template, service.decorate(template, userId, Optional.of(chosen)));
        verify(statsRepository, never()).findDaySessions(any(), any());
    }

    @Test
    void setCurrentSavesTheChoiceAndReturnsTheRoutineAsCurrent() {
        when(routineReadService.getRoutine(edited)).thenReturn(routine(edited, false));

        RoutineResponse result = service.setCurrent(edited);

        verify(routineWriteRepository).setCurrentRoutine(userId, edited);
        assertTrue(result.current());
    }

    @Test
    void setCurrentOnARoutineThatIsNotYoursIs404AndSavesNothing() {
        when(routineReadService.getRoutine(edited)).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Routine not found"));

        ApiException exception = assertThrows(ApiException.class, () -> service.setCurrent(edited));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        verify(routineWriteRepository, never()).setCurrentRoutine(any(), any());
    }

    private static RoutineDayResponse day(RoutineResponse routine, UUID id) {
        return routine.days().stream().filter(day -> day.id().equals(id.toString())).findFirst().orElseThrow();
    }

    private RoutineResponse routine(UUID id, boolean template) {
        // Dias desordenados a proposito: el ciclo sigue day_index.
        return new RoutineResponse(
                id.toString(), "Torso/Pierna", null, null, null, 60, 3, null, null, null,
                template ? "TEMPLATE" : "MANUAL", template, template ? "key" : null,
                List.of(
                        new RoutineDayResponse(dayC.toString(), 3, "C", null, List.of()),
                        new RoutineDayResponse(dayA.toString(), 1, "A", null, List.of()),
                        new RoutineDayResponse(dayB.toString(), 2, "B", null, List.of())),
                List.of());
    }
}
