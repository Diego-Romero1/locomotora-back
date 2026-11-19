package com.locomotora.demo.ai.service;

import com.locomotora.demo.ai.youtube.YouTubeClient.Video;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reglas fijas que descartan un video por su titulo antes de preguntarle a la IA (no gastan tokens y no
 * dependen del modelo): recopilaciones, otro equipo, otra inclinacion o una sola mano cuando el ejercicio no
 * lo es. Una palabra que aparece en el nombre del propio ejercicio nunca cuenta como conflicto.
 */
public final class VideoRules {
    private static final Pattern COMPILATION = Pattern.compile(
            "\\b(top ?\\d+|\\d+ (ejercicios|exercises|variaciones|variations)|mejores ejercicios|best \\d+|rutina|routine"
                    + "|full (body|workout)|entrenamiento completo|ejercicios para|exercises for|vs|versus)\\b");

    private static final Map<String, String> WORDS = Map.of(
            "BARBELL", "\\b(barbell|barra olimpica|con barra)\\b",
            "DUMBBELL", "\\b(mancuernas?|dumbbells?)\\b",
            "KETTLEBELL", "\\b(kettlebells?|pesa rusa|pesas rusas)\\b",
            "SMITH", "\\b(smith|multipower)\\b",
            "CABLE", "\\b(polea|poleas|cable|cables|crossover)\\b",
            "MACHINE", "\\b(maquina|machine)\\b",
            "BAND", "\\b(banda|bandas|band|bands|liga|ligas)\\b");

    /** Equipos que, si aparecen en el titulo, indican otro ejercicio. */
    private static final Map<String, List<String>> CONFLICTS = Map.ofEntries(
            Map.entry("BARBELL", List.of("DUMBBELL", "KETTLEBELL", "SMITH", "CABLE", "MACHINE", "BAND")),
            Map.entry("EZ_BAR", List.of("DUMBBELL", "KETTLEBELL", "SMITH", "CABLE", "BAND")),
            Map.entry("DUMBBELL", List.of("BARBELL", "KETTLEBELL", "SMITH", "CABLE", "MACHINE", "BAND")),
            Map.entry("KETTLEBELL", List.of("BARBELL", "DUMBBELL", "SMITH", "CABLE", "MACHINE")),
            Map.entry("CABLE", List.of("DUMBBELL", "KETTLEBELL", "SMITH", "BAND")),
            Map.entry("MACHINE", List.of("DUMBBELL", "KETTLEBELL", "BAND")),
            Map.entry("SMITH_MACHINE", List.of("DUMBBELL", "KETTLEBELL", "CABLE", "BAND")),
            Map.entry("BAND", List.of("BARBELL", "DUMBBELL", "KETTLEBELL", "SMITH", "CABLE", "MACHINE")),
            Map.entry("BODYWEIGHT", List.of("BARBELL", "DUMBBELL", "KETTLEBELL", "SMITH", "CABLE", "MACHINE")));

    private static final Pattern INCLINE = Pattern.compile("\\b(inclin\\w*|incline)\\b");
    private static final Pattern DECLINE = Pattern.compile("\\b(declin\\w*|decline)\\b");
    /** Inclinado/declinado solo distingue variantes en presses, aperturas y curls ("remo inclinado" es otra cosa). */
    private static final Pattern ANGLED_MOVES = Pattern.compile("\\b(press|banca|bench|apertura\\w*|fly\\w*|flye\\w*|curl\\w*|crunch\\w*)\\b");
    private static final Pattern ONE_SIDE = Pattern.compile(
            "\\b(una mano|un brazo|una pierna|one[ -]arm|single[ -]arm|one[ -]leg|single[ -]leg|unilateral)\\b");

    private VideoRules() {
    }

    /** true si el titulo no contradice el ejercicio. */
    public static boolean acceptable(ExerciseCatalogItemResponse exercise, Video video) {
        String title = normalize(video.title());
        String own = normalize(exercise.name() + " " + (exercise.nameEn() == null ? "" : exercise.nameEn()));
        if (COMPILATION.matcher(title).find()) {
            return false;
        }
        String equipment = exercise.equipment() == null ? "" : exercise.equipment();
        for (String other : CONFLICTS.getOrDefault(equipment, List.of())) {
            Pattern words = Pattern.compile(WORDS.get(other));
            if (words.matcher(title).find() && !words.matcher(own).find()) {
                return false;
            }
        }
        if (ANGLED_MOVES.matcher(own).find()) {
            boolean ownIncline = INCLINE.matcher(own).find();
            boolean ownDecline = DECLINE.matcher(own).find();
            if (!ownIncline && INCLINE.matcher(title).find()) {
                return false;
            }
            if (!ownDecline && DECLINE.matcher(title).find()) {
                return false;
            }
        }
        return ONE_SIDE.matcher(own).find() || !ONE_SIDE.matcher(title).find();
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 -]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
