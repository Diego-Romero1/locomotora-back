package com.locomotora.demo.user.dto;

import java.math.BigDecimal;

public record GoalResponse(
        String id,
        String goalType,
        BigDecimal targetValue,
        String targetUnit,
        String targetDate,
        int priority,
        String status,
        String notes
) {
}
