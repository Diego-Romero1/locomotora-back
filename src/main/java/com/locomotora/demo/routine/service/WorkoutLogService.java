package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.WorkoutLogRequest;
import com.locomotora.demo.routine.dto.WorkoutLogResponse;
import com.locomotora.demo.routine.repository.WorkoutSessionRepository;
import com.locomotora.demo.routine.repository.WorkoutSessionRepository.ExerciseLookup;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutLogService {
    private final WorkoutSessionRepository workoutSessionRepository;

    public WorkoutLogService(WorkoutSessionRepository workoutSessionRepository) {
        this.workoutSessionRepository = workoutSessionRepository;
    }

    @Transactional
    public WorkoutLogResponse logWorkout(WorkoutLogRequest request) {
        UUID userId = CurrentUser.id();
        UUID routineId = UUID.fromString(request.routineId());
        if (!workoutSessionRepository.activeRoutineExists(routineId, userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Routine not found");
        }

        UUID dayId = workoutSessionRepository.findFirstRoutineDayId(routineId);
        Instant completedAt = request.completedAt() == null ? Instant.now() : Instant.parse(request.completedAt());
        UUID sessionId = workoutSessionRepository.createCompletedSession(
                userId,
                routineId,
                dayId,
                Timestamp.from(completedAt)
        );

        for (String completedId : request.completedExerciseIds()) {
            UUID routineExerciseId = UUID.fromString(completedId);
            ExerciseLookup exercise = workoutSessionRepository.findExerciseForRoutineLog(routineExerciseId, routineId)
                    .orElse(null);
            if (exercise != null) {
                workoutSessionRepository.insertExerciseLog(sessionId, routineExerciseId, exercise);
            }
        }

        return new WorkoutLogResponse(sessionId.toString(), request.completedExerciseIds().size(), completedAt.toString());
    }
}
