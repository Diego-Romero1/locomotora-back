package com.locomotora.demo.progress.energy;

import com.locomotora.demo.progress.energy.EnergyEstimator.ExerciseEnergy;
import com.locomotora.demo.progress.energy.EnergyEstimator.ExerciseInput;
import com.locomotora.demo.progress.energy.EnergyEstimator.Profile;
import com.locomotora.demo.progress.energy.EnergyEstimator.SessionEnergy;
import com.locomotora.demo.progress.energy.EnergyEstimator.SessionInput;
import com.locomotora.demo.progress.energy.EnergyEstimator.SetInput;
import com.locomotora.demo.progress.energy.WorkoutEnergyRepository.LogData;
import com.locomotora.demo.progress.energy.WorkoutEnergyRepository.LogEnergy;
import com.locomotora.demo.progress.energy.WorkoutEnergyRepository.ProfileData;
import com.locomotora.demo.progress.energy.WorkoutEnergyRepository.SessionData;
import com.locomotora.demo.progress.energy.WorkoutEnergyRepository.SessionEnergyRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Calcula y guarda las kcal de las sesiones con {@link EnergyEstimator}. */
@Service
public class WorkoutEnergyService {
    private static final Logger log = LoggerFactory.getLogger(WorkoutEnergyService.class);
    /** Ventana para el mejor 1RM estimado con el que se mide la intensidad relativa. */
    public static final Duration BEST_WINDOW = Duration.ofDays(90);
    private static final int BACKFILL_BATCH = 50;
    private static final int BACKFILL_MAX_BATCHES = 2000;

    private final WorkoutEnergyRepository repository;
    private final TransactionTemplate transactions;

    public WorkoutEnergyService(WorkoutEnergyRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /** Recalcula y guarda la sesion. Vacio si la sesion no existe o no esta completada. */
    @Transactional
    public Optional<SessionEnergy> recalculate(UUID sessionId) {
        Optional<SessionData> found = repository.findSession(sessionId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        SessionData session = found.get();
        Instant end = session.completedAt() == null ? Instant.now() : session.completedAt();
        List<LogData> logs = repository.findCompletedLogs(sessionId);
        Map<UUID, BigDecimal> priorBest = repository.findPriorBest(session.userId(), sessionId, end, end.minus(BEST_WINDOW));
        ProfileData profile = repository.findProfile(session.userId());
        BigDecimal bodyWeight = session.bodyWeightKg() != null
                ? session.bodyWeightKg()
                : repository.findBodyWeightAt(session.userId(), end).orElse(profile.weightKg());

        Grouped grouped = group(logs);
        SessionInput input = new SessionInput(
                profile(profile, bodyWeight, end),
                activeSeconds(session.startedAt(), session.completedAt()),
                session.completedAt(),
                grouped.inputs(priorBest));
        SessionEnergy energy = EnergyEstimator.estimate(input);

        List<LogEnergy> logEnergy = new ArrayList<>();
        for (int i = 0; i < grouped.logs().size(); i++) {
            ExerciseEnergy exercise = energy.exercises().get(i);
            List<LogData> groupLogs = grouped.logs().get(i);
            for (int j = 0; j < groupLogs.size(); j++) {
                logEnergy.add(new LogEnergy(groupLogs.get(j).id(), kcal(exercise.sets().get(j).kcal())));
            }
        }
        repository.saveEnergy(sessionId, logEnergy, new SessionEnergyRow(
                kcal(energy.totalKcal()),
                kcal(energy.workKcal()),
                BigDecimal.valueOf(energy.bodyWeightKg()).setScale(2, RoundingMode.HALF_UP),
                energy.bodyWeightEstimated(),
                energy.personalized(),
                EnergyEstimator.MODEL_VERSION));
        return Optional.of(energy);
    }

    /** Perfil para el calculo, como lo usaria hoy el servidor (null en lo que falte). */
    public record EnergyProfile(String sex, Integer ageYears, Double heightCm, Double bodyWeightKg) {
    }

    public EnergyProfile currentProfile(UUID userId, Instant now) {
        ProfileData data = repository.findProfile(userId);
        BigDecimal bodyWeight = repository.findBodyWeightAt(userId, now).orElse(data.weightKg());
        Profile profile = profile(data, bodyWeight, now);
        return new EnergyProfile(profile.sex(), profile.ageYears(), profile.heightCm(), profile.bodyWeightKg());
    }

    /**
     * Recalcula las sesiones del usuario que se calcularon sin su peso o sin su perfil completo. Se llama cuando
     * el usuario completa esos datos.
     */
    @Transactional
    public void refreshIncomplete(UUID userId) {
        repository.findIncompleteSessionIds(userId).forEach(this::recalculate);
    }

    /** Calcula las sesiones pendientes (nuevas para el modelo o de una version anterior), una transaccion por sesion. */
    public int backfill() {
        int done = 0;
        for (int batch = 0; batch < BACKFILL_MAX_BATCHES; batch++) {
            List<UUID> pending = repository.findPendingSessionIds(EnergyEstimator.MODEL_VERSION, BACKFILL_BATCH);
            if (pending.isEmpty()) {
                break;
            }
            for (UUID sessionId : pending) {
                transactions.executeWithoutResult(status -> recalculate(sessionId));
                done += 1;
            }
        }
        if (done > 0) {
            log.info("Workout energy: {} sessions calculated with model v{}", done, EnergyEstimator.MODEL_VERSION);
        }
        return done;
    }

    // ------------------------------------------------------------------ armado

    /** Series agrupadas por ejercicio (posicion + ejercicio), en orden. */
    record Grouped(List<List<LogData>> logs) {
        List<ExerciseInput> inputs(Map<UUID, BigDecimal> priorBest) {
            return logs.stream().map(group -> {
                LogData first = group.get(0);
                BigDecimal best = priorBest.get(first.exerciseId());
                return new ExerciseInput(
                        first.name(), first.nameEn(), first.primaryMuscleGroup(), first.mechanic(), first.equipment(),
                        first.exerciseType(), best == null ? null : best.doubleValue(),
                        group.stream().map(log -> new SetInput(
                                log.reps(),
                                log.weightKg() == null ? null : log.weightKg().doubleValue(),
                                log.durationSeconds(),
                                log.completedAt())).toList());
            }).toList();
        }
    }

    static Grouped group(List<LogData> logs) {
        Map<String, List<LogData>> groups = new LinkedHashMap<>();
        for (LogData log : logs) {
            groups.computeIfAbsent(log.position() + ":" + log.exerciseId(), key -> new ArrayList<>()).add(log);
        }
        return new Grouped(List.copyOf(groups.values()));
    }

    static Profile profile(ProfileData profile, BigDecimal bodyWeight, Instant at) {
        Integer age = null;
        if (profile.birthDate() != null) {
            LocalDate day = at.atZone(ZoneOffset.UTC).toLocalDate();
            int years = Period.between(profile.birthDate(), day).getYears();
            age = years > 0 ? years : null;
        }
        return new Profile(
                profile.sex(),
                age,
                profile.heightCm() == null ? null : profile.heightCm().doubleValue(),
                bodyWeight == null || bodyWeight.signum() <= 0 ? null : bodyWeight.doubleValue());
    }

    static long activeSeconds(Instant startedAt, Instant completedAt) {
        if (startedAt == null || completedAt == null) {
            return 0;
        }
        return Math.max(0, Duration.between(startedAt, completedAt).toSeconds());
    }

    private static BigDecimal kcal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
