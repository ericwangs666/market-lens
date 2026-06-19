package com.marketlens.backend.service;

import com.marketlens.backend.entity.DailyQuote;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.mapper.DailyQuoteMapper;
import com.marketlens.backend.mapper.StockMapper;
import com.marketlens.backend.mapper.WatchlistMapper;
import com.marketlens.backend.marketdata.MarketDataProperties;
import com.marketlens.backend.marketdata.MarketDataProvider;
import com.marketlens.backend.marketdata.MarketDataProviderFactory;
import com.marketlens.backend.marketdata.MockMarketDataProvider;
import com.marketlens.backend.marketdata.dto.DailyQuoteData;
import com.marketlens.backend.marketdata.dto.MarketDataIngestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataIngestServiceTest {
    @Mock
    private MarketDataProviderFactory providerFactory;
    @Mock
    private DailyQuoteMapper dailyQuoteMapper;
    @Mock
    private StockMapper stockMapper;
    @Mock
    private WatchlistMapper watchlistMapper;
    @Mock
    private StockService stockService;
    @Mock
    private JobRunLogService jobRunLogService;

    private MarketDataProperties properties;
    private MarketDataIngestService service;

    @BeforeEach
    void setUp() {
        properties = new MarketDataProperties();
        properties.setFallbackToMock(true);
        service = new MarketDataIngestService(
                providerFactory,
                properties,
                dailyQuoteMapper,
                stockMapper,
                watchlistMapper,
                stockService,
                jobRunLogService
        );
    }

    @Test
    void ingestsAkshareQuoteAndStoresDataSource() {
        when(watchlistMapper.selectList(any())).thenReturn(List.of());
        MarketDataProvider provider = mock(MarketDataProvider.class);
        DailyQuoteData data = quote("AKSHARE");
        Stock stock = stock();
        when(provider.name()).thenReturn("AKSHARE");
        when(providerFactory.getProvider("A")).thenReturn(provider);
        when(provider.fetchDailyQuotes(eq("A"), any(), any())).thenReturn(List.of(data));
        when(stockService.findOrCreate("600000", "600000", "A")).thenReturn(stock);
        when(dailyQuoteMapper.selectOne(any())).thenReturn(null);

        MarketDataIngestResult result = service.ingest("A", data.tradeDate());

        assertThat(result.provider()).isEqualTo("AKSHARE");
        assertThat(result.fallbackUsed()).isFalse();
        ArgumentCaptor<DailyQuote> inserted = ArgumentCaptor.forClass(DailyQuote.class);
        verify(dailyQuoteMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getDataSource()).isEqualTo("AKSHARE");
        assertThat(inserted.getValue().getMarket()).isEqualTo("A");
        assertThat(inserted.getValue().getSymbol()).isEqualTo("600000");
    }

    @Test
    void fallsBackToMockWhenAkshareFails() {
        when(watchlistMapper.selectList(any())).thenReturn(List.of());
        MarketDataProvider failing = mock(MarketDataProvider.class);
        MockMarketDataProvider fallback = mock(MockMarketDataProvider.class);
        when(failing.name()).thenReturn("AKSHARE");
        when(failing.fetchDailyQuotes(any(), any(), any())).thenThrow(new RuntimeException("network down"));
        when(providerFactory.getProvider("A")).thenReturn(failing);
        when(providerFactory.getMockProvider()).thenReturn(fallback);
        when(fallback.name()).thenReturn("MOCK");
        when(fallback.fetchDailyQuotes(any(), any(), any())).thenReturn(List.of(quote("MOCK")));
        when(stockService.findOrCreate(any(), any(), any())).thenReturn(stock());
        when(dailyQuoteMapper.selectOne(any())).thenReturn(null);

        MarketDataIngestResult result = service.ingest("A", LocalDate.of(2026, 6, 19));

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.provider()).isEqualTo("MOCK");
        verify(jobRunLogService).record(
                eq("MARKET_DATA_INGEST"),
                eq("A"),
                any(),
                eq("WARNING"),
                eq("MOCK"),
                eq(1),
                any(),
                any()
        );
    }

    @Test
    void repeatedIngestUpdatesExistingQuoteInsteadOfInserting() {
        when(watchlistMapper.selectList(any())).thenReturn(List.of());
        MarketDataProvider provider = mock(MarketDataProvider.class);
        DailyQuote existing = new DailyQuote();
        existing.setId(9L);
        when(provider.name()).thenReturn("AKSHARE");
        when(providerFactory.getProvider("A")).thenReturn(provider);
        when(provider.fetchDailyQuotes(any(), any(), any())).thenReturn(List.of(quote("AKSHARE")));
        when(stockService.findOrCreate(any(), any(), any())).thenReturn(stock());
        when(dailyQuoteMapper.selectOne(any())).thenReturn(existing);

        service.ingest("A", LocalDate.of(2026, 6, 19));

        verify(dailyQuoteMapper, never()).insert(any(DailyQuote.class));
        verify(dailyQuoteMapper).updateById(existing);
    }

    @Test
    void recordsFailedWhenFallbackIsDisabled() {
        when(watchlistMapper.selectList(any())).thenReturn(List.of());
        properties.setFallbackToMock(false);
        MarketDataProvider failing = mock(MarketDataProvider.class);
        when(failing.name()).thenReturn("AKSHARE");
        when(failing.fetchDailyQuotes(any(), any(), any())).thenThrow(new RuntimeException("network down"));
        when(providerFactory.getProvider("A")).thenReturn(failing);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.ingest("A", LocalDate.of(2026, 6, 19))
        ).hasMessageContaining("network down");

        verify(jobRunLogService).record(
                eq("MARKET_DATA_INGEST"),
                eq("A"),
                any(),
                eq("FAILED"),
                eq("AKSHARE"),
                eq(0),
                any(),
                any()
        );
    }

    @Test
    void listsStoredAkshareQuotes() {
        DailyQuote stored = new DailyQuote();
        stored.setMarket("A");
        stored.setSymbol("600000");
        stored.setQuoteDate(LocalDate.of(2026, 6, 19));
        stored.setClosePrice(new BigDecimal("10.3"));
        stored.setDataSource("AKSHARE");
        when(dailyQuoteMapper.selectList(any())).thenReturn(List.of(stored));

        List<DailyQuoteData> result = service.list("A", stored.getQuoteDate());

        assertThat(result).singleElement()
                .extracting(DailyQuoteData::dataSource)
                .isEqualTo("AKSHARE");
    }

    private DailyQuoteData quote(String source) {
        return new DailyQuoteData(
                "600000",
                "A",
                LocalDate.of(2026, 6, 19),
                new BigDecimal("10.1"),
                new BigDecimal("10.5"),
                new BigDecimal("9.9"),
                new BigDecimal("10.3"),
                new BigDecimal("10.0"),
                new BigDecimal("0.3"),
                new BigDecimal("3.0"),
                new BigDecimal("12345678"),
                new BigDecimal("123456789"),
                source
        );
    }

    private Stock stock() {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setMarket("A");
        stock.setSymbol("600000");
        return stock;
    }
}
