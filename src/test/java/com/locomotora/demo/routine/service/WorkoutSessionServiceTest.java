package com.locomotora.demo.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.WorkoutSessionExerciseRequest;
import com.locomotora.demo.routine.dto.WorkoutSessionRequest;
import com.locomotora.demo.routine.dto.WorkoutSessionResponse;
import com.locomotora.demo.routine.dto.WorkoutSetRequest;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository.NewSession;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository.PlannedExercise;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository.RoutineDayRef;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository.SetLog;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class WorkoutSessionServiceTest {
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID routineId = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private final UUID firstDayId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID thirdDayId = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private final UUID squatId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID pressId = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private final UUID routineSquatId = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private final UUID clientSessionId = UUID.fromString("00000000-0000-0000-0000-00000000c11e");
    private final UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000301");

    @Mock
    private WorkoutSessionWriteRepository repository;

    private WorkoutSessionService service;

    @BeforeEach
    void setUp() {
        service = new WorkoutSessionService(repository);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savesOneRowPerSetWithTheDayAndPlanOfTheMoment() {
        stubRoutine();
        when(repository.insertSession(any())).thenReturn(Optional.of(sessionId));
        when(repository.findSessionSummary(sessionId, userId)).thenReturn(Optional.of(summary()));

        WorkoutSessionService.Result result = service.saveSession(request(thirdDayId, List.of(
                squat(set(1, 10, "80"), set(2, 10, "82.5"), set(3, 9, "82.5")),
                new WorkoutSessionExerciseRequest(null, pressId, 2, null, List.of(set(1, 12, "180")))
        )));

        assertTrue(result.created());

        ArgumentCaptor<NewSession> session = ArgumentCaptor.forClass(NewSession.class);
        verify(repository).insertSession(session.capture());
        assertEquals(thirdDayId, session.getValue().routineDayId());
        assertEquals("Lower A", session.getValue().dayTitle());
        assertEquals("Upper/Lower", session.getValue().routineTitle());
        assertEquals(WorkoutSessionService.KIND_ROUTINE, session.getValue().kind());
        assertEquals(clientSessionId, session.getValue().clientSessionId());

        List<SetLog> rows = capturedSets();
        assertEquals(4, rows.size());
        assertEquals(List.of(1, 2, 3), rows.subList(0, 3).stream().map(SetLog::setNumber).toList());
        assertEquals(new BigDecimal("82.5"), rows.get(1).weightKg());
        // El plan del momento se copia en cada serie del ejercicio de la rutina.
        assertEquals(routineSquatId, rows.get(0).routineExerciseId());
        assertEquals(8, rows.get(0).targetRepsMin());
        assertEquals(10, rows.get(0).targetRepsMax());
        assertEquals(120, rows.get(0).restSeconds());
        // Un ejercicio agregado durante la sesion queda sin vinculo al plan.
        assertNull(rows.get(3).routineExerciseId());
        assertNull(rows.get(3).targetRepsMin());
        assertEquals(2, rows.get(3).exercisePosition());
    }

    @Test
    void retryWithSameClientSessionIdReturnsExistingSessionWithoutInserting() {
        when(repository.findSessionIdByClientId(userId, clientSessionId)).thenReturn(Optional.of(sessionId));
        when(repository.findSessionSummary(sessionId, userId)).thenReturn(Optional.of(summary()));

        WorkoutSessionService.Result result = service.saveSession(request(thirdDayId, List.of(squat(set(1, 10, "80")))));

        assertFalse(result.created());
        assertEquals(sessionId.toString(), result.session().id());
        verify(repository, never()).insertSession(any());
        verify(repository, never()).insertSetLogs(any(), anyList());
    }

    @Test
    void concurrentDuplicateReturnsTheSessionThatWon() {
        stubRoutine();
        when(repository.findSessionIdByClientId(userId, clientSessionId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(sessionId));
        when(repository.insertSession(any())).thenReturn(Optional.empty());
        when(repository.findSessionSummary(sessionId, userId)).thenReturn(Optional.of(summary()));

        WorkoutSessionService.Result result = service.saveSession(request(thirdDayId, List.of(squat(set(1, 10, "80")))));

        assertFalse(result.created());
        verify(repository, never()).insertSetLogs(any(), anyList());
    }

    @Test
    void fallsBackToFirstDayWhenDayIsMissing() {
        when(repository.findOwnedRoutineTitle(routineId, userId)).thenReturn(Optional.of("Upper/Lower"));
        when(repository.findFirstRoutineDay(routineId)).thenReturn(Optional.of(new RoutineDayRef(firstDayId, "Upper A")));
        when(repository.findPlannedExercises(routineId)).thenReturn(Map.of());
        when(repository.findExistingExerciseIds(any())).thenReturn(Set.of(squatId));
        when(repository.insertSession(any())).thenReturn(Optional.of(sessionId));
        when(repository.findSessionSummary(sessionId, userId)).thenReturn(Optional.of(summary()));

        service.saveSession(request(null, List.of(squat(set(1, 10, "80")))));

        ArgumentCaptor<NewSession> session = ArgumentCaptor.forClass(NewSession.class);
        verify(repository).insertSession(session.capture());
        assertEquals(firstDayId, session.getValue().routineDayId());
    }

    @Test
    void rejectsDayFromAnotherRoutine() {
        when(repository.findOwnedRoutineTitle(routineId, userId)).thenReturn(Optional.of("Upper/Lower"));
        when(repository.findRoutineDay(thirdDayId, routineId)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> service.saveSession(request(thirdDayId, List.of(squat(set(1, 10, "80"))))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(repository, never()).insertSession(any());
    }

    @Test
    void rejectsRoutineOfAnotherUser() {
        when(repository.findOwnedRoutineTitle(routineId, userId)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> service.saveSession(request(thirdDayId, List.of(squat(set(1, 10, "80"))))));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
    }

    @Test
    void rejectsUnknownExercise() {
        stubRoutine();
        when(repository.findExistingExerciseIds(any())).thenReturn(Set.of());

        ApiException exception = assertThrows(ApiException.class,
                () -> service.saveSession(request(thirdDayId, List.of(squat(set(1, 10, "80"))))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(repository, never()).insertSession(any());
    }

    @Test
    void rejectsCompletedBeforeStarted() {
        WorkoutSessionRequest request = new WorkoutSessionRequest(
                clientSessionId, routineId, thirdDayId,
                "2026-11-11T19:00:00Z", "2026-11-11T18:00:00Z",
                null, null, List.of(squat(set(1, 10, "80"))));

        ApiException exception = assertThrows(ApiException.class, () -> service.saveSession(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    }

    @Test
    void rejectsDuplicateSetNumbers() {
        stubRoutine();

        ApiException exception = assertThrows(ApiException.class,
                () -> service.saveSession(request(thirdDayId, List.of(squat(set(1, 10, "80"), set(1, 9, "80"))))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    }

    @Test
    void rejectsSessionWithoutCompletedSets() {
        stubRoutine();
        WorkoutSetRequest skipped = new WorkoutSetRequest(1, 10, new BigDecimal("80"), null, false);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.saveSession(request(thirdDayId, List.of(squat(skipped)))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    }

    @Test
    void freeSessionWithoutRoutineIsMarkedFree() {
        when(repository.findExistingExerciseIds(any())).thenReturn(Set.of(squatId));
        when(repository.insertSession(any())).thenReturn(Optional.of(sessionId));
        when(repository.findSessionSummary(sessionId, userId)).thenReturn(Optional.of(summary()));
        WorkoutSessionRequest request = new WorkoutSessionRequest(
                clientSessionId, null, null,
                "2026-11-11T18:00:00Z", "2026-11-11T18:40:00Z",
                7, "  ", List.of(new WorkoutSessionExerciseRequest(null, squatId, null, null, List.of(set(1, 5, "100")))));

        service.saveSession(request);

        ArgumentCaptor<NewSession> session = ArgumentCaptor.forClass(NewSession.class);
        verify(repository).insertSession(session.capture());
        assertEquals(WorkoutSessionService.KIND_FREE, session.getValue().kind());
        assertNull(session.getValue().routineDayId());
        assertNull(session.getValue().notes());
        assertEquals(1, capturedSets().get(0).exercisePosition());
    }

    private void stubRoutine() {
        when(repository.findOwnedRoutineTitle(routineId, userId)).thenReturn(Optional.of("Upper/Lower"));
        when(repository.findRoutineDay(thirdDayId, routineId)).thenReturn(Optional.of(new RoutineDayRef(thirdDayId, "Lower A")));
        when(repository.findPlannedExercises(routineId)).thenReturn(Map.of(
                routineSquatId, new PlannedExercise(routineSquatId, squatId, 8, 10, null, 120)
        ));
        org.mockito.Mockito.lenient().when(repository.findExistingExerciseIds(any())).thenReturn(Set.of(squatId, pressId));
    }

    @SuppressWarnings("unchecked")
    private List<SetLog> capturedSets() {
        ArgumentCaptor<List<SetLog>> sets = ArgumentCaptor.forClass(List.class);
        verify(repository).insertSetLogs(eq(sessionId), sets.capture());
        return sets.getValue();
    }

    private WorkoutSessionRequest request(UUID dayId, List<WorkoutSessionExerciseRequest> exercises) {
        return new WorkoutSessionRequest(
                clientSessionId, routineId, dayId,
                "2026-11-11T18:02:00Z", "2026-11-11T18:56:00Z",
                8, "Buena sesion", exercises);
    }

    private WorkoutSessionExerciseRequest squat(WorkoutSetRequest... sets) {
        return new WorkoutSessionExerciseRequest(routineSquatId, squatId, 1, null, List.of(sets));
    }

    private WorkoutSetRequest set(int number, int reps, String weight) {
        return new WorkoutSetRequest(number, reps, new BigDecimal(weight), null, true);
    }

    private WorkoutSessionResponse summary() {
        return new WorkoutSessionResponse(
                sessionId.toString(), clientSessionId.toString(), "ROUTINE",
                routineId.toString(), thirdDayId.toString(), "Upper/Lower", "Lower A",
                "2026-11-11T18:02:00Z", "2026-11-11T18:56:00Z", 3240, 4, new BigDecimal("3027.5"));
    }
}
