package com.locomotora.demo.routine.dto;

import java.util.List;

/** {@code lastTrainedAt}: ultima sesion completada de este dia (ISO-8601), o null si nunca se entreno. */
public record RoutineDayResponse(
        String id,
        int dayIndex,
        String title,
        String focus,
        List<ExerciseResponse> exercises,
        String lastTrainedAt
) {
    public RoutineDayResponse(String id, int dayIndex, String title, String focus, List<ExerciseResponse> exercises) {
        this(id, dayIndex, title, focus, exercises, null);
    }

    public RoutineDayResponse withLastTrainedAt(String value) {
        return new RoutineDayResponse(id, dayIndex, title, focus, exercises, value);
    }
}
