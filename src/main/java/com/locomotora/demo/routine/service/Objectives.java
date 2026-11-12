package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.ApiException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

/** Vocabulario unico de objetivos de rutina (el mismo del onboarding). */
public final class Objectives {
    public static final String BUILD_MUSCLE = "BUILD_MUSCLE";
    public static final Set<String> CANONICAL = Set.of(
            BUILD_MUSCLE, "LOSE_WEIGHT", "GENERAL_FITNESS", "IMPROVE_ENDURANCE", "IMPROVE_STRENGTH");
    private static final Map<String, String> ALIASES = Map.of(
            "HYPERTROPHY", BUILD_MUSCLE,
            "HIPERTROFIA", BUILD_MUSCLE,
            "GAIN_MUSCLE", BUILD_MUSCLE,
            "STRENGTH", "IMPROVE_STRENGTH",
            "ENDURANCE", "IMPROVE_ENDURANCE");

    private Objectives() {
    }

    /** Valor canonico, o el texto en mayusculas si no se reconoce. null si viene vacio. */
    public static String canonicalOrRaw(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return ALIASES.getOrDefault(upper, upper);
    }

    /** Para rutinas nuevas o editadas: solo valores conocidos (o null). */
    public static String requireKnown(String value) {
        String normalized = canonicalOrRaw(value);
        if (normalized != null && !CANONICAL.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown routine objective");
        }
        return normalized;
    }
}
