package com.marketlens.backend.marketdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketlens.backend.marketdata.dto.DailyQuoteData;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AkshareMarketDataProviderTest {
    @Test
    void parsesWorkerJson() {
        MarketDataProperties properties = properties();
        AkshareMarketDataProvider provider = new AkshareMarketDataProvider(
                properties,
                new ObjectMapper().findAndRegisterModules()
        ) {
            @Override
            protected ProcessResult runProcess(List<String> command, Path workingDirectory) {
                return new ProcessResult(0, """
                        [{
                          "symbol":"600000",
                          "market":"A",
                          "tradeDate":"2026-06-19",
                          "openPrice":10.1,
                          "highPrice":10.5,
                          "lowPrice":9.9,
                          "closePrice":10.3,
                          "preClosePrice":10.0,
                          "changeAmount":0.3,
                          "pctChange":3.0,
                          "volume":12345678,
                          "amount":123456789,
                          "dataSource":"AKSHARE"
                        }]
                        """, "");
            }
        };

        List<DailyQuoteData> result = provider.fetchDailyQuotes(
                "A",
                LocalDate.of(2026, 6, 19),
                List.of("600000")
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).symbol()).isEqualTo("600000");
        assertThat(result.get(0).closePrice()).isEqualByComparingTo("10.3");
        assertThat(result.get(0).dataSource()).isEqualTo("AKSHARE");
    }

    @Test
    void reportsWorkerFailure() {
        AkshareMarketDataProvider provider = new AkshareMarketDataProvider(
                properties(),
                new ObjectMapper().findAndRegisterModules()
        ) {
            @Override
            protected ProcessResult runProcess(List<String> command, Path workingDirectory) {
                return new ProcessResult(1, "", "{\"error\":\"AKShare fetch failed\"}");
            }
        };

        assertThatThrownBy(() -> provider.fetchDailyQuotes(
                "A",
                LocalDate.of(2026, 6, 19),
                List.of("600000")
        )).isInstanceOf(MarketDataProviderException.class)
                .hasMessageContaining("AKShare fetch failed");
    }

    private MarketDataProperties properties() {
        MarketDataProperties properties = new MarketDataProperties();
        properties.getAkshare().setWorkerPath(
                Path.of("..", "..", "data-worker", "fetch_a_daily_quotes.py").toString()
        );
        return properties;
    }
}
