package com.locomotora.demo.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
        String goalType,
        BigDecimal targetValue,
        String targetUnit,
        LocalDate targetDate,
        Integer priority,
        String status,
        String notes
) {
}
