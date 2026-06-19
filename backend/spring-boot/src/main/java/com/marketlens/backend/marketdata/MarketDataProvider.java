package com.marketlens.backend.marketdata;

import com.marketlens.backend.marketdata.dto.DailyQuoteData;

import java.time.LocalDate;
import java.util.List;

public interface MarketDataProvider {
    String name();

    List<DailyQuoteData> fetchDailyQuotes(String market, LocalDate tradeDate, List<String> symbols);
}
