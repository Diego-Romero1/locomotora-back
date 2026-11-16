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
 * KETTLEBELLS o MACHINES; el catalogo usa BODYWEIGHT, DUMBBELL, BARBELL, FREE_WEIGHT, MACHINE,
 * CABLE y SMITH_MACHINE.
 */
public final class ExerciseCatalogFilter {
    /** Si el filtro deja menos ejercicios que esto, se usa el catalogo entero. */
    public static final int MIN_FILTERED = 12;

    private static final Map<String, Set<String>> CATALOG_BY_USER_EQUIPMENT = Map.of(
            "BODYWEIGHT", Set.of("BODYWEIGHT"),
            "DUMBBELLS", Set.of("DUMBBELL", "FREE_WEIGHT"),
            "BARBELL", Set.of("BARBELL", "FREE_WEIGHT"),
            "KETTLEBELLS", Set.of("FREE_WEIGHT"),
            "RESISTANCE_BANDS", Set.of(),
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
