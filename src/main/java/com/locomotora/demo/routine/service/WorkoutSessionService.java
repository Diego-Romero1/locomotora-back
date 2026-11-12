package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.WorkoutSessionExerciseRequest;
import com.locomotora.demo.routine.dto.WorkoutSessionRequest;
import com.locomotora.demo.routine.dto.WorkoutSessionResponse;
import com.locomotora.demo.routine.dto.WorkoutSetRequest;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository.NewSession;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository.PlannedExercise;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository.RoutineDayRef;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository.SetLog;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda una sesion de entrenamiento completa en un solo envio, con una fila por serie.
 *
 * <p>Es idempotente por {@code (usuario, clientSessionId)}: el telefono puede reintentar
 * el envio cuantas veces haga falta y siempre recibe la misma sesion.
 */
@Service
public class WorkoutSessionService {
    public static final String KIND_ROUTINE = "ROUTINE";
    public static final String KIND_FREE = "FREE";

    /** Margen para relojes de telefono adelantados. */
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(10);
    private static final Duration MAX_SESSION_LENGTH = Duration.ofHours(12);

    private final WorkoutSessionWriteRepository repository;

    public WorkoutSessionService(WorkoutSessionWriteRepository repository) {
        this.repository = repository;
    }

    public record Result(WorkoutSessionResponse session, boolean created) {
    }

    @Transactional
    public Result saveSession(WorkoutSessionRequest request) {
        UUID userId = CurrentUser.id();

        Optional<UUID> existing = repository.findSessionIdByClientId(userId, request.clientSessionId());
        if (existing.isPresent()) {
            return new Result(summary(existing.get(), userId), false);
        }

        Instant startedAt = parseInstant(request.startedAt(), "startedAt");
        Instant completedAt = parseInstant(request.completedAt(), "completedAt");
        validateTimes(startedAt, completedAt);

        String routineTitle = null;
        RoutineDayRef day = null;
        Map<UUID, PlannedExercise> planned = Map.of();
        if (request.routineId() != null) {
            routineTitle = repository.findOwnedRoutineTitle(request.routineId(), userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Routine not found"));
            day = resolveDay(request.routineId(), request.routineDayId());
            planned = repository.findPlannedExercises(request.routineId());
        } else if (request.routineDayId() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "routineDayId requires routineId");
        }

        validateExercisesExist(request.exercises());
        List<SetLog> sets = buildSetLogs(request.exercises(), planned);
        if (sets.stream().noneMatch(SetLog::completed)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The session has no completed sets");
        }

        NewSession session = new NewSession(
                userId,
                request.clientSessionId(),
                request.routineId() == null ? KIND_FREE : KIND_ROUTINE,
                request.routineId(),
                day == null ? null : day.id(),
                routineTitle,
                day == null ? null : day.title(),
                Timestamp.from(startedAt),
                Timestamp.from(completedAt),
                request.perceivedExertion(),
                blankToNull(request.notes())
        );

        Optional<UUID> inserted = repository.insertSession(session);
        if (inserted.isEmpty()) {
            // Otra peticion con el mismo clientSessionId gano la carrera.
            UUID winner = repository.findSessionIdByClientId(userId, request.clientSessionId())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Session could not be saved"));
            return new Result(summary(winner, userId), false);
        }

        repository.insertSetLogs(inserted.get(), sets);
        return new Result(summary(inserted.get(), userId), true);
    }

    private RoutineDayRef resolveDay(UUID routineId, UUID routineDayId) {
        if (routineDayId == null) {
            return repository.findFirstRoutineDay(routineId).orElse(null);
        }
        return repository.findRoutineDay(routineDayId, routineId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Routine day does not belong to the routine"));
    }

    private void validateExercisesExist(List<WorkoutSessionExerciseRequest> exercises) {
        Set<UUID> requested = exercises.stream()
                .map(WorkoutSessionExerciseRequest::exerciseId)
                .collect(Collectors.toSet());
        Set<UUID> found = repository.findExistingExerciseIds(requested);
        if (!found.containsAll(requested)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown exercise in session");
        }
    }

    private List<SetLog> buildSetLogs(List<WorkoutSessionExerciseRequest> exercises, Map<UUID, PlannedExercise> planned) {
        List<SetLog> sets = new ArrayList<>();
        for (int index = 0; index < exercises.size(); index++) {
            WorkoutSessionExerciseRequest exercise = exercises.get(index);
            int position = exercise.position() == null ? index + 1 : exercise.position();

            // Si el ejercicio ya no esta en la rutina (se edito mientras la sesion esperaba
            // en la cola) o es de otro ejercicio, se guarda igual pero sin vinculo al plan.
            PlannedExercise plan = exercise.routineExerciseId() == null ? null : planned.get(exercise.routineExerciseId());
            if (plan != null && !plan.exerciseId().equals(exercise.exerciseId())) {
                plan = null;
            }

            Set<Integer> seenSetNumbers = new HashSet<>();
            String notes = blankToNull(exercise.notes());
            for (WorkoutSetRequest set : exercise.sets()) {
                if (!seenSetNumbers.add(set.setNumber())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Duplicate set number " + set.setNumber());
                }
                sets.add(new SetLog(
                        plan == null ? null : plan.id(),
                        exercise.exerciseId(),
                        position,
                        set.setNumber(),
                        set.reps(),
                        set.weightKg(),
                        set.durationSeconds(),
                        set.isCompleted(),
                        // La nota del ejercicio va en su primera serie.
                        seenSetNumbers.size() == 1 ? notes : null,
                        plan == null ? null : plan.targetRepsMin(),
                        plan == null ? null : plan.targetRepsMax(),
                        plan == null ? null : plan.targetWeightKg(),
                        plan == null ? null : plan.restSeconds()
                ));
            }
        }
        return sets;
    }

    private void validateTimes(Instant startedAt, Instant completedAt) {
        if (completedAt.isBefore(startedAt)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "completedAt must not be before startedAt");
        }
        if (Duration.between(startedAt, completedAt).compareTo(MAX_SESSION_LENGTH) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The session is longer than 12 hours");
        }
        if (completedAt.isAfter(Instant.now().plus(FUTURE_TOLERANCE))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "completedAt is in the future");
        }
    }

    private WorkoutSessionResponse summary(UUID sessionId, UUID userId) {
        return repository.findSessionSummary(sessionId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found"));
    }

    private static Instant parseInstant(String value, String field) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, field + " must be an ISO-8601 instant");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
