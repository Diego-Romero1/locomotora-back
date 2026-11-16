package com.locomotora.demo.ai.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.ai.repository.AiContextRepository;
import com.locomotora.demo.ai.service.UserContext;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import com.locomotora.demo.routine.dto.ExerciseResponse;
import com.locomotora.demo.routine.dto.RoutineDayResponse;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.repository.RoutineRepository;
import com.locomotora.demo.routine.service.RoutineReadService;
import com.locomotora.demo.routine.service.WorkoutHistoryService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

/** Herramientas del agente: alcance por usuario, validacion de argumentos y resultados compactos. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiToolRegistryTest {
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private RoutineRepository routineRepository;
    @Mock
    private RoutineReadService routineReadService;
    @Mock
    private WorkoutHistoryService workoutHistoryService;
    @Mock
    private AiContextRepository contextRepository;

    private AiToolRegistry registry;

    private static ExerciseCatalogItemResponse item(String id, String name, String muscle, String equipment) {
        return new ExerciseCatalogItemResponse(id, name, null, muscle, equipment, null, null, null, false);
    }

    @BeforeEach
    void setUp() {
        registry = new AiToolRegistry(routineRepository, routineReadService, workoutHistoryService, contextRepository);
        List<ExerciseCatalogItemResponse> catalog = new ArrayList<>(List.of(
                item("00000000-0000-0000-0000-0000000000a1", "Sentadilla con barra", "Cuádriceps", "BARBELL"),
                item("00000000-0000-0000-0000-0000000000a2", "Prensa de piernas", "Cuádriceps", "MACHINE"),
                item("00000000-0000-0000-0000-0000000000a3", "Press banca", "Pecho", "BARBELL")));
        for (int index = 0; index < 20; index++) {
            catalog.add(item(UUID.randomUUID().toString(), "Curl " + index, "Bíceps", "DUMBBELL"));
        }
        when(routineRepository.findExerciseCatalog(userId)).thenReturn(catalog);
        when(contextRepository.equipment(userId)).thenReturn(List.of("BARBELL", "DUMBBELLS"));
    }

    @Test
    void offersFiveReadOnlyToolsWithObjectSchemas() {
        assertEquals(List.of("search_exercises", "list_my_routines", "get_routine", "get_recent_training", "get_exercise_progress"),
                registry.specs().stream().map(spec -> spec.name()).toList());
        registry.specs().forEach(spec -> assertEquals("object", spec.parameters().get("type")));
    }

    @Test
    void searchMatchesMuscleWithoutAccentsAndRespectsTheUserEquipment() {
        AiToolRegistry.Result result = registry.execute(userId, "search_exercises", "{\"muscle\":\"cuadriceps\"}");

        assertFalse(result.error());
        assertTrue(result.content().contains("Sentadilla con barra"));
        assertFalse(result.content().contains("Prensa de piernas"), "the user has no machines");
        assertFalse(result.content().contains("0000-0000"), "ids are not exposed");
        assertTrue(result.content().contains("\"equipment\":\"barra\""), "readable equipment names");

        AiToolRegistry.Result all = registry.execute(userId, "search_exercises", "{\"muscle\":\"Cuádriceps\",\"equipment_scope\":\"all\"}");
        assertTrue(all.content().contains("Prensa de piernas"));
        // Compatibilidad con el parametro booleano anterior.
        assertTrue(registry.execute(userId, "search_exercises", "{\"muscle\":\"Cuádriceps\",\"only_my_equipment\":false}")
                .content().contains("Prensa de piernas"));
    }

    @Test
    void toolSchemasAvoidBooleansBecauseQwenWritesThemAsTrue() {
        registry.specs().forEach(spec -> assertFalse(spec.parameters().toString().contains("type=boolean"), spec.name()));
    }

    @Test
    void searchReturnsAtMostFifteenExercises() {
        AiToolRegistry.Result result = registry.execute(userId, "search_exercises", "{\"query\":\"curl\"}");
        assertTrue(result.content().startsWith("{\"count\":15"), result.content());
    }

    @Test
    void invalidArgumentsAndUnknownToolsComeBackAsErrors() {
        assertEquals("{\"error\":\"invalid_arguments\"}", registry.execute(userId, "search_exercises", "{no json").content());
        assertEquals("{\"error\":\"invalid_arguments\"}", registry.execute(userId, "search_exercises", "[1,2]").content());
        assertEquals("{\"error\":\"unknown_tool\"}", registry.execute(userId, "drop_table", "{}").content());
        assertEquals("{\"error\":\"invalid_arguments\"}", registry.execute(userId, "get_routine", "{\"routine_id\":\"abc\"}").content());
        assertTrue(registry.execute(userId, "get_routine", "{}").error());
    }

    @Test
    void routinesOfOtherUsersAreNotFound() {
        UUID foreign = UUID.randomUUID();
        when(routineReadService.getRoutine(foreign)).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Routine not found"));

        AiToolRegistry.Result result = registry.execute(userId, "get_routine", "{\"routine_id\":\"" + foreign + "\"}");

        assertTrue(result.error());
        assertEquals("{\"error\":\"not_found\"}", result.content());
    }

    @Test
    void getRoutineIsCompact() {
        UUID id = UUID.randomUUID();
        ExerciseResponse squat = new ExerciseResponse("re1", "e1", "Sentadilla", 4, 8, 6, 8, 150, "Cuádriceps", null, "BARBELL");
        when(routineReadService.getRoutine(id)).thenReturn(new RoutineResponse(id.toString(), "Torso pierna", null, "BUILD_MUSCLE",
                "INTERMEDIATE", 60, 4, null, null, null, "MANUAL", false, null,
                List.of(new RoutineDayResponse("d1", 1, "Pierna", null, List.of(squat))), List.of()));

        AiToolRegistry.Result result = registry.execute(userId, "get_routine", "{\"routine_id\":\"" + id + "\"}");

        assertFalse(result.error());
        assertTrue(result.content().contains("Sentadilla 4x6-8 descanso 150s"), result.content());
    }

    @Test
    void recentTrainingClampsTheRange() {
        when(contextRepository.recentSessions(userId, 90, 10)).thenReturn(List.of(
                new UserContext.Session(LocalDate.of(2026, 11, 12), "Torso A", 18, 5200.4, 8)));

        AiToolRegistry.Result result = registry.execute(userId, "get_recent_training", "{\"days\":500}");

        verify(contextRepository).recentSessions(userId, 90, 10);
        assertTrue(result.content().contains("\"volume_kg\":5200"));
    }

    @Test
    void exerciseProgressForAnUnknownExerciseIsNotFound() {
        assertEquals("{\"error\":\"not_found\"}",
                registry.execute(userId, "get_exercise_progress", "{\"exercise_name\":\"Remo gorila\"}").content());
    }

    @Test
    void longResultsAreTruncated() {
        String truncated = AiToolRegistry.truncate("x".repeat(5000));
        assertTrue(truncated.length() < 2600);
        assertTrue(truncated.endsWith("(recortado)"));
    }
}
