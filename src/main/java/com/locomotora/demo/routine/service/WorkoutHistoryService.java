package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.progress.energy.WorkoutEnergyService;
import com.locomotora.demo.routine.dto.HistoryDtos.ExerciseHistory;
import com.locomotora.demo.routine.dto.HistoryDtos.MuscleEffort;
import com.locomotora.demo.routine.dto.HistoryDtos.PeriodBucket;
import com.locomotora.demo.routine.dto.HistoryDtos.PeriodSummary;
import com.locomotora.demo.routine.dto.HistoryDtos.PeriodTotals;
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
    private final WorkoutEnergyService energyService;

    public WorkoutHistoryService(WorkoutHistoryRepository repository, WorkoutEnergyService energyService) {
        this.repository = repository;
        this.energyService = energyService;
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
        energyService.recalculate(sessionId);

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
                    ? new WeekStats(week.toString(), 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0)
                    : new WeekStats(week.toString(), row.sessions(), row.trainedDays(), row.completedSets(), row.volume(),
                            row.energy(), row.activeSeconds()));
        }
        return result;
    }

    /** Esfuerzo por grupo muscular de las ultimas {@code weeks} semanas, contando la actual (por defecto, solo esta). */
    public List<MuscleEffort> muscleEffort(String timeZone, Integer weeks, Instant now) {
        ZoneId zone = TrainingService.parseZone(timeZone);
        int count = clamp(weeks, 1, MAX_WEEKS);
        LocalDate firstWeek = TrainingCalendar.weekStart(now.atZone(zone).toLocalDate()).minusWeeks(count - 1L);
        return repository.findMuscleEffort(CurrentUser.id(), firstWeek.atStartOfDay(zone).toInstant());
    }

    /** Periodo mas largo que se puede pedir (dos anos). */
    static final int MAX_PERIOD_DAYS = 731;
    static final int PERIOD_SESSIONS = 30;

    /**
     * Progreso entre dos fechas (incluidas) en la zona del telefono, comparado con {@code previousFrom..previousTo}
     * si se pide. El grafico va por dia hasta 14 dias, por semana hasta 120 y por mes en adelante.
     */
    public PeriodSummary period(
            String timeZone, String from, String to, String previousFrom, String previousTo) {
        UUID userId = CurrentUser.id();
        ZoneId zone = TrainingService.parseZone(timeZone);
        LocalDate start = parseDate(from, "from");
        LocalDate end = parseDate(to, "to");
        validateRange(start, end);
        LocalDate prevStart = previousFrom == null || previousFrom.isBlank() ? null : parseDate(previousFrom, "previousFrom");
        LocalDate prevEnd = previousTo == null || previousTo.isBlank() ? null : parseDate(previousTo, "previousTo");
        if ((prevStart == null) != (prevEnd == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "previousFrom and previousTo go together");
        }
        if (prevStart != null) {
            validateRange(prevStart, prevEnd);
        }

        Instant fromAt = start.atStartOfDay(zone).toInstant();
        Instant toAt = end.plusDays(1).atStartOfDay(zone).toInstant();
        PeriodTotals totals = repository.findPeriodTotals(userId, zone.getId(), fromAt, toAt);
        PeriodTotals previous = prevStart == null ? null : repository.findPeriodTotals(
                userId, zone.getId(), prevStart.atStartOfDay(zone).toInstant(), prevEnd.plusDays(1).atStartOfDay(zone).toInstant());

        String granularity = granularity(start, end);
        Map<LocalDate, PeriodTotals> byBucket = repository
                .findPeriodBuckets(userId, zone.getId(), fromAt, toAt, granularity.toLowerCase(java.util.Locale.ROOT))
                .stream()
                .collect(Collectors.toMap(WorkoutHistoryRepository.BucketRow::start, WorkoutHistoryRepository.BucketRow::totals));
        List<PeriodBucket> buckets = new ArrayList<>();
        for (LocalDate bucket : bucketStarts(start, end, granularity)) {
            PeriodTotals row = byBucket.get(bucket);
            buckets.add(row == null
                    ? new PeriodBucket(bucket.toString(), 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0)
                    : new PeriodBucket(bucket.toString(), row.sessions(), row.completedSets(), row.volumeKg(),
                            row.energyKcal(), row.activeSeconds()));
        }

        List<SessionRow> rows = repository.findSessionsBetween(userId, fromAt, toAt, PERIOD_SESSIONS);
        Map<UUID, Integer> records = repository.countRecords(
                userId, rows.stream().map(row -> UUID.fromString(row.item().id())).toList());
        List<SessionListItem> sessions = rows.stream()
                .map(row -> withRecords(row.item(), records.getOrDefault(UUID.fromString(row.item().id()), 0)))
                .toList();

        return new PeriodSummary(
                start.toString(), end.toString(), granularity, totals,
                prevStart == null ? null : prevStart.toString(), prevEnd == null ? null : prevEnd.toString(), previous,
                buckets,
                repository.findMuscleEffort(userId, fromAt, toAt),
                sessions,
                totals.sessions());
    }

    static String granularity(LocalDate start, LocalDate end) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        if (days <= 14) {
            return "DAY";
        }
        return days <= 120 ? "WEEK" : "MONTH";
    }

    /** Inicio de cada tramo que toca el periodo (la primera semana o mes puede empezar antes de {@code start}). */
    static List<LocalDate> bucketStarts(LocalDate start, LocalDate end, String granularity) {
        List<LocalDate> result = new ArrayList<>();
        LocalDate cursor = switch (granularity) {
            case "DAY" -> start;
            case "WEEK" -> TrainingCalendar.weekStart(start);
            default -> start.withDayOfMonth(1);
        };
        while (!cursor.isAfter(end)) {
            result.add(cursor);
            cursor = switch (granularity) {
                case "DAY" -> cursor.plusDays(1);
                case "WEEK" -> cursor.plusWeeks(1);
                default -> cursor.plusMonths(1);
            };
        }
        return result;
    }

    private static void validateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The period ends before it starts");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1 > MAX_PERIOD_DAYS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The period can be at most " + MAX_PERIOD_DAYS + " days");
        }
    }

    private static LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, field + " must be a date (YYYY-MM-DD)");
        }
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
                            log.id().equals(recordSetId), log.energyKcal()))
                    .toList();
            BigDecimal exerciseEnergy = group.stream().map(LogRow::energyKcal).filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal::add).orElse(null);
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
                    sets,
                    exerciseEnergy
            ));
        }
        return new SessionDetail(withRecords(row.item(), recordCount), row.notes(), exercises, row.energy());
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
                item.exerciseCount(), item.totalVolumeKg(), item.perceivedExertion(), records, item.plannedSets(),
                item.energyKcal());
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
