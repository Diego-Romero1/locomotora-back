package com.locomotora.demo.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.locomotora.demo.routine.repository.RoutineRepository;
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

/** Fase 5: rutinas generadas con Claude, validadas contra el catalogo. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoutineGenerationServiceTest {
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final List<ExerciseCatalogItemResponse> catalog = List.of(
            item("e1", "Press banca", "Pecho", "BARBELL"),
            item("e2", "Remo con mancuerna", "Espalda", "DUMBBELL"),
            item("e3", "Sentadilla", "Cuádriceps", "BARBELL"));

    @Mock
    private AiRepository aiRepository;
    @Mock
    private AiContextRepository contextRepository;
    @Mock
    private RoutineRepository routineRepository;
    @Mock
    private AiModelClient client;

    private RoutineGenerationService service;

    private static ExerciseCatalogItemResponse item(String id, String name, String muscle, String equipment) {
        return new ExerciseCatalogItemResponse(id, name, null, muscle, equipment, null, null, null, false);
    }

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties("sk-test", "claude-haiku-5-5,claude-sonnet-5-5,claude-opus-5-5",
                "claude-haiku-5-5", "claude-sonnet-5-5", 40, 6, 120, "");
        AiSettingsService settings = new AiSettingsService(new AiModelCatalog(properties), properties, aiRepository, client);
        service = new RoutineGenerationService(client, settings, contextRepository, routineRepository);
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

    private RoutineGenerationRequest request(Integer days, String model) {
        return new RoutineGenerationRequest("Quiero mas pecho", days, 50, null, null, List.of("Pecho"), model);
    }

    private void answer(String json) {
        when(client.complete(any())).thenReturn(new Completion(json, "claude-sonnet-5-5", StopReason.END, 3000, 900));
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

        GeneratedRoutineResponse routine = service.generate(request(2, null));

        assertEquals("Torso y pierna", routine.title());
        assertEquals("BUILD_MUSCLE", routine.objective());
        assertEquals("INTERMEDIATE", routine.difficulty());
        assertEquals(2, routine.daysPerWeek());
        assertEquals("Pecho, Espalda", routine.days().get(0).focus());
        assertEquals("e1", routine.days().get(0).exercises().get(0).exerciseId());
        assertEquals("Press banca", routine.days().get(0).exercises().get(0).name());
        assertNull(routine.days().get(0).exercises().get(1).notes());
        assertEquals(2, routine.days().get(1).dayIndex());
        assertEquals("claude-sonnet-5-5", routine.model());
        assertTrue(routine.warnings().isEmpty());
        verify(aiRepository).recordUsage(userId, "ROUTINE", "claude-sonnet-5-5", 3000, 900);
    }

    @Test
    void sendsTheCatalogAndStructuredSchemaToTheDefaultRoutineModel() {
        answer("{\"title\":\"R\",\"description\":\"\",\"difficulty\":\"BEGINNER\",\"estimatedDurationMinutes\":40,"
                + "\"days\":[{\"title\":\"A\",\"focus\":[],\"exercises\":[{\"ref\":1,\"sets\":3,\"repsMin\":8,\"repsMax\":12,\"restSeconds\":90,\"notes\":\"\"}]}]}");

        service.generate(request(1, null));

        verify(client).complete(argThat(request -> request.model().equals("claude-sonnet-5-5")
                && request.fallbacks()
                && request.effort().equals("high")
                && request.jsonSchema() != null
                && request.turns().get(0).text().contains("1 | Press banca | Pecho | BARBELL")
                && request.turns().get(0).text().contains("Dias por semana: 1")
                && request.turns().get(0).text().contains("Quiero mas pecho")));
    }

    @Test
    void requestModelOverridesTheSavedChoiceButMustBeAllowed() {
        answer("{\"title\":\"R\",\"description\":\"\",\"difficulty\":\"BEGINNER\",\"estimatedDurationMinutes\":40,"
                + "\"days\":[{\"title\":\"A\",\"focus\":[],\"exercises\":[{\"ref\":1,\"sets\":3,\"repsMin\":8,\"repsMax\":12,\"restSeconds\":90,\"notes\":\"\"}]}]}");

        service.generate(request(1, "claude-opus-5-5"));
        verify(client).complete(argThat(request -> request.model().equals("claude-opus-5-5")));

        ApiException exception = assertThrows(ApiException.class, () -> service.generate(request(1, "gpt-x")));
        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    }

    @Test
    void dropsUnknownExercisesEmptyDaysAndClampsValues() {
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
        assertEquals("Extra", routine.days().get(1).title());
        assertEquals(2, routine.days().get(1).dayIndex());
        assertEquals("Dia 1", routine.days().get(0).title());
        GeneratedRoutineResponse.Exercise exercise = routine.days().get(0).exercises().get(0);
        assertEquals(1, routine.days().get(0).exercises().size());
        assertEquals(10, exercise.sets());
        assertEquals(8, exercise.repsMin());
        assertEquals(12, exercise.repsMax());
        assertEquals(0, exercise.restSeconds());
        assertEquals("INTERMEDIATE", routine.difficulty());
        assertEquals(240, routine.estimatedDurationMinutes());
        assertTrue(routine.warnings().containsAll(List.of("UNKNOWN_EXERCISES_DROPPED", "VALUES_ADJUSTED", "FEWER_DAYS")));
    }

    @Test
    void keepsAtMostTheRequestedDays() {
        String day = "{\"title\":\"D\",\"focus\":[],\"exercises\":[{\"ref\":1,\"sets\":3,\"repsMin\":8,\"repsMax\":12,\"restSeconds\":90,\"notes\":\"\"}]}";
        answer("{\"title\":\"R\",\"description\":\"\",\"difficulty\":\"BEGINNER\",\"estimatedDurationMinutes\":40,\"days\":["
                + day + "," + day + "," + day + "]}");

        GeneratedRoutineResponse routine = service.generate(request(2, null));

        assertEquals(2, routine.days().size());
        assertTrue(routine.warnings().contains("EXTRA_DAYS_DROPPED"));
    }

    @Test
    void withoutApiKeyGenerationIsUnavailable() {
        when(client.isConfigured()).thenReturn(false);
        ApiException exception = assertThrows(ApiException.class, () -> service.generate(request(3, null)));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.status());
        verify(client, never()).complete(any());
    }

    @Test
    void apiFailureIsServiceUnavailable() {
        when(client.complete(any())).thenThrow(new AiUnavailableException("down"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());
    }

    @Test
    void refusalIsUnprocessable() {
        when(client.complete(any())).thenReturn(new Completion("", "claude-sonnet-5-5", StopReason.REFUSAL, 10, 0));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());
    }

    @Test
    void invalidOrEmptyJsonIsBadGateway() {
        answer("no json");
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());
        answer("{\"days\":[]}");
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());
    }

    @Test
    void dailyLimitApplies() {
        when(aiRepository.countUsageLastDay(userId, "ROUTINE")).thenReturn(6);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, assertThrows(ApiException.class, () -> service.generate(request(3, null))).status());
        verify(client, never()).complete(any());
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
    void schemaClosesEveryObject() {
        Map<String, Object> schema = RoutineGenerationService.schema();
        assertEquals(false, schema.get("additionalProperties"));
        assertTrue(schema.toString().split("additionalProperties=false", -1).length - 1 == 3);
    }
}
