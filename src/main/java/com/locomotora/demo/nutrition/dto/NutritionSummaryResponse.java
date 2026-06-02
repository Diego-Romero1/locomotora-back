package com.locomotora.demo.nutrition.dto;

import java.math.BigDecimal;

public record NutritionSummaryResponse(
        String entryDate,
        BigDecimal calories,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG,
        BigDecimal fiberG
) {
}
