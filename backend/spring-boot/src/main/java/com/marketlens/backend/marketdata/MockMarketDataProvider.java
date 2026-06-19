package com.marketlens.backend.marketdata;

import com.marketlens.backend.marketdata.dto.DailyQuoteData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class MockMarketDataProvider implements MarketDataProvider {
    @Override
    public String name() {
        return "MOCK";
    }

    @Override
    public List<DailyQuoteData> fetchDailyQuotes(String market, LocalDate tradeDate, List<String> symbols) {
        return IntStream.range(0, symbols.size())
                .mapToObj(index -> {
                    BigDecimal close = new BigDecimal("10.30").add(BigDecimal.valueOf(index));
                    return new DailyQuoteData(
                            symbols.get(index),
                            market.toUpperCase(),
                            tradeDate,
                            close.subtract(new BigDecimal("0.20")),
                            close.add(new BigDecimal("0.20")),
                            close.subtract(new BigDecimal("0.40")),
                            close,
                            close.subtract(new BigDecimal("0.30")),
                            new BigDecimal("0.30"),
                            new BigDecimal("3.00"),
                            new BigDecimal("12345678"),
                            new BigDecimal("123456789"),
                            "MOCK"
                    );
                })
                .toList();
    }
}
