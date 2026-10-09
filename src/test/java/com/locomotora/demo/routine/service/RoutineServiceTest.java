package com.locomotora.demo.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.ExerciseRequest;
import com.locomotora.demo.routine.dto.RoutineDayRequest;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.dto.RoutineRequest;
import com.locomotora.demo.routine.repository.RoutineWriteRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTest {
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID routineId = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private final UUID dayOneId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID dayTwoId = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private final UUID exerciseId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID routineExerciseOneId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private final UUID routineExerciseTwoId = UUID.fromString("00000000-0000-0000-0000-000000000302");

    @Mock
    private RoutineReadService routineReadService;

    @Mock
    private RoutineWriteRepository routineWriteRepository;

    private RoutineService routineService;

    @BeforeEach
    void setUp() {
        routineService = new RoutineService(routineReadService, routineWriteRepository);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRoutinePersistsMultiDayPayload() {
        RoutineRequest request = routineRequest(List.of(
                day(2, "Piernas", exercise("Sentadilla")),
                day(1, "Torso", exercise("Press banca"))
        ), null);
        when(routineReadService.routineCategoriesAvailable()).thenReturn(true);
        when(routineWriteRepository.createRoutine(eq(userId), same(request), eq("INTERMEDIATE"), isNull(), eq(true)))
                .thenReturn(routineId);
        when(routineWriteRepository.createDay(eq(routineId), eq(1), eq("Torso"), isNull())).thenReturn(dayOneId);
        when(routineWriteRepository.createDay(eq(routineId), eq(2), eq("Piernas"), isNull())).thenReturn(dayTwoId);
        when(routineWriteRepository.findOrCreateExercise(any(ExerciseRequest.class))).thenReturn(exerciseId);
        when(routineReadService.getRoutine(routineId)).thenReturn(response());

        routineService.createRoutine(request);

        verify(routineWriteRepository, never()).deleteDays(any());
        verify(routineWriteRepository).createDay(routineId, 1, "Torso", null);
        verify(routineWriteRepository).createDay(routineId, 2, "Piernas", null);
        verify(routineWriteRepository, times(2)).insertRoutineExercise(any(UUID.class), eq(exerciseId), eq(1), any(ExerciseRequest.class));
    }

    @Test
    void createRoutineKeepsLegacyExercisePayloadCompatible() {
        RoutineRequest request = routineRequest(null, List.of(exercise("Remo")));
        when(routineReadService.routineCategoriesAvailable()).thenReturn(false);
        when(routineWriteRepository.createRoutine(eq(userId), same(request), eq("INTERMEDIATE"), isNull(), eq(false)))
                .thenReturn(routineId);
        when(routineWriteRepository.createDay(eq(routineId), eq(1), eq("Dia 1"), eq("General"))).thenReturn(dayOneId);
        when(routineWriteRepository.findOrCreateExercise(any(ExerciseRequest.class))).thenReturn(exerciseId);
        when(routineReadService.getRoutine(routineId)).thenReturn(response());

        routineService.createRoutine(request);

        verify(routineWriteRepository).createDay(routineId, 1, "Dia 1", "General");
        verify(routineWriteRepository).insertRoutineExercise(dayOneId, exerciseId, 1, request.exercises().getFirst());
    }

    @Test
    void updateRoutineAllowsMultiDayRoutineOwnedByUser() {
        RoutineRequest request = routineRequest(List.of(day(1, "Full body", exercise("Peso muerto"))), null);
        when(routineReadService.routineCategoriesAvailable()).thenReturn(false);
        when(routineWriteRepository.updateRoutine(eq(routineId), eq(userId), same(request), eq("INTERMEDIATE"), isNull(), eq(false)))
                .thenReturn(1);
        when(routineWriteRepository.createDay(eq(routineId), eq(1), eq("Full body"), isNull())).thenReturn(dayOneId);
        when(routineWriteRepository.findOrCreateExercise(any(ExerciseRequest.class))).thenReturn(exerciseId);
        when(routineReadService.getRoutine(routineId)).thenReturn(response());

        routineService.updateRoutine(routineId, request);

        verify(routineWriteRepository).insertRoutineExercise(dayOneId, exerciseId, 1, request.days().getFirst().exercises().getFirst());
    }

    @Test
    void updateRoutinePreservesExistingDayAndExerciseIds() {
        ExerciseRequest kept = exercise(routineExerciseOneId, "Press banca");
        ExerciseRequest added = exercise("Remo");
        RoutineRequest request = routineRequest(List.of(day(dayOneId, 1, "Torso", kept, added)), null);
        stubOwnedUpdate(request);
        when(routineWriteRepository.findDayIds(routineId)).thenReturn(Set.of(dayOneId));
        when(routineWriteRepository.findRoutineExerciseIds(routineId)).thenReturn(Set.of(routineExerciseOneId));

        routineService.updateRoutine(routineId, request);

        verify(routineWriteRepository).updateDay(dayOneId, 1, "Torso", null);
        verify(routineWriteRepository).updateRoutineExercise(routineExerciseOneId, dayOneId, exerciseId, 1, kept);
        verify(routineWriteRepository).insertRoutineExercise(dayOneId, exerciseId, 2, added);
        verify(routineWriteRepository, never()).createDay(any(), anyInt(), any(), any());
        verify(routineWriteRepository, never()).deleteDays(any());
        verify(routineWriteRepository, never()).deleteRoutineExercises(any());
    }

    @Test
    void updateRoutineDeletesDaysAndExercisesMissingFromRequest() {
        RoutineRequest request = routineRequest(
                List.of(day(dayOneId, 1, "Torso", exercise(routineExerciseOneId, "Press banca"))),
                null
        );
        stubOwnedUpdate(request);
        when(routineWriteRepository.findDayIds(routineId)).thenReturn(Set.of(dayOneId, dayTwoId));
        when(routineWriteRepository.findRoutineExerciseIds(routineId))
                .thenReturn(Set.of(routineExerciseOneId, routineExerciseTwoId));

        routineService.updateRoutine(routineId, request);

        verify(routineWriteRepository).deleteRoutineExercises(List.of(routineExerciseTwoId));
        verify(routineWriteRepository).deleteDays(List.of(dayTwoId));
    }

    @Test
    void updateRoutineMovesExerciseOutOfDeletedDay() {
        ExerciseRequest moved = exercise(routineExerciseTwoId, "Sentadilla");
        RoutineRequest request = routineRequest(List.of(day(dayOneId, 1, "Full body", moved)), null);
        stubOwnedUpdate(request);
        when(routineWriteRepository.findDayIds(routineId)).thenReturn(Set.of(dayOneId, dayTwoId));
        when(routineWriteRepository.findRoutineExerciseIds(routineId)).thenReturn(Set.of(routineExerciseTwoId));

        routineService.updateRoutine(routineId, request);

        InOrder inOrder = inOrder(routineWriteRepository);
        inOrder.verify(routineWriteRepository).updateRoutineExercise(routineExerciseTwoId, dayOneId, exerciseId, 1, moved);
        inOrder.verify(routineWriteRepository).deleteDays(List.of(dayTwoId));
        verify(routineWriteRepository, never()).deleteRoutineExercises(any());
    }

    @Test
    void updateRoutineTreatsForeignOrRepeatedIdsAsNew() {
        UUID foreignDayId = UUID.fromString("00000000-0000-0000-0000-000000000999");
        ExerciseRequest first = exercise(routineExerciseOneId, "Press banca");
        ExerciseRequest repeated = exercise(routineExerciseOneId, "Press banca");
        RoutineRequest request = routineRequest(List.of(day(foreignDayId, 1, "Torso", first, repeated)), null);
        stubOwnedUpdate(request);
        when(routineWriteRepository.findDayIds(routineId)).thenReturn(Set.of());
        when(routineWriteRepository.findRoutineExerciseIds(routineId)).thenReturn(Set.of(routineExerciseOneId));
        when(routineWriteRepository.createDay(routineId, 1, "Torso", null)).thenReturn(dayOneId);

        routineService.updateRoutine(routineId, request);

        verify(routineWriteRepository, never()).updateDay(any(), anyInt(), any(), any());
        verify(routineWriteRepository).updateRoutineExercise(routineExerciseOneId, dayOneId, exerciseId, 1, first);
        verify(routineWriteRepository).insertRoutineExercise(dayOneId, exerciseId, 2, repeated);
    }

    @Test
    void updateRoutineRejectsGlobalTemplate() {
        RoutineRequest request = routineRequest(List.of(day(1, "Full body", exercise("Dominadas"))), null);
        when(routineReadService.routineCategoriesAvailable()).thenReturn(false);
        when(routineWriteRepository.updateRoutine(eq(routineId), eq(userId), same(request), eq("INTERMEDIATE"), isNull(), eq(false)))
                .thenReturn(0);
        when(routineWriteRepository.isActiveGlobalTemplate(routineId)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> routineService.updateRoutine(routineId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(routineWriteRepository, never()).findDayIds(any());
        verify(routineWriteRepository, never()).deleteDays(any());
    }

    @Test
    void createRoutineRejectsEmptyTrainingDay() {
        RoutineRequest request = routineRequest(List.of(new RoutineDayRequest(null, 1, "Torso", null, List.of())), null);
        when(routineReadService.routineCategoriesAvailable()).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> routineService.createRoutine(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(routineWriteRepository, never()).createRoutine(any(UUID.class), any(RoutineRequest.class), any(), any(), anyBoolean());
    }

    private RoutineRequest routineRequest(List<RoutineDayRequest> days, List<ExerciseRequest> exercises) {
        return new RoutineRequest(
                "Rutina",
                null,
                "Descripcion",
                "BUILD_MUSCLE",
                "intermediate",
                60,
                3,
                days,
                exercises
        );
    }

    private void stubOwnedUpdate(RoutineRequest request) {
        when(routineReadService.routineCategoriesAvailable()).thenReturn(false);
        when(routineWriteRepository.updateRoutine(eq(routineId), eq(userId), same(request), eq("INTERMEDIATE"), isNull(), eq(false)))
                .thenReturn(1);
        when(routineWriteRepository.findOrCreateExercise(any(ExerciseRequest.class))).thenReturn(exerciseId);
        when(routineReadService.getRoutine(routineId)).thenReturn(response());
    }

    private RoutineDayRequest day(int dayIndex, String title, ExerciseRequest exercise) {
        return new RoutineDayRequest(null, dayIndex, title, null, List.of(exercise));
    }

    private RoutineDayRequest day(UUID id, int dayIndex, String title, ExerciseRequest... exercises) {
        return new RoutineDayRequest(id.toString(), dayIndex, title, null, List.of(exercises));
    }

    private ExerciseRequest exercise(String name) {
        return exercise(null, name);
    }

    private ExerciseRequest exercise(UUID id, String name) {
        return new ExerciseRequest(id == null ? null : id.toString(), null, name, 3, 10, null, null, 60, null, null, null);
    }

    private RoutineResponse response() {
        return new RoutineResponse(
                routineId.toString(),
                "Rutina",
                "Descripcion",
                "BUILD_MUSCLE",
                "INTERMEDIATE",
                60,
                3,
                null,
                null,
                null,
                "MANUAL",
                false,
                null,
                List.of(),
                List.of()
        );
    }
}