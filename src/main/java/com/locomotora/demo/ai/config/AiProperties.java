package com.locomotora.demo.ai.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuracion del coach IA (locomotora.ai.*). Todo se puede cambiar por variables de entorno
 * sin recompilar: la clave, los modelos permitidos y el modelo por defecto de cada tarea.
 */
@Component
public class AiProperties {
    private final String apiKey;
    private final List<String> allowedModels;
    private final String coachDefaultModel;
    private final String routineDefaultModel;
    private final int dailyCoachMessages;
    private final int dailyRoutineGenerations;
    private final int timeoutSeconds;
    private final String baseUrl;

    public AiProperties(
            @Value("${locomotora.ai.api-key:}") String apiKey,
            @Value("${locomotora.ai.allowed-models:claude-haiku-5-5,claude-sonnet-5-5,claude-opus-5-5}") String allowedModels,
            @Value("${locomotora.ai.coach-model:claude-haiku-5-5}") String coachDefaultModel,
            @Value("${locomotora.ai.routine-model:claude-sonnet-5-5}") String routineDefaultModel,
            @Value("${locomotora.ai.daily-coach-messages:40}") int dailyCoachMessages,
            @Value("${locomotora.ai.daily-routine-generations:6}") int dailyRoutineGenerations,
            @Value("${locomotora.ai.timeout-seconds:120}") int timeoutSeconds,
            @Value("${locomotora.ai.base-url:}") String baseUrl
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.allowedModels = Arrays.stream(allowedModels.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
        this.coachDefaultModel = coachDefaultModel.trim();
        this.routineDefaultModel = routineDefaultModel.trim();
        this.dailyCoachMessages = dailyCoachMessages;
        this.dailyRoutineGenerations = dailyRoutineGenerations;
        this.timeoutSeconds = timeoutSeconds;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    /** Vacio = la API de Anthropic. Sirve para un proxy o para pruebas locales. */
    public String baseUrl() {
        return baseUrl;
    }

    public String apiKey() {
        return apiKey;
    }

    public boolean hasApiKey() {
        return !apiKey.isEmpty();
    }

    public List<String> allowedModels() {
        return allowedModels;
    }

    public String coachDefaultModel() {
        return coachDefaultModel;
    }

    public String routineDefaultModel() {
        return routineDefaultModel;
    }

    public int dailyCoachMessages() {
        return dailyCoachMessages;
    }

    public int dailyRoutineGenerations() {
        return dailyRoutineGenerations;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }
}
