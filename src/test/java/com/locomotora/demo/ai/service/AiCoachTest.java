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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.client.AiModelClient.Completion;
import com.locomotora.demo.ai.client.AiModelClient.Role;
import com.locomotora.demo.ai.client.AiModelClient.StopReason;
import com.locomotora.demo.ai.client.AiModelClient.ToolCall;
import com.locomotora.demo.ai.client.AiModelClient.ToolChoice;
import com.locomotora.demo.ai.client.AiModelClient.ToolSpec;
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
import com.locomotora.demo.ai.tools.AiToolRegistry;
import com.locomotora.demo.common.ApiException;
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

/** Fase 5 (Groq): agente con herramientas, presupuesto de tokens, datos personales y seguridad. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiCoachTest {
    static final String QWEN = "qwen/qwen3.8-27b";
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000050");

    @Mock
    private AiRepository aiRepository;
    @Mock
    private AiContextRepository contextRepository;
    @Mock
    private AiModelClient client;
    @Mock
    private AiToolRegistry tools;

    private AiProperties properties;
    private AiModelCatalog catalog;
    private AiSettingsService settings;
    private AiService aiService;
    /** Copia de los turnos de cada llamada (la lista del agente cambia entre llamadas). */
    private final List<List<Turn>> sentTurns = new ArrayList<>();
    private final List<AiModelClient.Request> sentRequests = new ArrayList<>();

    static UserContext context() {
        return new UserContext(
                new UserContext.Profile(30, "MALE", 178.0, 80.0, "MODERATE", "INTERMEDIATE", 4, 60, null),
                null,
                List.of("BUILD_MUSCLE"),
                List.of("SHOULDER: molestia en press por encima de la cabeza"),
                List.of("DUMBBELLS", "BARBELL"),
                List.of("vegetariano"),
                List.of("Torso pierna"),
                9,
                List.of(new UserContext.Lift("Press banca", 80, 6))
        );
    }

    private void build(AiProperties props) {
        properties = props;
        catalog = new AiModelCatalog(properties);
        settings = new AiSettingsService(catalog, properties, aiRepository, client);
        aiService = new AiService(aiRepository, contextRepository, settings, client, tools, properties);
    }

    @BeforeEach
    void setUp() {
        build(AiProperties.withKey("gsk-test", ""));
        when(client.isConfigured()).thenReturn(true);
        when(contextRepository.load(userId)).thenReturn(context());
        when(aiRepository.findModelChoice(userId)).thenReturn(new ModelChoice(null, null));
        when(aiRepository.createConversation(eq(userId), anyString(), anyString())).thenReturn(conversationId);
        when(aiRepository.findRecentMessages(eq(conversationId), anyInt())).thenReturn(List.of(
                new MessageResponse("1", "USER", "¿Como mejoro mi press banca? escribeme a juan@mail.com", null, "t")));
        when(tools.specs()).thenReturn(List.of(new ToolSpec("search_exercises", "busca", Map.of("type", "object"))));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** El cliente simulado responde en orden y guarda una copia de cada peticion. */
    private void answers(Completion... completions) {
        org.mockito.stubbing.Stubber stubber = null;
        for (Completion completion : completions) {
            org.mockito.stubbing.Answer<Completion> answer = invocation -> {
                AiModelClient.Request request = invocation.getArgument(0);
                sentRequests.add(request);
                sentTurns.add(List.copyOf(request.turns()));
                return completion;
            };
            stubber = stubber == null ? org.mockito.Mockito.doAnswer(answer) : stubber.doAnswer(answer);
        }
        stubber.when(client).complete(any());
    }

    private static Completion text(String text, long in, long out) {
        return new Completion(text, QWEN, StopReason.END, List.of(), in, out);
    }

    private static Completion toolCall(String name, String args, long in, long out) {
        return new Completion("", QWEN, StopReason.TOOL_CALLS, List.of(new ToolCall("call_1", name, args)), in, out);
    }

    // ------------------------------------------------------------------ modelos

    @Test
    void onlyQwenIsOfferedAndTheUserCannotChoose() {
        assertEquals(List.of(QWEN), catalog.available().stream().map(AiModelCatalog.ModelInfo::id).toList());
        assertFalse(catalog.selectable());
        assertTrue(catalog.describe(QWEN).strictJson());
        assertEquals(QWEN, catalog.resolve(Feature.COACH, "claude-haiku-5-5"));

        AiModelsResponse response = settings.models();
        assertTrue(response.configured());
        assertFalse(response.selectable());
        assertEquals("Qwen 3.8 27B", response.models().get(0).label());
        assertEquals(QWEN, response.coach().selected());
        assertEquals(30, response.coach().dailyLimit());
    }

    @Test
    void aModelPreviouslyChosenButNoLongerAllowedFallsBackToTheDefault() {
        when(aiRepository.findModelChoice(userId)).thenReturn(new ModelChoice("claude-opus-5-5", "claude-sonnet-5-5"));
        AiModelsResponse response = settings.models();
        assertEquals(QWEN, response.coach().selected());
        assertFalse(response.coach().custom());
        assertEquals(QWEN, response.routine().selected());
    }

    @Test
    void preferencesRejectModelsThatAreNotAllowed() {
        ApiException exception = assertThrows(ApiException.class,
                () -> settings.updatePreferences(new AiPreferencesRequest("claude-haiku-5-5", null)));
        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        settings.updatePreferences(new AiPreferencesRequest(" " + QWEN + " ", ""));
        verify(aiRepository).saveModelChoice(userId, QWEN, null);
    }

    // ------------------------------------------------------------------ agente

    @Test
    void agentRunsAToolAndAnswersWithItsResult() {
        when(tools.execute(eq(userId), eq("search_exercises"), anyString()))
                .thenReturn(new AiToolRegistry.Result("{\"count\":1,\"exercises\":[{\"name\":\"Press inclinado\"}]}", false));
        answers(toolCall("search_exercises", "{\"muscle\":\"Pecho\"}", 900, 30), text("Prueba el press inclinado.", 1100, 60));

        ChatResponse response = aiService.chat(new ChatRequest(null, "¿Que ejercicio de pecho hago? escribeme a juan@mail.com"));

        assertEquals("AI", response.source());
        assertEquals(QWEN, response.model());
        assertEquals("Prueba el press inclinado.", response.answer());
        assertEquals(List.of("search_exercises"), response.toolsUsed());
        assertEquals(2, sentRequests.size());
        AiModelClient.Request first = sentRequests.get(0);
        assertEquals(QWEN, first.model());
        assertEquals("none", first.reasoningEffort());
        assertEquals(ToolChoice.AUTO, first.toolChoice());
        assertEquals(1, first.tools().size());
        assertTrue(first.system().contains("Equipo disponible: mancuernas, barra"));
        assertTrue(first.system().contains("Objetivos activos: ganar músculo"));
        assertTrue(first.system().contains("Experiencia: intermedio"));
        assertTrue(first.system().contains("Preferencias de alimentación: vegetariano"));
        // El email del usuario no sale hacia el proveedor.
        assertTrue(first.turns().get(0).text().contains("[email]"));
        assertFalse(first.turns().get(0).text().contains("juan@mail.com"));
        // La segunda llamada lleva la llamada y su resultado.
        List<Turn> second = sentTurns.get(1);
        assertEquals(Role.ASSISTANT, second.get(1).role());
        assertEquals("search_exercises", second.get(1).toolCalls().get(0).name());
        assertEquals(Role.TOOL, second.get(2).role());
        assertEquals("call_1", second.get(2).toolCallId());
        verify(aiRepository).createToolMessage(eq(conversationId), eq("search_exercises"),
                argThat(json -> json.contains("\"tool\":\"search_exercises\"") && json.contains("Press inclinado")));
        // Un solo registro de uso por mensaje, con los tokens de las dos llamadas.
        verify(aiRepository, times(1)).recordUsage(userId, "COACH", QWEN, 2000, 90);
        verify(aiRepository).createAssistantMessage(eq(conversationId), eq("Prueba el press inclinado."), eq(QWEN),
                argThat(json -> json.contains("\"source\":\"AI\"") && json.contains("\"toolCalls\":1") && json.contains("\"modelCalls\":2")));
    }

    @Test
    void theSystemPromptLimitsTheCoachToFitnessWithoutOverRestricting() {
        answers(text("Solo te ayudo con entrenamiento y nutrición deportiva.", 500, 20));

        aiService.chat(new ChatRequest(null, "Cuanto es 2 + 2?"));

        String system = sentRequests.get(0).system();
        assertTrue(system.contains("Alcance (qué respondes)"));
        assertTrue(system.contains("Fuera de alcance (no lo respondes, ni siquiera en parte)"));
        assertTrue(system.contains("Matemáticas o cálculos sin relación con entrenar o comer"));
        // No sobre-restringir: ante la duda, en clave de fitness; los calculos de entrenamiento siguen permitidos.
        assertTrue(system.contains("Ante la duda, interpreta la pregunta en clave de entrenamiento"));
        assertTrue(system.contains("kg y lb, calorías de los macros, volumen, 1RM"));
        assertTrue(system.contains("aunque antes, en esta conversación, se haya respondido algo fuera de tema"));
        assertTrue(system.contains("frases o mensajes para motivarse"));
        // El recordatorio viaja al final del ultimo turno del usuario, pero no se guarda.
        List<Turn> turns = sentRequests.get(0).turns();
        assertTrue(turns.get(turns.size() - 1).text().startsWith("Cuanto es 2 + 2?"));
        assertTrue(turns.get(turns.size() - 1).text().endsWith(CoachPrompts.SCOPE_NOTE));
        verify(aiRepository).createUserMessage(conversationId, "Cuanto es 2 + 2?");
    }

    @Test
    void theScopeNoteGoesOnlyOnTheLastUserTurn() {
        List<Turn> turns = AiService.withScopeNote(new java.util.ArrayList<>(List.of(
                Turn.user("Hola"), Turn.assistant("Hola, ¿en qué te ayudo?"), Turn.user("¿Y cuánto es 10 dividido 2?"))));
        assertEquals("Hola", turns.get(0).text());
        assertEquals("¿Y cuánto es 10 dividido 2?" + CoachPrompts.SCOPE_NOTE, turns.get(2).text());
    }

    @Test
    void aMalformedToolCallIsRetriedWithoutTools() {
        org.mockito.Mockito.doAnswer(invocation -> {
                    sentRequests.add(invocation.getArgument(0));
                    throw new AiUnavailableException("tool_use_failed", 400, null);
                })
                .doAnswer(invocation -> {
                    sentRequests.add(invocation.getArgument(0));
                    return text("Prueba remo con mancuerna.", 1200, 40);
                })
                .when(client).complete(any());

        ChatResponse response = aiService.chat(new ChatRequest(null, "Dame ejercicios de espalda"));

        assertEquals("AI", response.source());
        assertEquals(ToolChoice.AUTO, sentRequests.get(0).toolChoice());
        assertEquals(ToolChoice.NONE, sentRequests.get(1).toolChoice());
    }

    @Test
    void afterTheMaximumToolRoundsTheAgentMustAnswer() {
        when(tools.execute(any(), anyString(), anyString())).thenReturn(new AiToolRegistry.Result("{}", false));
        answers(toolCall("search_exercises", "{}", 500, 10), toolCall("list_my_routines", "{}", 600, 10), text("Listo", 700, 20));

        aiService.chat(new ChatRequest(null, "Hola"));

        assertEquals(3, sentRequests.size());
        assertEquals(ToolChoice.AUTO, sentRequests.get(0).toolChoice());
        assertEquals(ToolChoice.AUTO, sentRequests.get(1).toolChoice());
        assertEquals(ToolChoice.NONE, sentRequests.get(2).toolChoice());
    }

    @Test
    void tokenBudgetStopsToolUseEarly() {
        build(new AiProperties("gsk-test", "", QWEN, QWEN, QWEN, "none", "none", 1024, 4096, 3, 3000, 30, 5, 150000, 60));
        when(tools.execute(any(), anyString(), anyString())).thenReturn(new AiToolRegistry.Result("{}", false));
        answers(toolCall("search_exercises", "{}", 1500, 20), text("Listo", 1600, 20));

        aiService.chat(new ChatRequest(null, "Hola"));

        assertEquals(ToolChoice.NONE, sentRequests.get(1).toolChoice());
    }

    @Test
    void riskSignalsAddInstructionsAndAFixedNotice() {
        answers(text("Para y descansa.", 500, 30));

        ChatResponse response = aiService.chat(new ChatRequest(null, "Me dio dolor en el pecho haciendo sentadillas"));

        assertTrue(sentRequests.get(0).system().contains("síntomas cardiovasculares"));
        assertTrue(response.answer().startsWith("Para y descansa."));
        assertTrue(response.answer().contains("consulta a un médico"));
        verify(aiRepository).createAssistantMessage(any(), anyString(), anyString(), argThat(json -> json.contains("CARDIO")));
    }

    @Test
    void noFixedNoticeWhenTheModelAlreadyRefersToAProfessional() {
        answers(text("Para ahora mismo y consulta a un médico hoy.", 500, 30));

        ChatResponse response = aiService.chat(new ChatRequest(null, "Me dio dolor en el pecho haciendo sentadillas"));

        assertEquals("Para ahora mismo y consulta a un médico hoy.", response.answer());
    }

    @Test
    void dangerousNutritionAdviceGetsAWarning() {
        answers(text("Puedes comer 900 kcal al dia durante un mes.", 500, 30));
        ChatResponse response = aiService.chat(new ChatRequest(null, "¿Cuanto como para bajar rapido?"));
        assertTrue(response.answer().contains("menos de 1200 kcal"));
    }

    @Test
    void chatRejectsAConversationOfAnotherUserBeforeSavingAnything() {
        UUID foreign = UUID.randomUUID();
        when(aiRepository.conversationBelongsTo(foreign, userId)).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> aiService.chat(new ChatRequest(foreign.toString(), "Hola")));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        verify(aiRepository, never()).createUserMessage(any(), any());
        verify(client, never()).complete(any());
    }

    @Test
    void chatRejectsMalformedConversationId() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ApiException.class, () -> aiService.chat(new ChatRequest("abc", "Hola"))).status());
    }

    /** Ante cualquier fallo no se guarda nada: ni conversacion, ni pregunta, ni respuesta. */
    private void verifyNothingSaved() {
        verify(aiRepository, never()).createConversation(any(), any(), any());
        verify(aiRepository, never()).createUserMessage(any(), any());
        verify(aiRepository, never()).createToolMessage(any(), any(), any());
        verify(aiRepository, never()).createAssistantMessage(any(), any(), any(), any());
    }

    @Test
    void withoutApiKeyThereIsNoAnswerAt503() {
        when(client.isConfigured()).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> aiService.chat(new ChatRequest(null, "¿Cuanta proteina necesito?")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.status());
        verify(client, never()).complete(any());
        verify(aiRepository, never()).recordUsage(any(), any(), any(), anyLong(), anyLong());
        verifyNothingSaved();
    }

    @Test
    void whenTheAppBudgetIsSpentThereIsNoAnswerAndGroqIsNotCalled() {
        when(aiRepository.sumTokensLastDay()).thenReturn(150000L);

        ApiException exception = assertThrows(ApiException.class, () -> aiService.chat(new ChatRequest(null, "Hola")));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.status());
        verify(client, never()).complete(any());
        verifyNothingSaved();
    }

    @Test
    void whenTheProviderFailsNothingIsSavedButSpentTokensAreRecorded() {
        when(tools.execute(any(), anyString(), anyString())).thenReturn(new AiToolRegistry.Result("{}", false));
        var stub = when(client.complete(any()));
        stub.thenReturn(toolCall("search_exercises", "{}", 400, 10)).thenThrow(new AiUnavailableException("busy", 429, 30L));

        ApiException busy = assertThrows(ApiException.class, () -> aiService.chat(new ChatRequest(null, "Dame una rutina")));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, busy.status());
        verify(aiRepository).recordUsage(userId, "COACH", QWEN, 400, 10);
        verifyNothingSaved();

        org.mockito.Mockito.doThrow(new AiUnavailableException("down", 503, null)).when(client).complete(any());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ApiException.class, () -> aiService.chat(new ChatRequest(null, "Hola"))).status());
        verifyNothingSaved();
    }

    @Test
    void aRefusalIsAnsweredWithTheModelsOwnTextAndAnEmptyAnswerIsAnError() {
        answers(new Completion("No puedo ayudarte con eso, pero si con tu tecnica.", QWEN, StopReason.REFUSAL, List.of(), 10, 12));
        assertEquals("No puedo ayudarte con eso, pero si con tu tecnica.", aiService.chat(new ChatRequest(null, "...")).answer());

        sentRequests.clear();
        org.mockito.Mockito.reset(aiRepository);
        when(aiRepository.findModelChoice(userId)).thenReturn(new ModelChoice(null, null));
        answers(new Completion("   ", QWEN, StopReason.END, List.of(), 10, 0));
        ApiException empty = assertThrows(ApiException.class, () -> aiService.chat(new ChatRequest(null, "...")));
        assertEquals(HttpStatus.BAD_GATEWAY, empty.status());
        verifyNothingSaved();
    }

    @Test
    void aSuccessfulAnswerIsSavedInOrderInTheExistingConversation() {
        UUID existing = UUID.fromString("00000000-0000-0000-0000-000000000077");
        when(aiRepository.conversationBelongsTo(existing, userId)).thenReturn(true);
        when(aiRepository.findRecentMessages(eq(existing), anyInt())).thenReturn(List.of(
                new MessageResponse("1", "USER", "Hola", null, "t"),
                new MessageResponse("2", "ASSISTANT", "Hola, ¿en que te ayudo?", QWEN, "t")));
        when(tools.execute(any(), anyString(), anyString())).thenReturn(new AiToolRegistry.Result("{\"sessions\":[]}", false));
        answers(toolCall("get_recent_training", "{\"days\":7}", 800, 20), text("Esta semana no entrenaste.", 900, 30));

        ChatResponse response = aiService.chat(new ChatRequest(existing.toString(), "¿Como voy?"));

        assertEquals(existing.toString(), response.conversationId());
        // El historial guardado mas la pregunta nueva, que todavia no esta en la base.
        List<Turn> first = sentTurns.get(0);
        assertEquals(3, first.size());
        assertEquals("¿Como voy?" + CoachPrompts.SCOPE_NOTE, first.get(2).text());
        var order = org.mockito.Mockito.inOrder(aiRepository);
        order.verify(aiRepository).createUserMessage(existing, "¿Como voy?");
        order.verify(aiRepository).createToolMessage(eq(existing), eq("get_recent_training"), anyString());
        order.verify(aiRepository).createAssistantMessage(eq(existing), eq("Esta semana no entrenaste."), eq(QWEN), anyString());
        verify(aiRepository, never()).createConversation(any(), any(), any());
    }

    @Test
    void dailyLimitStopsTheChatBeforeSaving() {
        when(aiRepository.countUsageLastDay(userId, "COACH")).thenReturn(30);

        ApiException exception = assertThrows(ApiException.class, () -> aiService.chat(new ChatRequest(null, "Hola")));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.status());
        verifyNothingSaved();
    }

    @Test
    void markdownIsRemovedFromAnswers() {
        assertEquals("- Remo con barra: tiron horizontal.\nTecnica", AiService.plainText("- **Remo con barra**: tiron horizontal.\n## Tecnica"));
        assertEquals("2 * 3 = 6", AiService.plainText("2 * 3 = 6"));
    }

    @Test
    void historyStartsWithUserMergesRepeatedRolesAndIsTruncated() {
        String longText = "x".repeat(3000);
        List<Turn> turns = AiService.turns(List.of(
                new MessageResponse("1", "ASSISTANT", "hola", null, "t"),
                new MessageResponse("2", "USER", "a", null, "t"),
                new MessageResponse("3", "USER", "b", null, "t"),
                new MessageResponse("4", "ASSISTANT", longText, null, "t"),
                new MessageResponse("5", "USER", "d", null, "t")));

        assertEquals(3, turns.size());
        assertEquals("a\n\nb", turns.get(0).text());
        assertEquals(Role.ASSISTANT, turns.get(1).role());
        assertTrue(turns.get(1).text().length() <= AiService.HISTORY_MESSAGE_CHARS + 1);
        assertEquals("d", turns.get(2).text());
    }
}
