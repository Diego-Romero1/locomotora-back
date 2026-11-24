package com.locomotora.demo.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.locomotora.demo.activity.ActivityRepository;
import com.locomotora.demo.admin.dto.AdminDtos.AuditEntry;
import com.locomotora.demo.admin.dto.AdminDtos.UserSummary;
import com.locomotora.demo.security.AccountStatusService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** V22 sobre un Postgres real: pings, lista de usuarios, cambio de estado con registro y estado para el filtro. */
@Testcontainers
class AdminRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private static JdbcTemplate jdbc;
    private static AdminRepository admin;
    private static ActivityRepository activity;

    private static final LocalDate DAY = LocalDate.parse("2026-11-24");
    private static final Instant T0 = Instant.parse("2026-11-24T15:00:00Z");

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        admin = new AdminRepository(jdbc);
        activity = new ActivityRepository(jdbc);
    }

    private static UUID user(String email, String name) {
        return jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, name) VALUES (?, 'x', ?) RETURNING id", UUID.class, email, name);
    }

    @Test
    void pingsKeepTheLastSeenTimeAndAddUpPerDay() {
        UUID id = user("pings@test.local", "Pings");

        assertThat(activity.touchLastSeen(id, T0)).isEmpty();
        activity.addUsage(id, DAY, 0, true, T0);
        assertThat(activity.touchLastSeen(id, T0.plusSeconds(60))).contains(T0);
        activity.addUsage(id, DAY, 60, false, T0.plusSeconds(60));
        activity.addUsage(id, DAY, 45, false, T0.plusSeconds(105));

        var row = jdbc.queryForMap(
                "SELECT active_seconds, sessions FROM user_daily_activity WHERE user_id = ? AND activity_date = ?", id, DAY);
        assertThat(row.get("active_seconds")).isEqualTo(105);
        assertThat(row.get("sessions")).isEqualTo(1);
    }

    @Test
    void aDayNeverGoesOver24Hours() {
        UUID id = user("cap@test.local", "Tope");

        activity.addUsage(id, DAY, 86_000, true, T0);
        activity.addUsage(id, DAY, 1_000, false, T0);

        assertThat(jdbc.queryForObject(
                "SELECT active_seconds FROM user_daily_activity WHERE user_id = ?", Integer.class, id)).isEqualTo(86_400);
    }

    @Test
    void theListSearchesFiltersAndHidesDeletedAccounts() {
        UUID found = user("busqueda.zeta@test.local", "Zeta Busqueda");
        UUID deleted = user("busqueda.borrada@test.local", "Zeta Borrada");
        jdbc.update("UPDATE users SET status = 'DELETED' WHERE id = ?", deleted);
        activity.addUsage(found, DAY, 300, true, T0);

        List<UserSummary> page = admin.listUsers("zeta", null, DAY.minusDays(29), T0.minusSeconds(30L * 86400), 0);

        assertThat(page).extracting(UserSummary::id).containsExactly(found.toString());
        assertThat(page.get(0).usageSeconds30d()).isEqualTo(300);
        assertThat(page.get(0).provider()).isEqualTo("PASSWORD");
        assertThat(admin.countUsers("zeta", null)).isEqualTo(1);
        assertThat(admin.countUsers("zeta", "DISABLED")).isZero();
        // Los comodines de LIKE se buscan como texto.
        assertThat(admin.countUsers("%", null)).isZero();
    }

    @Test
    void changingTheStatusIsAuditedAndSeenByTheJwtFilter() {
        UUID adminId = user("admin.audit@test.local", "Admin");
        UUID target = user("target.audit@test.local", "Objetivo");
        AccountStatusService accounts = new AccountStatusService(jdbc);
        assertThat(accounts.find(target).orElseThrow().isActive()).isTrue();

        admin.changeStatus(adminId, target, "DISABLED", "DISABLE_USER", "spam");
        accounts.evict(target);

        assertThat(accounts.find(target).orElseThrow().isActive()).isFalse();
        assertThat(admin.findAccount(target).orElseThrow().status()).isEqualTo("DISABLED");
        List<AuditEntry> history = admin.history(target, 10);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).action()).isEqualTo("DISABLE_USER");
        assertThat(history.get(0).reason()).isEqualTo("spam");
        assertThat(history.get(0).adminName()).isEqualTo("Admin");
    }

    @Test
    void trainingTimeComesFromCompletedWorkouts() {
        UUID id = user("training@test.local", "Entrena");
        jdbc.update("""
                INSERT INTO workout_sessions (user_id, started_at, completed_at, status, kind)
                VALUES (?, now() - interval '2 days 1 hour', now() - interval '2 days', 'COMPLETED', 'ROUTINE'),
                       (?, now() - interval '1 day', now() - interval '1 day', 'COMPLETED', 'QUICK_LOG')
                """, id, id);

        var training = admin.userTraining(id, Instant.now().minusSeconds(30L * 86400));

        assertThat(training.workouts()).isEqualTo(1);
        assertThat(training.secondsTotal()).isBetween(3599L, 3601L);
        assertThat(training.secondsRecent()).isBetween(3599L, 3601L);
    }
}
