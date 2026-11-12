package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.ExerciseRequest;
import com.locomotora.demo.routine.dto.RoutineDayRequest;
import com.locomotora.demo.routine.dto.RoutineRequest;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.repository.RoutineWriteRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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
    private static final Set<String> ALLOWED_SOURCES = Set.of("MANUAL", "AI", "TEMPLATE");
    static final int MAX_TITLE_LENGTH = 180;

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
        String objective = Objectives.requireKnown(request.objective());
        String source = normalizeSource(request.source());
        List<RoutineDayRequest> days = normalizeDays(request);
        UUID routineId = routineWriteRepository.createRoutine(userId, request, objective, source, difficulty, categoryId, categoriesAvailable);
        syncDays(routineId, days, userId);
        return routineReadService.getRoutine(routineId);
    }

    @Transactional
    public RoutineResponse updateRoutine(UUID routineId, RoutineRequest request) {
        UUID userId = CurrentUser.id();
        boolean categoriesAvailable = routineReadService.routineCategoriesAvailable();
        UUID categoryId = validateCategoryId(request.categoryId(), categoriesAvailable);
        String difficulty = normalizeDifficulty(request.difficulty(), "Routine difficulty");
        String objective = Objectives.requireKnown(request.objective());
        List<RoutineDayRequest> days = normalizeDays(request);

        int updated = routineWriteRepository.updateRoutine(routineId, userId, request, objective, difficulty, categoryId, categoriesAvailable);
        if (updated == 0) {
            if (routineWriteRepository.isActiveGlobalTemplate(routineId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Global templates cannot be edited");
            }
            throw new ApiException(HttpStatus.NOT_FOUND, "Routine not found");
        }
        syncDays(routineId, days, userId);
        return routineReadService.getRoutine(routineId);
    }

    /** Copia una rutina propia con sus dias y ejercicios. La copia es independiente: no comparte ids. */
    @Transactional
    public RoutineResponse duplicateRoutine(UUID routineId, String copySuffix) {
        RoutineResponse original = routineReadService.getRoutine(routineId);
        String suffix = copySuffix == null || copySuffix.isBlank() ? " (copia)" : " " + copySuffix.trim();
        String base = original.title();
        if (base.length() + suffix.length() > MAX_TITLE_LENGTH) {
            base = base.substring(0, MAX_TITLE_LENGTH - suffix.length()).trim();
        }
        List<RoutineDayRequest> days = original.days().stream()
                .map(day -> new RoutineDayRequest(
                        null,
                        day.dayIndex(),
                        day.title(),
                        day.focus(),
                        day.exercises().stream().map(RoutineService::copyExercise).toList()))
                .toList();
        String source = "AI".equals(original.source()) || "TEMPLATE".equals(original.source()) ? original.source() : "MANUAL";
        return createRoutine(new RoutineRequest(
                base + suffix,
                original.categoryId() == null ? null : UUID.fromString(original.categoryId()),
                original.description(),
                original.objective(),
                original.difficulty(),
                original.estimatedDurationMinutes(),
                original.daysPerWeek(),
                days,
                null,
                source));
    }

    static ExerciseRequest copyExercise(com.locomotora.demo.routine.dto.ExerciseResponse exercise) {
        boolean hasRange = exercise.repsMin() != null || exercise.repsMax() != null;
        return new ExerciseRequest(
                null,
                exercise.exerciseId(),
                exercise.name(),
                exercise.sets(),
                hasRange || exercise.reps() < 1 ? null : exercise.reps(),
                exercise.repsMin(),
                exercise.repsMax(),
                exercise.restSeconds(),
                exercise.primaryMuscleGroup(),
                null,
                exercise.notes());
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "MANUAL";
        }
        String normalized = source.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_SOURCES.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Routine source must be MANUAL, AI or TEMPLATE");
        }
        return normalized;
    }

    public void deleteRoutine(UUID routineId) {
        int updated = routineWriteRepository.softDelete(routineId, CurrentUser.id());
        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Routine not found");
        }
    }

    private List<RoutineDayRequest> normalizeDays(RoutineRequest request) {
        if (request.days() != null && !request.days().isEmpty()) {
            return request.days().stream()
                    .sorted(Comparator.comparing(day -> day.dayIndex() == null ? Integer.MAX_VALUE : day.dayIndex()))
                    .map(this::validateDay)
                    .toList();
        }

        if (request.exercises() != null && !request.exercises().isEmpty()) {
            List<ExerciseRequest> exercises = request.exercises().stream().map(this::validateExercise).toList();
            return List.of(new RoutineDayRequest(null, 1, "Dia 1", "General", exercises));
        }

        throw new ApiException(HttpStatus.BAD_REQUEST, "Add at least one training day or exercise");
    }

    private RoutineDayRequest validateDay(RoutineDayRequest day) {
        if (day == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Routine day is required");
        }
        int dayIndex = day.dayIndex() == null ? 1 : day.dayIndex();
        if (dayIndex < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Day index must be greater than 0");
        }
        List<ExerciseRequest> exercises = day.exercises() == null
                ? List.of()
                : day.exercises().stream().map(this::validateExercise).toList();
        if (exercises.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Each routine day needs at least one exercise");
        }
        String title = normalizeText(day.title());
        return new RoutineDayRequest(
                day.id(),
                dayIndex,
                title == null ? "Dia " + dayIndex : title,
                normalizeText(day.focus()),
                exercises
        );
    }

    private ExerciseRequest validateExercise(ExerciseRequest exercise) {
        if (exercise == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exercise is required");
        }
        if (exercise.name() == null || exercise.name().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exercise name is required");
        }
        int sets = exercise.sets() == null ? 0 : exercise.sets();
        int restSeconds = exercise.restSeconds() == null ? 0 : exercise.restSeconds();
        Integer reps = exercise.reps();
        Integer repsMin = exercise.repsMin();
        Integer repsMax = exercise.repsMax();
        if (sets < 1 || restSeconds < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exercise sets and rest values are invalid");
        }
        if (reps == null && repsMin == null && repsMax == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exercise reps are required");
        }
        if (repsMin != null && repsMax != null && repsMin > repsMax) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exercise reps range is invalid");
        }
        String exerciseDifficulty = normalizeDifficulty(exercise.difficulty(), "Exercise difficulty");
        return new ExerciseRequest(
                exercise.id(),
                exercise.exerciseId(),
                exercise.name().trim(),
                sets,
                reps,
                repsMin,
                repsMax,
                restSeconds,
                normalizeText(exercise.primaryMuscleGroup()),
                exerciseDifficulty,
                normalizeText(exercise.notes())
        );
    }

    /**
     * Deja los dias y ejercicios de la rutina iguales al request, conservando los ids
     * que ya existen: asi las sesiones registradas siguen apuntando a su dia y a su
     * ejercicio despues de editar. Un id que no pertenece a la rutina (o repetido)
     * se trata como nuevo. Lo que no aparece en el request se borra.
     */
    private void syncDays(UUID routineId, List<RoutineDayRequest> days, UUID userId) {
        Set<UUID> existingDayIds = routineWriteRepository.findDayIds(routineId);
        Set<UUID> existingExerciseIds = routineWriteRepository.findRoutineExerciseIds(routineId);
        Set<UUID> keptDayIds = new HashSet<>();
        Set<UUID> keptExerciseIds = new HashSet<>();

        List<Integer> usedDayIndexes = new ArrayList<>();
        int fallbackDayIndex = 1;
        for (RoutineDayRequest day : days) {
            int dayIndex = day.dayIndex() == null ? fallbackDayIndex : day.dayIndex();
            while (usedDayIndexes.contains(dayIndex)) {
                dayIndex++;
            }
            usedDayIndexes.add(dayIndex);
            fallbackDayIndex = dayIndex + 1;

            UUID requestedDayId = parseUuid(day.id());
            UUID routineDayId;
            if (requestedDayId != null && existingDayIds.contains(requestedDayId) && keptDayIds.add(requestedDayId)) {
                routineWriteRepository.updateDay(requestedDayId, dayIndex, day.title(), day.focus());
                routineDayId = requestedDayId;
            } else {
                routineDayId = routineWriteRepository.createDay(routineId, dayIndex, day.title(), day.focus());
            }

            int position = 1;
            for (ExerciseRequest exercise : day.exercises()) {
                UUID exerciseId = routineWriteRepository.findOrCreateExercise(exercise, userId);
                UUID requestedRowId = parseUuid(exercise.id());
                if (requestedRowId != null
                        && existingExerciseIds.contains(requestedRowId)
                        && keptExerciseIds.add(requestedRowId)) {
                    routineWriteRepository.updateRoutineExercise(requestedRowId, routineDayId, exerciseId, position++, exercise);
                } else {
                    routineWriteRepository.insertRoutineExercise(routineDayId, exerciseId, position++, exercise);
                }
            }
        }

        // Primero los ejercicios: uno movido desde un dia que se borra ya cambio de dia
        // arriba, asi que el borrado en cascada del dia no lo alcanza.
        List<UUID> removedExerciseIds = existingExerciseIds.stream().filter(id -> !keptExerciseIds.contains(id)).toList();
        if (!removedExerciseIds.isEmpty()) {
            routineWriteRepository.deleteRoutineExercises(removedExerciseIds);
        }
        List<UUID> removedDayIds = existingDayIds.stream().filter(id -> !keptDayIds.contains(id)).toList();
        if (!removedDayIds.isEmpty()) {
            routineWriteRepository.deleteDays(removedDayIds);
        }
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
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
    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
