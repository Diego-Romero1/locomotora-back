package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.LastPerformanceResponse;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.dto.TodayResponse;
import com.locomotora.demo.routine.dto.WorkoutSessionResponse;
import com.locomotora.demo.routine.repository.TrainingStatsRepository;
import com.locomotora.demo.routine.repository.WorkoutHistoryRepository;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TrainingService {
    static final int MAX_EXERCISE_IDS = 100;
    /** Cuanto historial se mira para la racha. */
    static final int STREAK_LOOKBACK_WEEKS = 104;
    /** Semanas que muestran las tarjetas de progreso de Hoy (racha y volumen). */
    static final int PROGRESS_WEEKS = 8;

    private final TrainingStatsRepository statsRepository;
    private final WorkoutSessionWriteRepository sessionRepository;
    private final CurrentRoutineService currentRoutineService;
    private final WorkoutHistoryRepository historyRepository;

    public TrainingService(
            TrainingStatsRepository statsRepository,
            WorkoutSessionWriteRepository sessionRepository,
            CurrentRoutineService currentRoutineService,
            WorkoutHistoryRepository historyRepository
    ) {
        this.statsRepository = statsRepository;
        this.sessionRepository = sessionRepository;
        this.currentRoutineService = currentRoutineService;
        this.historyRepository = historyRepository;
    }

    public List<LastPerformanceResponse> lastPerformance(List<UUID> exerciseIds) {
        UUID userId = CurrentUser.id();
        Set<UUID> ids = new LinkedHashSet<>(exerciseIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > MAX_EXERCISE_IDS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At most " + MAX_EXERCISE_IDS + " exercise ids");
        }
        var last = statsRepository.findLastSessions(userId, ids);
        var best = statsRepository.findBestSets(userId, ids);
        return ids.stream()
                .map(id -> new LastPerformanceResponse(id.toString(), last.get(id), best.get(id)))
                .toList();
    }

    public TodayResponse today(String timeZone, Instant now) {
        UUID userId = CurrentUser.id();
        ZoneId zone = parseZone(timeZone);
        LocalDate today = now.atZone(zone).toLocalDate();

        // La rutina actual (elegida o deducida) con su ciclo: el dia que toca viene calculado.
        Optional<UUID> currentId = currentRoutineService.resolve(userId);
        Optional<RoutineResponse> routine = currentId
                .map(id -> currentRoutineService.decorate(currentRoutineService.readRoutine(id), userId, currentId));
        String nextDayId = routine.map(RoutineResponse::nextDayId).orElse(null);
        int target = targetDays(userId, routine);

        LocalDate weekStart = TrainingCalendar.weekStart(today);
        Instant since = weekStart.minusWeeks(STREAK_LOOKBACK_WEEKS).atStartOfDay(zone).toInstant();
        List<LocalDate> trainedDates = statsRepository.findTrainedDates(userId, zone.getId(), since);
        List<String> thisWeek = trainedDates.stream()
                .filter(date -> !date.isBefore(weekStart) && date.isBefore(weekStart.plusWeeks(1)))
                .map(LocalDate::toString)
                .toList();

        WorkoutSessionResponse lastSession = statsRepository.findLastSessionId(userId)
                .flatMap(id -> sessionRepository.findSessionSummary(id, userId))
                .orElse(null);

        List<TodayResponse.WeekSession> weekSessions = statsRepository.findWeekSessions(
                userId, zone.getId(), weekStart.atStartOfDay(zone).toInstant(),
                weekStart.plusWeeks(1).atStartOfDay(zone).toInstant());

        return new TodayResponse(
                routine.orElse(null),
                nextDayId,
                new TodayResponse.Week(weekStart.toString(), thisWeek, target, weekSessions),
                TrainingCalendar.weekStreak(trainedDates, today, target),
                lastSession,
                progress(userId, zone, today, trainedDates, target)
        );
    }

    /** Tarjetas de progreso de Hoy: semanas cumplidas, entrenos del mes, volumen semanal y ultimo record. */
    private TodayResponse.Progress progress(UUID userId, ZoneId zone, LocalDate today, List<LocalDate> trainedDates, int target) {
        LocalDate monthStart = today.withDayOfMonth(1);
        TrainingStatsRepository.MonthCounts months = statsRepository.countMonthWorkouts(
                userId,
                monthStart.minusMonths(1).atStartOfDay(zone).toInstant(),
                monthStart.atStartOfDay(zone).toInstant());

        LocalDate firstWeek = TrainingCalendar.weekStart(today).minusWeeks(PROGRESS_WEEKS - 1L);
        Map<LocalDate, BigDecimal> volumeByWeek = new HashMap<>();
        historyRepository.findWeeklyTotals(userId, zone.getId(), firstWeek.atStartOfDay(zone).toInstant())
                .forEach(row -> volumeByWeek.put(row.weekStart(), row.volume()));
        List<BigDecimal> volume = new ArrayList<>(PROGRESS_WEEKS);
        for (int i = 0; i < PROGRESS_WEEKS; i++) {
            volume.add(volumeByWeek.getOrDefault(firstWeek.plusWeeks(i), BigDecimal.ZERO));
        }

        return new TodayResponse.Progress(
                TrainingCalendar.weeksMet(trainedDates, today, target, PROGRESS_WEEKS),
                months == null ? 0 : months.current(),
                months == null ? 0 : months.previous(),
                volume,
                statsRepository.findLastRecord(userId).orElse(null)
        );
    }

    private int targetDays(UUID userId, Optional<RoutineResponse> routine) {
        Integer fromRoutine = routine.map(RoutineResponse::daysPerWeek).orElse(null);
        if (fromRoutine == null || fromRoutine < 1) {
            fromRoutine = routine.map(r -> r.days().size()).filter(size -> size > 0).orElse(null);
        }
        int value = fromRoutine != null ? fromRoutine : statsRepository.findProfileTrainingDays(userId).orElse(3);
        return Math.min(7, Math.max(1, value));
    }

    static ZoneId parseZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timeZone.trim());
        } catch (DateTimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown time zone");
        }
    }
}
