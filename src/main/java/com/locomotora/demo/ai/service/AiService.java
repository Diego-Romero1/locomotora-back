package com.locomotora.demo.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.client.AiModelClient.Completion;
import com.locomotora.demo.ai.client.AiModelClient.Role;
import com.locomotora.demo.ai.client.AiModelClient.StopReason;
import com.locomotora.demo.ai.client.AiModelClient.ToolCall;
import com.locomotora.demo.ai.client.AiModelClient.ToolChoice;
import com.locomotora.demo.ai.client.AiModelClient.Turn;
import com.locomotora.demo.ai.client.AiUnavailableException;
import com.locomotora.demo.ai.config.AiModelCatalog.Feature;
import com.locomotora.demo.ai.config.AiProperties;
import com.locomotora.demo.ai.dto.ChatRequest;
import com.locomotora.demo.ai.dto.ChatResponse;
import com.locomotora.demo.ai.dto.MessageResponse;
import com.locomotora.demo.ai.dto.RecommendationResponse;
import com.locomotora.demo.ai.repository.AiContextRepository;
import com.locomotora.demo.ai.repository.AiRepository;
import com.locomotora.demo.ai.tools.AiToolRegistry;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Coach IA: un agente que responde con el modelo (Qwen 3.8 27B en GroqCloud) y puede consultar,
 * con herramientas del backend, las rutinas, sesiones, progreso y catalogo del usuario.
 * Si el modelo no esta configurado, se agoto el presupuesto o falla, responden las reglas locales
 * para que el chat nunca quede mudo. No abre una transaccion durante las llamadas al modelo.
 */
@Service
public class AiService {
    static final int HISTORY_MESSAGES = 8;
    static final int HISTORY_MESSAGE_CHARS = 1500;
    static final int MAX_CALLS_PER_ROUND = 4;
    static final String SOURCE_AI = "AI";
    static final String SOURCE_LOCAL = "LOCAL";
    static final String REFUSAL_ANSWER = "No puedo ayudarte con eso. Si tienes dolor o una lesion, consulta con un medico "
            + "o fisioterapeuta. Puedo ayudarte con tu rutina, la tecnica de los ejercicios o tu alimentacion.";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AiRepository aiRepository;
    private final AiContextRepository contextRepository;
    private final AiSettingsService settings;
    private final AiModelClient client;
    private final AiToolRegistry tools;
    private final AiProperties properties;

    public AiService(
            AiRepository aiRepository,
            AiContextRepository contextRepository,
            AiSettingsService settings,
            AiModelClient client,
            AiToolRegistry tools,
            AiProperties properties
    ) {
        this.aiRepository = aiRepository;
        this.contextRepository = contextRepository;
        this.settings = settings;
        this.client = client;
        this.tools = tools;
        this.properties = properties;
    }

    /** Tokens gastados en un mensaje, sumando todas sus llamadas (tambien si una fallo a mitad). */
    static final class Usage {
        long input;
        long output;
        int calls;
        int toolRounds;
        int toolCalls;
        String model;

        void add(Completion completion) {
            input += completion.inputTokens();
            output += completion.outputTokens();
            calls++;
            model = completion.model();
        }

        long total() {
            return input + output;
        }
    }

    public ChatResponse chat(ChatRequest request) {
        UUID userId = CurrentUser.id();
        String message = request.message().trim();
        UserContext context = contextRepository.load(userId);

        // Antes de guardar nada: una conversacion ajena es un 404 y el limite diario un 429.
        UUID conversationId = request.conversationId() == null || request.conversationId().isBlank()
                ? null
                : ownedConversation(request.conversationId(), userId);
        boolean configured = client.isConfigured();
        if (configured) {
            settings.checkQuota(userId, Feature.COACH);
        }
        boolean budgetLeft = configured && !settings.globalBudgetExhausted();
        if (conversationId == null) {
            conversationId = aiRepository.createConversation(userId, title(message),
                    json(Map.of("sessionsLast28Days", context.sessionsLast28Days())));
        }
        aiRepository.createUserMessage(conversationId, message);

        String model = settings.modelFor(userId, Feature.COACH, null);
        Set<SafetyGuard.Risk> risks = SafetyGuard.detect(message);
        Usage usage = new Usage();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestedModel", model);
        metadata.put("risks", risks.stream().map(Enum::name).toList());
        String answer;
        String answeredBy;
        String source;
        try {
            if (!configured) {
                throw new AiUnavailableException("not configured");
            }
            if (!budgetLeft) {
                throw new AiUnavailableException("daily token budget exhausted", 429, null);
            }
            Completion completion = runAgent(userId, conversationId, model, context, risks, usage);
            String text = plainText(completion.text());
            boolean refused = completion.stopReason() == StopReason.REFUSAL;
            if (text.isEmpty() && !refused) {
                throw new AiUnavailableException("empty answer", 502, null);
            }
            Set<SafetyGuard.Issue> issues = refused ? Set.of() : SafetyGuard.review(text);
            answer = SafetyGuard.withNotices(refused ? REFUSAL_ANSWER : text, risks, issues);
            answeredBy = completion.model();
            source = SOURCE_AI;
            metadata.put("stopReason", completion.stopReason().name());
            metadata.put("issues", issues.stream().map(Enum::name).toList());
        } catch (AiUnavailableException unavailable) {
            answer = SafetyGuard.withNotices(LocalCoach.answer(context, message), risks, Set.of());
            answeredBy = LocalCoach.MODEL;
            source = SOURCE_LOCAL;
            metadata.put("reason", !configured ? "not_configured"
                    : !budgetLeft ? "budget_exhausted"
                    : unavailable.rateLimited() ? "rate_limited" : "provider_error");
        } finally {
            if (usage.calls > 0) {
                settings.recordUsage(userId, Feature.COACH, usage.model, usage.input, usage.output);
            }
        }

        metadata.put("source", source);
        metadata.put("inputTokens", usage.input);
        metadata.put("outputTokens", usage.output);
        metadata.put("modelCalls", usage.calls);
        metadata.put("toolCalls", usage.toolCalls);
        aiRepository.createAssistantMessage(conversationId, answer, answeredBy, json(metadata));
        aiRepository.touchConversation(conversationId);
        return new ChatResponse(conversationId.toString(), answer, answeredBy, source);
    }

