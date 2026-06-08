package com.locomotora.demo.routine;

import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import com.locomotora.demo.routine.dto.ExerciseProgressLogRequest;
import com.locomotora.demo.routine.dto.ExerciseProgressLogResponse;
import com.locomotora.demo.routine.dto.RoutineCategoryResponse;
import com.locomotora.demo.routine.dto.RoutineRequest;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.dto.WorkoutLogRequest;
import com.locomotora.demo.routine.dto.WorkoutLogResponse;
import com.locomotora.demo.routine.service.ExerciseProgressService;
import com.locomotora.demo.routine.service.RoutineReadService;
import com.locomotora.demo.routine.service.RoutineService;
import com.locomotora.demo.routine.service.WorkoutLogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoutineController {
    private final RoutineReadService routineReadService;
    private final RoutineService routineService;
    private final WorkoutLogService workoutLogService;
    private final ExerciseProgressService exerciseProgressService;

    public RoutineController(
            RoutineReadService routineReadService,
            RoutineService routineService,
            WorkoutLogService workoutLogService,
            ExerciseProgressService exerciseProgressService
    ) {
        this.routineReadService = routineReadService;
        this.routineService = routineService;
        this.workoutLogService = workoutLogService;
        this.exerciseProgressService = exerciseProgressService;
    }

    @GetMapping("/routine")
    public RoutineResponse todayRoutine() {
        return routineReadService.todayRoutine();
    }

    @GetMapping("/routines")
    public List<RoutineResponse> routines(@RequestParam(required = false) UUID categoryId) {
        return routineReadService.routines(categoryId);
    }

    @GetMapping("/routine-categories")
    public List<RoutineCategoryResponse> routineCategories() {
        return routineReadService.routineCategories();
    }

    @GetMapping("/exercises")
    public List<ExerciseCatalogItemResponse> exercises() {
        return routineReadService.exerciseCatalog();
    }

    @PostMapping("/exercises/{id}/logs")
    public ExerciseProgressLogResponse logExerciseProgress(
            @PathVariable UUID id,
            @Valid @RequestBody ExerciseProgressLogRequest request
    ) {
        return exerciseProgressService.logExerciseProgress(id, request);
    }

    @GetMapping("/routines/{id}")
    public RoutineResponse getRoutine(@PathVariable UUID id) {
        return routineReadService.getRoutine(id);
    }

    @PostMapping("/routines")
    public RoutineResponse createRoutine(@Valid @RequestBody RoutineRequest request) {
        return routineService.createRoutine(request);
    }

    @PutMapping("/routines/{id}")
    public RoutineResponse updateRoutine(@PathVariable UUID id, @Valid @RequestBody RoutineRequest request) {
        return routineService.updateRoutine(id, request);
    }

    @DeleteMapping("/routines/{id}")
    public void deleteRoutine(@PathVariable UUID id) {
        routineService.deleteRoutine(id);
    }

    @PostMapping("/workout-log")
    public WorkoutLogResponse logWorkout(@Valid @RequestBody WorkoutLogRequest request) {
        return workoutLogService.logWorkout(request);
    }
}
