package com.locomotora.demo.routine.service;

import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.routine.dto.RoutineDayResponse;
import com.locomotora.demo.routine.dto.RoutineResponse;
import com.locomotora.demo.routine.repository.RoutineWriteRepository;
import com.locomotora.demo.routine.repository.TrainingStatsRepository;
import com.locomotora.demo.routine.repository.TrainingStatsRepository.RoutineCandidate;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Rutina actual (R2) y el ciclo de dias de cada rutina.
 *
 * <p>La actual es la que el usuario eligio ("Usar como rutina actual"). Si no eligio ninguna, o la
 * borro, se deduce como antes: la mas reciente entre la ultima entrenada y la ultima creada o editada.
 * Hoy, la lista de rutinas y el detalle usan la misma regla, asi que siempre coinciden.
 */
@Service
public class CurrentRoutineService {
    private final TrainingStatsRepository statsRepository;
    private final RoutineReadService routineReadService;
    private final RoutineWriteRepository routineWriteRepository;

    public CurrentRoutineService(
            TrainingStatsRepository statsRepository,
            RoutineReadService routineReadService,
            RoutineWriteRepository routineWriteRepository
    ) {
        this.statsRepository = statsRepository;
        this.routineReadService = routineReadService;
        this.routineWriteRepository = routineWriteRepository;
    }

    public Optional<UUID> resolve(UUID userId) {
        Optional<UUID> chosen = statsRepository.findChosenCurrentRoutine(userId);
        if (chosen.isPresent()) {
            return chosen;
        }
        return Stream.of(
                        statsRepository.findLastTrainedActiveRoutine(userId),
                        statsRepository.findLatestUpdatedActiveRoutine(userId))
                .flatMap(Optional::stream)
                .max(Comparator.comparing(RoutineCandidate::at))
                .map(RoutineCandidate::routineId);
    }

    public List<RoutineResponse> routines(UUID categoryId) {
        UUID userId = CurrentUser.id();
        Optional<UUID> current = resolve(userId);
        return routineReadService.routines(categoryId).stream()
                .map(routine -> decorate(routine, userId, current))
                .toList();
    }

    /** Rutina sin el ciclo (lo agrega {@link #decorate}). */
    RoutineResponse readRoutine(UUID routineId) {
        return routineReadService.getRoutine(routineId);
    }

    public RoutineResponse getRoutine(UUID routineId) {
        return decorate(routineReadService.getRoutine(routineId));
    }

    /** La rutina con su ciclo para el usuario actual (respuesta de crear, editar o duplicar). */
    public RoutineResponse decorate(RoutineResponse routine) {
        UUID userId = CurrentUser.id();
        return decorate(routine, userId, resolve(userId));
    }

    /** Elige la rutina actual. 404 si no es una rutina activa del usuario. */
    public RoutineResponse setCurrent(UUID routineId) {
        UUID userId = CurrentUser.id();
        RoutineResponse routine = routineReadService.getRoutine(routineId);
        routineWriteRepository.setCurrentRoutine(userId, routineId);
        return decorate(routine, userId, Optional.of(routineId));
    }

    /**
     * Agrega a la rutina cuando se entreno cada dia, cual toca (el que sigue al ultimo entrenado, en orden
     * de dia) y si es la actual. Las plantillas se devuelven tal cual.
     */
    public RoutineResponse decorate(RoutineResponse routine, UUID userId, Optional<UUID> currentId) {
        if (routine.isTemplate()) {
            return routine;
        }
        UUID routineId = UUID.fromString(routine.id());
        Map<UUID, Instant> lastByDay = statsRepository.findLastTrainedByDay(userId, routineId);
        List<RoutineDayResponse> days = routine.days().stream()
                .map(day -> day.withLastTrainedAt(iso(lastByDay.get(UUID.fromString(day.id())))))
                .toList();
        List<UUID> orderedDayIds = days.stream()
                .sorted(Comparator.comparingInt(RoutineDayResponse::dayIndex))
                .map(day -> UUID.fromString(day.id()))
                .toList();
        Optional<Map.Entry<UUID, Instant>> latest = lastByDay.entrySet().stream().max(Map.Entry.comparingByValue());
        String nextDayId = TrainingCalendar.nextDayId(orderedDayIds, latest.map(Map.Entry::getKey))
                .map(UUID::toString)
                .orElse(null);
        String lastTrainedAt = latest.map(entry -> entry.getValue().toString()).orElse(null);
        boolean isCurrent = currentId.map(routineId::equals).orElse(false);
        return routine.withTraining(isCurrent, lastTrainedAt, nextDayId, days);
    }

    private static String iso(Instant value) {
        return value == null ? null : value.toString();
    }
}
