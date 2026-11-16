package com.locomotora.demo.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.client.AiModelClient.Completion;
import com.locomotora.demo.ai.client.AiModelClient.Role;
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
 * Solo responde el modelo: no hay respuestas predeterminadas. No abre una transaccion durante las
 * llamadas al modelo.
 */
@Service
public class AiService {
    static final int HISTORY_MESSAGES = 8;
    static final int HISTORY_MESSAGE_CHARS = 1500;
    static final int MAX_CALLS_PER_ROUND = 4;
    static final String SOURCE_AI = "AI";
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
        final java.util.LinkedHashSet<String> toolNames = new java.util.LinkedHashSet<>();

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

    /**
     * Responde solo el agente (el modelo de IA). Si no puede responder (sin clave, sin presupuesto,
     * proveedor saturado o caido, respuesta vacia) se devuelve un error y no se guarda nada: ni una
     * respuesta predeterminada ni el mensaje del usuario, que la app permite reintentar.
     */
    public ChatResponse chat(ChatRequest request) {
        UUID userId = CurrentUser.id();
        String message = request.message().trim();

        // Antes de llamar al modelo: conversacion ajena 404, IA no activa 503, limites 429.
        UUID existing = request.conversationId() == null || request.conversationId().isBlank()
                ? null
                : ownedConversation(request.conversationId(), userId);
        if (!client.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI is not configured");
        }
        settings.checkQuota(userId, Feature.COACH);
        if (settings.globalBudgetExhausted()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI daily budget reached");
        }

        UserContext context = contextRepository.load(userId);
        String model = settings.modelFor(userId, Feature.COACH, null);
        Set<SafetyGuard.Risk> risks = SafetyGuard.detect(message);
        List<MessageResponse> history = new ArrayList<>(existing == null
                ? List.of()
                : aiRepository.findRecentMessages(existing, HISTORY_MESSAGES - 1));
        history.add(new MessageResponse(null, "USER", message, null, null));

        Usage usage = new Usage();
        List<ToolLog> toolLogs = new ArrayList<>();
        Completion completion;
        try {
            completion = runAgent(userId, model, context, risks, withScopeNote(turns(history)), usage, toolLogs);
        } catch (AiUnavailableException unavailable) {
            recordUsage(userId, usage);
            if (unavailable.rateLimited()) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI provider is busy, try again in a minute");
            }
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable");
        }
        recordUsage(userId, usage);

        // Si el modelo se niega, su propio texto es la respuesta; si no dice nada, es un error.
        String text = plainText(completion.text());
        if (text.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI returned an empty answer");
        }
        Set<SafetyGuard.Issue> issues = SafetyGuard.review(text);
        String answer = SafetyGuard.withNotices(text, SafetyGuard.unaddressed(text, risks), issues);

        // Recien ahora se guarda: conversacion, pregunta, llamadas a herramientas y respuesta.
        UUID conversationId = existing != null
                ? existing
                : aiRepository.createConversation(userId, title(message),
                        json(Map.of("sessionsLast28Days", context.sessionsLast28Days())));
        aiRepository.createUserMessage(conversationId, message);
        toolLogs.forEach(log -> aiRepository.createToolMessage(conversationId, log.tool(), log.metadata()));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", SOURCE_AI);
        metadata.put("requestedModel", model);
        metadata.put("stopReason", completion.stopReason().name());
        metadata.put("risks", risks.stream().map(Enum::name).toList());
        metadata.put("issues", issues.stream().map(Enum::name).toList());
        metadata.put("inputTokens", usage.input);
        metadata.put("outputTokens", usage.output);
        metadata.put("modelCalls", usage.calls);
        metadata.put("toolCalls", usage.toolCalls);
        aiRepository.createAssistantMessage(conversationId, answer, completion.model(), json(metadata));
        aiRepository.touchConversation(conversationId);
        return new ChatResponse(conversationId.toString(), answer, completion.model(), SOURCE_AI, List.copyOf(usage.toolNames));
    }

    /** Agrega el recordatorio de alcance al ultimo mensaje del usuario (solo en lo que se envia al modelo). */
    static List<Turn> withScopeNote(List<Turn> turns) {
        int last = turns.size() - 1;
        if (last >= 0 && turns.get(last).role() == Role.USER) {
            turns.set(last, Turn.user(turns.get(last).text() + CoachPrompts.SCOPE_NOTE));
        }
        return turns;
    }

    /** Llamada a una herramienta, para guardarla junto con la respuesta. */
    record ToolLog(String tool, String metadata) {
    }

    private void recordUsage(UUID userId, Usage usage) {
        if (usage.calls > 0) {
            settings.recordUsage(userId, Feature.COACH, usage.model, usage.input, usage.output);
        }
    }

    public List<RecommendationResponse> recommendations() {
        return aiRepository.findRecommendations(CurrentUser.id());
    }

    /**
     * Bucle del agente: el modelo puede pedir herramientas hasta {@code max-tool-rounds} veces; despues,
     * o si el mensaje se acerca al presupuesto de tokens, se le pide la respuesta final sin herramientas.
     * Las llamadas se anotan en {@code toolLogs}; se guardan solo si hay respuesta.
     */
    Completion runAgent(UUID userId, String model, UserContext context, Set<SafetyGuard.Risk> risks, List<Turn> turns,
                        Usage usage, List<ToolLog> toolLogs) {
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
                if (!result.error()) {
                    usage.toolNames.add(call.name());
                }
                resultChars += result.content().length();
                turns.add(Turn.toolResult(call.id(), call.name(), result.content()));
                toolLogs.add(new ToolLog(call.name(), json(Map.of(
                        "tool", call.name(),
                        "arguments", PersonalDataFilter.redact(truncate(call.arguments(), 300)),
                        "error", result.error(),
                        "result", truncate(result.content(), 500)))));
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
