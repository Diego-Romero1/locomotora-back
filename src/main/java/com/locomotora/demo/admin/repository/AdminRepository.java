package com.locomotora.demo.admin.repository;

import com.locomotora.demo.admin.dto.AdminDtos.AuditEntry;
import com.locomotora.demo.admin.dto.AdminDtos.TrainingStats;
import com.locomotora.demo.admin.dto.AdminDtos.UserCounts;
import com.locomotora.demo.admin.dto.AdminDtos.UserSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Consultas del panel de admin. Las cuentas DELETED no aparecen en ningun lado. */
@Repository
public class AdminRepository {
    public static final int PAGE_SIZE = 30;

    /** Entrenamientos que cuentan (los registros sueltos QUICK_LOG no). Una sesion de mas de 6 h se corta. */
    private static final String WORKOUT_FILTER = "ws.status = 'COMPLETED' AND ws.kind <> 'QUICK_LOG'";
    private static final String WORKOUT_SECONDS = """
            LEAST(EXTRACT(EPOCH FROM (ws.completed_at - ws.started_at)), 21600)
            """.trim();
    private static final String HAS_DURATION = "ws.started_at IS NOT NULL AND ws.completed_at > ws.started_at";

    private static final String SUMMARY_SELECT = """
            SELECT u.id, u.name, u.email, u.status, u.role, u.created_at,
                   u.password_hash IS NOT NULL AS has_password,
                   u.google_subject IS NOT NULL AS has_google,
                   GREATEST(u.last_seen_at, w.last_workout_at) AS last_activity_at,
                   COALESCE(a.seconds, 0) AS usage_seconds,
                   COALESCE(w.recent, 0) AS recent_workouts
            FROM users u
            LEFT JOIN LATERAL (
                SELECT SUM(d.active_seconds) AS seconds
                FROM user_daily_activity d
                WHERE d.user_id = u.id AND d.activity_date >= ?
            ) a ON true
            LEFT JOIN LATERAL (
                SELECT max(ws.completed_at) AS last_workout_at,
                       count(*) FILTER (WHERE ws.completed_at >= ?) AS recent
                FROM workout_sessions ws
                WHERE ws.user_id = u.id AND %s
            ) w ON true
            """.formatted(WORKOUT_FILTER);

    private final JdbcTemplate jdbcTemplate;

    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserCounts userCounts(Instant since7d, Instant since30d) {
        return jdbcTemplate.queryForObject(
                """
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE status = 'ACTIVE') AS active,
                       count(*) FILTER (WHERE status = 'DISABLED') AS disabled,
                       count(*) FILTER (WHERE role = 'ADMIN') AS admins,
                       count(*) FILTER (WHERE created_at >= ?) AS new7d,
                       count(*) FILTER (WHERE created_at >= ?) AS new30d,
                       count(*) FILTER (WHERE google_subject IS NOT NULL) AS google,
                       count(*) FILTER (WHERE password_hash IS NOT NULL) AS password
                FROM users
                WHERE status <> 'DELETED'
                """,
                (rs, rowNum) -> new UserCounts(
                        rs.getInt("total"),
                        rs.getInt("active"),
                        rs.getInt("disabled"),
                        rs.getInt("admins"),
                        rs.getInt("new7d"),
                        rs.getInt("new30d"),
                        rs.getInt("google"),
                        rs.getInt("password")
                ),
                Timestamp.from(since7d),
                Timestamp.from(since30d)
        );
    }

    /** Usuarios con un ping en las ultimas 24 h, 7 y 30 dias (en ese orden). */
    public int[] activeUsers(Instant since24h, Instant since7d, Instant since30d) {
        return jdbcTemplate.queryForObject(
                """
                SELECT count(*) FILTER (WHERE last_seen_at >= ?) AS a24,
                       count(*) FILTER (WHERE last_seen_at >= ?) AS a7,
                       count(*) FILTER (WHERE last_seen_at >= ?) AS a30
                FROM users
                WHERE status <> 'DELETED'
                """,
                (rs, rowNum) -> new int[] {rs.getInt("a24"), rs.getInt("a7"), rs.getInt("a30")},
                Timestamp.from(since24h),
                Timestamp.from(since7d),
                Timestamp.from(since30d)
        );
    }

