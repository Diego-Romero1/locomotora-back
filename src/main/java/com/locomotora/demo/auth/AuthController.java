package com.locomotora.demo.auth;

import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserRow user = jdbcTemplate.queryForObject(
                    """
                    INSERT INTO users (email, password_hash, name)
                    VALUES (?, ?, ?)
                    RETURNING id, email, name
                    """,
                    this::mapUser,
                    request.email().trim().toLowerCase(),
                    passwordEncoder.encode(request.password()),
                    request.name().trim()
            );
            jdbcTemplate.update("INSERT INTO user_profiles (user_id) VALUES (?)", user.id());
            return toResponse(user);
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Optional<LoginRow> login = jdbcTemplate.query(
                "SELECT id, email, name, password_hash FROM users WHERE email = ? AND status = 'ACTIVE'",
                this::mapLogin,
                request.email().trim().toLowerCase()
        ).stream().findFirst();

        LoginRow row = login.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), row.passwordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return toResponse(new UserRow(row.id(), row.email(), row.name()));
    }

    private AuthResponse toResponse(UserRow user) {
        return new AuthResponse(
                jwtService.createToken(user.id(), user.email()),
                new UserResponse(user.id().toString(), user.name(), user.email())
        );
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

    public record RegisterRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 120) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record AuthResponse(String token, UserResponse user) {
    }

    public record UserResponse(String id, String name, String email) {
    }

    private record UserRow(UUID id, String email, String name) {
    }

    private record LoginRow(UUID id, String email, String name, String passwordHash) {
    }
}
