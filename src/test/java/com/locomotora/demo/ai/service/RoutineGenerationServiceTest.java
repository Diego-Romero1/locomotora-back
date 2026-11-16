package com.locomotora.demo.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.client.AiModelClient.Completion;
import com.locomotora.demo.ai.client.AiModelClient.StopReason;
import com.locomotora.demo.ai.client.AiUnavailableException;
import com.locomotora.demo.ai.config.AiModelCatalog;
import com.locomotora.demo.ai.config.AiProperties;
import com.locomotora.demo.ai.dto.GeneratedRoutineResponse;
import com.locomotora.demo.ai.dto.RoutineGenerationRequest;
import com.locomotora.demo.ai.repository.AiContextRepository;
import com.locomotora.demo.ai.repository.AiRepository;
import com.locomotora.demo.ai.repository.AiRepository.ModelChoice;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Fase 5 (Groq): rutinas con JSON estricto, catalogo filtrado por el backend y reglas de negocio. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoutineGenerationServiceTest {
    private static final String QWEN = AiCoachTest.QWEN;
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private List<ExerciseCatalogItemResponse> catalog;

    @Mock
    private AiRepository aiRepository;
    @Mock
    private AiContextRepository contextRepository;
    @Mock
    private com.locomotora.demo.routine.repository.RoutineRepository routineRepository;
    @Mock
    private AiModelClient client;

    private RoutineGenerationService service;

    private static ExerciseCatalogItemResponse item(String id, String name, String muscle, String equipment) {
        return new ExerciseCatalogItemResponse(id, name, null, muscle, equipment, null, null, null, false);
    }

    @BeforeEach
    void setUp() {
        // e1..e3 primero (refs 1..3), y relleno con mancuernas para que el filtro no quede corto.
        catalog = new ArrayList<>(List.of(
                item("e1", "Press banca", "Pecho", "BARBELL"),
                item("e2", "Remo con mancuerna", "Espalda", "DUMBBELL"),
                item("e3", "Sentadilla", "Cuádriceps", "BARBELL"),
                item("m1", "Prensa de piernas", "Cuádriceps", "MACHINE")));
        for (int index = 0; index < 12; index++) {
            catalog.add(item("d" + index, "Mancuerna " + index, "Hombros", "DUMBBELL"));
        }
        AiProperties properties = AiProperties.withKey("gsk-test", "");
        AiSettingsService settings = new AiSettingsService(new AiModelCatalog(properties), properties, aiRepository, client);
        service = new RoutineGenerationService(client, settings, contextRepository, routineRepository, properties);
        when(client.isConfigured()).thenReturn(true);
        when(contextRepository.load(userId)).thenReturn(AiCoachTest.context());
        when(routineRepository.findExerciseCatalog(userId)).thenReturn(catalog);
        when(aiRepository.findModelChoice(userId)).thenReturn(new ModelChoice(null, null));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RoutineGenerationRequest request(Integer days, String prompt) {
        return new RoutineGenerationRequest(prompt, days, 50, null, null, List.of("Pecho"), null);
    }

    private void answer(String json) {
        org.mockito.Mockito.doReturn(new Completion(json, QWEN, StopReason.END, 3000, 900)).when(client).complete(any());
    }

    private static String oneDay(String exercises) {
        return "{\"title\":\"R\",\"description\":\"\",\"difficulty\":\"INTERMEDIATE\",\"estimatedDurationMinutes\":45,"
                + "\"days\":[{\"title\":\"A\",\"focus\":[],\"exercises\":[" + exercises + "]}]}";
    }

    private static String exercise(int ref, int sets, int min, int max, int rest) {
        return "{\"ref\":" + ref + ",\"sets\":" + sets + ",\"repsMin\":" + min + ",\"repsMax\":" + max
                + ",\"restSeconds\":" + rest + ",\"notes\":\"\"}";
    }

    @Test
    void generatesAValidatedDraftFromTheCatalog() {
        answer("""
                {"title":"Torso y pierna","description":"Para ganar musculo.","difficulty":"INTERMEDIATE",
                 "estimatedDurationMinutes":50,
                 "days":[
                   {"title":"Torso","focus":["Pecho","Espalda"],"exercises":[
                     {"ref":1,"sets":4,"repsMin":6,"repsMax":10,"restSeconds":120,"notes":"Escapulas atras"},
                     {"ref":2,"sets":3,"repsMin":8,"repsMax":12,"restSeconds":90,"notes":""}]},
                   {"title":"Pierna","focus":["Cuádriceps"],"exercises":[
                     {"ref":3,"sets":4,"repsMin":5,"repsMax":8,"restSeconds":150,"notes":""}]}]}
                """);

        GeneratedRoutineResponse routine = service.generate(request(2, "Quiero mas pecho"));

        assertEquals("Torso y pierna", routine.title());
        assertEquals("BUILD_MUSCLE", routine.objective());
        assertEquals(2, routine.daysPerWeek());
        assertEquals("Pecho, Espalda", routine.days().get(0).focus());
        assertEquals("e1", routine.days().get(0).exercises().get(0).exerciseId());
        assertNull(routine.days().get(0).exercises().get(1).notes());
        assertEquals(QWEN, routine.model());
        assertTrue(routine.warnings().isEmpty(), routine.warnings().toString());
        verify(aiRepository).recordUsage(userId, "ROUTINE", QWEN, 3000, 900);
    }

    @Test
    void sendsAFilteredCatalogAndAStrictSchemaWithoutTools() {
        answer(oneDay(exercise(1, 3, 8, 12, 90)));

        service.generate(request(1, "Quiero mas pecho, llamame al +54 11 5555-1234"));

        verify(client).complete(argThat(request -> {
            String user = request.turns().get(0).text();
            return request.model().equals(QWEN)
                    && request.strictJson()
                    && "routine".equals(request.schemaName())
                    && request.jsonSchema() != null
                    && request.tools().isEmpty()
                    && "none".equals(request.reasoningEffort())
                    && request.maxTokens() == 1000
                    // El usuario tiene mancuernas y barra: la maquina queda fuera del catalogo enviado.
                    && user.contains("1 | Press banca | Pecho | BARBELL")
                    && !user.contains("Prensa de piernas")
                    && user.contains("Quiero mas pecho")
                    && user.contains("[telefono]")
                    && !user.contains("5555-1234");
        }));
    }

    @Test
    void dropsUnknownExercisesAndClampsValues() {
        answer("""
                {"title":"R","description":"","difficulty":"EXPERT","estimatedDurationMinutes":500,
                 "days":[
                   {"title":"","focus":[],"exercises":[
                     {"ref":99,"sets":3,"repsMin":8,"repsMax":12,"restSeconds":90,"notes":""},
                     {"ref":1,"sets":30,"repsMin":12,"repsMax":8,"restSeconds":-5,"notes":""},
                     {"ref":1,"sets":3,"repsMin":8,"repsMax":12,"restSeconds":90,"notes":""}]},
                   {"title":"Vacio","focus":[],"exercises":[{"ref":0,"sets":3,"repsMin":8,"repsMax":12,"restSeconds":90,"notes":""}]},
                   {"title":"Extra","focus":[],"exercises":[{"ref":2,"sets":3,"repsMin":8,"repsMax":12,"restSeconds":90,"notes":""}]}]}
                """);

        GeneratedRoutineResponse routine = service.generate(request(3, null));

        assertEquals(2, routine.days().size());
        assertEquals("Dia 1", routine.days().get(0).title());
        GeneratedRoutineResponse.Exercise exercise = routine.days().get(0).exercises().get(0);
        assertEquals(10, exercise.sets());
        assertEquals(8, exercise.repsMin());
        assertEquals(12, exercise.repsMax());
        assertEquals(0, exercise.restSeconds());
        assertEquals("INTERMEDIATE", routine.difficulty());
        assertEquals(240, routine.estimatedDurationMinutes());
        assertTrue(routine.warnings().containsAll(List.of("UNKNOWN_EXERCISES_DROPPED", "VALUES_ADJUSTED", "FEWER_DAYS")));
    }

    @Test
    void beginnersGetAtMostFourSets() {
        when(contextRepository.load(userId)).thenReturn(new UserContext(UserContext.Profile.EMPTY, null, List.of(), List.of(),
                List.of(), List.of(), List.of(), 0, List.of()));
        answer(oneDay(exercise(1, 6, 8, 12, 90)));

        GeneratedRoutineResponse routine = service.generate(new RoutineGenerationRequest(null, 1, 60, null, "BEGINNER", null, null));

        assertEquals(4, routine.days().get(0).exercises().get(0).sets());
        assertTrue(routine.warnings().contains("VALUES_ADJUSTED"));
    }

    @Test
    void warnsWhenADayDoesNotFitTheSessionOrVolumeIsTooHigh() {
        StringBuilder exercises = new StringBuilder();
        for (int ref = 5; ref <= 12; ref++) {
            exercises.append(ref == 5 ? "" : ",").append(exercise(ref, 5, 8, 12, 180));
        }
        answer(oneDay(exercises.toString()));

        GeneratedRoutineResponse routine = service.generate(request(1, null));

        assertTrue(RoutineGenerationService.estimatedMinutes(routine.days().get(0)) > 50 * 1.25);
        assertTrue(routine.warnings().contains("DAY_TOO_LONG"));
        assertTrue(routine.warnings().contains("HIGH_VOLUME"));
    }

    @Test
    void injuryInThePromptAddsInstructionsAndAWarning() {
        answer(oneDay(exercise(1, 3, 8, 12, 90)));

        GeneratedRoutineResponse routine = service.generate(request(1, "Tengo una hernia de disco"));

        assertTrue(routine.warnings().contains("SEE_PROFESSIONAL"));
        verify(client).complete(argThat(request -> request.system().contains("lesion o dolor")));
    }

    @Test
    void keepsAtMostTheRequestedDays() {
        String day = "{\"title\":\"D\",\"focus\":[],\"exercises\":[" + exercise(1, 3, 8, 12, 90) + "]}";
        answer("{\"title\":\"R\",\"description\":\"\",\"difficulty\":\"BEGINNER\",\"estimatedDurationMinutes\":40,\"days\":["
                + day + "," + day + "," + day + "]}");

        GeneratedRoutineResponse routine = service.generate(request(2, null));

        assertEquals(2, routine.days().size());
        assertTrue(routine.warnings().contains("EXTRA_DAYS_DROPPED"));
    }

    @Test
    void withoutApiKeyGenerationIsUnavailable() {
        when(client.isConfigured()).thenReturn(false);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());
        verify(client, never()).complete(any());
    }

    @Test
    void providerErrorsBecomeClearStatuses() {
        org.mockito.Mockito.doThrow(new AiUnavailableException("busy", 429, 20L)).when(client).complete(any());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());

        org.mockito.Mockito.doThrow(new AiUnavailableException("schema", 400, null)).when(client).complete(any());
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());

        org.mockito.Mockito.doThrow(new AiUnavailableException("down", 503, null)).when(client).complete(any());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());
    }

    @Test
    void refusalAndCutOffAndBadJson() {
        org.mockito.Mockito.doReturn(new Completion("", QWEN, StopReason.REFUSAL, 10, 0)).when(client).complete(any());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());
        org.mockito.Mockito.doReturn(new Completion("{\"days\":[", QWEN, StopReason.MAX_TOKENS, 10, 4096)).when(client).complete(any());
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());
        answer("no json");
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());
        answer("{\"days\":[]}");
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());
    }

    @Test
    void dailyLimitsApply() {
        when(aiRepository.countUsageLastDay(userId, "ROUTINE")).thenReturn(5);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());

        when(aiRepository.countUsageLastDay(userId, "ROUTINE")).thenReturn(0);
        when(aiRepository.sumTokensLastDay()).thenReturn(200000L);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());
        verify(client, never()).complete(any());
    }

    @Test
    void requestModelMustBeAllowed() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.generate(new RoutineGenerationRequest(null, 3, 60, null, null, null, "claude-opus-5-5"))).status());
    }

    @Test
    void planUsesProfileWhenRequestIsEmpty() {
        RoutineGenerationService.Plan plan = RoutineGenerationService.plan(
                new RoutineGenerationRequest(null, null, null, null, null, null, null), AiCoachTest.context());
        assertEquals(4, plan.daysPerWeek());
        assertEquals(60, plan.sessionMinutes());
        assertEquals("BUILD_MUSCLE", plan.objective());
        assertEquals("INTERMEDIATE", plan.level());
    }

    @Test
    void schemaIsStrictCompatibleAndFixesTheNumberOfDays() {
        Map<String, Object> schema = RoutineGenerationService.schema(4);
        assertEquals(false, schema.get("additionalProperties"));
        assertEquals(3, schema.toString().split("additionalProperties=false", -1).length - 1);
        assertFalse(schema.toString().contains("minimum"));
        @SuppressWarnings("unchecked")
        Map<String, Object> days = (Map<String, Object>) ((Map<String, Object>) schema.get("properties")).get("days");
        assertEquals(4, days.get("minItems"));
        assertEquals(4, days.get("maxItems"));
        assertEquals(List.of("title", "description", "days"), schema.get("required"));
        assertTrue(schema.toString().contains("tip"));
        assertFalse(RoutineGenerationService.schema(6).toString().contains("tip"), "no tip with 6 days");
        assertEquals(6, RoutineGenerationService.maxExercisesFor(3));
        assertEquals(5, RoutineGenerationService.maxExercisesFor(5));
        assertEquals(4, RoutineGenerationService.maxExercisesFor(6));
    }

    @Test
    void readsTheCompactFormatWithRepsTextAndADayTip() {
        answer("{\"title\":\"R\",\"description\":\"\",\"difficulty\":\"INTERMEDIATE\",\"estimatedDurationMinutes\":45,"
                + "\"days\":[{\"title\":\"A\",\"tip\":\"Escapulas atras\",\"exercises\":["
                + "{\"ref\":1,\"sets\":4,\"reps\":\"6-8\",\"rest\":150},{\"ref\":2,\"sets\":3,\"reps\":\"12\",\"rest\":60}]}]}");

        GeneratedRoutineResponse.Day day = service.generate(request(1, null)).days().get(0);

        assertEquals(6, day.exercises().get(0).repsMin());
        assertEquals(8, day.exercises().get(0).repsMax());
        assertEquals("Escapulas atras", day.exercises().get(0).notes());
        assertEquals(12, day.exercises().get(1).repsMin());
        assertEquals(12, day.exercises().get(1).repsMax());
        assertNull(day.exercises().get(1).notes());
        assertEquals("Pecho, Espalda", day.focus(), "focus comes from the catalog");
        assertNull(RoutineGenerationService.parseReps("seis"));
    }

    @Test
    void readsTheShortKeysTheModelReturns() {
        answer("{\"title\":\"R\",\"description\":\"\",\"difficulty\":\"INTERMEDIATE\",\"estimatedDurationMinutes\":45,"
                + "\"days\":[{\"title\":\"A\",\"focus\":[],\"exercises\":[{\"ref\":1,\"sets\":4,\"min\":6,\"max\":8,\"rest\":150,\"note\":\"Pausa abajo\"}]}]}");

        GeneratedRoutineResponse.Exercise exercise = service.generate(request(1, null)).days().get(0).exercises().get(0);

        assertEquals(6, exercise.repsMin());
        assertEquals(8, exercise.repsMax());
        assertEquals(150, exercise.restSeconds());
        assertEquals("Pausa abajo", exercise.notes());
    }
}
