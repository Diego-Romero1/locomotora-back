package com.locomotora.demo.progress.dto;

import java.math.BigDecimal;

public record BodyMetricResponse(
        String id,
        String measuredAt,
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
