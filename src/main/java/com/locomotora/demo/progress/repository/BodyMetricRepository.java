package com.locomotora.demo.progress.repository;

import com.locomotora.demo.progress.dto.BodyMetricRequest;
import com.locomotora.demo.progress.dto.BodyMetricResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BodyMetricRepository {
    private final JdbcTemplate jdbcTemplate;

    public BodyMetricRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<BodyMetricResponse> findLatestByUserId(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id, measured_at, weight_kg, body_fat_percentage, waist_cm, chest_cm, hip_cm, arm_cm, leg_cm, notes
                FROM body_metrics
                WHERE user_id = ?
                ORDER BY measured_at DESC
                LIMIT 1
                """,
                this::mapMetric,
                userId
        ).stream().findFirst();
    }

    public List<BodyMetricResponse> findRecentByUserId(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id, measured_at, weight_kg, body_fat_percentage, waist_cm, chest_cm, hip_cm, arm_cm, leg_cm, notes
                FROM body_metrics
                WHERE user_id = ?
                ORDER BY measured_at DESC
                LIMIT 100
                """,
                this::mapMetric,
                userId
        );
    }

    public Optional<BodyMetricResponse> findByIdAndUserId(UUID id, UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id, measured_at, weight_kg, body_fat_percentage, waist_cm, chest_cm, hip_cm, arm_cm, leg_cm, notes
                FROM body_metrics
                WHERE id = ? AND user_id = ?
                """,
                this::mapMetric,
                id,
                userId
        ).stream().findFirst();
    }

    public UUID create(UUID userId, BodyMetricRequest request) {
        if (request.measuredAt() == null) {
            return jdbcTemplate.queryForObject(
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
        }

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO body_metrics
                    (user_id, measured_at, weight_kg, body_fat_percentage, waist_cm, chest_cm, hip_cm, arm_cm, leg_cm, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                UUID.class,
                userId,
                Timestamp.from(request.measuredAt()),
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
}
