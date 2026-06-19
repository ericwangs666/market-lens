package com.marketlens.backend.controller;

import com.marketlens.backend.common.ApiResponse;
import com.marketlens.backend.marketdata.dto.DailyQuoteData;
import com.marketlens.backend.service.MarketDataIngestService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketDailyQuoteController {
    private final MarketDataIngestService ingestService;

    public MarketDailyQuoteController(MarketDataIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @GetMapping("/daily-quotes")
    public ApiResponse<List<DailyQuoteData>> list(
            @RequestParam(defaultValue = "A") String market,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return ApiResponse.success(ingestService.list(market, tradeDate));
    }
}
