package com.locomotora.demo.ai.service;

import com.locomotora.demo.ai.dto.ChatRequest;
import com.locomotora.demo.ai.dto.ChatResponse;
import com.locomotora.demo.ai.dto.RecommendationResponse;
import com.locomotora.demo.ai.repository.AiRepository;
import com.locomotora.demo.common.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiService {
    private final AiRepository aiRepository;

    public AiService(AiRepository aiRepository) {
        this.aiRepository = aiRepository;
    }

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        UUID userId = CurrentUser.id();
        UUID conversationId = request.conversationId() == null
                ? createConversation(userId, request.message())
                : UUID.fromString(request.conversationId());

        aiRepository.createUserMessage(conversationId, request.message());

        String answer = buildCoachAnswer(userId, request.message());
        aiRepository.createAssistantMessage(conversationId, answer);
        aiRepository.touchConversation(conversationId);

        UUID recommendationId = aiRepository.createRecommendation(userId, conversationId, answer);
        return new ChatResponse(conversationId.toString(), answer, recommendationId.toString());
    }

    public List<RecommendationResponse> recommendations() {
        return aiRepository.findRecommendations(CurrentUser.id());
    }

    private UUID createConversation(UUID userId, String message) {
        String title = message.length() > 60 ? message.substring(0, 60) : message;
        return aiRepository.createConversation(userId, title, contextSnapshot(userId));
    }

    private String buildCoachAnswer(UUID userId, String message) {
        int workouts = aiRepository.countCompletedWorkouts(userId);
        String goal = aiRepository.findPrimaryActiveGoal(userId);
        String routine = aiRepository.findLatestActiveRoutineTitle(userId);

        String lower = message.toLowerCase();
        if (lower.contains("nutri") || lower.contains("comida") || lower.contains("prote")) {
            return "Para tu objetivo " + goal + ", prioriza proteina en cada comida, hidratos alrededor del entrenamiento y registra tus comidas en Nutricion. Con " + workouts + " entrenamientos completados, el siguiente paso es sostener consistencia y ajustar calorias segun tus metricas.";
        }
        if (lower.contains("rutina") || lower.contains("entren")) {
            return "Tu foco actual es " + goal + ". Usa " + routine + " como base, completa las sesiones y registra pesos/reps para progresar. Si una semana se siente demasiado pesada, reduce volumen antes de cambiar el objetivo.";
        }
        if (lower.contains("peso") || lower.contains("metrica") || lower.contains("progreso")) {
            return "Mira tendencia, no solo una medicion aislada. Registra peso y medidas 1 o 2 veces por semana, y compara contra entrenamientos completados. Hoy tengo registrados " + workouts + " entrenamientos completados.";
        }
        return "Puedo ayudarte con rutinas, nutricion y progreso usando tu perfil, objetivos, metricas y entrenamientos. Para darte una recomendacion mas fina, dime tu objetivo principal, dias disponibles y cualquier limitacion fisica.";
    }

    private String contextSnapshot(UUID userId) {
        return "{\"completedWorkouts\":" + aiRepository.countCompletedWorkouts(userId) + "}";
    }
}