    public List<RecommendationResponse> recommendations() {
        return aiRepository.findRecommendations(CurrentUser.id());
    }

    /**
     * Bucle del agente: el modelo puede pedir herramientas hasta {@code max-tool-rounds} veces; despues,
     * o si el mensaje se acerca al presupuesto de tokens, se le pide la respuesta final sin herramientas.
     */
    Completion runAgent(UUID userId, UUID conversationId, String model, UserContext context, Set<SafetyGuard.Risk> risks, Usage usage) {
        List<Turn> turns = turns(aiRepository.findRecentMessages(conversationId, HISTORY_MESSAGES));
        String system = CoachPrompts.coachSystem(context) + SafetyGuard.instructions(risks);
        String effort = properties.reasoningEffortFor(Feature.COACH);
        long maxTokens = properties.maxTokensFor(Feature.COACH);
        boolean allowTools = properties.maxToolRounds() > 0;

        boolean retriedWithoutTools = false;
        while (true) {
            Completion completion;
            try {
                completion = client.complete(new AiModelClient.Request(
                        model, system, turns, maxTokens, effort, null, null, false,
                        tools.specs(), allowTools ? ToolChoice.AUTO : ToolChoice.NONE));
            } catch (AiUnavailableException rejected) {
                // Groq responde 400 (tool_use_failed) si el modelo arma mal una llamada: se pide la respuesta sin herramientas.
                if (!rejected.rejected() || !allowTools || retriedWithoutTools) {
                    throw rejected;
                }
                retriedWithoutTools = true;
                allowTools = false;
                continue;
            }
            usage.add(completion);
            if (!allowTools || completion.toolCalls().isEmpty()) {
                return completion;
            }

            usage.toolRounds++;
            List<ToolCall> calls = completion.toolCalls().stream().limit(MAX_CALLS_PER_ROUND).toList();
            turns.add(Turn.assistantToolCalls(completion.text(), calls));
            long resultChars = 0;
            for (ToolCall call : calls) {
                AiToolRegistry.Result result = tools.execute(userId, call.name(), call.arguments());
                usage.toolCalls++;
                resultChars += result.content().length();
                turns.add(Turn.toolResult(call.id(), call.name(), result.content()));
                aiRepository.createToolMessage(conversationId, call.name(), json(Map.of(
                        "tool", call.name(),
                        "arguments", PersonalDataFilter.redact(truncate(call.arguments(), 300)),
                        "error", result.error(),
                        "result", truncate(result.content(), 500))));
            }
            // Siguiente llamada: lo ya enviado mas los resultados (~3 caracteres por token).
            long nextPrompt = completion.inputTokens() + completion.outputTokens() + resultChars / 3 + 100;
            allowTools = usage.toolRounds < properties.maxToolRounds()
                    && usage.total() + nextPrompt + maxTokens <= properties.chatTokenBudget();
        }
    }

    private UUID ownedConversation(String rawId, UUID userId) {
        UUID id;
        try {
            id = UUID.fromString(rawId);
        } catch (IllegalArgumentException invalid) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid conversationId");
        }
        if (!aiRepository.conversationBelongsTo(id, userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Conversation not found");
        }
        return id;
    }

    /**
     * Historial para el modelo: empieza por un mensaje del usuario y alterna roles (dos seguidos del
     * mismo rol se unen). Los mensajes se recortan y pasan por el filtro de datos personales.
     */
    static List<Turn> turns(List<MessageResponse> messages) {
        List<Turn> turns = new ArrayList<>();
        for (MessageResponse message : messages) {
            Role role = "ASSISTANT".equals(message.role()) ? Role.ASSISTANT : Role.USER;
            String text = PersonalDataFilter.redact(truncate(message.content(), HISTORY_MESSAGE_CHARS));
            if (turns.isEmpty() && role == Role.ASSISTANT) {
                continue;
            }
            if (!turns.isEmpty() && turns.get(turns.size() - 1).role() == role) {
                Turn last = turns.remove(turns.size() - 1);
                text = last.text() + "\n\n" + text;
            }
            turns.add(role == Role.USER ? Turn.user(text) : Turn.assistant(text));
        }
        return turns;
    }

    /** La app muestra texto plano: se quitan negritas y titulos de markdown que el modelo agregue igual. */
    static String plainText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("__(.+?)__", "$1")
                .replaceAll("(?m)^#{1,6}\\s+", "")
                .trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private static String json(Map<String, Object> values) {
        try {
            return JSON.writeValueAsString(values);
        } catch (JsonProcessingException impossible) {
            return "{}";
        }
    }

    private static String title(String message) {
        String singleLine = message.replaceAll("\\s+", " ");
        return singleLine.length() > 60 ? singleLine.substring(0, 60) : singleLine;
    }
}
