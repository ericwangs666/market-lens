package com.marketlens.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marketlens.backend.entity.DailyQuote;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.entity.Watchlist;
import com.marketlens.backend.mapper.DailyQuoteMapper;
import com.marketlens.backend.mapper.StockMapper;
import com.marketlens.backend.mapper.WatchlistMapper;
import com.marketlens.backend.marketdata.MarketDataProperties;
import com.marketlens.backend.marketdata.MarketDataProvider;
import com.marketlens.backend.marketdata.MarketDataProviderFactory;
import com.marketlens.backend.marketdata.dto.DailyQuoteData;
import com.marketlens.backend.marketdata.dto.MarketDataIngestResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class MarketDataIngestService {
    private static final long DEFAULT_USER_ID = 1L;
    private static final List<String> DEFAULT_A_SYMBOLS = List.of("600000", "000001");

    private final MarketDataProviderFactory providerFactory;
    private final MarketDataProperties properties;
    private final DailyQuoteMapper dailyQuoteMapper;
    private final StockMapper stockMapper;
    private final WatchlistMapper watchlistMapper;
    private final StockService stockService;
    private final JobRunLogService jobRunLogService;

    public MarketDataIngestService(
            MarketDataProviderFactory providerFactory,
            MarketDataProperties properties,
            DailyQuoteMapper dailyQuoteMapper,
            StockMapper stockMapper,
            WatchlistMapper watchlistMapper,
            StockService stockService,
            JobRunLogService jobRunLogService
    ) {
        this.providerFactory = providerFactory;
        this.properties = properties;
        this.dailyQuoteMapper = dailyQuoteMapper;
        this.stockMapper = stockMapper;
        this.watchlistMapper = watchlistMapper;
        this.stockService = stockService;
        this.jobRunLogService = jobRunLogService;
    }

    public MarketDataIngestResult ingest(String market, LocalDate tradeDate) {
        String normalizedMarket = market.trim().toUpperCase(Locale.ROOT);
        LocalDateTime startedAt = LocalDateTime.now();
        List<String> symbols = resolveSymbols(normalizedMarket);
        MarketDataProvider provider = providerFactory.getProvider(normalizedMarket);
        boolean fallbackUsed = false;
        String warningMessage = null;

        try {
            List<DailyQuoteData> quotes;
            try {
                quotes = provider.fetchDailyQuotes(normalizedMarket, tradeDate, symbols);
            } catch (RuntimeException exception) {
                if (!properties.isFallbackToMock() || "MOCK".equals(provider.name())) {
                    throw exception;
                }
                provider = providerFactory.getMockProvider();
                quotes = provider.fetchDailyQuotes(normalizedMarket, tradeDate, symbols);
                fallbackUsed = true;
                warningMessage = "Primary provider failed; mock fallback used: " + exception.getMessage();
            }

            quotes.forEach(this::upsert);
            jobRunLogService.record(
                    "MARKET_DATA_INGEST",
                    normalizedMarket,
                    tradeDate,
                    fallbackUsed ? "WARNING" : "SUCCESS",
                    provider.name(),
                    quotes.size(),
                    fallbackUsed ? warningMessage : "Daily quotes ingested",
                    startedAt
            );
            return new MarketDataIngestResult(
                    normalizedMarket,
                    tradeDate,
                    provider.name(),
                    fallbackUsed,
                    quotes.size(),
                    quotes
            );
        } catch (RuntimeException exception) {
            jobRunLogService.record(
                    "MARKET_DATA_INGEST",
                    normalizedMarket,
                    tradeDate,
                    "FAILED",
                    provider.name(),
                    0,
                    exception.getMessage(),
                    startedAt
            );
            throw exception;
        }
    }

    public List<DailyQuoteData> list(String market, LocalDate tradeDate) {
        return dailyQuoteMapper.selectList(new LambdaQueryWrapper<DailyQuote>()
                        .eq(DailyQuote::getMarket, market.trim().toUpperCase(Locale.ROOT))
                        .eq(DailyQuote::getQuoteDate, tradeDate)
                        .orderByAsc(DailyQuote::getSymbol))
                .stream()
                .map(this::toData)
                .toList();
    }

    private void upsert(DailyQuoteData data) {
        String market = data.market().trim().toUpperCase(Locale.ROOT);
        String symbol = data.symbol().trim().toUpperCase(Locale.ROOT);
        Stock stock = stockService.findOrCreate(symbol, symbol, market);
        DailyQuote existing = dailyQuoteMapper.selectOne(new LambdaQueryWrapper<DailyQuote>()
                .eq(DailyQuote::getMarket, market)
                .eq(DailyQuote::getSymbol, symbol)
                .eq(DailyQuote::getQuoteDate, data.tradeDate())
                .last("LIMIT 1"));

        DailyQuote quote = existing == null ? new DailyQuote() : existing;
        quote.setStockId(stock.getId());
        quote.setMarket(market);
        quote.setSymbol(symbol);
        quote.setQuoteDate(data.tradeDate());
        quote.setOpenPrice(data.openPrice());
        quote.setHighPrice(data.highPrice());
        quote.setLowPrice(data.lowPrice());
        quote.setClosePrice(data.closePrice());
        quote.setPreviousClosePrice(data.preClosePrice());
        quote.setChangeAmount(data.changeAmount());
        quote.setPctChange(data.pctChange());
        quote.setVolume(data.volume());
        quote.setTurnover(data.amount());
        quote.setDataSource(data.dataSource());

        if (existing == null) {
            dailyQuoteMapper.insert(quote);
        } else {
            dailyQuoteMapper.updateById(quote);
        }
    }

    private List<String> resolveSymbols(String market) {
        if (!"A".equals(market)) {
            return DEFAULT_A_SYMBOLS;
        }
        List<Long> stockIds = watchlistMapper.selectList(new LambdaQueryWrapper<Watchlist>()
                        .eq(Watchlist::getUserId, DEFAULT_USER_ID))
                .stream()
                .map(Watchlist::getStockId)
                .filter(Objects::nonNull)
                .toList();
        if (stockIds.isEmpty()) {
            return DEFAULT_A_SYMBOLS;
        }
        List<String> symbols = stockMapper.selectBatchIds(stockIds).stream()
                .filter(stock -> "A".equalsIgnoreCase(stock.getMarket())
                        || "CN".equalsIgnoreCase(stock.getMarket()))
                .map(Stock::getSymbol)
                .distinct()
                .toList();
        return symbols.isEmpty() ? DEFAULT_A_SYMBOLS : symbols;
    }

    private DailyQuoteData toData(DailyQuote quote) {
        return new DailyQuoteData(
                quote.getSymbol(),
                quote.getMarket(),
                quote.getQuoteDate(),
                quote.getOpenPrice(),
                quote.getHighPrice(),
                quote.getLowPrice(),
                quote.getClosePrice(),
                quote.getPreviousClosePrice(),
                quote.getChangeAmount(),
                quote.getPctChange(),
                quote.getVolume(),
                quote.getTurnover(),
                quote.getDataSource()
        );
    }
}
