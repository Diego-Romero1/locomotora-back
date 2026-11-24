package com.locomotora.demo.activity;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ActivityRepository {
    private final JdbcTemplate jdbcTemplate;

    public ActivityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Guarda el ping y devuelve el anterior (vacio si es el primero). Bloquea la fila del usuario para que dos
     * pings a la vez no sumen el mismo tramo dos veces.
     */
    @Transactional
    public Optional<Instant> touchLastSeen(UUID userId, Instant now) {
        return jdbcTemplate.query(
                """
                UPDATE users u
                SET last_seen_at = ?
                FROM (SELECT id, last_seen_at AS previous FROM users WHERE id = ? FOR UPDATE) p
                WHERE u.id = p.id
                RETURNING p.previous
                """,
                (rs, rowNum) -> Optional.ofNullable(rs.getTimestamp("previous")).map(Timestamp::toInstant),
                Timestamp.from(now),
                userId
        ).stream().findFirst().flatMap(previous -> previous);
    }

    /** Suma segundos (y una sesion si empieza una nueva) al dia local del usuario. Tope: 24 h por dia. */
    public void addUsage(UUID userId, LocalDate day, int seconds, boolean newSession, Instant now) {
        jdbcTemplate.update(
                """
                INSERT INTO user_daily_activity (user_id, activity_date, active_seconds, sessions, last_seen_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (user_id, activity_date) DO UPDATE SET
                    active_seconds = LEAST(86400, user_daily_activity.active_seconds + EXCLUDED.active_seconds),
                    sessions = user_daily_activity.sessions + EXCLUDED.sessions,
                    last_seen_at = GREATEST(user_daily_activity.last_seen_at, EXCLUDED.last_seen_at)
                """,
                userId,
                day,
                Math.min(seconds, 86400),
                newSession ? 1 : 0,
                Timestamp.from(now)
        );
    }
}
