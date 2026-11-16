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
        add(lines, "Preferencias de alimentacion", join(dietaryPreferences));
        add(lines, "Rutinas activas", join(activeRoutines));
        lines.add("- Sesiones completadas en los ultimos 28 dias: " + sessionsLast28Days);
        return String.join("\n", lines);
    }

    /** Contexto para generar rutinas: perfil, objetivos, limites, equipo y cargas de referencia. */
    public String routinePrompt() {
        List<String> lines = profileLines();
        lines.add("- Sesiones completadas en los ultimos 28 dias: " + sessionsLast28Days);
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
        add(lines, "Edad", p.age() == null ? null : p.age() + " anos");
        add(lines, "Sexo", p.sex());
        add(lines, "Altura", p.heightCm() == null ? null : number(p.heightCm()) + " cm");
        add(lines, "Peso", weightKg() == null ? null : number(weightKg()) + " kg");
        add(lines, "Nivel de actividad", p.activityLevel());
        add(lines, "Experiencia", p.experienceLevel());
        add(lines, "Dias disponibles por semana", p.trainingDaysPerWeek() == null ? null : p.trainingDaysPerWeek().toString());
        add(lines, "Minutos por sesion", p.sessionDurationMinutes() == null ? null : p.sessionDurationMinutes().toString());
        add(lines, "Estilo preferido", PersonalDataFilter.redact(p.preferredTrainingStyle()));
        add(lines, "Objetivos activos", join(goals));
        add(lines, "Lesiones o limitaciones", join(limitations));
        add(lines, "Equipo disponible", join(equipment));
        return lines;
    }

    private static void add(List<String> lines, String label, String value) {
        if (value != null && !value.isBlank()) {
            lines.add("- " + label + ": " + value);
        }
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
