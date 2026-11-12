package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.HistoryDtos.ExerciseHistory;
import com.locomotora.demo.routine.dto.HistoryDtos.Plan;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionDetail;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionExercise;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionListItem;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionPage;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionSet;
import com.locomotora.demo.routine.dto.HistoryDtos.SessionUpdateRequest;
import com.locomotora.demo.routine.dto.HistoryDtos.SetUpdate;
import com.locomotora.demo.routine.dto.HistoryDtos.TrainedExercise;
import com.locomotora.demo.routine.dto.HistoryDtos.WeekStats;
import com.locomotora.demo.routine.repository.WorkoutHistoryRepository;
import com.locomotora.demo.routine.repository.WorkoutHistoryRepository.LogRow;
import com.locomotora.demo.routine.repository.WorkoutHistoryRepository.SessionRow;
import com.locomotora.demo.routine.repository.WorkoutHistoryRepository.WeekRow;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutHistoryService {
    static final int DEFAULT_PAGE = 20;
    static final int MAX_PAGE = 50;
    static final int DEFAULT_POINTS = 30;
    static final int MAX_POINTS = 100;
    static final int DEFAULT_WEEKS = 8;
    static final int MAX_WEEKS = 52;
    private static final BigDecimal RECORD_MARGIN = new BigDecimal("0.05");

    private final WorkoutHistoryRepository repository;

    public WorkoutHistoryService(WorkoutHistoryRepository repository) {
        this.repository = repository;
    }

    // ------------------------------------------------------------------ historial

    public SessionPage list(String cursor, Integer limit) {
        UUID userId = CurrentUser.id();
        int size = clamp(limit, DEFAULT_PAGE, MAX_PAGE);
        Cursor after = Cursor.decode(cursor);

        List<SessionRow> rows = repository.findSessions(
                userId, after == null ? null : after.completedAt(), after == null ? null : after.id(), size + 1);
        boolean hasMore = rows.size() > size;
        List<SessionRow> page = hasMore ? rows.subList(0, size) : rows;

        Map<UUID, Integer> records = repository.countRecords(
                userId, page.stream().map(row -> UUID.fromString(row.item().id())).toList());
        List<SessionListItem> items = page.stream()
                .map(row -> withRecords(row.item(), records.getOrDefault(UUID.fromString(row.item().id()), 0)))
                .toList();

        String next = null;
        if (hasMore) {
            SessionRow last = page.get(page.size() - 1);
            next = new Cursor(last.completedAt(), UUID.fromString(last.item().id())).encode();
        }
        return new SessionPage(items, next);
    }

    public SessionDetail detail(UUID sessionId) {
        UUID userId = CurrentUser.id();
        SessionRow row = repository.findSession(sessionId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found"));
        return buildDetail(userId, row);
    }

    @Transactional
    public SessionDetail update(UUID sessionId, SessionUpdateRequest request) {
        UUID userId = CurrentUser.id();
        repository.findSession(sessionId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found"));

        List<SetUpdate> updates = request.sets() == null ? List.of() : request.sets();
        List<UUID> deleted = request.deletedSetIds() == null ? List.of() : request.deletedSetIds();
        Set<UUID> deletedSet = new HashSet<>(deleted);
        for (SetUpdate update : updates) {
            if (deletedSet.contains(update.id())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A set cannot be updated and deleted at once");
            }
            if (!repository.updateSet(sessionId, update.id(), update.reps(), update.weightKg())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Set does not belong to the session");
            }
        }
        for (UUID setId : deletedSet) {
            if (!repository.deleteSet(sessionId, setId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Set does not belong to the session");
            }
        }
        if (repository.countCompletedSets(sessionId) == 0) {
            // Se revierte la transaccion: para dejarla sin series hay que borrar la sesion.
            throw new ApiException(HttpStatus.BAD_REQUEST, "A session needs at least one set; delete the session instead");
        }
        String notes = request.notes() == null || request.notes().isBlank() ? null : request.notes().trim();
        repository.updateSessionInfo(sessionId, request.perceivedExertion(), notes);

        return buildDetail(userId, repository.findSession(sessionId, userId).orElseThrow());
    }

    @Transactional
    public void delete(UUID sessionId) {
        UUID userId = CurrentUser.id();
        if (!repository.deleteSession(sessionId, userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Session not found");
        }
    }

    // ------------------------------------------------------------------ progreso

    public List<TrainedExercise> trainedExercises() {
        return repository.findTrainedExercises(CurrentUser.id());
    }

    public ExerciseHistory exerciseHistory(UUID exerciseId, Integer limit) {
        UUID userId = CurrentUser.id();
        var exercise = repository.findExercise(exerciseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exercise not found"));
        return new ExerciseHistory(
                exercise.id().toString(),
                exercise.name(),
                exercise.primaryMuscleGroup(),
                repository.findExercisePoints(userId, exerciseId, clamp(limit, DEFAULT_POINTS, MAX_POINTS)),
                repository.findExerciseRecords(userId, exerciseId)
        );
    }

    /** Las ultimas {@code weeks} semanas, de la mas vieja a la actual, con ceros en las semanas sin entrenar. */
    public List<WeekStats> weekly(String timeZone, Integer weeks, Instant now) {
        UUID userId = CurrentUser.id();
        ZoneId zone = TrainingService.parseZone(timeZone);
        int count = clamp(weeks, DEFAULT_WEEKS, MAX_WEEKS);
        LocalDate currentWeek = TrainingCalendar.weekStart(now.atZone(zone).toLocalDate());
        LocalDate firstWeek = currentWeek.minusWeeks(count - 1L);

        Map<LocalDate, WeekRow> byWeek = repository
                .findWeeklyTotals(userId, zone.getId(), firstWeek.atStartOfDay(zone).toInstant())
                .stream()
                .collect(Collectors.toMap(WeekRow::weekStart, Function.identity()));

        List<WeekStats> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            LocalDate week = firstWeek.plusWeeks(i);
            WeekRow row = byWeek.get(week);
            result.add(row == null
                    ? new WeekStats(week.toString(), 0, 0, 0, BigDecimal.ZERO)
                    : new WeekStats(week.toString(), row.sessions(), row.trainedDays(), row.completedSets(), row.volume()));
        }
        return result;
    }

    // ------------------------------------------------------------------ armado

    private SessionDetail buildDetail(UUID userId, SessionRow row) {
        UUID sessionId = UUID.fromString(row.item().id());
        List<LogRow> logs = repository.findLogs(sessionId);
        Map<UUID, BigDecimal> priorBest = repository.findPriorBest(userId, sessionId);

        // Agrupa por posicion y ejercicio, respetando el orden de la sesion.
        Map<String, List<LogRow>> groups = new LinkedHashMap<>();
        for (LogRow log : logs) {
            groups.computeIfAbsent(log.position() + ":" + log.exerciseId(), key -> new ArrayList<>()).add(log);
        }

        List<SessionExercise> exercises = new ArrayList<>();
        int recordCount = 0;
        int index = 0;
        for (List<LogRow> group : groups.values()) {
            index += 1;
            LogRow first = group.get(0);
            UUID recordSetId = recordSet(group, priorBest.get(first.exerciseId()));
            if (recordSetId != null) {
                recordCount += 1;
            }
            List<SessionSet> sets = group.stream()
                    .map(log -> new SessionSet(
                            log.id().toString(), log.setNumber(), log.reps(), log.weightKg(), log.completed(),
                            log.id().equals(recordSetId)))
                    .toList();
            boolean hasPlan = first.targetRepsMin() != null || first.targetRepsMax() != null
                    || first.targetWeightKg() != null || first.restSeconds() != null;
            exercises.add(new SessionExercise(
                    first.position() > 0 ? first.position() : index,
                    first.exerciseId().toString(),
                    first.exerciseName(),
                    first.primaryMuscleGroup(),
                    first.routineExerciseId() == null ? null : first.routineExerciseId().toString(),
                    hasPlan ? new Plan(first.targetRepsMin(), first.targetRepsMax(), first.targetWeightKg(), first.restSeconds()) : null,
                    group.stream().map(LogRow::notes).filter(note -> note != null && !note.isBlank()).findFirst().orElse(null),
                    sets
            ));
        }
        return new SessionDetail(withRecords(row.item(), recordCount), row.notes(), exercises);
    }

    /**
     * La serie de mayor fuerza estimada del ejercicio, si supera la mejor de las sesiones anteriores.
     * Sin historial previo no hay record.
     */
    static UUID recordSet(List<LogRow> group, BigDecimal priorBest) {
        if (priorBest == null) {
            return null;
        }
        LogRow best = null;
        BigDecimal bestValue = null;
        for (LogRow log : group) {
            BigDecimal value = estimatedOneRepMax(log);
            if (value != null && (bestValue == null || value.compareTo(bestValue) > 0)) {
                best = log;
                bestValue = value;
            }
        }
        return bestValue != null && bestValue.compareTo(priorBest.add(RECORD_MARGIN)) > 0 ? best.id() : null;
    }

    static BigDecimal estimatedOneRepMax(LogRow log) {
        if (!log.completed() || log.reps() == null || log.reps() <= 0
                || log.weightKg() == null || log.weightKg().signum() <= 0) {
            return null;
        }
        if (log.reps() == 1) {
            return log.weightKg();
        }
        return log.weightKg().multiply(BigDecimal.ONE.add(
                BigDecimal.valueOf(log.reps()).divide(BigDecimal.valueOf(30), 10, java.math.RoundingMode.HALF_UP)));
    }

    private static SessionListItem withRecords(SessionListItem item, int records) {
        return new SessionListItem(
                item.id(), item.kind(), item.routineId(), item.routineTitle(), item.dayTitle(),
                item.startedAt(), item.completedAt(), item.durationSeconds(), item.completedSets(),
                item.exerciseCount(), item.totalVolumeKg(), item.perceivedExertion(), records);
    }

    private static int clamp(Integer value, int fallback, int max) {
        if (value == null) {
            return fallback;
        }
        return Math.max(1, Math.min(max, value));
    }

    /** Cursor opaco: fecha de fin e id de la ultima sesion de la pagina. */
    record Cursor(Instant completedAt, UUID id) {
        String encode() {
            String raw = completedAt.toString() + "|" + id;
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }

        static Cursor decode(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
                String[] parts = raw.split("\\|", 2);
                return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
            } catch (RuntimeException exception) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid cursor");
            }
        }
    }
}
