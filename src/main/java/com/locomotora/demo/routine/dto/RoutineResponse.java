package com.locomotora.demo.routine.dto;

import java.util.List;

/**
 * Rutina con sus dias. Los ultimos tres campos (R2) solo vienen en las rutinas del usuario:
 * {@code current} = es la rutina de Hoy; {@code lastTrainedAt} = ultima sesion de la rutina;
 * {@code nextDayId} = dia que toca en el ciclo.
 */
public record RoutineResponse(
        String id,
        String title,
        String description,
        String objective,
        String difficulty,
        Integer estimatedDurationMinutes,
        Integer daysPerWeek,
        String categoryId,
        String categoryName,
        String routineSplit,
        String source,
        boolean isTemplate,
        String templateKey,
        List<RoutineDayResponse> days,
        List<ExerciseResponse> exercises,
        boolean current,
        String lastTrainedAt,
        String nextDayId
) {
    public RoutineResponse(String id, String title, String description, String objective, String difficulty,
                           Integer estimatedDurationMinutes, Integer daysPerWeek, String categoryId, String categoryName,
                           String routineSplit, String source, boolean isTemplate, String templateKey,
                           List<RoutineDayResponse> days, List<ExerciseResponse> exercises) {
        this(id, title, description, objective, difficulty, estimatedDurationMinutes, daysPerWeek, categoryId,
                categoryName, routineSplit, source, isTemplate, templateKey, days, exercises, false, null, null);
    }

    public RoutineResponse withTraining(boolean isCurrent, String lastTrained, String nextDay, List<RoutineDayResponse> trainedDays) {
        return new RoutineResponse(id, title, description, objective, difficulty, estimatedDurationMinutes, daysPerWeek,
                categoryId, categoryName, routineSplit, source, isTemplate, templateKey, trainedDays, exercises,
                isCurrent, lastTrained, nextDay);
    }
}
