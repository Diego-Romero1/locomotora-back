package com.locomotora.demo.nutrition.dto;

import java.math.BigDecimal;

public record NutritionEntryResponse(
        String id,
        String entryDate,
        String mealType,
        String description,
        BigDecimal calories,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG,
        BigDecimal fiberG,
        String source,
        String metadataJson
) {
}
