package com.locomotora.demo.ai.client;

import java.util.List;
import java.util.Map;

/** Acceso al modelo de lenguaje. La implementacion real es Claude; en tests se simula. */
public interface AiModelClient {

    /** false si no hay clave configurada: los servicios usan su respuesta local. */
    boolean isConfigured();

    /** Lanza {@link AiUnavailableException} si la API falla o no responde. */
    Completion complete(Request request);

    enum Role { USER, ASSISTANT }

    record Turn(Role role, String text) {
    }

    /**
     * @param jsonSchema esquema JSON de la respuesta (salida estructurada) o null para texto libre.
     * @param fallbacks  pedir al servidor que use otro modelo si el elegido no esta disponible.
     */
    record Request(
            String model,
            String system,
            List<Turn> turns,
            long maxTokens,
            String effort,
            Map<String, Object> jsonSchema,
            boolean fallbacks
    ) {
    }

    enum StopReason { END, MAX_TOKENS, REFUSAL, OTHER }

    /** @param model el modelo que respondio de verdad (puede ser el de respaldo). */
    record Completion(String text, String model, StopReason stopReason, long inputTokens, long outputTokens) {
    }
}
