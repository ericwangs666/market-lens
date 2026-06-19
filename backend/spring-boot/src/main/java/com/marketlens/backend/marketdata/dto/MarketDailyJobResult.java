package com.marketlens.backend.marketdata.dto;

import com.marketlens.backend.model.MarketReview;

import java.time.LocalDate;

public record MarketDailyJobResult(
        String market,
        LocalDate tradeDate,
        String provider,
        boolean fallbackUsed,
        int records,
        MarketReview review
) {
}
