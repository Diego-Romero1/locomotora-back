package com.locomotora.demo.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.LastPerformanceResponse;
import com.locomotora.demo.routine.dto.RoutineDayResponse;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.dto.TodayResponse;
import com.locomotora.demo.routine.repository.TrainingStatsRepository;
import com.locomotora.demo.routine.repository.TrainingStatsRepository.RoutineCandidate;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
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
class TrainingServiceTest {
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID trainedRoutine = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private final UUID newRoutine = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private final UUID dayOne = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID dayTwo = UUID.fromString("00000000-0000-0000-0000-000000000102");
    // Miercoles 11 nov 2026, 23:30 en Buenos Aires = jueves 12 02:30 UTC.
    private final Instant now = Instant.parse("2026-11-12T02:30:00Z");

    @Mock
    private TrainingStatsRepository statsRepository;
    @Mock
    private WorkoutSessionWriteRepository sessionRepository;
    @Mock
    private RoutineReadService routineReadService;

    private TrainingService service;

    @BeforeEach
    void setUp() {
        service = new TrainingService(statsRepository, sessionRepository, routineReadService);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void todayKeepsTheTrainedRoutineAndRotatesTheDay() {
        when(statsRepository.findLastTrainedActiveRoutine(userId))
                .thenReturn(Optional.of(new RoutineCandidate(trainedRoutine, Instant.parse("2026-11-09T19:00:00Z"))));
        when(statsRepository.findLatestUpdatedActiveRoutine(userId))
                .thenReturn(Optional.of(new RoutineCandidate(trainedRoutine, Instant.parse("2026-10-01T10:00:00Z"))));
        when(routineReadService.getRoutine(trainedRoutine)).thenReturn(routine(trainedRoutine, 2));
        when(statsRepository.findLastTrainedDayId(userId, trainedRoutine)).thenReturn(Optional.of(dayOne));
        when(statsRepository.findTrainedDates(eq(userId), anyString(), any())).thenReturn(List.of(LocalDate.of(2026, 11, 9)));

        TodayResponse today = service.today("America/Argentina/Buenos_Aires", now);

        assertEquals(trainedRoutine.toString(), today.routine().id());
        assertEquals(dayTwo.toString(), today.nextDayId());
        // En Buenos Aires todavia es miercoles 11: la semana empieza el lunes 9.
        assertEquals("2026-11-09", today.week().weekStart());
        assertEquals(List.of("2026-11-09"), today.week().trainedDates());
        assertEquals(2, today.week().targetDays());
        assertNull(today.lastSession());
    }

    @Test
    void aRoutineCreatedAfterTheLastWorkoutBecomesTheCurrentOne() {
        when(statsRepository.findLastTrainedActiveRoutine(userId))
                .thenReturn(Optional.of(new RoutineCandidate(trainedRoutine, Instant.parse("2026-11-09T19:00:00Z"))));
        when(statsRepository.findLatestUpdatedActiveRoutine(userId))
                .thenReturn(Optional.of(new RoutineCandidate(newRoutine, Instant.parse("2026-11-10T12:00:00Z"))));
        when(routineReadService.getRoutine(newRoutine)).thenReturn(routine(newRoutine, 4));
        when(statsRepository.findLastTrainedDayId(userId, newRoutine)).thenReturn(Optional.empty());

        TodayResponse today = service.today(null, now);

        assertEquals(newRoutine.toString(), today.routine().id());
        assertEquals(dayOne.toString(), today.nextDayId());
        assertEquals(4, today.week().targetDays());
    }

    @Test
    void withoutRoutinesUsesTheProfileTarget() {
        when(statsRepository.findProfileTrainingDays(userId)).thenReturn(Optional.of(5));

        TodayResponse today = service.today("UTC", now);

        assertNull(today.routine());
        assertNull(today.nextDayId());
        assertEquals(5, today.week().targetDays());
        assertEquals(0, today.weekStreak());
    }

    @Test
    void rejectsUnknownTimeZone() {
        ApiException exception = assertThrows(ApiException.class, () -> service.today("Mars/Olympus", now));
        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    }

    @Test
    void lastPerformanceKeepsRequestOrderAndFillsMissingWithNull() {
        UUID squat = UUID.randomUUID();
        UUID press = UUID.randomUUID();
        when(statsRepository.findLastSessions(eq(userId), any())).thenReturn(Map.of(
                squat, new LastPerformanceResponse.LastSession("s1", "2026-11-03T18:00:00Z", List.of())));
        when(statsRepository.findBestSets(eq(userId), any())).thenReturn(Map.of());

        List<LastPerformanceResponse> result = service.lastPerformance(List.of(press, squat, press));

        assertEquals(List.of(press.toString(), squat.toString()), result.stream().map(LastPerformanceResponse::exerciseId).toList());
        assertNull(result.get(0).lastSession());
        assertEquals("s1", result.get(1).lastSession().sessionId());
    }

    @Test
    void lastPerformanceRejectsTooManyIds() {
        List<UUID> ids = IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID()).toList();
        ApiException exception = assertThrows(ApiException.class, () -> service.lastPerformance(ids));
        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(statsRepository, never()).findLastSessions(any(), any());
    }

    private RoutineResponse routine(UUID id, Integer daysPerWeek) {
        return new RoutineResponse(
                id.toString(), "Rutina", null, null, null, 60, daysPerWeek, null, null, null, "MANUAL", false, null,
                List.of(
                        new RoutineDayResponse(dayTwo.toString(), 2, "Lower A", null, List.of()),
                        new RoutineDayResponse(dayOne.toString(), 1, "Upper A", null, List.of())),
                List.of());
    }
}
