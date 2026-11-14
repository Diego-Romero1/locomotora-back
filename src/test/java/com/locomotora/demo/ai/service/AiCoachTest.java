package com.locomotora.demo.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.client.AiModelClient.Completion;
import com.locomotora.demo.ai.client.AiModelClient.Role;
import com.locomotora.demo.ai.client.AiModelClient.StopReason;
import com.locomotora.demo.ai.client.AiModelClient.Turn;
import com.locomotora.demo.ai.client.AiUnavailableException;
import com.locomotora.demo.ai.config.AiModelCatalog;
import com.locomotora.demo.ai.config.AiModelCatalog.Feature;
import com.locomotora.demo.ai.config.AiProperties;
import com.locomotora.demo.ai.dto.AiModelsResponse;
import com.locomotora.demo.ai.dto.AiPreferencesRequest;
import com.locomotora.demo.ai.dto.ChatRequest;
import com.locomotora.demo.ai.dto.ChatResponse;
import com.locomotora.demo.ai.dto.MessageResponse;
import com.locomotora.demo.ai.repository.AiContextRepository;
import com.locomotora.demo.ai.repository.AiRepository;
import com.locomotora.demo.ai.repository.AiRepository.ModelChoice;
import com.locomotora.demo.common.ApiException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Fase 5: coach con Claude, modelos intercambiables, limites y respuesta local. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiCoachTest {
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000050");

    @Mock
    private AiRepository aiRepository;
    @Mock
    private AiContextRepository contextRepository;
    @Mock
    private AiModelClient client;

    private AiModelCatalog catalog;
    private AiSettingsService settings;
    private AiService aiService;

    static UserContext context() {
        return new UserContext(
                new UserContext.Profile(30, "MALE", 178.0, 80.0, "MODERATE", "INTERMEDIATE", 4, 60, null),
                null,
                List.of("BUILD_MUSCLE"),
                List.of("Hombro: molestia en press por encima de la cabeza"),
                List.of("DUMBBELL"),
                List.of("Torso pierna"),
                List.of(new UserContext.Session(LocalDate.of(2026, 11, 12), "Torso A", 18, 5200, 8)),
                9,
                List.of(new UserContext.Lift("Press banca", 80, 6))
        );
    }

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties("sk-test", "claude-haiku-5-5,claude-sonnet-5-5,claude-opus-5-5",
                "claude-haiku-5-5", "claude-sonnet-5-5", 40, 6, 120, "");
        catalog = new AiModelCatalog(properties);
        settings = new AiSettingsService(catalog, properties, aiRepository, client);
        aiService = new AiService(aiRepository, contextRepository, settings, client);
        when(contextRepository.load(userId)).thenReturn(context());
        when(aiRepository.findModelChoice(userId)).thenReturn(new ModelChoice(null, null));
        when(aiRepository.createConversation(eq(userId), anyString(), anyString())).thenReturn(conversationId);
        when(aiRepository.findRecentMessages(eq(conversationId), anyInt())).thenReturn(List.of(
                new MessageResponse("1", "USER", "¿Como mejoro mi press banca?", null, "t")));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ modelos

    @Test
    void catalogResolvesUserChoiceOnlyWhenAllowed() {
        assertEquals("claude-haiku-5-5", catalog.resolve(Feature.COACH, null));
        assertEquals("claude-sonnet-5-5", catalog.resolve(Feature.ROUTINE, null));
        assertEquals("claude-opus-5-5", catalog.resolve(Feature.COACH, "claude-opus-5-5"));
        assertEquals("claude-haiku-5-5", catalog.resolve(Feature.COACH, "claude-retired-1"));
        assertFalse(catalog.describe("claude-haiku-5-5").supportsFallbacks());
        assertTrue(catalog.describe("claude-sonnet-5-5").supportsFallbacks());
    }

    @Test
    void defaultFallsBackToFirstAllowedModelWhenMisconfigured() {
        AiProperties properties = new AiProperties("", "claude-sonnet-5-5", "claude-haiku-5-5", "claude-opus-5-5", 40, 6, 120, "");
        AiModelCatalog restricted = new AiModelCatalog(properties);
        assertEquals("claude-sonnet-5-5", restricted.defaultFor(Feature.COACH));
        assertEquals("claude-sonnet-5-5", restricted.defaultFor(Feature.ROUTINE));
        assertEquals(1, restricted.available().size());
    }

    @Test
    void modelsListsChoicesAndMarksCustomSelection() {
        when(client.isConfigured()).thenReturn(true);
        when(aiRepository.findModelChoice(userId)).thenReturn(new ModelChoice("claude-opus-5-5", null));
        when(aiRepository.countUsageLastDay(userId, "COACH")).thenReturn(3);

        AiModelsResponse response = settings.models();

        assertTrue(response.configured());
        assertEquals(3, response.models().size());
        assertEquals("claude-opus-5-5", response.coach().selected());
        assertTrue(response.coach().custom());
        assertEquals(3, response.coach().usedToday());
        assertEquals(40, response.coach().dailyLimit());
        assertEquals("claude-sonnet-5-5", response.routine().selected());
        assertFalse(response.routine().custom());
    }

    @Test
    void preferencesRejectUnknownModelsAndBlankMeansDefault() {
        ApiException exception = assertThrows(ApiException.class,
                () -> settings.updatePreferences(new AiPreferencesRequest("gpt-x", null)));
        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(aiRepository, never()).saveModelChoice(any(), any(), any());

        settings.updatePreferences(new AiPreferencesRequest(" claude-sonnet-5-5 ", ""));
        verify(aiRepository).saveModelChoice(userId, "claude-sonnet-5-5", null);
    }

    // ------------------------------------------------------------------ chat

    @Test
    void chatUsesTheUserChosenModelWithContextAndFallbacks() {
        when(client.isConfigured()).thenReturn(true);
        when(aiRepository.findModelChoice(userId)).thenReturn(new ModelChoice("claude-sonnet-5-5", null));
        when(client.complete(any())).thenReturn(new Completion("Baja la barra controlada.", "claude-sonnet-5-5", StopReason.END, 900, 120));

        ChatResponse response = aiService.chat(new ChatRequest(null, "¿Como mejoro mi press banca?"));

        ArgumentCaptor<AiModelClient.Request> request = ArgumentCaptor.forClass(AiModelClient.Request.class);
        verify(client).complete(request.capture());
        assertEquals("claude-sonnet-5-5", request.getValue().model());
        assertTrue(request.getValue().fallbacks());
        assertEquals("medium", request.getValue().effort());
        assertTrue(request.getValue().system().contains("Press banca: 80 kg x 6"));
        assertTrue(request.getValue().system().contains("Hombro"));
        assertEquals("CLAUDE", response.source());
        assertEquals("claude-sonnet-5-5", response.model());
        assertEquals("Baja la barra controlada.", response.answer());
        verify(aiRepository).recordUsage(userId, "COACH", "claude-sonnet-5-5", 900, 120);
        verify(aiRepository).createAssistantMessage(eq(conversationId), eq("Baja la barra controlada."), eq("claude-sonnet-5-5"),
                argThat(json -> json.contains("\"source\":\"CLAUDE\"") && json.contains("\"outputTokens\":120")));
    }

    @Test
    void haikuIsCalledWithoutServerFallbacks() {
        when(client.isConfigured()).thenReturn(true);
        when(client.complete(any())).thenReturn(new Completion("Ok", "claude-haiku-5-5", StopReason.END, 10, 5));

        aiService.chat(new ChatRequest(null, "Hola"));

        verify(client).complete(argThat(request -> request.model().equals("claude-haiku-5-5") && !request.fallbacks()));
    }

    @Test
    void chatRejectsAConversationOfAnotherUserBeforeSavingAnything() {
        UUID foreign = UUID.randomUUID();
        when(aiRepository.conversationBelongsTo(foreign, userId)).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class,
                () -> aiService.chat(new ChatRequest(foreign.toString(), "Hola")));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        verify(aiRepository, never()).createUserMessage(any(), any());
        verify(client, never()).complete(any());
    }

    @Test
    void chatRejectsMalformedConversationId() {
        ApiException exception = assertThrows(ApiException.class, () -> aiService.chat(new ChatRequest("abc", "Hola")));
        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    }

    @Test
    void withoutApiKeyTheLocalCoachAnswersWithUserData() {
        when(client.isConfigured()).thenReturn(false);

        ChatResponse response = aiService.chat(new ChatRequest(null, "¿Cuanta proteina necesito?"));

        assertEquals("LOCAL", response.source());
        assertEquals(LocalCoach.MODEL, response.model());
        assertTrue(response.answer().contains("128"), response.answer());
        verify(client, never()).complete(any());
        verify(aiRepository, never()).recordUsage(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void whenClaudeFailsTheLocalCoachAnswers() {
        when(client.isConfigured()).thenReturn(true);
        when(client.complete(any())).thenThrow(new AiUnavailableException("timeout"));

        ChatResponse response = aiService.chat(new ChatRequest(null, "Dame una rutina"));

        assertEquals("LOCAL", response.source());
        verify(aiRepository).createAssistantMessage(eq(conversationId), anyString(), eq(LocalCoach.MODEL),
                argThat(json -> json.contains("\"source\":\"LOCAL\"")));
    }

    @Test
    void refusalIsReplacedBySafeAnswer() {
        when(client.isConfigured()).thenReturn(true);
        when(client.complete(any())).thenReturn(new Completion("", "claude-haiku-5-5", StopReason.REFUSAL, 10, 0));

        ChatResponse response = aiService.chat(new ChatRequest(null, "..."));

        assertEquals(AiService.REFUSAL_ANSWER, response.answer());
    }

    @Test
    void dailyLimitStopsTheChatBeforeSaving() {
        when(client.isConfigured()).thenReturn(true);
        when(aiRepository.countUsageLastDay(userId, "COACH")).thenReturn(40);

        ApiException exception = assertThrows(ApiException.class, () -> aiService.chat(new ChatRequest(null, "Hola")));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.status());
        verify(aiRepository, never()).createConversation(any(), any(), any());
    }

    @Test
    void historyStartsWithUserAndMergesRepeatedRoles() {
        List<Turn> turns = AiService.turns(List.of(
                new MessageResponse("1", "ASSISTANT", "hola", null, "t"),
                new MessageResponse("2", "USER", "a", null, "t"),
                new MessageResponse("3", "USER", "b", null, "t"),
                new MessageResponse("4", "ASSISTANT", "c", null, "t"),
                new MessageResponse("5", "USER", "d", null, "t")));

        assertEquals(3, turns.size());
        assertEquals(new Turn(Role.USER, "a\n\nb"), turns.get(0));
        assertEquals(Role.ASSISTANT, turns.get(1).role());
        assertEquals(new Turn(Role.USER, "d"), turns.get(2));
    }
}
