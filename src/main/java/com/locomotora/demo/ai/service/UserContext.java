package com.locomotora.demo.ai.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Datos autorizados del usuario que se le pasan al modelo. Nunca incluye nombre, email, fecha de
 * nacimiento (solo la edad), foto ni notas medicas. Los textos libres pasan por
 * {@link PersonalDataFilter}. Lo que pesa (sesiones, rutinas, progreso) lo pide el agente con
 * herramientas, para gastar pocos tokens.
 */
public record UserContext(
        Profile profile,
        Double latestWeightKg,
        List<String> goals,
        List<String> limitations,
        List<String> equipment,
        List<String> dietaryPreferences,
        List<String> activeRoutines,
        int sessionsLast28Days,
        List<Lift> bestLifts
) {
    public record Profile(
            Integer age,
            String sex,
            Double heightCm,
            Double weightKg,
            String activityLevel,
            String experienceLevel,
            Integer trainingDaysPerWeek,
            Integer sessionDurationMinutes,
            String preferredTrainingStyle
    ) {
        public static final Profile EMPTY = new Profile(null, null, null, null, null, null, null, null, null);
    }

    public record Session(LocalDate day, String title, int completedSets, double volumeKg, Integer perceivedExertion) {
    }

    public record Lift(String exercise, double weightKg, int reps) {
    }

    public Double weightKg() {
        return latestWeightKg != null ? latestWeightKg : profile.weightKg();
    }

    /** Contexto del chat: perfil, objetivos, limites, equipo, alimentacion y constancia. */
    public String coachPrompt() {
        List<String> lines = profileLines();
        add(lines, "Preferencias de alimentación", join(dietaryPreferences));
        add(lines, "Rutinas activas", join(activeRoutines));
        lines.add("- Sesiones completadas en los últimos 28 días: " + sessionsLast28Days);
        return String.join("\n", lines);
    }

    /** Contexto para generar rutinas: perfil, objetivos, limites, equipo y cargas de referencia. */
    public String routinePrompt() {
        List<String> lines = profileLines();
        lines.add("- Sesiones completadas en los últimos 28 días: " + sessionsLast28Days);
        if (!bestLifts.isEmpty()) {
            List<String> lifts = bestLifts.stream().limit(5)
                    .map(lift -> lift.exercise() + " " + number(lift.weightKg()) + " kg x " + lift.reps())
                    .toList();
            add(lines, "Mejores series recientes", String.join(", ", lifts));
        }
        return String.join("\n", lines);
    }

    private List<String> profileLines() {
        List<String> lines = new ArrayList<>();
        Profile p = profile;
        add(lines, "Edad", p.age() == null ? null : p.age() + " años");
        add(lines, "Sexo", label(p.sex()));
        add(lines, "Altura", p.heightCm() == null ? null : number(p.heightCm()) + " cm");
        add(lines, "Peso", weightKg() == null ? null : number(weightKg()) + " kg");
        add(lines, "Nivel de actividad", label(p.activityLevel()));
        add(lines, "Experiencia", label(p.experienceLevel()));
        add(lines, "Días disponibles por semana", p.trainingDaysPerWeek() == null ? null : p.trainingDaysPerWeek().toString());
        add(lines, "Minutos por sesión", p.sessionDurationMinutes() == null ? null : p.sessionDurationMinutes().toString());
        add(lines, "Estilo preferido", PersonalDataFilter.redact(p.preferredTrainingStyle()));
        add(lines, "Objetivos activos", join(goals.stream().map(UserContext::goalLabel).toList()));
        add(lines, "Lesiones o limitaciones", join(limitations));
        add(lines, "Equipo disponible", join(equipment.stream().map(UserContext::label).toList()));
        return lines;
    }

    private static void add(List<String> lines, String label, String value) {
        if (value != null && !value.isBlank()) {
            lines.add("- " + label + ": " + value);
        }
    }

    /**
     * Codigos del perfil en palabras, solo para el texto que lee el modelo (los codigos se siguen usando
     * para filtrar el catalogo). Con codigos en ingles el modelo tiende a responder "barbell" o en ingles.
     */
    private static final java.util.Map<String, String> LABELS = java.util.Map.ofEntries(
            java.util.Map.entry("MALE", "hombre"),
            java.util.Map.entry("FEMALE", "mujer"),
            java.util.Map.entry("OTHER", "otro"),
            java.util.Map.entry("PREFER_NOT_TO_SAY", "prefiere no decirlo"),
            java.util.Map.entry("SEDENTARY", "sedentario"),
            java.util.Map.entry("LIGHT", "ligero"),
            java.util.Map.entry("MODERATE", "moderado"),
            java.util.Map.entry("ACTIVE", "activo"),
            java.util.Map.entry("VERY_ACTIVE", "muy activo"),
            java.util.Map.entry("BEGINNER", "principiante"),
            java.util.Map.entry("INTERMEDIATE", "intermedio"),
            java.util.Map.entry("ADVANCED", "avanzado"),
            java.util.Map.entry("BODYWEIGHT", "peso corporal"),
            java.util.Map.entry("DUMBBELLS", "mancuernas"),
            java.util.Map.entry("BARBELL", "barra"),
            java.util.Map.entry("KETTLEBELLS", "pesas rusas"),
            java.util.Map.entry("MACHINES", "máquinas"),
            java.util.Map.entry("RESISTANCE_BANDS", "bandas elásticas"),
            java.util.Map.entry("BUILD_MUSCLE", "ganar músculo"),
            java.util.Map.entry("LOSE_WEIGHT", "bajar de peso"),
            java.util.Map.entry("GENERAL_FITNESS", "estar en forma"),
            java.util.Map.entry("IMPROVE_ENDURANCE", "mejorar la resistencia"),
            java.util.Map.entry("IMPROVE_STRENGTH", "ganar fuerza"));

    static String label(String code) {
        return code == null ? null : LABELS.getOrDefault(code.trim().toUpperCase(Locale.ROOT), code);
    }

    /** "BUILD_MUSCLE (meta 80 kg ...)" → "ganar músculo (meta 80 kg ...)". */
    static String goalLabel(String goal) {
        int detail = goal.indexOf(" (");
        return detail < 0 ? label(goal) : label(goal.substring(0, detail)) + goal.substring(detail);
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return PersonalDataFilter.redact(String.join(", ", values));
    }

    private static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
