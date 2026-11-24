package com.locomotora.demo.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Estado y rol actuales de cada cuenta, para el filtro JWT: un token valido de una cuenta inhabilitada deja de
 * servir. Se guarda en memoria unos segundos para no consultar la base en cada peticion; el panel de admin
 * borra la entrada al cambiar una cuenta (efecto inmediato, el backend corre en una sola instancia). Un cambio
 * hecho a mano en la base tarda como mucho {@link #TTL} en aplicarse.
 */
@Service
public class AccountStatusService {
    static final Duration TTL = Duration.ofSeconds(60);
    private static final int MAX_ENTRIES = 10_000;

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final Map<UUID, Cached> cache = new ConcurrentHashMap<>();

    @Autowired
    public AccountStatusService(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Clock.systemUTC());
    }

    AccountStatusService(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    /** Vacio si la cuenta no existe. */
    public Optional<AccountState> find(UUID userId) {
        Instant now = clock.instant();
        Cached cached = cache.get(userId);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.state();
        }
        Optional<AccountState> state = jdbcTemplate.query(
                "SELECT status, role FROM users WHERE id = ?",
                (rs, rowNum) -> new AccountState(rs.getString("status"), rs.getString("role")),
                userId
        ).stream().findFirst();
        if (cache.size() >= MAX_ENTRIES) {
            cache.clear();
        }
        cache.put(userId, new Cached(state, now.plus(TTL)));
        return state;
    }

    public void evict(UUID userId) {
        cache.remove(userId);
    }

    public record AccountState(String status, String role) {
        public boolean isActive() {
            return "ACTIVE".equals(status);
        }

        public boolean isAdmin() {
            return "ADMIN".equals(role);
        }
    }

    private record Cached(Optional<AccountState> state, Instant expiresAt) {
    }
}
