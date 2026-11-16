package com.locomotora.demo.ai.client;

import java.util.List;
import java.util.Map;

/**
 * Acceso al modelo de lenguaje, sin depender del proveedor. La implementacion real es
 * {@link GroqModelClient}; en tests se simula.
 */
public interface AiModelClient {

    /** false si no hay clave configurada: los servicios usan su respuesta local. */
    boolean isConfigured();

    /** Lanza {@link AiUnavailableException} si la API falla, no responde o rechaza la peticion. */
    Completion complete(Request request);

    enum Role { USER, ASSISTANT, TOOL }

    /** Llamada a una herramienta pedida por el modelo. {@code arguments} es el JSON tal como llego. */
    record ToolCall(String id, String name, String arguments) {
    }

    /**
     * Un mensaje de la conversacion. Para ASSISTANT puede traer {@code toolCalls}; para TOOL,
     * {@code toolCallId} y {@code toolName} identifican la llamada que responde.
     */
    record Turn(Role role, String text, List<ToolCall> toolCalls, String toolCallId, String toolName) {
        public static Turn user(String text) {
            return new Turn(Role.USER, text, List.of(), null, null);
        }

        public static Turn assistant(String text) {
            return new Turn(Role.ASSISTANT, text, List.of(), null, null);
        }

        public static Turn assistantToolCalls(String text, List<ToolCall> calls) {
            return new Turn(Role.ASSISTANT, text, List.copyOf(calls), null, null);
        }

        public static Turn toolResult(String toolCallId, String toolName, String content) {
            return new Turn(Role.TOOL, content, List.of(), toolCallId, toolName);
        }
    }

    /** Herramienta que el modelo puede pedir. {@code parameters} es un JSON Schema de objeto. */
    record ToolSpec(String name, String description, Map<String, Object> parameters) {
    }

    enum ToolChoice { AUTO, NONE }

    /**
     * @param reasoningEffort none, default, low, medium o high (Qwen 3.8 27B).
     * @param jsonSchema      esquema de la salida (JSON estricto) o null para texto libre.
     *                        No se puede combinar con herramientas (limitacion de Groq).
     * @param tools           herramientas disponibles; vacio = ninguna.
     */
    record Request(
            String model,
            String system,
            List<Turn> turns,
            long maxTokens,
            String reasoningEffort,
            String schemaName,
            Map<String, Object> jsonSchema,
            boolean strictJson,
            List<ToolSpec> tools,
            ToolChoice toolChoice
    ) {
        public static Request text(String model, String system, List<Turn> turns, long maxTokens, String effort) {
            return new Request(model, system, turns, maxTokens, effort, null, null, false, List.of(), ToolChoice.NONE);
        }
    }

    enum StopReason { END, MAX_TOKENS, TOOL_CALLS, REFUSAL, OTHER }

    /** @param model el modelo que respondio. */
    record Completion(
            String text,
            String model,
            StopReason stopReason,
            List<ToolCall> toolCalls,
            long inputTokens,
            long outputTokens
    ) {
        public Completion(String text, String model, StopReason stopReason, long inputTokens, long outputTokens) {
            this(text, model, stopReason, List.of(), inputTokens, outputTokens);
        }

        public long totalTokens() {
            return inputTokens + outputTokens;
        }
    }
}
