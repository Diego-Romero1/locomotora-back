package com.locomotora.demo.nutrition;

import com.locomotora.demo.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NutritionController {
    private final JdbcTemplate jdbcTemplate;

    public NutritionController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/nutrition")
    public List<NutritionEntryResponse> entries(@RequestParam(required = false) LocalDate date) {
        UUID userId = CurrentUser.id();
        if (date != null) {
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

    @PostMapping("/nutrition")
    public NutritionEntryResponse create(@Valid @RequestBody NutritionEntryRequest request) {
        UUID userId = CurrentUser.id();
        UUID id = jdbcTemplate.queryForObject(
                """
                INSERT INTO nutrition_entries
                    (user_id, entry_date, meal_type, description, calories, protein_g, carbs_g, fat_g, fiber_g, source, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
                RETURNING id
                """,
                UUID.class,
                userId,
                request.entryDate() == null ? LocalDate.now() : request.entryDate(),
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
        return entries(null).stream().filter(entry -> entry.id().equals(id.toString())).findFirst().orElseThrow();
    }

    @GetMapping("/nutrition/summary")
    public NutritionSummary summary(@RequestParam(required = false) LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
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
                (rs, rowNum) -> new NutritionSummary(
                        targetDate.toString(),
                        rs.getBigDecimal("calories"),
                        rs.getBigDecimal("protein_g"),
                        rs.getBigDecimal("carbs_g"),
                        rs.getBigDecimal("fat_g"),
                        rs.getBigDecimal("fiber_g")
                ),
                CurrentUser.id(),
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

    public record NutritionEntryRequest(
            LocalDate entryDate,
            String mealType,
            @NotBlank String description,
            BigDecimal calories,
            BigDecimal proteinG,
            BigDecimal carbsG,
            BigDecimal fatG,
            BigDecimal fiberG,
            String source,
            String metadataJson
    ) {
    }

    public record NutritionEntryResponse(String id, String entryDate, String mealType, String description,
                                         BigDecimal calories, BigDecimal proteinG, BigDecimal carbsG,
                                         BigDecimal fatG, BigDecimal fiberG, String source, String metadataJson) {
    }

    public record NutritionSummary(String entryDate, BigDecimal calories, BigDecimal proteinG,
                                   BigDecimal carbsG, BigDecimal fatG, BigDecimal fiberG) {
    }
}
