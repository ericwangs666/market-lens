package com.marketlens.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marketlens.backend.entity.DailyQuote;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.mapper.DailyQuoteMapper;
import com.marketlens.backend.model.MarketReview;
import com.marketlens.backend.model.MockDataSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class MockDataService {
    private final StockService stockService;
    private final DailyQuoteMapper dailyQuoteMapper;
    private final MarketReviewService marketReviewService;

    public MockDataService(StockService stockService, DailyQuoteMapper dailyQuoteMapper, MarketReviewService marketReviewService) {
        this.stockService = stockService;
        this.dailyQuoteMapper = dailyQuoteMapper;
        this.marketReviewService = marketReviewService;
    }

    public MockDataSummary seedDevelopmentData() {
        LocalDate today = LocalDate.now();
        Stock cnStock = stockService.findOrCreate("300244", "Mock A-share Leader", "CN");
        Stock usStock = stockService.findOrCreate("NVDA", "Mock US AI Leader", "US");
        Stock triggerStock = stockService.findOrCreate("MOCK1", "Mock Alert Trigger", "US");

        upsertQuote(cnStock, today, "18.20", "8.88", "1200000000");
        upsertQuote(usStock, today, "203.00", "5.00", "8800000000");
        upsertQuote(triggerStock, today, "101.00", "12.50", "660000000");

        MarketReview savedReview = marketReviewService.saveOrUpdate(generateMarketReview());
        return new MockDataSummary(3, 3, savedReview);
    }

    public MarketReview generateMarketReview() {
        return new MarketReview(
                LocalDate.now(),
                "Mock daily market review",
                List.of("AI hardware led the board", "High-turnover names stayed active", "Alert trigger quote is available"),
                List.of("300244", "NVDA", "MOCK1"),
                "development mock"
        );
    }

    private void upsertQuote(Stock stock, LocalDate quoteDate, String closePrice, String pctChange, String turnover) {
        DailyQuote existing = dailyQuoteMapper.selectOne(new LambdaQueryWrapper<DailyQuote>()
                .eq(DailyQuote::getStockId, stock.getId())
                .eq(DailyQuote::getQuoteDate, quoteDate)
                .last("LIMIT 1"));

        DailyQuote quote = existing == null ? new DailyQuote() : existing;
        quote.setStockId(stock.getId());
        quote.setMarket(stock.getMarket());
        quote.setSymbol(stock.getSymbol());
        quote.setQuoteDate(quoteDate);
        quote.setClosePrice(new BigDecimal(closePrice));
        quote.setPctChange(new BigDecimal(pctChange));
        quote.setTurnover(new BigDecimal(turnover));
        quote.setDataSource("MOCK");

        if (existing == null) {
            dailyQuoteMapper.insert(quote);
        } else {
            dailyQuoteMapper.updateById(quote);
        }
    }
}
