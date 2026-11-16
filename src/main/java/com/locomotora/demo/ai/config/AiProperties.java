package com.locomotora.demo.ai.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuracion del agente IA (locomotora.ai.*). Proveedor: GroqCloud (API compatible con OpenAI).
 * Todo se cambia por variables de entorno sin recompilar (ver docs/API.md → IA).
 */
@Component
public class AiProperties {
    public static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1";
    public static final String DEFAULT_MODEL = "qwen/qwen3.8-27b";

    private final String apiKey;
    private final String baseUrl;
    private final List<String> allowedModels;
    private final String coachDefaultModel;
    private final String routineDefaultModel;
    private final String coachReasoningEffort;
    private final String routineReasoningEffort;
    private final int coachMaxTokens;
    private final int routineMaxTokens;
    private final int maxToolRounds;
    private final int chatTokenBudget;
    private final int dailyCoachMessages;
    private final int dailyRoutineGenerations;
    private final long dailyTokenBudget;
    private final int timeoutSeconds;

    public AiProperties(
            @Value("${locomotora.ai.api-key:}") String apiKey,
            @Value("${locomotora.ai.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
            @Value("${locomotora.ai.allowed-models:" + DEFAULT_MODEL + "}") String allowedModels,
            @Value("${locomotora.ai.coach-model:" + DEFAULT_MODEL + "}") String coachDefaultModel,
            @Value("${locomotora.ai.routine-model:" + DEFAULT_MODEL + "}") String routineDefaultModel,
            @Value("${locomotora.ai.coach-reasoning-effort:none}") String coachReasoningEffort,
            @Value("${locomotora.ai.routine-reasoning-effort:none}") String routineReasoningEffort,
            // Groq (plan gratis) rechaza de entrada una peticion que espera mas de 1.000 tokens de salida por minuto.
            @Value("${locomotora.ai.coach-max-tokens:700}") int coachMaxTokens,
            @Value("${locomotora.ai.routine-max-tokens:1000}") int routineMaxTokens,
            @Value("${locomotora.ai.max-tool-rounds:2}") int maxToolRounds,
            @Value("${locomotora.ai.chat-token-budget:7000}") int chatTokenBudget,
            @Value("${locomotora.ai.daily-coach-messages:30}") int dailyCoachMessages,
            @Value("${locomotora.ai.daily-routine-generations:5}") int dailyRoutineGenerations,
            @Value("${locomotora.ai.daily-token-budget:150000}") long dailyTokenBudget,
            @Value("${locomotora.ai.timeout-seconds:60}") int timeoutSeconds
    ) {
        this.apiKey = trim(apiKey);
        this.baseUrl = trim(baseUrl).isEmpty() ? DEFAULT_BASE_URL : trim(baseUrl).replaceAll("/+$", "");
        this.allowedModels = Arrays.stream(allowedModels.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
        this.coachDefaultModel = trim(coachDefaultModel);
        this.routineDefaultModel = trim(routineDefaultModel);
        this.coachReasoningEffort = trim(coachReasoningEffort);
        this.routineReasoningEffort = trim(routineReasoningEffort);
        this.coachMaxTokens = coachMaxTokens;
        this.routineMaxTokens = routineMaxTokens;
        this.maxToolRounds = Math.max(0, maxToolRounds);
        this.chatTokenBudget = chatTokenBudget;
        this.dailyCoachMessages = dailyCoachMessages;
        this.dailyRoutineGenerations = dailyRoutineGenerations;
        this.dailyTokenBudget = dailyTokenBudget;
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Valores por defecto con una clave: para tests. */
    public static AiProperties withKey(String apiKey, String baseUrl) {
        return new AiProperties(apiKey, baseUrl, DEFAULT_MODEL, DEFAULT_MODEL, DEFAULT_MODEL, "none", "none",
                700, 1000, 2, 7000, 30, 5, 150000, 60);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public String apiKey() {
        return apiKey;
    }

    public boolean hasApiKey() {
        return !apiKey.isEmpty();
    }

    public String baseUrl() {
        return baseUrl;
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

    /** Qwen 3.8 27B en Groq: none (sin razonamiento), default, low, medium o high. */
    public String reasoningEffortFor(AiModelCatalog.Feature feature) {
        return feature == AiModelCatalog.Feature.COACH ? coachReasoningEffort : routineReasoningEffort;
    }

    public int maxTokensFor(AiModelCatalog.Feature feature) {
        return feature == AiModelCatalog.Feature.COACH ? coachMaxTokens : routineMaxTokens;
    }

    /** Rondas de herramientas por mensaje del chat antes de pedir la respuesta final. */
    public int maxToolRounds() {
        return maxToolRounds;
    }

    /** Tokens maximos de un mensaje del chat sumando todas sus rondas (el plan gratis de Groq da 8K por minuto). */
    public int chatTokenBudget() {
        return chatTokenBudget;
    }

    public int dailyCoachMessages() {
        return dailyCoachMessages;
    }

    public int dailyRoutineGenerations() {
        return dailyRoutineGenerations;
    }

    /** Tokens de toda la app en 24 h (la clave es una sola para todos los usuarios). 0 = sin tope. */
    public long dailyTokenBudget() {
        return dailyTokenBudget;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }
}
