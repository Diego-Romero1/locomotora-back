package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.ExerciseResponse;
import com.locomotora.demo.routine.dto.RoutineCategoryResponse;
import com.locomotora.demo.routine.dto.RoutineDayResponse;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.model.RoutineDayHeader;
import com.locomotora.demo.routine.model.RoutineHeader;
import com.locomotora.demo.routine.repository.RoutineRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RoutineReadService {
    private final RoutineRepository routineRepository;

    public RoutineReadService(RoutineRepository routineRepository) {
        this.routineRepository = routineRepository;
    }

    public RoutineResponse todayRoutine() {
        UUID userId = CurrentUser.id();
        UUID routineId = routineRepository.findLatestActiveRoutineId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No active routine found"));
        return getRoutine(routineId);
    }

    public List<RoutineResponse> routines(UUID categoryId) {
        UUID userId = CurrentUser.id();
        boolean categoriesAvailable = routineRepository.routineCategoriesAvailable();
        if (categoryId != null && !categoriesAvailable) {
            return List.of();
        }
        if (categoryId == null || !categoriesAvailable) {
            return routineRepository.findActiveRoutines(userId, categoriesAvailable)
                    .stream()
                    .map(this::buildRoutineResponse)
                    .toList();
        }
        return routineRepository.findActiveRoutinesByCategory(userId, categoryId)
                .stream()
                .map(this::buildRoutineResponse)
                .toList();
    }

    public List<RoutineCategoryResponse> routineCategories() {
        if (!routineRepository.routineCategoriesAvailable()) {
            return List.of();
        }
        return routineRepository.findCategories();
    }

    public RoutineResponse getRoutine(UUID id) {
        UUID userId = CurrentUser.id();
        boolean categoriesAvailable = routineRepository.routineCategoriesAvailable();
        RoutineHeader header = routineRepository.findActiveRoutine(id, userId, categoriesAvailable)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Routine not found"));
        return buildRoutineResponse(header);
    }

    public boolean routineCategoriesAvailable() {
        return routineRepository.routineCategoriesAvailable();
    }

    public boolean categoryExists(UUID categoryId) {
        return routineRepository.categoryExists(categoryId);
    }

    private RoutineResponse buildRoutineResponse(RoutineHeader header) {
        List<RoutineDayResponse> days = daysForRoutine(header.id());
        List<ExerciseResponse> legacyExercises = days.isEmpty() ? List.of() : days.get(0).exercises();
        return new RoutineResponse(
                header.id().toString(),
                header.title(),
                header.description(),
                header.objective(),
                header.difficulty(),
                header.estimatedDurationMinutes(),
                header.daysPerWeek(),
                header.categoryId() == null ? null : header.categoryId().toString(),
                header.categoryName(),
                header.routineSplit(),
                header.source(),
                "TEMPLATE".equalsIgnoreCase(header.source()) && header.templateKey() != null,
                header.templateKey(),
                days,
                legacyExercises
        );
    }

    private List<RoutineDayResponse> daysForRoutine(UUID routineId) {
        return routineRepository.findDays(routineId)
                .stream()
                .map(this::toDayResponse)
                .toList();
    }

    private RoutineDayResponse toDayResponse(RoutineDayHeader day) {
        return new RoutineDayResponse(
                day.id().toString(),
                day.dayIndex(),
                day.title(),
                day.focus(),
                routineRepository.findExercisesForDay(day.id())
        );
    }
}
