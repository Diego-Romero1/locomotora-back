package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.LastPerformanceResponse;
import com.locomotora.demo.routine.dto.RoutineDayResponse;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.dto.TodayResponse;
import com.locomotora.demo.routine.dto.WorkoutSessionResponse;
import com.locomotora.demo.routine.repository.TrainingStatsRepository;
import com.locomotora.demo.routine.repository.TrainingStatsRepository.RoutineCandidate;
import com.locomotora.demo.routine.repository.WorkoutSessionWriteRepository;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TrainingService {
    static final int MAX_EXERCISE_IDS = 100;
    /** Cuanto historial se mira para la racha. */
    static final int STREAK_LOOKBACK_WEEKS = 104;

    private final TrainingStatsRepository statsRepository;
    private final WorkoutSessionWriteRepository sessionRepository;
    private final RoutineReadService routineReadService;

    public TrainingService(
            TrainingStatsRepository statsRepository,
            WorkoutSessionWriteRepository sessionRepository,
            RoutineReadService routineReadService
    ) {
        this.statsRepository = statsRepository;
        this.sessionRepository = sessionRepository;
        this.routineReadService = routineReadService;
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

        Optional<RoutineResponse> routine = pickActiveRoutine(userId).map(routineReadService::getRoutine);
        String nextDayId = routine.flatMap(r -> nextDay(userId, r)).map(UUID::toString).orElse(null);
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

        return new TodayResponse(
                routine.orElse(null),
                nextDayId,
                new TodayResponse.Week(weekStart.toString(), thisWeek, target),
                TrainingCalendar.weekStreak(trainedDates, today, target),
                lastSession
        );
    }

    /**
     * La rutina "actual" es la mas reciente entre la ultima entrenada y la ultima creada o editada:
     * si se arma una rutina nueva, pasa a ser la de Hoy; si no, sigue la que se viene entrenando.
     */
    private Optional<UUID> pickActiveRoutine(UUID userId) {
        return Stream.of(
                        statsRepository.findLastTrainedActiveRoutine(userId),
                        statsRepository.findLatestUpdatedActiveRoutine(userId))
                .flatMap(Optional::stream)
                .max(Comparator.comparing(RoutineCandidate::at))
                .map(RoutineCandidate::routineId);
    }

    private Optional<UUID> nextDay(UUID userId, RoutineResponse routine) {
        List<UUID> dayIds = routine.days().stream()
                .sorted(Comparator.comparingInt(RoutineDayResponse::dayIndex))
                .map(day -> UUID.fromString(day.id()))
                .toList();
        return TrainingCalendar.nextDayId(dayIds, statsRepository.findLastTrainedDayId(userId, UUID.fromString(routine.id())));
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
