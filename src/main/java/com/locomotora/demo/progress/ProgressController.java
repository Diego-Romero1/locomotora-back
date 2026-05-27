package com.locomotora.demo.progress;

import com.locomotora.demo.common.CurrentUser;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProgressController {
    private final JdbcTemplate jdbcTemplate;

    public ProgressController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/progress/summary")
    public ProgressSummary summary() {
        UUID userId = CurrentUser.id();
        Integer workouts = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM workout_sessions WHERE user_id = ? AND status = 'COMPLETED'",
                Integer.class,
                userId
        );
        Integer activeRoutines = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM routines WHERE user_id = ? AND is_active = true",
                Integer.class,
                userId
        );
        BodyMetricResponse latestMetric = jdbcTemplate.query(
                """
                SELECT id, measured_at, weight_kg, body_fat_percentage, waist_cm, chest_cm, hip_cm, arm_cm, leg_cm, notes
                FROM body_metrics
                WHERE user_id = ?
                ORDER BY measured_at DESC
                LIMIT 1
                """,
                this::mapMetric,
                userId
        ).stream().findFirst().orElse(null);
        Integer streak = jdbcTemplate.queryForObject(
                """
                SELECT count(DISTINCT completed_at::date)
                FROM workout_sessions
                WHERE user_id = ? AND status = 'COMPLETED' AND completed_at >= now() - interval '30 days'
                """,
                Integer.class,
                userId
        );
        return new ProgressSummary(
                workouts == null ? 0 : workouts,
                activeRoutines == null ? 0 : activeRoutines,
                streak == null ? 0 : streak,
                latestMetric
        );
    }

    @GetMapping("/metrics")
    public List<BodyMetricResponse> metrics() {
        return jdbcTemplate.query(
                """
                SELECT id, measured_at, weight_kg, body_fat_percentage, waist_cm, chest_cm, hip_cm, arm_cm, leg_cm, notes
                FROM body_metrics
                WHERE user_id = ?
                ORDER BY measured_at DESC
                LIMIT 100
                """,
                this::mapMetric,
                CurrentUser.id()
        );
    }

    @PostMapping("/metrics")
    public BodyMetricResponse addMetric(@Valid @RequestBody BodyMetricRequest request) {
        UUID userId = CurrentUser.id();
        UUID id;
        if (request.measuredAt() == null) {
            id = jdbcTemplate.queryForObject(
                    """
                    INSERT INTO body_metrics
                        (user_id, weight_kg, body_fat_percentage, waist_cm, chest_cm, hip_cm, arm_cm, leg_cm, notes)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """,
                    UUID.class,
                    userId,
                    request.weightKg(),
                    request.bodyFatPercentage(),
                    request.waistCm(),
                    request.chestCm(),
                    request.hipCm(),
                    request.armCm(),
                    request.legCm(),
                    request.notes()
            );
        } else {
            id = jdbcTemplate.queryForObject(
                    """
                    INSERT INTO body_metrics
                        (user_id, measured_at, weight_kg, body_fat_percentage, waist_cm, chest_cm, hip_cm, arm_cm, leg_cm, notes)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """,
                    UUID.class,
                    userId,
                    java.sql.Timestamp.from(request.measuredAt()),
                    request.weightKg(),
                    request.bodyFatPercentage(),
                    request.waistCm(),
                    request.chestCm(),
                    request.hipCm(),
                    request.armCm(),
                    request.legCm(),
                    request.notes()
            );
        }
        return metrics().stream().filter(metric -> metric.id().equals(id.toString())).findFirst().orElseThrow();
    }

    private BodyMetricResponse mapMetric(ResultSet rs, int rowNum) throws SQLException {
        return new BodyMetricResponse(
                rs.getObject("id", UUID.class).toString(),
                rs.getTimestamp("measured_at").toInstant().toString(),
                rs.getBigDecimal("weight_kg"),
                rs.getBigDecimal("body_fat_percentage"),
                rs.getBigDecimal("waist_cm"),
                rs.getBigDecimal("chest_cm"),
                rs.getBigDecimal("hip_cm"),
                rs.getBigDecimal("arm_cm"),
                rs.getBigDecimal("leg_cm"),
                rs.getString("notes")
        );
    }

    public record ProgressSummary(int completedWorkouts, int activeRoutines, int activeDaysLast30, BodyMetricResponse latestMetric) {
    }

    public record BodyMetricRequest(
            Instant measuredAt,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal waistCm,
            BigDecimal chestCm,
            BigDecimal hipCm,
            BigDecimal armCm,
            BigDecimal legCm,
            String notes
    ) {
    }

    public record BodyMetricResponse(String id, String measuredAt, BigDecimal weightKg, BigDecimal bodyFatPercentage,
                                     BigDecimal waistCm, BigDecimal chestCm, BigDecimal hipCm, BigDecimal armCm,
                                     BigDecimal legCm, String notes) {
    }
}
