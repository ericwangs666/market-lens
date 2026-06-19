package com.marketlens.backend.marketdata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyQuoteData(
        String symbol,
        String market,
        LocalDate tradeDate,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal preClosePrice,
        BigDecimal changeAmount,
        BigDecimal pctChange,
        BigDecimal volume,
        BigDecimal amount,
        String dataSource
) {
}
