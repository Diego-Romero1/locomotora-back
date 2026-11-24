package com.locomotora.demo.auth.repository;

import com.locomotora.demo.auth.dto.RegisterRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AuthRepository {
    private static final String ACCOUNT_COLUMNS = "id, email, name, status, google_subject";

    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserRow createUser(RegisterRequest request, String passwordHash) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO users (email, password_hash, name)
                VALUES (?, ?, ?)
                RETURNING id, email, name
                """,
                this::mapUser,
                request.email().trim().toLowerCase(),
                passwordHash,
                request.name().trim()
        );
    }

    public void createProfile(UUID userId) {
        jdbcTemplate.update("INSERT INTO user_profiles (user_id) VALUES (?)", userId);
    }

    /** Cuenta nueva desde Google (sin contrasena) con su perfil vacio, en una sola transaccion. */
    @Transactional
    public UserRow createGoogleUser(String email, String name, String googleSubject, String photoUrl) {
        UserRow user = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (email, password_hash, name, google_subject, profile_photo_url)
                VALUES (?, NULL, ?, ?, ?)
                RETURNING id, email, name
                """,
                this::mapUser,
                email,
                name,
                googleSubject,
                photoUrl
        );
        createProfile(user.id());
        return user;
    }

    public Optional<AccountRow> findAccountByGoogleSubject(String googleSubject) {
        return jdbcTemplate.query(
                "SELECT " + ACCOUNT_COLUMNS + " FROM users WHERE google_subject = ?",
                this::mapAccount,
                googleSubject
        ).stream().findFirst();
    }

    public Optional<AccountRow> findAccountByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT " + ACCOUNT_COLUMNS + " FROM users WHERE email = ?",
                this::mapAccount,
                email.trim().toLowerCase()
        ).stream().findFirst();
    }

    /** Vincula Google a una cuenta que todavia no tiene. Devuelve false si ya tenia otra vinculada. */
    public boolean linkGoogleSubject(UUID userId, String googleSubject) {
        return jdbcTemplate.update(
                "UPDATE users SET google_subject = ?, updated_at = now() WHERE id = ? AND google_subject IS NULL",
                googleSubject,
                userId
        ) == 1;
    }

    public Optional<LoginRow> findActiveLoginByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT id, email, name, password_hash FROM users WHERE email = ? AND status = 'ACTIVE'",
                this::mapLogin,
                email.trim().toLowerCase()
        ).stream().findFirst();
    }

    private UserRow mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new UserRow(rs.getObject("id", UUID.class), rs.getString("email"), rs.getString("name"));
    }

    private AccountRow mapAccount(ResultSet rs, int rowNum) throws SQLException {
        return new AccountRow(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("name"),
                rs.getString("status"),
                rs.getString("google_subject")
        );
    }

    private LoginRow mapLogin(ResultSet rs, int rowNum) throws SQLException {
        return new LoginRow(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("name"),
                rs.getString("password_hash")
        );
    }

    public record UserRow(UUID id, String email, String name) {
    }

    public record AccountRow(UUID id, String email, String name, String status, String googleSubject) {
        public boolean isActive() {
            return "ACTIVE".equals(status);
        }

        public UserRow toUser() {
            return new UserRow(id, email, name);
        }
    }

    /** {@code passwordHash} es null en las cuentas creadas con Google. */
    public record LoginRow(UUID id, String email, String name, String passwordHash) {
    }
}
