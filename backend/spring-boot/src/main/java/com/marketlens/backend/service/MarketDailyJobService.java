package com.marketlens.backend.service;

import com.marketlens.backend.marketdata.dto.DailyQuoteData;
import com.marketlens.backend.marketdata.dto.MarketDailyJobResult;
import com.marketlens.backend.marketdata.dto.MarketDataIngestResult;
import com.marketlens.backend.model.MarketReview;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class MarketDailyJobService {
    private final MarketDataIngestService ingestService;
    private final MarketReviewService marketReviewService;
    private final JobRunLogService jobRunLogService;

    public MarketDailyJobService(
            MarketDataIngestService ingestService,
            MarketReviewService marketReviewService,
            JobRunLogService jobRunLogService
    ) {
        this.ingestService = ingestService;
        this.marketReviewService = marketReviewService;
        this.jobRunLogService = jobRunLogService;
    }

    public MarketDailyJobResult run(String market, LocalDate tradeDate) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            MarketDataIngestResult ingestResult = ingestService.ingest(market, tradeDate);
            MarketReview review = marketReviewService.saveOrUpdate(buildReview(ingestResult));
            jobRunLogService.record(
                    "MARKET_DAILY_JOB",
                    ingestResult.market(),
                    tradeDate,
                    ingestResult.fallbackUsed() ? "WARNING" : "SUCCESS",
                    ingestResult.provider(),
                    ingestResult.records(),
                    "Daily quotes ingested and market review generated",
                    startedAt
            );
            return new MarketDailyJobResult(
                    ingestResult.market(),
                    tradeDate,
                    ingestResult.provider(),
                    ingestResult.fallbackUsed(),
                    ingestResult.records(),
                    review
            );
        } catch (RuntimeException exception) {
            jobRunLogService.record(
                    "MARKET_DAILY_JOB",
                    market.toUpperCase(),
                    tradeDate,
                    "FAILED",
                    null,
                    0,
                    exception.getMessage(),
                    startedAt
            );
            throw exception;
        }
    }

    private MarketReview buildReview(MarketDataIngestResult result) {
        List<DailyQuoteData> leaders = result.quotes().stream()
                .sorted(Comparator.comparing(
                        DailyQuoteData::pctChange,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(5)
                .toList();
        List<String> leadingStocks = leaders.stream()
                .map(DailyQuoteData::symbol)
                .toList();
        List<String> highlights = leaders.stream()
                .map(quote -> quote.symbol() + " pctChange=" + quote.pctChange())
                .toList();
        if (highlights.isEmpty()) {
            highlights = List.of("No daily quotes were returned");
        }
        return new MarketReview(
                result.tradeDate(),
                result.market() + "-share daily review",
                highlights,
                leadingStocks,
                result.provider()
        );
    }
}