    /** Segundos de uso desde {@code from7d}, desde {@code from30d}, y cuantos usuarios usaron la app desde {@code from30d}. */
    public long[] usageTotals(LocalDate from7d, LocalDate from30d) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(d.active_seconds) FILTER (WHERE d.activity_date >= ?), 0) AS seconds7d,
                       COALESCE(SUM(d.active_seconds), 0) AS seconds30d,
                       count(DISTINCT d.user_id) AS users30d
                FROM user_daily_activity d
                JOIN users u ON u.id = d.user_id AND u.status <> 'DELETED'
                WHERE d.activity_date >= ?
                """,
                (rs, rowNum) -> new long[] {rs.getLong("seconds7d"), rs.getLong("seconds30d"), rs.getLong("users30d")},
                from7d,
                from30d
        );
    }

    public TrainingStats training(Instant since30d) {
        return jdbcTemplate.queryForObject(
                """
                SELECT count(*) AS workouts,
                       COALESCE(SUM(%s) FILTER (WHERE %s), 0)::bigint AS seconds
                FROM workout_sessions ws
                JOIN users u ON u.id = ws.user_id AND u.status <> 'DELETED'
                WHERE %s AND ws.completed_at >= ?
                """.formatted(WORKOUT_SECONDS, HAS_DURATION, WORKOUT_FILTER),
                (rs, rowNum) -> new TrainingStats(rs.getInt("workouts"), rs.getLong("seconds")),
                Timestamp.from(since30d)
        );
    }

    /** Por dia desde {@code from}: [usuarios, segundos]. Solo los dias con uso. */
    public Map<LocalDate, long[]> dailyUsage(LocalDate from) {
        Map<LocalDate, long[]> days = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                SELECT d.activity_date, count(*) AS users, SUM(d.active_seconds) AS seconds
                FROM user_daily_activity d
                JOIN users u ON u.id = d.user_id AND u.status <> 'DELETED'
                WHERE d.activity_date >= ?
                GROUP BY d.activity_date
                """,
                rs -> {
                    days.put(rs.getObject("activity_date", LocalDate.class),
                            new long[] {rs.getLong("users"), rs.getLong("seconds")});
                },
                from
        );
        return days;
    }

    /**
     * Lista paginada, primero los que usaron la app mas recientemente. {@code query} busca en nombre y email;
     * {@code status} null = todos.
     */
    public List<UserSummary> listUsers(
            String query, String status, LocalDate usageFrom, Instant workoutsSince, int page) {
        List<Object> params = new ArrayList<>(List.of(usageFrom, Timestamp.from(workoutsSince)));
        String where = where(query, status, params);
        params.add(PAGE_SIZE);
        params.add(page * PAGE_SIZE);
        return jdbcTemplate.query(
                SUMMARY_SELECT + where + """
                         ORDER BY last_activity_at DESC NULLS LAST, u.created_at DESC, u.id
                         LIMIT ? OFFSET ?
                        """,
                this::mapSummary,
                params.toArray()
        );
    }

    public long countUsers(String query, String status) {
        List<Object> params = new ArrayList<>();
        String where = where(query, status, params);
        Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM users u" + where, Long.class, params.toArray());
        return total == null ? 0 : total;
    }

    public Optional<UserSummary> findUser(UUID userId, LocalDate usageFrom, Instant workoutsSince) {
        return jdbcTemplate.query(
                SUMMARY_SELECT + " WHERE u.id = ? AND u.status <> 'DELETED'",
                this::mapSummary,
                usageFrom,
                Timestamp.from(workoutsSince),
                userId
        ).stream().findFirst();
    }

    public boolean onboardingCompleted(UUID userId) {
        return Boolean.TRUE.equals(jdbcTemplate.query(
                "SELECT onboarding_completed FROM user_profiles WHERE user_id = ?",
                (rs, rowNum) -> rs.getBoolean("onboarding_completed"),
                userId
        ).stream().findFirst().orElse(false));
    }

    public int activeRoutines(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM routines WHERE user_id = ? AND is_active", Integer.class, userId);
        return count == null ? 0 : count;
    }

    public UserTraining userTraining(UUID userId, Instant since30d) {
        return jdbcTemplate.queryForObject(
                """
                SELECT count(*) AS workouts,
                       COALESCE(SUM(%1$s) FILTER (WHERE %2$s), 0)::bigint AS seconds_total,
                       COALESCE(SUM(%1$s) FILTER (WHERE %2$s AND ws.completed_at >= ?), 0)::bigint AS seconds_recent,
                       max(ws.completed_at) AS last_workout_at
                FROM workout_sessions ws
                WHERE ws.user_id = ? AND %3$s
                """.formatted(WORKOUT_SECONDS, HAS_DURATION, WORKOUT_FILTER),
                (rs, rowNum) -> new UserTraining(
                        rs.getInt("workouts"),
                        rs.getLong("seconds_total"),
                        rs.getLong("seconds_recent"),
                        instant(rs, "last_workout_at")
                ),
                Timestamp.from(since30d),
                userId
        );
    }

    public int aiRequests(UUID userId, Instant since) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ai_usage WHERE user_id = ? AND created_at >= ?",
                Integer.class,
                userId,
                Timestamp.from(since)
        );
        return count == null ? 0 : count;
    }

    /** Por dia desde {@code from}: [segundos, sesiones]. Solo los dias con uso. */
    public Map<LocalDate, long[]> userDailyUsage(UUID userId, LocalDate from) {
        Map<LocalDate, long[]> days = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                SELECT activity_date, active_seconds, sessions
                FROM user_daily_activity
                WHERE user_id = ? AND activity_date >= ?
                """,
                rs -> {
                    days.put(rs.getObject("activity_date", LocalDate.class),
                            new long[] {rs.getLong("active_seconds"), rs.getLong("sessions")});
                },
                userId,
                from
        );
        return days;
    }

    public List<AuditEntry> history(UUID userId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT l.action, l.reason, l.created_at, a.name AS admin_name
                FROM admin_audit_log l
                LEFT JOIN users a ON a.id = l.admin_user_id
                WHERE l.target_user_id = ?
                ORDER BY l.created_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new AuditEntry(
                        rs.getString("action"),
                        rs.getString("reason"),
                        rs.getString("admin_name"),
                        iso(instant(rs, "created_at"))
                ),
                userId,
                limit
        );
    }

    /** Estado y rol actuales (vacio si no existe o esta borrada). */
    public Optional<AccountRow> findAccount(UUID userId) {
        return jdbcTemplate.query(
                "SELECT id, status, role FROM users WHERE id = ? AND status <> 'DELETED'",
                (rs, rowNum) -> new AccountRow(rs.getObject("id", UUID.class), rs.getString("status"), rs.getString("role")),
                userId
        ).stream().findFirst();
    }

    /** Cambia el estado y lo deja registrado, en una sola transaccion. */
    @Transactional
    public void changeStatus(UUID adminId, UUID userId, String status, String action, String reason) {
        jdbcTemplate.update(
                "UPDATE users SET status = ?, updated_at = now() WHERE id = ? AND status <> 'DELETED'",
                status,
                userId
        );
        jdbcTemplate.update(
                "INSERT INTO admin_audit_log (admin_user_id, target_user_id, action, reason) VALUES (?, ?, ?, ?)",
                adminId,
                userId,
                action,
                reason
        );
    }

    private static String where(String query, String status, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE u.status <> 'DELETED'");
        if (status != null) {
            where.append(" AND u.status = ?");
            params.add(status);
        }
        if (query != null) {
            String like = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            where.append(" AND (u.email ILIKE ? OR u.name ILIKE ?)");
            params.add(like);
            params.add(like);
        }
        return where.toString();
    }

    private UserSummary mapSummary(ResultSet rs, int rowNum) throws SQLException {
        boolean google = rs.getBoolean("has_google");
        boolean password = rs.getBoolean("has_password");
        return new UserSummary(
                rs.getObject("id", UUID.class).toString(),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("status"),
                rs.getString("role"),
                google && password ? "BOTH" : google ? "GOOGLE" : "PASSWORD",
                iso(instant(rs, "created_at")),
                iso(instant(rs, "last_activity_at")),
                rs.getLong("usage_seconds"),
                rs.getInt("recent_workouts")
        );
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    public record AccountRow(UUID id, String status, String role) {
        public boolean isAdmin() {
            return "ADMIN".equals(role);
        }
    }

    public record UserTraining(int workouts, long secondsTotal, long secondsRecent, Instant lastWorkoutAt) {
    }
}
