package com.locomotora.demo.ai.dto;

/** Modelo elegido para cada tarea. null o vacio = volver al modelo por defecto. */
public record AiPreferencesRequest(String coachModel, String routineModel) {
}
