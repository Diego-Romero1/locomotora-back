package com.locomotora.demo.ai.dto;

/**
 * @param model  modelo que respondio (o local-context-coach-v1 si respondieron las reglas locales).
 * @param source AI (respondio el modelo) o LOCAL (sin clave, sin presupuesto o el proveedor no respondio).
 */
public record ChatResponse(String conversationId, String answer, String model, String source) {
}
