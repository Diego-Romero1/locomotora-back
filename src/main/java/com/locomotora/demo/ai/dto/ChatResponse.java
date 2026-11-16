package com.locomotora.demo.ai.dto;

import java.util.List;

/**
 * @param model  modelo que respondio (o local-context-coach-v1 si respondieron las reglas locales).
 * @param toolsUsed herramientas que consulto el agente para responder (sin repetir, en orden).
 * @param source siempre AI: solo responde el modelo (si no puede, el endpoint devuelve un error).
 */
public record ChatResponse(String conversationId, String answer, String model, String source, List<String> toolsUsed) {
    /** Compatibilidad: respuesta sin herramientas. */
    public ChatResponse(String conversationId, String answer, String model, String source) {
        this(conversationId, answer, model, source, List.of());
    }
}
