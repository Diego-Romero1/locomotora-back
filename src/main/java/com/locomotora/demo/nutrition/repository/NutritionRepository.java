package com.locomotora.demo.nutrition.repository;

import com.locomotora.demo.nutrition.dto.NutritionEntryRequest;
import com.locomotora.demo.nutrition.dto.NutritionEntryResponse;
import com.locomotora.demo.nutrition.dto.NutritionSummaryResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NutritionRepository {
    private final JdbcTemplate jdbcTemplate;

    public NutritionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<NutritionEntryResponse> findByUserIdAndDate(UUID userId, LocalDate date) {
        return jdbcTemplate.query(
                """
                SELECT id, entry_date, meal_type, description, calories, protein_g, carbs_g, fat_g, fiber_g, source, metadata::text AS metadata
                FROM nutrition_entries
                WHERE user_id = ? AND entry_date = ?
                ORDER BY created_at DESC
                """,
                this::mapEntry,
                userId,
                date
        );
    }

    public List<NutritionEntryResponse> findRecentByUserId(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id, entry_date, meal_type, description, calories, protein_g, carbs_g, fat_g, fiber_g, source, metadata::text AS metadata
                FROM nutrition_entries
                WHERE user_id = ?
                ORDER BY entry_date DESC, created_at DESC
                LIMIT 100
                """,
                this::mapEntry,
                userId
        );
    }

    public Optional<NutritionEntryResponse> findByIdAndUserId(UUID id, UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT id, entry_date, meal_type, description, calories, protein_g, carbs_g, fat_g, fiber_g, source, metadata::text AS metadata
                FROM nutrition_entries
                WHERE id = ? AND user_id = ?
                """,
                this::mapEntry,
                id,
                userId
        ).stream().findFirst();
    }

    public UUID create(UUID userId, NutritionEntryRequest request, LocalDate entryDate) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO nutrition_entries
                    (user_id, entry_date, meal_type, description, calories, protein_g, carbs_g, fat_g, fiber_g, source, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
                RETURNING id
                """,
                UUID.class,
                userId,
                entryDate,
                request.mealType(),
                request.description(),
                request.calories(),
                request.proteinG(),
                request.carbsG(),
                request.fatG(),
                request.fiberG(),
                request.source() == null ? "USER" : request.source(),
                request.metadataJson() == null ? "{}" : request.metadataJson()
        );
    }

    public NutritionSummaryResponse summary(UUID userId, LocalDate targetDate) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(sum(calories), 0) AS calories,
                       COALESCE(sum(protein_g), 0) AS protein_g,
                       COALESCE(sum(carbs_g), 0) AS carbs_g,
                       COALESCE(sum(fat_g), 0) AS fat_g,
                       COALESCE(sum(fiber_g), 0) AS fiber_g
                FROM nutrition_entries
                WHERE user_id = ? AND entry_date = ?
                """,
                (rs, rowNum) -> new NutritionSummaryResponse(
                        targetDate.toString(),
                        rs.getBigDecimal("calories"),
                        rs.getBigDecimal("protein_g"),
                        rs.getBigDecimal("carbs_g"),
                        rs.getBigDecimal("fat_g"),
                        rs.getBigDecimal("fiber_g")
                ),
                userId,
                targetDate
        );
    }

    private NutritionEntryResponse mapEntry(ResultSet rs, int rowNum) throws SQLException {
        LocalDate date = rs.getObject("entry_date", LocalDate.class);
        return new NutritionEntryResponse(
                rs.getObject("id", UUID.class).toString(),
                date.toString(),
                rs.getString("meal_type"),
                rs.getString("description"),
                rs.getBigDecimal("calories"),
                rs.getBigDecimal("protein_g"),
                rs.getBigDecimal("carbs_g"),
                rs.getBigDecimal("fat_g"),
                rs.getBigDecimal("fiber_g"),
                rs.getString("source"),
                rs.getString("metadata")
        );
    }
}
