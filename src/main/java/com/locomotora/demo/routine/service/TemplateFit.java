package com.locomotora.demo.routine.service;

import java.util.List;
import java.util.Set;

/**
 * Cuanto le sirve una plantilla al usuario mas alla de objetivo, division y dias: ejercicios que
 * piden equipo que no tiene o que cargan una zona lesionada. El recomendador prefiere la de menos
 * penalizacion entre las que ya encajan en lo demas.
 */
public final class TemplateFit {
    private TemplateFit() {
    }

    public record ExerciseInfo(String equipment, List<String> stressZones) {
    }

    /**
     * @param allowedEquipment equipo del catalogo que el usuario puede usar; null = todo.
     * @param injuredZones zonas lesionadas declaradas (KNEE, SHOULDER...).
     */
    public static int penalty(List<ExerciseInfo> exercises, Set<String> allowedEquipment, Set<String> injuredZones) {
        int penalty = 0;
        for (ExerciseInfo exercise : exercises) {
            String equipment = exercise.equipment();
            if (allowedEquipment != null && equipment != null && !equipment.isBlank() && !allowedEquipment.contains(equipment)) {
                penalty++;
            }
            if (exercise.stressZones() != null && exercise.stressZones().stream().anyMatch(injuredZones::contains)) {
                penalty++;
            }
        }
        return penalty;
    }

    /** Minutos que la plantilla excede lo que el usuario tiene por sesion (0 si entra). */
    public static int minutesOver(Integer templateMinutes, int sessionMinutes) {
        return templateMinutes == null ? 0 : Math.max(0, templateMinutes - sessionMinutes);
    }

    /** Con una condicion de salud o siendo menor, el plan arranca un nivel por debajo. */
    public static String cautiousLevel(String level, boolean cautious) {
        if (!cautious) {
            return level;
        }
        return switch (level) {
            case "ADVANCED" -> "INTERMEDIATE";
            default -> "BEGINNER";
        };
    }
}
