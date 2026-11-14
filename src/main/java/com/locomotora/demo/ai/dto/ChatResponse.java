package com.locomotora.demo.ai.dto;

/**
 * @param model  modelo que respondio (o local-context-coach-v1 si respondieron las reglas locales).
 * @param source CLAUDE o LOCAL (sin clave configurada o Claude no disponible).
 */
public record ChatResponse(String conversationId, String answer, String model, String source) {
}
