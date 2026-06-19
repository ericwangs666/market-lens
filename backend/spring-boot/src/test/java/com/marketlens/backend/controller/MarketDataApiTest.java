package com.marketlens.backend.controller;

import com.marketlens.backend.marketdata.AkshareMarketDataProvider;
import com.marketlens.backend.marketdata.MarketDataProperties;
import com.marketlens.backend.marketdata.MarketDataProviderFactory;
import com.marketlens.backend.marketdata.dto.DailyQuoteData;
import com.marketlens.backend.marketdata.dto.MarketDataIngestResult;
import com.marketlens.backend.service.MarketDailyJobService;
import com.marketlens.backend.service.MarketDataIngestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MarketDataApiTest {
    private MockMvc mockMvc;

    @Mock
    private MarketDataProviderFactory providerFactory;
    @Mock
    private AkshareMarketDataProvider akshareProvider;
    @Mock
    private MarketDataIngestService ingestService;
    @Mock
    private MarketDailyJobService dailyJobService;

    private final MarketDataProperties properties = new MarketDataProperties();

    @BeforeEach
    void setUp() {
        properties.setUseMock(false);
        properties.setFallbackToMock(true);
        properties.setProviderA("AKSHARE");
        mockMvc = MockMvcBuilders.standaloneSetup(
                new MarketDataAdminController(properties, providerFactory, ingestService, dailyJobService),
                new MarketDailyQuoteController(ingestService)
        ).build();
    }

    @Test
    void returnsProviderConfiguration() throws Exception {
        when(providerFactory.getAkshareProvider()).thenReturn(akshareProvider);
        when(akshareProvider.isConfigured()).thenReturn(true);

        mockMvc.perform(get("/api/admin/market-data/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerA").value("AKSHARE"))
                .andExpect(jsonPath("$.data.useMock").value(false))
                .andExpect(jsonPath("$.data.fallbackToMock").value(true))
                .andExpect(jsonPath("$.data.akshareWorkerConfigured").value(true));
    }

    @Test
    void testsAkshareWithDefaultSymbols() throws Exception {
        when(providerFactory.getAkshareProvider()).thenReturn(akshareProvider);
        when(akshareProvider.fetchDailyQuotes(any(), any(), any()))
                .thenReturn(List.of(quote()));

        mockMvc.perform(post("/api/admin/market-data/test").param("market", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].dataSource").value("AKSHARE"));
    }

    @Test
    void ingestsAndQueriesDailyQuotes() throws Exception {
        LocalDate tradeDate = LocalDate.of(2026, 6, 19);
        when(ingestService.ingest("A", tradeDate)).thenReturn(
                new MarketDataIngestResult("A", tradeDate, "AKSHARE", false, 1, List.of(quote()))
        );
        when(ingestService.list("A", tradeDate)).thenReturn(List.of(quote()));

        mockMvc.perform(post("/api/admin/market-data/ingest")
                        .param("market", "A")
                        .param("tradeDate", "2026-06-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").value(1));

        mockMvc.perform(get("/api/market/daily-quotes")
                        .param("market", "A")
                        .param("tradeDate", "2026-06-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].symbol").value("600000"))
                .andExpect(jsonPath("$.data[0].dataSource").value("AKSHARE"));
    }

    private DailyQuoteData quote() {
        return new DailyQuoteData(
                "600000", "A", LocalDate.of(2026, 6, 19),
                new BigDecimal("10.1"), new BigDecimal("10.5"),
                new BigDecimal("9.9"), new BigDecimal("10.3"),
                new BigDecimal("10.0"), new BigDecimal("0.3"),
                new BigDecimal("3.0"), new BigDecimal("12345678"),
                new BigDecimal("123456789"), "AKSHARE"
        );
    }
}
