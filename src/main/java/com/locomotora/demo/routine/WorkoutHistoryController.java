package com.locomotora.demo.routine;

import com.locomotora.demo.routine.dto.HistoryDtos.ExerciseHistory;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionDetail;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionPage;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionUpdateRequest;
import com.locomotora.demo.routine.dto.HistoryDtos.TrainedExercise;
import com.locomotora.demo.routine.dto.HistoryDtos.WeekStats;
import com.locomotora.demo.routine.service.WorkoutHistoryService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Historial y progreso (Fase 3). */
@RestController
public class WorkoutHistoryController {
    private final WorkoutHistoryService historyService;

    public WorkoutHistoryController(WorkoutHistoryService historyService) {
        this.historyService = historyService;
    }

    /** Historial paginado por cursor, de la sesion mas nueva a la mas vieja. Sin registros sueltos. */
    @GetMapping("/workout-sessions")
    public SessionPage list(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit
    ) {
        return historyService.list(cursor, limit);
    }

    @GetMapping("/workout-sessions/{id}")
    public SessionDetail detail(@PathVariable UUID id) {
        return historyService.detail(id);
    }

    /** Corrige una sesion guardada: valores de series, series borradas, esfuerzo y notas. */
    @PutMapping("/workout-sessions/{id}")
    public SessionDetail update(@PathVariable UUID id, @Valid @RequestBody SessionUpdateRequest request) {
        return historyService.update(id, request);
    }

    @DeleteMapping("/workout-sessions/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        historyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/progress/exercises")
    public List<TrainedExercise> trainedExercises() {
        return historyService.trainedExercises();
    }

    @GetMapping("/exercises/{id}/history")
    public ExerciseHistory exerciseHistory(@PathVariable UUID id, @RequestParam(required = false) Integer limit) {
        return historyService.exerciseHistory(id, limit);
    }

    /** Totales por semana en la zona horaria del telefono (por defecto, las ultimas 8). */
    @GetMapping("/progress/weekly")
    public List<WeekStats> weekly(
            @RequestParam(required = false) String tz,
            @RequestParam(required = false) Integer weeks
    ) {
        return historyService.weekly(tz, weeks, Instant.now());
    }
}
