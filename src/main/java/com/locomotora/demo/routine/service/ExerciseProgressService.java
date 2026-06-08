package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.ExerciseProgressLogRequest;
import com.locomotora.demo.routine.dto.ExerciseProgressLogResponse;
import com.locomotora.demo.routine.repository.WorkoutSessionRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseProgressService {
    private final WorkoutSessionRepository workoutSessionRepository;

    public ExerciseProgressService(WorkoutSessionRepository workoutSessionRepository) {
        this.workoutSessionRepository = workoutSessionRepository;
    }

    @Transactional
    public ExerciseProgressLogResponse logExerciseProgress(UUID exerciseId, ExerciseProgressLogRequest request) {
        UUID userId = CurrentUser.id();
        if (!workoutSessionRepository.activeExerciseExists(exerciseId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Exercise not found");
        }

        Instant completedAt = request.completedAt() == null ? Instant.now() : Instant.parse(request.completedAt());
        UUID sessionId = workoutSessionRepository.createCompletedSession(
                userId,
                null,
                null,
                Timestamp.from(completedAt)
        );

        workoutSessionRepository.insertStandaloneExerciseLog(
                sessionId,
                exerciseId,
                request.reps(),
                request.weightKg()
        );

        return new ExerciseProgressLogResponse(
                sessionId.toString(),
                exerciseId.toString(),
                request.weightKg(),
                request.reps(),
                completedAt.toString()
        );
    }
}