package com.marketlens.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketQuote(
        Long stockId,
        String stockCode,
        LocalDate quoteDate,
        BigDecimal closePrice,
        BigDecimal pctChange,
        BigDecimal turnover
) {
}
