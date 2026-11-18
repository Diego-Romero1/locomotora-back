package com.locomotora.demo.routine.dto;

import java.util.List;
import java.util.Map;

/**
 * Musculos del catalogo y grupos para navegarlo. El musculo principal es uno de MUSCLES; el grupo
 * junta musculos cercanos (Lumbares y Trapecio en Espalda) y separa estiramientos y cardio, que no
 * se eligen por musculo.
 */
public final class ExerciseGroups {
    public static final List<String> MUSCLES = List.of(
            "Pecho", "Espalda", "Lumbares", "Trapecio", "Hombros", "Cuello", "Bíceps", "Tríceps", "Antebrazos",
            "Abdomen", "Glúteos", "Cuádriceps", "Isquiotibiales", "Pantorrillas", "Aductores", "Abductores");

    public static final String STRETCHING = "Estiramientos";
    public static final String CARDIO = "Cardio y pliometría";

    /** Grupos en el orden en que se muestran. */
    public static final List<String> GROUPS = List.of(
            "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps", "Antebrazos", "Abdomen", "Glúteos", "Piernas",
            STRETCHING, CARDIO);

    private static final Map<String, String> GROUP_BY_MUSCLE = Map.ofEntries(
            Map.entry("Lumbares", "Espalda"),
            Map.entry("Trapecio", "Espalda"),
            Map.entry("Cuello", "Hombros"),
            Map.entry("Cuádriceps", "Piernas"),
            Map.entry("Isquiotibiales", "Piernas"),
            Map.entry("Pantorrillas", "Piernas"),
            Map.entry("Aductores", "Piernas"),
            Map.entry("Abductores", "Piernas"));

    private ExerciseGroups() {
    }

    /** Grupo de navegacion; null si el ejercicio no tiene musculo (ejercicio propio sin datos). */
    public static String groupOf(String primaryMuscle, String exerciseType) {
        if ("STRETCHING".equals(exerciseType)) {
            return STRETCHING;
        }
        if ("CARDIO".equals(exerciseType) || "PLYOMETRICS".equals(exerciseType)) {
            return CARDIO;
        }
        if (primaryMuscle == null || primaryMuscle.isBlank()) {
            return null;
        }
        return GROUP_BY_MUSCLE.getOrDefault(primaryMuscle, primaryMuscle);
    }
}
