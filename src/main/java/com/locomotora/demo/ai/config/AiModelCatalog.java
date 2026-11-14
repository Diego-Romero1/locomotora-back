package com.locomotora.demo.ai.config;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Modelos de Claude que la app sabe usar. Solo se ofrecen los que esten en
 * locomotora.ai.allowed-models; un id permitido que no este aqui se ofrece con datos genericos,
 * para poder probar un modelo nuevo sin cambiar codigo.
 */
@Component
public class AiModelCatalog {
    public enum Feature {
        /** Chat: respuestas cortas, esfuerzo medio para que sea rapido. */
        COACH("medium"),
        /** Rutinas: mas razonamiento para que el plan sea coherente. */
        ROUTINE("high");

        private final String effort;

        Feature(String effort) {
            this.effort = effort;
        }

        public String effort() {
            return effort;
        }
    }

    /**
     * @param supportsFallbacks si se piden reintentos en el servidor con otro modelo cuando este
     *                          no esta disponible (Sonnet 5.5 y Opus 5.5 los necesitan; Haiku no).
     */
    public record ModelInfo(
            String id,
            String label,
            String description,
            String speed,
            String cost,
            boolean supportsFallbacks
    ) {
    }

    private static final List<ModelInfo> KNOWN = List.of(
            new ModelInfo("claude-haiku-5-5", "Claude Haiku 5.5",
                    "Rapido y economico. Ideal para preguntas del dia a dia.",
                    "FAST", "LOW", false),
            new ModelInfo("claude-sonnet-5-5", "Claude Sonnet 5.5",
                    "Equilibrio entre calidad y costo. Recomendado para crear rutinas.",
                    "MEDIUM", "MEDIUM", true),
            new ModelInfo("claude-opus-5-5", "Claude Opus 5.5",
                    "El mas capaz. Mas lento y caro; para planes complejos.",
                    "SLOW", "HIGH", true)
    );

    private final AiProperties properties;

    public AiModelCatalog(AiProperties properties) {
        this.properties = properties;
    }

    /** Modelos ofrecidos, en el orden de allowed-models. */
    public List<ModelInfo> available() {
        return properties.allowedModels().stream().map(this::describe).toList();
    }

    public boolean isAllowed(String modelId) {
        return modelId != null && properties.allowedModels().contains(modelId);
    }

    public Optional<ModelInfo> find(String modelId) {
        return isAllowed(modelId) ? Optional.of(describe(modelId)) : Optional.empty();
    }

    /** Modelo por defecto de la tarea; si el configurado no esta permitido, el primero permitido. */
    public String defaultFor(Feature feature) {
        String configured = feature == Feature.COACH ? properties.coachDefaultModel() : properties.routineDefaultModel();
        if (isAllowed(configured) || properties.allowedModels().isEmpty()) {
            return configured;
        }
        return properties.allowedModels().get(0);
    }

    /** El elegido por el usuario si sigue permitido; si no, el de por defecto. */
    public String resolve(Feature feature, String userChoice) {
        return isAllowed(userChoice) ? userChoice : defaultFor(feature);
    }

    public ModelInfo describe(String modelId) {
        return KNOWN.stream()
                .filter(model -> model.id().equals(modelId))
                .findFirst()
                .orElseGet(() -> new ModelInfo(modelId, modelId, "Modelo configurado en el servidor.",
                        "MEDIUM", "MEDIUM", false));
    }
}
