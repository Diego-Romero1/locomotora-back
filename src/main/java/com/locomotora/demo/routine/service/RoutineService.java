package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.ExerciseRequest;
import com.locomotora.demo.routine.dto.RoutineRequest;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.repository.RoutineWriteRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoutineService {
    private static final Set<String> ALLOWED_DIFFICULTIES = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED");

    private final RoutineReadService routineReadService;
    private final RoutineWriteRepository routineWriteRepository;

    public RoutineService(RoutineReadService routineReadService, RoutineWriteRepository routineWriteRepository) {
        this.routineReadService = routineReadService;
        this.routineWriteRepository = routineWriteRepository;
    }

    @Transactional
    public RoutineResponse createRoutine(RoutineRequest request) {
        UUID userId = CurrentUser.id();
        boolean categoriesAvailable = routineReadService.routineCategoriesAvailable();
        UUID categoryId = validateCategoryId(request.categoryId(), categoriesAvailable);
        String difficulty = normalizeDifficulty(request.difficulty(), "Routine difficulty");
        UUID routineId = routineWriteRepository.createRoutine(userId, request, difficulty, categoryId, categoriesAvailable);
        replaceExercises(routineId, request.exercises());
        return routineReadService.getRoutine(routineId);
    }

    @Transactional
    public RoutineResponse updateRoutine(UUID routineId, RoutineRequest request) {
        UUID userId = CurrentUser.id();
        boolean categoriesAvailable = routineReadService.routineCategoriesAvailable();
        UUID categoryId = validateCategoryId(request.categoryId(), categoriesAvailable);
        String difficulty = normalizeDifficulty(request.difficulty(), "Routine difficulty");
        if (routineWriteRepository.countRoutineDays(routineId, userId) > 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This routine has multiple days and cannot be edited with the legacy single-day payload");
        }

        int updated = routineWriteRepository.updateRoutine(routineId, userId, request, difficulty, categoryId, categoriesAvailable);
        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Routine not found");
        }
        replaceExercises(routineId, request.exercises());
        return routineReadService.getRoutine(routineId);
    }

    public void deleteRoutine(UUID routineId) {
        int updated = routineWriteRepository.softDelete(routineId, CurrentUser.id());
        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Routine not found");
        }
    }

    private void replaceExercises(UUID routineId, List<ExerciseRequest> exercises) {
        routineWriteRepository.deleteDays(routineId);
        UUID routineDayId = routineWriteRepository.createDefaultDay(routineId);
        int position = 1;
        for (ExerciseRequest exercise : exercises) {
            String exerciseDifficulty = normalizeDifficulty(exercise.difficulty(), "Exercise difficulty");
            UUID exerciseId = routineWriteRepository.findOrCreateExercise(exercise, exerciseDifficulty);
            routineWriteRepository.insertRoutineExercise(routineDayId, exerciseId, position++, exercise);
        }
    }

    private UUID validateCategoryId(UUID categoryId, boolean categoriesAvailable) {
        if (categoryId == null) {
            return null;
        }
        if (!categoriesAvailable) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Routine categories are not available until migrations are applied");
        }
        if (!routineReadService.categoryExists(categoryId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Routine category not found");
        }
        return categoryId;
    }

    private String normalizeDifficulty(String difficulty, String fieldName) {
        if (difficulty == null) {
            return null;
        }
        String normalized = difficulty.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_DIFFICULTIES.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    fieldName + " must be BEGINNER, INTERMEDIATE or ADVANCED");
        }
        return normalized;
    }
}
