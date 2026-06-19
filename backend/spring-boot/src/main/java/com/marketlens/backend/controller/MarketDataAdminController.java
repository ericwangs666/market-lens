package com.marketlens.backend.controller;

import com.marketlens.backend.common.ApiResponse;
import com.marketlens.backend.marketdata.MarketDataProperties;
import com.marketlens.backend.marketdata.MarketDataProviderException;
import com.marketlens.backend.marketdata.MarketDataProviderFactory;
import com.marketlens.backend.marketdata.dto.DailyQuoteData;
import com.marketlens.backend.marketdata.dto.MarketDailyJobResult;
import com.marketlens.backend.marketdata.dto.MarketDataIngestResult;
import com.marketlens.backend.marketdata.dto.MarketDataProviderStatus;
import com.marketlens.backend.service.MarketDailyJobService;
import com.marketlens.backend.service.MarketDataIngestService;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Profile("dev")
@RestController
@RequestMapping("/api/admin")
public class MarketDataAdminController {
    private static final List<String> DEFAULT_TEST_SYMBOLS = List.of("600000", "000001");

    private final MarketDataProperties properties;
    private final MarketDataProviderFactory providerFactory;
    private final MarketDataIngestService ingestService;
    private final MarketDailyJobService dailyJobService;

    public MarketDataAdminController(
            MarketDataProperties properties,
            MarketDataProviderFactory providerFactory,
            MarketDataIngestService ingestService,
            MarketDailyJobService dailyJobService
    ) {
        this.properties = properties;
        this.providerFactory = providerFactory;
        this.ingestService = ingestService;
        this.dailyJobService = dailyJobService;
    }

    @GetMapping("/market-data/providers")
    public ApiResponse<MarketDataProviderStatus> providers() {
        return ApiResponse.success(new MarketDataProviderStatus(
                properties.getProviderA(),
                properties.isUseMock(),
                properties.isFallbackToMock(),
                providerFactory.getAkshareProvider().isConfigured()
        ));
    }

    @PostMapping("/market-data/test")
    public ApiResponse<List<DailyQuoteData>> test(
            @RequestParam(defaultValue = "A") String market
    ) {
        MarketDataProviderException lastFailure = null;
        for (int daysAgo = 0; daysAgo <= 7; daysAgo++) {
            try {
                return ApiResponse.success(providerFactory.getAkshareProvider()
                        .fetchDailyQuotes(
                                market,
                                LocalDate.now().minusDays(daysAgo),
                                DEFAULT_TEST_SYMBOLS
                        ));
            } catch (MarketDataProviderException exception) {
                lastFailure = exception;
            }
        }
        throw lastFailure == null
                ? new MarketDataProviderException("No recent AKShare trading day was available")
                : lastFailure;
    }

    @PostMapping("/market-data/ingest")
    public ApiResponse<MarketDataIngestResult> ingest(
            @RequestParam(defaultValue = "A") String market,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return ApiResponse.success(ingestService.ingest(market, tradeDate));
    }

    @PostMapping("/market-daily-job/run")
    public ApiResponse<MarketDailyJobResult> run(
            @RequestParam(defaultValue = "A") String market,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return ApiResponse.success(dailyJobService.run(market, tradeDate));
    }
}
