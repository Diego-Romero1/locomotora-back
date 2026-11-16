package com.locomotora.demo.ai.dto;

import java.util.List;

/**
 * Modelos de IA que ofrece el servidor y cual usa ahora el usuario para cada tarea.
 *
 * @param configured false si el servidor no tiene clave: el coach responde con reglas locales
 *                   y la generacion de rutinas no esta disponible.
 * @param selectable false si el servidor ofrece un solo modelo: Perfil lo muestra sin poder elegir.
 */
public record AiModelsResponse(boolean configured, boolean selectable, Selection coach, Selection routine, List<Model> models) {

    /**
     * @param selected     modelo que se usara.
     * @param defaultModel el del servidor; {@code custom} es true si el usuario eligio otro.
     */
    public record Selection(String selected, String defaultModel, boolean custom, int dailyLimit, int usedToday) {
    }

    public record Model(String id, String label, String description, String speed, String cost) {
    }
}
