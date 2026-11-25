package com.locomotora.demo.progress.energy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.locomotora.demo.routine.dto.HistoryDtos.MuscleEffort;
import com.locomotora.demo.routine.repository.WorkoutHistoryRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** V25 sobre un Postgres real: lo que se lee para calcular, lo que se guarda y los totales que lo usan. */
@Testcontainers
class WorkoutEnergyIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private static JdbcTemplate jdbc;
    private static WorkoutEnergyService service;
    private static WorkoutHistoryRepository history;
    private static UUID squat;
    private static UUID legPress;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        service = new WorkoutEnergyService(new WorkoutEnergyRepository(jdbc), new DataSourceTransactionManager(dataSource));
        history = new WorkoutHistoryRepository(jdbc);
        squat = exercise("Sentadilla trasera");
        legPress = exercise("Prensa 45 grados");
    }

    @Test
    void halfFinishedLegDayMatchesTheSharedExample() {
        UUID userId = user("energy-legs@test.local");
        jdbc.update("UPDATE user_profiles SET sex = 'MALE', birth_date = '1996-03-01', height_cm = 178 WHERE user_id = ?", userId);
        weight(userId, "80", "2026-10-01T08:00:00Z");
        weight(userId, "75", "2026-11-01T08:00:00Z");
        // Despues de la sesion bajo de peso: no debe cambiar el calculo.
        weight(userId, "72", "2026-11-30T08:00:00Z");
        // Mejores marcas previas: 105 kg en sentadilla y 220 kg en prensa (1 rep = el peso).
        UUID previous = session(userId, "2026-11-10T18:00:00Z", "2026-11-10T18:30:00Z", null);
        log(previous, squat, 1, 1, 1, "105", null);
        log(previous, legPress, 2, 1, 1, "220", null);
        // Una marca de hace mas de 90 dias no cuenta.
        UUID old = session(userId, "2026-06-01T18:00:00Z", "2026-06-01T18:30:00Z", null);
        log(old, squat, 1, 1, 1, "140", null);

        UUID legs = session(userId, "2026-11-24T18:00:00Z", "2026-11-24T18:15:42Z", 15);
        for (int set = 1; set <= 3; set++) {
            log(legs, squat, 1, set, 8, "80", null);
        }
        for (int set = 1; set <= 2; set++) {
            log(legs, legPress, 2, set, 12, "150", null);
        }

        EnergyEstimator.SessionEnergy energy = service.recalculate(legs).orElseThrow();

        assertThat(energy.personalized()).isTrue();
        assertThat(energy.bodyWeightKg()).isEqualTo(75.0);
        assertThat(energy.totalKcal()).isCloseTo(109.40, within(0.1));
        Map<String, Object> stored = jdbc.queryForMap(
                "SELECT energy_kcal, body_weight_kg, body_weight_estimated, energy_model_version, planned_sets "
                        + "FROM workout_sessions WHERE id = ?", legs);
        assertThat((BigDecimal) stored.get("energy_kcal")).isCloseTo(new BigDecimal("109.40"), within(new BigDecimal("0.1")));
        assertThat((BigDecimal) stored.get("body_weight_kg")).isEqualByComparingTo("75");
        assertThat(stored.get("body_weight_estimated")).isEqualTo(false);
        assertThat(((Number) stored.get("energy_model_version")).intValue()).isEqualTo(EnergyEstimator.MODEL_VERSION);
        BigDecimal perSet = jdbc.queryForObject(
                "SELECT sum(energy_kcal) FROM workout_exercise_logs WHERE workout_session_id = ?", BigDecimal.class, legs);
        assertThat(perSet).isCloseTo(new BigDecimal("109.40"), within(new BigDecimal("0.1")));

        // Los totales semanales no se multiplican por la cantidad de series.
        var week = history.findWeeklyTotals(userId, "UTC", Instant.parse("2026-11-23T00:00:00Z"));
        assertThat(week).singleElement().satisfies(row -> {
            assertThat(row.sessions()).isEqualTo(1);
            assertThat(row.completedSets()).isEqualTo(5);
            assertThat(row.activeSeconds()).isEqualTo(942);
            assertThat(row.volume()).isEqualByComparingTo("5520");
        });

        List<MuscleEffort> effort = history.findMuscleEffort(userId, Instant.parse("2026-11-23T00:00:00Z"));
        assertThat(effort).singleElement().satisfies(item -> {
            assertThat(item.muscleGroup()).isEqualTo("Cuádriceps");
            assertThat(item.completedSets()).isEqualTo(5);
            assertThat(item.energyKcal()).isCloseTo(new BigDecimal("109.40"), within(new BigDecimal("0.1")));
        });
    }

    @Test
    void setTimesGiveRealRestsAndLongPausesAreCapped() {
        UUID userId = user("energy-times@test.local");
        weight(userId, "75", "2026-11-01T08:00:00Z");
        UUID id = session(userId, "2026-11-24T18:00:00Z", "2026-11-24T18:50:00Z", null);
        log(id, squat, 1, 1, 8, "80", "2026-11-24T18:04:00Z");
        log(id, squat, 1, 2, 8, "80", "2026-11-24T18:07:00Z");
        // 30 minutos de pausa antes de la tercera: cuenta como 5.
        log(id, squat, 1, 3, 8, "80", "2026-11-24T18:37:00Z");

        EnergyEstimator.SessionEnergy energy = service.recalculate(id).orElseThrow();

        List<Double> rests = energy.exercises().get(0).sets().stream().map(EnergyEstimator.SetEnergy::restSeconds).toList();
        assertThat(rests).containsExactly(240 - 32.0, 180 - 32.0, 300.0);
        assertThat(energy.personalized()).isFalse();
    }

    @Test
    void backfillCalculatesPendingSessionsAndAddingTheWeightRefreshesEstimatedOnes() {
        UUID userId = user("energy-backfill@test.local");
        UUID id = session(userId, "2026-11-20T18:00:00Z", "2026-11-20T18:20:00Z", null);
        log(id, squat, 1, 1, 10, "60", null);

        assertThat(service.backfill()).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT body_weight_estimated FROM workout_sessions WHERE id = ?", Boolean.class, id))
                .isTrue();
        assertThat(service.backfill()).isZero();

        weight(userId, "90", "2026-11-24T08:00:00Z");
        service.refreshIncomplete(userId);

        Map<String, Object> stored = jdbc.queryForMap(
                "SELECT body_weight_kg, body_weight_estimated FROM workout_sessions WHERE id = ?", id);
        // Sin peso anterior a la sesion se usa el primero medido despues.
        assertThat((BigDecimal) stored.get("body_weight_kg")).isEqualByComparingTo("90");
        assertThat(stored.get("body_weight_estimated")).isEqualTo(false);
    }

    @Test
    void currentProfileAndEnergyInfoForTheLiveEstimate() {
        UUID userId = user("energy-live@test.local");
        jdbc.update("UPDATE user_profiles SET sex = 'FEMALE', birth_date = '2000-06-15', height_cm = 165 WHERE user_id = ?", userId);
        weight(userId, "61.5", "2026-11-01T08:00:00Z");
        UUID recent = session(userId, "2026-11-20T18:00:00Z", "2026-11-20T18:30:00Z", null);
        log(recent, squat, 1, 1, 5, "60", null);
        UUID old = session(userId, "2026-05-01T18:00:00Z", "2026-05-01T18:30:00Z", null);
        log(old, squat, 1, 1, 1, "90", null);

        WorkoutEnergyService.EnergyProfile profile = service.currentProfile(userId, Instant.parse("2026-11-24T12:00:00Z"));
        assertThat(profile).isEqualTo(new WorkoutEnergyService.EnergyProfile("FEMALE", 26, 165.0, 61.5));

        var info = new com.locomotora.demo.routine.repository.TrainingStatsRepository(jdbc)
                .findEnergyInfo(userId, List.of(squat, legPress), Instant.parse("2026-08-26T12:00:00Z"));
        assertThat(info.get(squat).energyClass()).isEqualTo("A");
        // 60 × (1 + 5/30) = 70; la marca de 90 kg es de hace mas de 90 dias.
        assertThat(info.get(squat).bestE1rmKg()).isEqualByComparingTo("70");
        assertThat(info.get(legPress).energyClass()).isEqualTo("C");
        assertThat(info.get(legPress).bestE1rmKg()).isNull();
    }

    // ------------------------------------------------------------------ datos

    private static UUID exercise(String name) {
        return jdbc.queryForObject(
                "SELECT id FROM exercises WHERE name = ? AND created_by_user_id IS NULL LIMIT 1", UUID.class, name);
    }

    private static UUID user(String email) {
        UUID id = jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, name) VALUES (?, 'x', 'Prueba') RETURNING id", UUID.class, email);
        jdbc.update("INSERT INTO user_profiles (user_id) VALUES (?)", id);
        return id;
    }

    private static void weight(UUID userId, String kg, String at) {
        jdbc.update("INSERT INTO body_metrics (user_id, weight_kg, measured_at) VALUES (?, ?, ?)",
                userId, new BigDecimal(kg), Timestamp.from(Instant.parse(at)));
    }

    private static UUID session(UUID userId, String startedAt, String completedAt, Integer plannedSets) {
        return jdbc.queryForObject(
                """
                INSERT INTO workout_sessions (user_id, kind, started_at, completed_at, status, planned_sets)
                VALUES (?, 'FREE', ?, ?, 'COMPLETED', ?) RETURNING id
                """,
                UUID.class, userId, Timestamp.from(Instant.parse(startedAt)), Timestamp.from(Instant.parse(completedAt)),
                plannedSets);
    }

    private static void log(UUID sessionId, UUID exerciseId, int position, int set, int reps, String kg, String at) {
        jdbc.update(
                """
                INSERT INTO workout_exercise_logs
                    (workout_session_id, exercise_id, exercise_position, set_number, reps, weight_kg, completed, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, true, ?)
                """,
                sessionId, exerciseId, position, set, reps, new BigDecimal(kg),
                at == null ? null : Timestamp.from(Instant.parse(at)));
    }
}
