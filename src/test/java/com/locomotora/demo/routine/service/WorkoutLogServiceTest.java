package com.locomotora.demo.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.WorkoutExerciseLogRequest;
import com.locomotora.demo.routine.dto.WorkoutLogRequest;
import com.locomotora.demo.routine.dto.WorkoutLogResponse;
import com.locomotora.demo.routine.repository.WorkoutSessionRepository;
import com.locomotora.demo.routine.repository.WorkoutSessionRepository.ExerciseLookup;
import java.math.BigDecimal;
import java.sql.Timestamp;
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
class WorkoutLogServiceTest {
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID routineId = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private final UUID firstDayId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID thirdDayId = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private final UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private final UUID routineExerciseId = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private final UUID exerciseId = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @Mock
    private WorkoutSessionRepository workoutSessionRepository;

    private WorkoutLogService workoutLogService;

    @BeforeEach
    void setUp() {
        workoutLogService = new WorkoutLogService(workoutSessionRepository);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logWorkoutUsesRequestedRoutineDay() {
        when(workoutSessionRepository.activeRoutineExists(routineId, userId)).thenReturn(true);
        when(workoutSessionRepository.routineDayBelongsToRoutine(thirdDayId, routineId)).thenReturn(true);
        when(workoutSessionRepository.createCompletedSession(eq(userId), eq(routineId), eq(thirdDayId), any(Timestamp.class)))
                .thenReturn(sessionId);
        ExerciseLookup lookup = new ExerciseLookup(exerciseId, 10, null);
        when(workoutSessionRepository.findExerciseForRoutineLog(routineExerciseId, routineId)).thenReturn(Optional.of(lookup));

        WorkoutLogResponse response = workoutLogService.logWorkout(request(thirdDayId.toString()));

        assertEquals(sessionId.toString(), response.sessionId());
        assertEquals(1, response.completedExercises());
        verify(workoutSessionRepository, never()).findFirstRoutineDayId(any());
        verify(workoutSessionRepository).insertExerciseLog(sessionId, routineExerciseId, lookup, 8, new BigDecimal("60"));
    }

    @Test
    void logWorkoutFallsBackToFirstDayWhenDayIsMissing() {
        when(workoutSessionRepository.activeRoutineExists(routineId, userId)).thenReturn(true);
        when(workoutSessionRepository.findFirstRoutineDayId(routineId)).thenReturn(firstDayId);
        when(workoutSessionRepository.createCompletedSession(eq(userId), eq(routineId), eq(firstDayId), any(Timestamp.class)))
                .thenReturn(sessionId);
        when(workoutSessionRepository.findExerciseForRoutineLog(routineExerciseId, routineId))
                .thenReturn(Optional.of(new ExerciseLookup(exerciseId, 10, null)));

        workoutLogService.logWorkout(request(null));

        verify(workoutSessionRepository).createCompletedSession(eq(userId), eq(routineId), eq(firstDayId), any(Timestamp.class));
    }

    @Test
    void logWorkoutRejectsDayFromAnotherRoutine() {
        when(workoutSessionRepository.activeRoutineExists(routineId, userId)).thenReturn(true);
        when(workoutSessionRepository.routineDayBelongsToRoutine(thirdDayId, routineId)).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class,
                () -> workoutLogService.logWorkout(request(thirdDayId.toString())));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(workoutSessionRepository, never()).createCompletedSession(any(), any(), any(), any());
    }

    @Test
    void logWorkoutRejectsMalformedDayId() {
        when(workoutSessionRepository.activeRoutineExists(routineId, userId)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> workoutLogService.logWorkout(request("routine-day-1")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(workoutSessionRepository, never()).createCompletedSession(any(), any(), any(), any());
    }

    private WorkoutLogRequest request(String routineDayId) {
        return new WorkoutLogRequest(
                routineId.toString(),
                routineDayId,
                List.of(routineExerciseId.toString()),
                List.of(new WorkoutExerciseLogRequest(routineExerciseId.toString(), 8, new BigDecimal("60"))),
                "2026-10-09T19:10:00Z"
        );
    }
}
