package com.locomotora.demo.routine.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.locomotora.demo.routine.repository.RoutineDraftRepository.DraftRow;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** V20 sobre un Postgres real: gana el borrador mas nuevo, no se pisa entre usuarios y se lista bien. */
@Testcontainers
class RoutineDraftRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private static JdbcTemplate jdbc;
    private static RoutineDraftRepository repository;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        repository = new RoutineDraftRepository(jdbc);
    }

    private static UUID user(String email) {
        return jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, name) VALUES (?, 'x', 'Prueba') RETURNING id", UUID.class, email);
    }

    private static UUID routine(UUID userId, boolean active) {
        return jdbc.queryForObject(
                "INSERT INTO routines (user_id, title, is_active) VALUES (?, 'Torso/Pierna', ?) RETURNING id",
                UUID.class, userId, active);
    }

    private static DraftRow draft(UUID id, UUID userId, String title, String at) {
        return new DraftRow(id, userId, "NEW", null, title, 2, 3, 9, "{\"title\":\"" + title + "\"}", Instant.parse(at));
    }

    @Test
    void theNewestDraftWinsAndAnOlderOneDoesNotOverwrite() {
        UUID userId = user("drafts-newest@test.local");
        UUID id = UUID.randomUUID();

        assertThat(repository.upsert(draft(id, userId, "Primera", "2026-11-23T10:00:00Z"))).isTrue();
        assertThat(repository.upsert(draft(id, userId, "Segunda", "2026-11-23T10:05:00Z"))).isTrue();
        // Llega tarde una version vieja (por ejemplo, de otro telefono sin conexion): no pisa.
        assertThat(repository.upsert(draft(id, userId, "Vieja", "2026-11-23T09:00:00Z"))).isFalse();

        DraftRow stored = repository.findById(id).orElseThrow();
        assertThat(stored.title()).isEqualTo("Segunda");
        assertThat(stored.payload()).contains("Segunda");
        assertThat(stored.exerciseCount()).isEqualTo(9);
    }

    @Test
    void anotherUserCannotOverwriteOrDeleteADraft() {
        UUID owner = user("drafts-owner@test.local");
        UUID other = user("drafts-other@test.local");
        UUID id = UUID.randomUUID();
        repository.upsert(draft(id, owner, "Mia", "2026-11-23T10:00:00Z"));

        assertThat(repository.upsert(draft(id, other, "Ajena", "2026-11-23T11:00:00Z"))).isFalse();
        assertThat(repository.delete(id, other)).isFalse();
        assertThat(repository.findById(id).orElseThrow().title()).isEqualTo("Mia");
        assertThat(repository.delete(id, owner)).isTrue();
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void listsTheNewestFirstAndHidesEditsOfInactiveRoutines() {
        UUID userId = user("drafts-list@test.local");
        UUID active = routine(userId, true);
        UUID archived = routine(userId, false);
        UUID older = UUID.randomUUID();
        UUID newer = UUID.randomUUID();
        UUID editActive = UUID.randomUUID();
        UUID editArchived = UUID.randomUUID();
        repository.upsert(draft(older, userId, "Vieja", "2026-11-20T10:00:00Z"));
        repository.upsert(draft(newer, userId, "Nueva", "2026-11-23T10:00:00Z"));
        repository.upsert(new DraftRow(editActive, userId, "EDIT", active, "Torso/Pierna", 1, 2, 4, "{}", Instant.parse("2026-11-22T10:00:00Z")));
        repository.upsert(new DraftRow(editArchived, userId, "EDIT", archived, "Vieja rutina", 1, 2, 4, "{}", Instant.parse("2026-11-23T11:00:00Z")));

        assertThat(repository.findByUser(userId)).extracting(DraftRow::id).containsExactly(newer, editActive, older);
        assertThat(repository.countByUser(userId)).isEqualTo(4);
        assertThat(repository.isActiveRoutineOf(active, userId)).isTrue();
        assertThat(repository.isActiveRoutineOf(archived, userId)).isFalse();
    }
}
