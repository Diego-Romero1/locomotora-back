package com.locomotora.demo.ai.repository;

import com.locomotora.demo.ai.service.UserContext;
import com.locomotora.demo.common.JsonCodes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Lee lo que el coach necesita saber del usuario. Solo lectura. */
@Repository
public class AiContextRepository {
    private final JdbcTemplate jdbcTemplate;

    public AiContextRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserContext load(UUID userId) {
        return new UserContext(
                profile(userId),
                latestWeight(userId),
                goals(userId),
                strings("SELECT limitation_type || COALESCE(': ' || notes, '') FROM user_limitations WHERE user_id = ? ORDER BY created_at", userId),
                equipment(userId),
                dietaryPreferences(userId),
                strings("SELECT title FROM routines WHERE user_id = ? AND is_active = true ORDER BY updated_at DESC LIMIT 5", userId),
                sessionsLast28Days(userId),
                bestLifts(userId)
        );
    }

    /** Equipo del onboarding (BODYWEIGHT, DUMBBELLS, BARBELL, ...). */
    public List<String> equipment(UUID userId) {
        return strings("SELECT equipment_name FROM user_equipment WHERE user_id = ? ORDER BY equipment_name", userId);
    }

    /** Preferencias de alimentacion: el perfil las guarda como un arreglo JSON de textos. */
    private List<String> dietaryPreferences(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT value
                FROM user_profiles p, jsonb_array_elements_text(
                    CASE WHEN jsonb_typeof(p.dietary_preferences) = 'array' THEN p.dietary_preferences ELSE '[]'::jsonb END) AS value
                WHERE p.user_id = ?
                LIMIT 10
                """,
                (rs, rowNum) -> rs.getString(1),
                userId
        );
    }

    private UserContext.Profile profile(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT p.birth_date, p.sex, p.height_cm, p.weight_kg, p.activity_level, p.experience_level,
                       p.training_days_per_week, p.session_duration_minutes, pref.preferred_training_style,
                       p.training_location, p.health_flags::text AS health_flags
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                LEFT JOIN user_preferences pref ON pref.user_id = u.id
                WHERE u.id = ?
                LIMIT 1
                """,
                rs -> {
                    if (!rs.next()) {
                        return UserContext.Profile.EMPTY;
                    }
                    java.sql.Date birth = rs.getDate("birth_date");
                    Integer age = birth == null ? null : Period.between(birth.toLocalDate(), LocalDate.now()).getYears();
                    return new UserContext.Profile(
                            age,
                            rs.getString("sex"),
                            decimal(rs.getBigDecimal("height_cm")),
                            decimal(rs.getBigDecimal("weight_kg")),
                            rs.getString("activity_level"),
                            rs.getString("experience_level"),
                            (Integer) rs.getObject("training_days_per_week", Integer.class),
                            (Integer) rs.getObject("session_duration_minutes", Integer.class),
                            rs.getString("preferred_training_style"),
                            rs.getString("training_location"),
                            JsonCodes.parse(rs.getString("health_flags"))
                    );
                },
                userId
        );
    }

    private Double latestWeight(UUID userId) {
        return jdbcTemplate.query(
                "SELECT weight_kg FROM body_metrics WHERE user_id = ? AND weight_kg IS NOT NULL ORDER BY measured_at DESC LIMIT 1",
                rs -> rs.next() ? decimal(rs.getBigDecimal("weight_kg")) : null,
                userId
        );
    }

    private List<String> goals(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT goal_type, target_value, target_unit, target_date
                FROM user_goals
                WHERE user_id = ? AND status = 'ACTIVE'
                ORDER BY priority, created_at
                LIMIT 5
                """,
                (rs, rowNum) -> {
                    StringBuilder goal = new StringBuilder(rs.getString("goal_type"));
                    BigDecimal target = rs.getBigDecimal("target_value");
                    if (target != null) {
                        goal.append(" (meta ").append(target.stripTrailingZeros().toPlainString());
                        if (rs.getString("target_unit") != null) {
                            goal.append(' ').append(rs.getString("target_unit"));
                        }
                        if (rs.getDate("target_date") != null) {
                            goal.append(" para ").append(rs.getDate("target_date").toLocalDate());
                        }
                        goal.append(')');
                    }
                    return goal.toString();
                },
                userId
        );
    }

    /** Sesiones completadas de los ultimos {@code days} dias, la mas reciente primero. */
    public List<UserContext.Session> recentSessions(UUID userId, int days, int limit) {
        return jdbcTemplate.query(
                """
                SELECT s.completed_at::date AS day,
                       COALESCE(s.day_title, s.routine_title, 'Sesion libre') AS title,
                       s.perceived_exertion,
                       count(l.id) FILTER (WHERE l.completed) AS sets,
                       COALESCE(sum(l.reps * l.weight_kg) FILTER (WHERE l.completed), 0) AS volume
                FROM workout_sessions s
                LEFT JOIN workout_exercise_logs l ON l.workout_session_id = s.id
                WHERE s.user_id = ? AND s.status = 'COMPLETED' AND s.kind <> 'QUICK_LOG'
                  AND s.completed_at > now() - make_interval(days => ?)
                GROUP BY s.id
                ORDER BY s.completed_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new UserContext.Session(
                        rs.getDate("day").toLocalDate(),
                        rs.getString("title"),
                        rs.getInt("sets"),
                        rs.getBigDecimal("volume").doubleValue(),
                        (Integer) rs.getObject("perceived_exertion", Integer.class)
                ),
                userId,
                days,
                limit
        );
    }

    private int sessionsLast28Days(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM workout_sessions
                WHERE user_id = ? AND status = 'COMPLETED' AND kind <> 'QUICK_LOG'
                  AND completed_at > now() - interval '28 days'
                """,
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    /** Mejor serie (mas peso) de los ejercicios mas entrenados en los ultimos 90 dias. */
    private List<UserContext.Lift> bestLifts(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT e.name, best.weight_kg, best.reps, best.sets
                FROM (
                    SELECT l.exercise_id,
                           (array_agg(l.weight_kg ORDER BY l.weight_kg DESC, l.reps DESC))[1] AS weight_kg,
                           (array_agg(l.reps ORDER BY l.weight_kg DESC, l.reps DESC))[1] AS reps,
                           count(*) AS sets
                    FROM workout_exercise_logs l
                    JOIN workout_sessions s ON s.id = l.workout_session_id
                    WHERE s.user_id = ? AND s.status = 'COMPLETED' AND l.completed
                      AND l.weight_kg IS NOT NULL AND l.reps IS NOT NULL
                      AND s.completed_at > now() - interval '90 days'
                    GROUP BY l.exercise_id
                ) best
                JOIN exercises e ON e.id = best.exercise_id
                ORDER BY best.sets DESC, e.name
                LIMIT 8
                """,
                (rs, rowNum) -> new UserContext.Lift(
                        rs.getString("name"),
                        rs.getBigDecimal("weight_kg").doubleValue(),
                        rs.getInt("reps")
                ),
                userId
        );
    }

    private List<String> strings(String sql, UUID userId) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), userId);
    }

    private static Double decimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
