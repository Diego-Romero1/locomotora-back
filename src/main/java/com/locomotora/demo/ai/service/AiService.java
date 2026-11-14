package com.locomotora.demo.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locomotora.demo.ai.client.AiModelClient;
import com.locomotora.demo.ai.client.AiModelClient.Completion;
import com.locomotora.demo.ai.client.AiModelClient.Role;
import com.locomotora.demo.ai.client.AiModelClient.StopReason;
import com.locomotora.demo.ai.client.AiModelClient.Turn;
import com.locomotora.demo.ai.client.AiUnavailableException;
import com.locomotora.demo.ai.config.AiModelCatalog.Feature;
import com.locomotora.demo.ai.dto.ChatRequest;
import com.locomotora.demo.ai.dto.ChatResponse;
import com.locomotora.demo.ai.dto.MessageResponse;
import com.locomotora.demo.ai.dto.RecommendationResponse;
import com.locomotora.demo.ai.repository.AiContextRepository;
import com.locomotora.demo.ai.repository.AiRepository;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Coach IA. Responde con Claude usando el perfil y el historial del usuario; si Claude no esta
 * configurado o falla, responde con reglas locales para que el chat nunca quede mudo.
 * No abre una transaccion durante la llamada al modelo (puede tardar varios segundos).
 */
@Service
public class AiService {
    static final int HISTORY_MESSAGES = 12;
    static final long MAX_TOKENS = 4000;
    static final String REFUSAL_ANSWER = "No puedo ayudarte con eso. Si tienes dolor o una lesion, consulta con un medico "
            + "o fisioterapeuta. Puedo ayudarte con tu rutina, la tecnica de los ejercicios o tu alimentacion.";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final AiRepository aiRepository;
    private final AiContextRepository contextRepository;
    private final AiSettingsService settings;
    private final AiModelClient client;

    public AiService(AiRepository aiRepository, AiContextRepository contextRepository, AiSettingsService settings, AiModelClient client) {
        this.aiRepository = aiRepository;
        this.contextRepository = contextRepository;
        this.settings = settings;
        this.client = client;
    }

    public ChatResponse chat(ChatRequest request) {
        UUID userId = CurrentUser.id();
        String message = request.message().trim();
        UserContext context = contextRepository.load(userId);

        // Antes de guardar nada: una conversacion ajena es un 404 y el limite diario un 429.
        UUID conversationId = request.conversationId() == null || request.conversationId().isBlank()
                ? null
                : ownedConversation(request.conversationId(), userId);
        boolean useClaude = client.isConfigured();
        if (useClaude) {
            settings.checkQuota(userId, Feature.COACH);
        }
        if (conversationId == null) {
            conversationId = aiRepository.createConversation(userId, title(message),
                    json(Map.of("sessionsLast28Days", context.sessionsLast28Days())));
        }
        aiRepository.createUserMessage(conversationId, message);

        String model = settings.modelFor(userId, Feature.COACH, null);
        String answer;
        String answeredBy;
        String source;
        String metadata;
        try {
            if (!useClaude) {
                throw new AiUnavailableException("not configured");
            }
            Completion completion = client.complete(new AiModelClient.Request(
                    model,
                    CoachPrompts.coachSystem(context),
                    turns(aiRepository.findRecentMessages(conversationId, HISTORY_MESSAGES)),
                    MAX_TOKENS,
                    Feature.COACH.effort(),
                    null,
                    settings.usesFallbacks(model)
            ));
            settings.recordUsage(userId, Feature.COACH, completion);
            answer = completion.stopReason() == StopReason.REFUSAL || completion.text().isBlank()
                    ? REFUSAL_ANSWER
                    : completion.text().trim();
            answeredBy = completion.model();
            source = "CLAUDE";
            metadata = json(Map.of(
                    "source", source,
                    "requestedModel", model,
                    "stopReason", completion.stopReason().name(),
                    "inputTokens", completion.inputTokens(),
                    "outputTokens", completion.outputTokens()));
        } catch (AiUnavailableException unavailable) {
            answer = LocalCoach.answer(context, message);
            answeredBy = LocalCoach.MODEL;
            source = "LOCAL";
            metadata = json(Map.of("source", source, "requestedModel", model));
        }

        aiRepository.createAssistantMessage(conversationId, answer, answeredBy, metadata);
        aiRepository.touchConversation(conversationId);
        return new ChatResponse(conversationId.toString(), answer, answeredBy, source);
    }

    public List<RecommendationResponse> recommendations() {
        return aiRepository.findRecommendations(CurrentUser.id());
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
     * Historial para el modelo: empieza por un mensaje del usuario y alterna roles
     * (dos mensajes seguidos del mismo rol se unen, por ejemplo tras un fallo).
     */
    static List<Turn> turns(List<MessageResponse> messages) {
        List<Turn> turns = new ArrayList<>();
        for (MessageResponse message : messages) {
            Role role = "ASSISTANT".equals(message.role()) ? Role.ASSISTANT : Role.USER;
            if (turns.isEmpty() && role == Role.ASSISTANT) {
                continue;
            }
            if (!turns.isEmpty() && turns.get(turns.size() - 1).role() == role) {
                Turn last = turns.remove(turns.size() - 1);
                turns.add(new Turn(role, last.text() + "\n\n" + message.content()));
            } else {
                turns.add(new Turn(role, message.content()));
            }
        }
        return turns;
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
