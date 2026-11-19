package com.locomotora.demo.ai.dto;

import java.util.List;

/**
 * "Detalles con video" de un ejercicio.
 *
 * @param tips        consejos de tecnica generados con IA; null si la IA no esta disponible.
 * @param video       video corto de YouTube que muestra exactamente este ejercicio; null si no hay.
 * @param videoStatus FOUND, NOT_FOUND (se busco y ninguno era este ejercicio) o UNAVAILABLE (la busqueda
 *                    no esta configurada o fallo; se puede reintentar).
 * @param searchUrl   busqueda en YouTube para que el usuario busque por su cuenta.
 */
public record ExerciseGuideResponse(
        String exerciseId,
        String exerciseName,
        Tips tips,
        Video video,
        String videoStatus,
        String searchUrl,
        boolean aiAvailable
) {
    public record Tips(String summary, List<String> cues, List<String> mistakes, String breathing) {
        public Tips {
            cues = cues == null ? List.of() : List.copyOf(cues);
            mistakes = mistakes == null ? List.of() : List.copyOf(mistakes);
        }
    }

    public record Video(String youtubeId, String title, String channel, int durationSeconds, String watchUrl, String thumbnailUrl) {
    }
}
