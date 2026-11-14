package com.locomotora.demo.ai.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Foto del usuario que se le pasa al modelo: perfil, objetivos, limites y como viene entrenando. */
public record UserContext(
        Profile profile,
        Double latestWeightKg,
        List<String> goals,
        List<String> limitations,
        List<String> equipment,
        List<String> activeRoutines,
        List<Session> recentSessions,
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

    /** Texto compacto para el prompt. Solo incluye lo que se conoce. */
    public String toPrompt() {
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
        add(lines, "Estilo preferido", p.preferredTrainingStyle());
        add(lines, "Objetivos activos", join(goals));
        add(lines, "Lesiones o limitaciones", join(limitations));
        add(lines, "Equipo disponible", join(equipment));
        add(lines, "Rutinas activas", join(activeRoutines));
        lines.add("- Sesiones completadas en los ultimos 28 dias: " + sessionsLast28Days);
        if (!recentSessions.isEmpty()) {
            lines.add("- Ultimas sesiones:");
            for (Session session : recentSessions) {
                lines.add("  - " + session.day() + " " + session.title() + ": " + session.completedSets() + " series, "
                        + number(session.volumeKg()) + " kg de volumen"
                        + (session.perceivedExertion() == null ? "" : ", esfuerzo percibido " + session.perceivedExertion() + "/10"));
            }
        }
        if (!bestLifts.isEmpty()) {
            lines.add("- Mejores series recientes (90 dias):");
            for (Lift lift : bestLifts) {
                lines.add("  - " + lift.exercise() + ": " + number(lift.weightKg()) + " kg x " + lift.reps());
            }
        }
        return String.join("\n", lines);
    }

    private static void add(List<String> lines, String label, String value) {
        if (value != null && !value.isBlank()) {
            lines.add("- " + label + ": " + value);
        }
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(", ", values);
    }

    private static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
