package com.locomotora.demo.progress.dto;

import java.math.BigDecimal;
import java.time.Instant;

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
