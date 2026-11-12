package com.locomotora.demo.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionDetail;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionListItem;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionPage;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionUpdateRequest;
import com.locomotora.demo.routine.dto.HistoryDtos.SetUpdate;
import com.locomotora.demo.routine.dto.HistoryDtos.WeekStats;
import com.locomotora.demo.routine.repository.WorkoutHistoryRepository;
import com.locomotora.demo.routine.repository.WorkoutHistoryRepository.LogRow;
import com.locomotora.demo.routine.repository.WorkoutHistoryRepository.SessionRow;
import com.locomotora.demo.routine.repository.WorkoutHistoryRepository.WeekRow;
import java.math.BigDecimal;
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
class WorkoutHistoryServiceTest {
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private final UUID squat = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID press = UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Mock
    private WorkoutHistoryRepository repository;

    private WorkoutHistoryService service;

    @BeforeEach
    void setUp() {
        service = new WorkoutHistoryService(repository);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ lista

    @Test
    void listReturnsACursorOnlyWhenThereIsAnotherPage() {
        List<SessionRow> rows = IntStream.range(0, 3).mapToObj(i -> row(UUID.randomUUID(), Instant.parse("2026-11-1" + (3 - i) + "T19:00:00Z"))).toList();
        when(repository.findSessions(userId, null, null, 3)).thenReturn(rows);
        when(repository.countRecords(eq(userId), any())).thenReturn(Map.of(UUID.fromString(rows.get(0).item().id()), 2));

        SessionPage page = service.list(null, 2);

        assertEquals(2, page.items().size());
        assertEquals(2, page.items().get(0).recordCount());
        assertEquals(0, page.items().get(1).recordCount());
        assertNotNull(page.nextCursor());

        // El cursor apunta a la ultima sesion de la pagina.
        WorkoutHistoryService.Cursor cursor = WorkoutHistoryService.Cursor.decode(page.nextCursor());
        assertEquals(rows.get(1).completedAt(), cursor.completedAt());
        assertEquals(rows.get(1).item().id(), cursor.id().toString());
    }

    @Test
    void lastPageHasNoCursorAndPageSizeIsCapped() {
        when(repository.findSessions(userId, null, null, WorkoutHistoryService.MAX_PAGE + 1)).thenReturn(List.of());

        SessionPage page = service.list("", 500);

        assertTrue(page.items().isEmpty());
        assertNull(page.nextCursor());
    }

    @Test
    void cursorRoundTripsAndRejectsGarbage() {
        Instant at = Instant.parse("2026-11-11T19:00:00.123456Z");
        WorkoutHistoryService.Cursor cursor = new WorkoutHistoryService.Cursor(at, sessionId);
        assertEquals(cursor, WorkoutHistoryService.Cursor.decode(cursor.encode()));

        ApiException exception = assertThrows(ApiException.class, () -> service.list("no-es-un-cursor", null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    }

    // ------------------------------------------------------------------ detalle

    @Test
    void detailGroupsSetsByExerciseAndMarksTheRecordSet() {
        when(repository.findSession(sessionId, userId)).thenReturn(Optional.of(row(sessionId, Instant.parse("2026-11-11T19:00:00Z"))));
        UUID set1 = UUID.randomUUID();
        UUID set2 = UUID.randomUUID();
        UUID set3 = UUID.randomUUID();
        when(repository.findLogs(sessionId)).thenReturn(List.of(
                log(set1, squat, 1, 1, 10, "80"),
                log(set2, squat, 1, 2, 10, "82.5"),
                log(set3, press, 2, 1, 12, "180")));
        // Mejor previa de sentadilla: 80 x 10 (106.7). La prensa nunca se hizo: no hay record.
        when(repository.findPriorBest(userId, sessionId)).thenReturn(Map.of(squat, new BigDecimal("106.67")));

        SessionDetail detail = service.detail(sessionId);

        assertEquals(2, detail.exercises().size());
        assertEquals(List.of(false, true), detail.exercises().get(0).sets().stream().map(s -> s.record()).toList());
        assertFalse(detail.exercises().get(1).sets().get(0).record());
        assertEquals(1, detail.session().recordCount());
        assertEquals(8, detail.exercises().get(0).plan().repsMin());
    }

    @Test
    void recordNeedsToBeatThePriorBestByMoreThanRounding() {
        List<LogRow> same = List.of(log(UUID.randomUUID(), squat, 1, 1, 10, "80"));
        assertNull(WorkoutHistoryService.recordSet(same, new BigDecimal("106.66")));
        assertNull(WorkoutHistoryService.recordSet(same, null));
        assertNotNull(WorkoutHistoryService.recordSet(same, new BigDecimal("100")));
    }

    @Test
    void detailOfAnotherUsersSessionIsNotFound() {
        when(repository.findSession(sessionId, userId)).thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class, () -> service.detail(sessionId));
        assertEquals(HttpStatus.NOT_FOUND, exception.status());
    }

    // ------------------------------------------------------------------ correccion

    @Test
    void updateChangesSetsDeletesAndCleansNotes() {
        UUID kept = UUID.randomUUID();
        UUID removed = UUID.randomUUID();
        when(repository.findSession(sessionId, userId)).thenReturn(Optional.of(row(sessionId, Instant.now())));
        when(repository.updateSet(sessionId, kept, 9, new BigDecimal("80"))).thenReturn(true);
        when(repository.deleteSet(sessionId, removed)).thenReturn(true);
        when(repository.countCompletedSets(sessionId)).thenReturn(1);

        service.update(sessionId, new SessionUpdateRequest(7, "   ", List.of(new SetUpdate(kept, 9, new BigDecimal("80"))), List.of(removed)));

        verify(repository).updateSessionInfo(sessionId, 7, null);
    }

    @Test
    void updateRejectsSetsFromAnotherSession() {
        UUID foreign = UUID.randomUUID();
        when(repository.findSession(sessionId, userId)).thenReturn(Optional.of(row(sessionId, Instant.now())));
        when(repository.updateSet(eq(sessionId), eq(foreign), any(), any())).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> service.update(sessionId,
                new SessionUpdateRequest(null, null, List.of(new SetUpdate(foreign, 5, BigDecimal.TEN)), null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(repository, never()).updateSessionInfo(any(), any(), any());
    }

    @Test
    void updateCannotLeaveTheSessionWithoutSets() {
        UUID only = UUID.randomUUID();
        when(repository.findSession(sessionId, userId)).thenReturn(Optional.of(row(sessionId, Instant.now())));
        when(repository.deleteSet(sessionId, only)).thenReturn(true);
        when(repository.countCompletedSets(sessionId)).thenReturn(0);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.update(sessionId, new SessionUpdateRequest(null, null, null, List.of(only))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(repository, never()).updateSessionInfo(any(), any(), any());
    }

    @Test
    void updateRejectsUpdatingAndDeletingTheSameSet() {
        UUID set = UUID.randomUUID();
        when(repository.findSession(sessionId, userId)).thenReturn(Optional.of(row(sessionId, Instant.now())));

        assertThrows(ApiException.class, () -> service.update(sessionId,
                new SessionUpdateRequest(null, null, List.of(new SetUpdate(set, 5, BigDecimal.TEN)), List.of(set))));
        verify(repository, never()).updateSet(any(), any(), any(), any());
    }

    @Test
    void deleteOfMissingSessionIsNotFound() {
        when(repository.deleteSession(sessionId, userId)).thenReturn(false);
        ApiException exception = assertThrows(ApiException.class, () -> service.delete(sessionId));
        assertEquals(HttpStatus.NOT_FOUND, exception.status());
    }

    // ------------------------------------------------------------------ progreso

    @Test
    void weeklyFillsEmptyWeeksOldestFirst() {
        // Miercoles 11 nov 2026 en Buenos Aires: semana actual desde el lunes 9.
        when(repository.findWeeklyTotals(eq(userId), eq("America/Argentina/Buenos_Aires"), any())).thenReturn(List.of(
                new WeekRow(LocalDate.of(2026, 10, 26), 3, 3, 40, new BigDecimal("30000")),
                new WeekRow(LocalDate.of(2026, 11, 9), 1, 1, 13, new BigDecimal("12797.5"))));

        List<WeekStats> weeks = service.weekly("America/Argentina/Buenos_Aires", 4, Instant.parse("2026-11-12T02:30:00Z"));

        assertEquals(List.of("2026-10-19", "2026-10-26", "2026-11-02", "2026-11-09"), weeks.stream().map(WeekStats::weekStart).toList());
        assertEquals(List.of(0, 3, 0, 1), weeks.stream().map(WeekStats::sessions).toList());
        assertEquals(BigDecimal.ZERO, weeks.get(2).volumeKg());
    }

    @Test
    void exerciseHistoryOfUnknownExerciseIsNotFound() {
        when(repository.findExercise(squat)).thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class, () -> service.exerciseHistory(squat, null));
        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        verify(repository, never()).findExercisePoints(any(), any(), anyInt());
    }

    @Test
    void exerciseHistoryUsesDefaultLimit() {
        when(repository.findExercise(squat)).thenReturn(Optional.of(new WorkoutHistoryRepository.ExerciseInfo(squat, "Sentadilla", "Cuádriceps")));
        when(repository.findExercisePoints(userId, squat, WorkoutHistoryService.DEFAULT_POINTS)).thenReturn(List.of());

        assertEquals("Sentadilla", service.exerciseHistory(squat, null).name());
        verify(repository).findExerciseRecords(eq(userId), eq(squat));
        verify(repository, never()).findSessions(any(), isNull(), isNull(), anyInt());
    }

    // ------------------------------------------------------------------ helpers

    private SessionRow row(UUID id, Instant completedAt) {
        SessionListItem item = new SessionListItem(
                id.toString(), "ROUTINE", null, "Upper/Lower", "Lower A",
                completedAt.minusSeconds(3240).toString(), completedAt.toString(), 3240,
                4, 2, new BigDecimal("3027.50"), 8, 0);
        return new SessionRow(item, "nota", completedAt);
    }

    private LogRow log(UUID id, UUID exerciseId, int position, int setNumber, int reps, String weight) {
        return new LogRow(id, exerciseId, exerciseId.equals(squat) ? "Sentadilla" : "Prensa", "Cuádriceps", null,
                position, setNumber, reps, new BigDecimal(weight), true, null, 8, 10, null, 120);
    }
}
