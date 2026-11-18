package com.locomotora.demo.ai.tools;

import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Filtro del catalogo por el equipo del usuario. Lo usan la herramienta search_exercises y la
 * generacion de rutinas: una sola regla para las dos.
 *
 * <p>El onboarding guarda el equipo como BODYWEIGHT, DUMBBELLS, BARBELL, RESISTANCE_BANDS,
 * KETTLEBELLS o MACHINES; el catalogo usa BODYWEIGHT, DUMBBELL, BARBELL, EZ_BAR, FREE_WEIGHT,
 * KETTLEBELL, BAND, MACHINE, CABLE, SMITH_MACHINE y OTHER (implementos poco comunes: solo entran
 * si el usuario no cargo equipo).
 */
public final class ExerciseCatalogFilter {
    /** Si el filtro deja menos ejercicios que esto, se usa el catalogo entero. */
    public static final int MIN_FILTERED = 12;
    /**
     * Tope de ejercicios en el prompt de rutinas. Cada linea son ~12 tokens: 120 lineas entran en
     * el limite de tokens por minuto de Groq junto con el resto del prompt.
     */
    public static final int MAX_PROMPT_EXERCISES = 120;
    /** Tipos que no van en una rutina de gimnasio generada (se hacen aparte). */
    private static final Set<String> NOT_FOR_ROUTINES = Set.of("STRETCHING", "CARDIO");

    private static final Map<String, Set<String>> CATALOG_BY_USER_EQUIPMENT = Map.of(
            "BODYWEIGHT", Set.of("BODYWEIGHT"),
            "DUMBBELLS", Set.of("DUMBBELL", "FREE_WEIGHT"),
            "BARBELL", Set.of("BARBELL", "EZ_BAR", "FREE_WEIGHT"),
            "KETTLEBELLS", Set.of("KETTLEBELL", "FREE_WEIGHT"),
            "RESISTANCE_BANDS", Set.of("BAND"),
            "MACHINES", Set.of("MACHINE", "CABLE", "SMITH_MACHINE"));

    private ExerciseCatalogFilter() {
    }

    /** Equipos del catalogo que el usuario puede usar, o null si no cargo equipo (todo vale). */
    public static Set<String> allowedEquipment(List<String> userEquipment) {
        if (userEquipment == null || userEquipment.isEmpty()) {
            return null;
        }
        Set<String> allowed = new HashSet<>(Set.of("BODYWEIGHT"));
        for (String equipment : userEquipment) {
            if (equipment == null) {
                continue;
            }
            String key = equipment.trim().toUpperCase(Locale.ROOT);
            allowed.addAll(CATALOG_BY_USER_EQUIPMENT.getOrDefault(key, Set.of(key)));
        }
        return allowed;
    }

    /** Un ejercicio sin equipo cargado se considera disponible. */
    public static boolean fits(ExerciseCatalogItemResponse exercise, Set<String> allowed) {
        return allowed == null || exercise.equipment() == null || exercise.equipment().isBlank()
                || allowed.contains(exercise.equipment());
    }

    public record Result(List<ExerciseCatalogItemResponse> exercises, boolean filtered) {
    }

    /** Catalogo que puede usar el usuario; si queda demasiado chico, el catalogo entero. */
    public static Result forUser(List<ExerciseCatalogItemResponse> catalog, List<String> userEquipment) {
        Set<String> allowed = allowedEquipment(userEquipment);
        if (allowed == null) {
            return new Result(catalog, false);
        }
        List<ExerciseCatalogItemResponse> filtered = catalog.stream().filter(exercise -> fits(exercise, allowed)).toList();
        return filtered.size() >= MIN_FILTERED ? new Result(filtered, true) : new Result(catalog, false);
    }

    /**
     * Saca los ejercicios que cargan alguna de las zonas lesionadas del usuario. Si quedan muy
     * pocos, devuelve el catalogo como vino (el prompt igual pide cuidar esas zonas).
     */
    public static Result withoutStress(List<ExerciseCatalogItemResponse> catalog, Set<String> injuredZones) {
        if (injuredZones == null || injuredZones.isEmpty()) {
            return new Result(catalog, false);
        }
        List<ExerciseCatalogItemResponse> safe = catalog.stream()
                .filter(exercise -> exercise.stressZones().stream().noneMatch(injuredZones::contains))
                .toList();
        return safe.size() >= MIN_FILTERED ? new Result(safe, true) : new Result(catalog, false);
    }

    /**
     * Catalogo para el prompt de rutinas: sin estiramientos ni cardio, primero los ejercicios
     * "core" (los curados y los propios) y despues, si el usuario pidio grupos, el resto de esos
     * grupos (compuestos antes). Respeta el orden del catalogo y no pasa de MAX_PROMPT_EXERCISES.
     */
    public static List<ExerciseCatalogItemResponse> forPrompt(List<ExerciseCatalogItemResponse> catalog,
            List<String> focusMuscles) {
        Set<String> focus = new HashSet<>();
        if (focusMuscles != null) {
            focusMuscles.forEach(muscle -> focus.add(normalize(muscle)));
        }
        List<ExerciseCatalogItemResponse> usable = catalog.stream()
                .filter(exercise -> !NOT_FOR_ROUTINES.contains(exercise.exerciseType()))
                .toList();
        Map<ExerciseCatalogItemResponse, Integer> priority = new java.util.IdentityHashMap<>();
        for (ExerciseCatalogItemResponse exercise : usable) {
            boolean focused = focus.contains(normalize(exercise.primaryMuscleGroup()))
                    || focus.contains(normalize(exercise.muscleGroup()));
            boolean compound = "COMPOUND".equals(exercise.mechanic());
            int rank = exercise.core() ? (focused ? 0 : 1) : focused ? (compound ? 2 : 3) : -1;
            if (rank >= 0) {
                priority.put(exercise, rank);
            }
        }
        // Catalogo sin "core" (bases viejas o filtro muy estrecho): entra todo lo usable.
        if (priority.size() < MIN_FILTERED) {
            usable.forEach(exercise -> priority.putIfAbsent(exercise, "COMPOUND".equals(exercise.mechanic()) ? 4 : 5));
        }
        Set<ExerciseCatalogItemResponse> kept = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        usable.stream()
                .filter(priority::containsKey)
                .sorted(java.util.Comparator.comparingInt(priority::get))
                .limit(MAX_PROMPT_EXERCISES)
                .forEach(kept::add);
        return usable.stream().filter(kept::contains).toList();
    }

    /** Minusculas sin tildes, para comparar nombres y grupos ("Cuádriceps" = "cuadriceps"). */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
