package com.marketlens.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlertRule(
        Long id,
        String stockCode,
        String ruleType,
        String operator,
        BigDecimal threshold,
        Boolean enabled,
        Boolean oncePerDay,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
