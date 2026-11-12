package com.locomotora.demo.routine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Contratos del historial (Fase 3): lista, detalle, correccion de sesiones y progreso por ejercicio. */
public final class HistoryDtos {
    private HistoryDtos() {
    }

    /** Una fila del historial. */
    public record SessionListItem(
            String id,
            String kind,
            String routineId,
            String routineTitle,
            String dayTitle,
            String startedAt,
            String completedAt,
            long durationSeconds,
            int completedSets,
            int exerciseCount,
            BigDecimal totalVolumeKg,
            Integer perceivedExertion,
            int recordCount
    ) {
    }

    public record SessionPage(List<SessionListItem> items, String nextCursor) {
    }

    public record SessionDetail(
            SessionListItem session,
            String notes,
            List<SessionExercise> exercises
    ) {
    }

    public record SessionExercise(
            int position,
            String exerciseId,
            String name,
            String primaryMuscleGroup,
            String routineExerciseId,
            Plan plan,
            String notes,
            List<SessionSet> sets
    ) {
    }

    /** El plan del momento, copiado al guardar (null en sesiones anteriores a la Fase 1). */
    public record Plan(Integer repsMin, Integer repsMax, BigDecimal weightKg, Integer restSeconds) {
    }

    public record SessionSet(String id, int setNumber, Integer reps, BigDecimal weightKg, boolean completed, boolean record) {
    }

    /** Correccion de una sesion: valores de series existentes, series a borrar, esfuerzo y notas. */
    public record SessionUpdateRequest(
            @Min(1) @Max(10) Integer perceivedExertion,
            @Size(max = 2000) String notes,
            @Size(max = 500) List<@Valid @NotNull SetUpdate> sets,
            @Size(max = 500) List<@NotNull UUID> deletedSetIds
    ) {
    }

    public record SetUpdate(
            @NotNull UUID id,
            @Min(0) @Max(1000) Integer reps,
            @DecimalMin("0.0") @DecimalMax("2000.0") BigDecimal weightKg
    ) {
    }

    /** Ejercicio entrenado alguna vez, para la lista de Progreso › Ejercicios. */
    public record TrainedExercise(
            String exerciseId,
            String name,
            String primaryMuscleGroup,
            int sessionCount,
            String lastCompletedAt,
            BigDecimal bestEstimatedOneRepMaxKg
    ) {
    }

    public record ExerciseHistory(
            String exerciseId,
            String name,
            String primaryMuscleGroup,
            List<ExercisePoint> points,
            ExerciseRecords records
    ) {
    }

    /** Una sesion en la historia del ejercicio: su mejor serie y el volumen del ejercicio. */
    public record ExercisePoint(
            String sessionId,
            String completedAt,
            BigDecimal bestWeightKg,
            Integer bestReps,
            BigDecimal estimatedOneRepMaxKg,
            BigDecimal volumeKg,
            int setCount
    ) {
    }

    public record ExerciseRecords(RecordValue bestEstimatedOneRepMax, RecordValue heaviest, RecordValue mostVolume) {
    }

    /** {@code value} es la fuerza estimada, el peso o el volumen segun el record. */
    public record RecordValue(BigDecimal value, BigDecimal weightKg, Integer reps, String completedAt, String sessionId) {
    }

    /** Una semana (lunes a domingo, en la zona horaria pedida). */
    public record WeekStats(String weekStart, int sessions, int trainedDays, int completedSets, BigDecimal volumeKg) {
    }
}
