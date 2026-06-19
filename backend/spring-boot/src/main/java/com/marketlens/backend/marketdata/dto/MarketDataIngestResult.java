package com.marketlens.backend.marketdata.dto;

import java.time.LocalDate;
import java.util.List;

public record MarketDataIngestResult(
        String market,
        LocalDate tradeDate,
        String provider,
        boolean fallbackUsed,
        int records,
        List<DailyQuoteData> quotes
) {
}
