package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.ExerciseCatalogItemResponse;
import com.locomotora.demo.routine.dto.OwnExerciseRequest;
import com.locomotora.demo.routine.repository.RoutineRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseService {
    static final Set<String> EQUIPMENT = Set.of(
            "BARBELL", "DUMBBELL", "MACHINE", "CABLE", "BODYWEIGHT", "FREE_WEIGHT", "SMITH_MACHINE", "BAND", "KETTLEBELL", "OTHER");

    private final RoutineRepository routineRepository;

    public ExerciseService(RoutineRepository routineRepository) {
        this.routineRepository = routineRepository;
    }

    public record Result(ExerciseCatalogItemResponse exercise, boolean created) {
    }

    /**
     * Crea un ejercicio propio. Si el usuario ya ve uno con ese nombre (global o propio),
     * devuelve ese en vez de duplicarlo.
     */
    @Transactional
    public Result createOwn(OwnExerciseRequest request) {
        UUID userId = CurrentUser.id();
        String name = request.name().trim().replaceAll("\\s+", " ");
        if (name.length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exercise name is too short");
        }
        Optional<ExerciseCatalogItemResponse> existing = routineRepository.findVisibleExerciseByName(name, userId);
        if (existing.isPresent()) {
            return new Result(existing.get(), false);
        }
        String equipment = request.equipment() == null || request.equipment().isBlank()
                ? null
                : request.equipment().trim().toUpperCase(Locale.ROOT);
        if (equipment != null && !EQUIPMENT.contains(equipment)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown equipment");
        }
        String muscle = request.primaryMuscleGroup() == null || request.primaryMuscleGroup().isBlank()
                ? null
                : request.primaryMuscleGroup().trim();
        return new Result(routineRepository.insertOwnExercise(userId, name, muscle, equipment), true);
    }
}
