package com.locomotora.demo.routine.dto;

import java.util.List;

/**
 * Dia de una rutina. Los ultimos tres campos solo vienen en las rutinas del usuario:
 * {@code lastTrainedAt} = ultima sesion completada de este dia (ISO-8601) o null;
 * {@code doneInRound} = ya se hizo en la vuelta actual (R5);
 * {@code pendingSince} = tocaba y se hizo otro dia en su lugar: desde cuando esta pendiente (ISO-8601) o null.
 */
public record RoutineDayResponse(
        String id,
        int dayIndex,
        String title,
        String focus,
        List<ExerciseResponse> exercises,
        String lastTrainedAt,
        boolean doneInRound,
        String pendingSince
) {
    public RoutineDayResponse(String id, int dayIndex, String title, String focus, List<ExerciseResponse> exercises) {
        this(id, dayIndex, title, focus, exercises, null, false, null);
    }

    public RoutineDayResponse withTraining(String lastTrained, boolean done, String pending) {
        return new RoutineDayResponse(id, dayIndex, title, focus, exercises, lastTrained, done, pending);
    }
}
