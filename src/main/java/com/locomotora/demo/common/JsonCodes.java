package com.locomotora.demo.common;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Arreglos JSON de codigos simples (A-Z, 0-9 y _) guardados en columnas jsonb, como
 * user_profiles.health_flags o exercises.stress_zones. Sin dependencia de un parser JSON.
 */
public final class JsonCodes {
    private JsonCodes() {
    }

    /** "[\"A\", \"B\"]" → [A, B]; null si la columna es null. */
    public static List<String> parse(String json) {
        if (json == null) {
            return null;
        }
        return Arrays.stream(json.replaceAll("[\\[\\]\" ]", "").split(","))
                .filter(code -> !code.isEmpty())
                .toList();
    }

    /** Solo para codigos ya validados: no escapa caracteres. */
    public static String toArray(List<String> codes) {
        return codes.stream().map(code -> "\"" + code + "\"").collect(Collectors.joining(",", "[", "]"));
    }
}
