package com.marketlens.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlertHistory(
        Long id,
        Long ruleId,
        String stockCode,
        BigDecimal triggerPrice,
        BigDecimal triggerPct,
        BigDecimal triggeredValue,
        String channel,
        String message,
        LocalDateTime triggeredAt
) {
}
