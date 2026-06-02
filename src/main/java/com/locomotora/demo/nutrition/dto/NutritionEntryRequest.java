package com.locomotora.demo.nutrition.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

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
