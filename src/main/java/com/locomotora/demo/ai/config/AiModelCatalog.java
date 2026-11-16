package com.locomotora.demo.ai.config;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Modelos que la app sabe usar. Solo se ofrecen los de locomotora.ai.allowed-models (hoy, solo
 * Qwen 3.8 27B en GroqCloud). Un id permitido que no este aqui se ofrece con datos genericos, para
 * poder probar otro modelo de Groq cambiando solo la configuracion.
 */
@Component
public class AiModelCatalog {
    public enum Feature { COACH, ROUTINE }

    /**
     * @param strictJson el modelo admite salida JSON estricta (json_schema con strict); si no, se pide
     *                   en modo de mejor esfuerzo y el backend valida igual.
     */
    public record ModelInfo(String id, String label, String description, String speed, String cost, boolean strictJson) {
    }

    private static final List<ModelInfo> KNOWN = List.of(
            new ModelInfo(AiProperties.DEFAULT_MODEL, "Qwen 3.8 27B",
                    "Modelo abierto de Qwen servido por GroqCloud. Rápido, con herramientas y JSON estricto.",
                    "FAST", "LOW", true)
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

    /** true si hay mas de un modelo: solo entonces el usuario puede elegir. */
    public boolean selectable() {
        return properties.allowedModels().size() > 1;
    }

    public ModelInfo describe(String modelId) {
        return KNOWN.stream()
                .filter(model -> model.id().equals(modelId))
                .findFirst()
                .orElseGet(() -> new ModelInfo(modelId, modelId, "Modelo configurado en el servidor.", "MEDIUM", "MEDIUM", false));
    }
}
