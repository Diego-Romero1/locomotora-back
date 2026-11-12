package com.locomotora.demo.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import com.locomotora.demo.routine.dto.ExerciseRequest;
import com.locomotora.demo.routine.dto.ExerciseResponse;
import com.locomotora.demo.routine.dto.OwnExerciseRequest;
import com.locomotora.demo.routine.dto.RoutineDayRequest;
import com.locomotora.demo.routine.dto.RoutineDayResponse;
import com.locomotora.demo.routine.dto.RoutineRequest;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.repository.RoutineRepository;
import com.locomotora.demo.routine.repository.RoutineWriteRepository;
import java.util.List;
import java.util.Optional;
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

/** Fase 4: duplicar, origen, vocabulario de objetivo y ejercicios propios. */
@ExtendWith(MockitoExtension.class)
class RoutineBuilderBackendTest {
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID routineId = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private final UUID copyId = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private final UUID exerciseId = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @Mock
    private RoutineReadService routineReadService;
    @Mock
    private RoutineWriteRepository routineWriteRepository;
    @Mock
    private RoutineRepository routineRepository;

    private RoutineService routineService;
    private ExerciseService exerciseService;

    @BeforeEach
    void setUp() {
        routineService = new RoutineService(routineReadService, routineWriteRepository);
        exerciseService = new ExerciseService(routineRepository);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ objetivo y origen

    @Test
    void objectiveAliasesBecomeTheAppVocabulary() {
        assertEquals("BUILD_MUSCLE", Objectives.requireKnown("hypertrophy"));
        assertEquals("BUILD_MUSCLE", Objectives.requireKnown(" HIPERTROFIA "));
        assertEquals("LOSE_WEIGHT", Objectives.requireKnown("lose_weight"));
        assertNull(Objectives.requireKnown("  "));
        ApiException exception = assertThrows(ApiException.class, () -> Objectives.requireKnown("fuerza, hipertrofia"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    }

    @Test
    void createStoresTheRequestedSourceAndCanonicalObjective() {
        RoutineRequest request = request("HYPERTROPHY", "ai");
        when(routineReadService.routineCategoriesAvailable()).thenReturn(false);
        when(routineWriteRepository.createRoutine(eq(userId), eq(request), eq("BUILD_MUSCLE"), eq("AI"), isNull(), isNull(), eq(false)))
                .thenReturn(routineId);
        when(routineWriteRepository.createDay(eq(routineId), anyInt(), anyString(), any())).thenReturn(UUID.randomUUID());
        when(routineWriteRepository.findOrCreateExercise(any(ExerciseRequest.class), eq(userId))).thenReturn(exerciseId);

        routineService.createRoutine(request);

        verify(routineWriteRepository).createRoutine(eq(userId), eq(request), eq("BUILD_MUSCLE"), eq("AI"), isNull(), isNull(), eq(false));
    }

    @Test
    void createRejectsUnknownSource() {
        when(routineReadService.routineCategoriesAvailable()).thenReturn(false);
        ApiException exception = assertThrows(ApiException.class, () -> routineService.createRoutine(request("BUILD_MUSCLE", "GLOBAL")));
        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(routineWriteRepository, never()).createRoutine(any(), any(), any(), any(), any(), any(), anyBoolean());
    }

    // ------------------------------------------------------------------ duplicar

    @Test
    void duplicateCopiesDaysAndRangesWithoutIds() {
        ExerciseResponse squat = new ExerciseResponse("re-1", exerciseId.toString(), "Sentadilla", 4, 10, 8, 10, 120, "Cuádriceps", "nota", "BARBELL");
        ExerciseResponse plank = new ExerciseResponse("re-2", UUID.randomUUID().toString(), "Plancha", 3, 30, null, null, 60, "Abdomen", null, "BODYWEIGHT");
        RoutineResponse original = new RoutineResponse(
                routineId.toString(), "Upper/Lower", "desc", "BUILD_MUSCLE", "INTERMEDIATE", 60, 4, null, null, null,
                "TEMPLATE", false, null,
                List.of(new RoutineDayResponse("day-1", 1, "Lower A", "Cuádriceps, Glúteos", List.of(squat, plank))),
                List.of());
        when(routineReadService.getRoutine(routineId)).thenReturn(original);
        when(routineReadService.routineCategoriesAvailable()).thenReturn(false);
        ArgumentCaptor<RoutineRequest> captor = ArgumentCaptor.forClass(RoutineRequest.class);
        when(routineWriteRepository.createRoutine(eq(userId), captor.capture(), any(), any(), any(), any(), anyBoolean())).thenReturn(copyId);
        when(routineWriteRepository.createDay(eq(copyId), eq(1), eq("Lower A"), eq("Cuádriceps, Glúteos"))).thenReturn(UUID.randomUUID());
        when(routineWriteRepository.findOrCreateExercise(any(ExerciseRequest.class), eq(userId))).thenReturn(exerciseId);
        when(routineReadService.getRoutine(copyId)).thenReturn(original);

        routineService.duplicateRoutine(routineId, null);

        RoutineRequest copy = captor.getValue();
        assertEquals("Upper/Lower (copia)", copy.title());
        assertEquals("TEMPLATE", copy.source());
        RoutineDayRequest day = copy.days().get(0);
        assertNull(day.id());
        assertNull(day.exercises().get(0).id());
        // Con rango se copia el rango y no un valor fijo; sin rango, el valor.
        assertEquals(8, day.exercises().get(0).repsMin());
        assertNull(day.exercises().get(0).reps());
        assertEquals(30, day.exercises().get(1).reps());
        verify(routineWriteRepository, never()).updateDay(any(), anyInt(), any(), any());
    }

    @Test
    void duplicateKeepsTheTitleWithinTheColumnLimit() {
        String longTitle = "R".repeat(RoutineService.MAX_TITLE_LENGTH);
        RoutineResponse original = new RoutineResponse(routineId.toString(), longTitle, null, null, null, null, null, null, null, null,
                "MANUAL", false, null,
                List.of(new RoutineDayResponse("d", 1, "Día 1", null, List.of(
                        new ExerciseResponse("re", exerciseId.toString(), "Remo", 3, 10, null, null, 60, null, null, null)))),
                List.of());
        when(routineReadService.getRoutine(routineId)).thenReturn(original);
        when(routineReadService.routineCategoriesAvailable()).thenReturn(false);
        ArgumentCaptor<RoutineRequest> captor = ArgumentCaptor.forClass(RoutineRequest.class);
        when(routineWriteRepository.createRoutine(eq(userId), captor.capture(), any(), any(), any(), any(), anyBoolean())).thenReturn(copyId);
        when(routineWriteRepository.createDay(any(), anyInt(), any(), any())).thenReturn(UUID.randomUUID());
        when(routineWriteRepository.findOrCreateExercise(any(ExerciseRequest.class), any())).thenReturn(exerciseId);

        routineService.duplicateRoutine(routineId, "(copy)");

        assertEquals(RoutineService.MAX_TITLE_LENGTH, captor.getValue().title().length());
        assertTrue(captor.getValue().title().endsWith(" (copy)"));
        assertEquals("MANUAL", captor.getValue().source());
    }

    // ------------------------------------------------------------------ ejercicios propios

    @Test
    void createOwnReturnsTheExistingExerciseWithTheSameName() {
        ExerciseCatalogItemResponse global = item("Hip thrust", false);
        when(routineRepository.findVisibleExerciseByName("Hip thrust", userId)).thenReturn(Optional.of(global));

        ExerciseService.Result result = exerciseService.createOwn(new OwnExerciseRequest("  Hip   thrust ", null, null));

        assertFalse(result.created());
        assertEquals(global, result.exercise());
        verify(routineRepository, never()).insertOwnExercise(any(), any(), any(), any());
    }

    @Test
    void createOwnNormalizesEquipmentAndRejectsUnknownValues() {
        when(routineRepository.findVisibleExerciseByName(any(), eq(userId))).thenReturn(Optional.empty());
        when(routineRepository.insertOwnExercise(userId, "Remo con banda", "Espalda", "BAND")).thenReturn(item("Remo con banda", true));

        assertTrue(exerciseService.createOwn(new OwnExerciseRequest("Remo con banda", " Espalda ", "band")).created());

        ApiException exception = assertThrows(ApiException.class,
                () -> exerciseService.createOwn(new OwnExerciseRequest("Otro", null, "rocket")));
        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    }

    private RoutineRequest request(String objective, String source) {
        return new RoutineRequest("Rutina", null, null, objective, null, null, null,
                List.of(new RoutineDayRequest(null, 1, "Día 1", null,
                        List.of(new ExerciseRequest(null, exerciseId.toString(), "Remo", 3, 10, null, null, 60, null, null, null)))),
                null, source);
    }

    private ExerciseCatalogItemResponse item(String name, boolean custom) {
        return new ExerciseCatalogItemResponse(UUID.randomUUID().toString(), name, null, null, null, null, null, null, custom);
    }
}
