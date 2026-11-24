package com.locomotora.demo.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.locomotora.demo.auth.repository.AuthRepository.AccountRow;
import com.locomotora.demo.auth.repository.AuthRepository.UserRow;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** V21 sobre un Postgres real: cuentas sin contrasena, vinculacion y unicidad de la cuenta de Google. */
@Testcontainers
class AuthRepositoryGoogleTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private static JdbcTemplate jdbc;
    private static AuthRepository repository;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        repository = new AuthRepository(jdbc);
    }

    @Test
    void aGoogleUserIsCreatedWithoutPasswordWithPhotoAndProfile() {
        UserRow user = repository.createGoogleUser("new@test.local", "Nueva", "sub-new", "https://photo");

        assertThat(repository.findAccountByGoogleSubject("sub-new").map(AccountRow::id)).contains(user.id());
        assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, user.id())).isNull();
        assertThat(jdbc.queryForObject("SELECT profile_photo_url FROM users WHERE id = ?", String.class, user.id()))
                .isEqualTo("https://photo");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_profiles WHERE user_id = ?", Integer.class, user.id()))
                .isEqualTo(1);
        assertThat(repository.findLoginByEmail("new@test.local").orElseThrow().passwordHash()).isNull();
    }

    @Test
    void anExistingAccountIsLinkedOnlyOnce() {
        UUID id = jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, name) VALUES ('link@test.local', 'x', 'Prueba') RETURNING id",
                UUID.class);

        AccountRow account = repository.findAccountByEmail(" LINK@test.local ").orElseThrow();
        assertThat(account.id()).isEqualTo(id);
        assertThat(account.googleSubject()).isNull();
        assertThat(repository.linkGoogleSubject(id, "sub-link")).isTrue();
        assertThat(repository.linkGoogleSubject(id, "sub-other")).isFalse();
        assertThat(repository.findAccountByGoogleSubject("sub-link").map(AccountRow::id)).contains(id);
    }

    @Test
    void theSameGoogleAccountOrEmailCannotBeCreatedTwiceAndNothingIsLeftBehind() {
        repository.createGoogleUser("dup@test.local", "Uno", "sub-dup", null);

        assertThatThrownBy(() -> repository.createGoogleUser("dup2@test.local", "Dos", "sub-dup", null))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> repository.createGoogleUser("dup@test.local", "Tres", "sub-dup-3", null))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email LIKE 'dup%'", Integer.class)).isEqualTo(1);
    }
}
