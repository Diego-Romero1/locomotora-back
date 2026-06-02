package com.locomotora.demo.auth.repository;

import com.locomotora.demo.auth.dto.RegisterRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {
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

    public record LoginRow(UUID id, String email, String name, String passwordHash) {
    }
}
