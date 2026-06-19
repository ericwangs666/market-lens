package com.marketlens.backend.controller;

import com.marketlens.backend.model.AlertHistory;
import com.marketlens.backend.model.AlertRule;
import com.marketlens.backend.model.MarketReview;
import com.marketlens.backend.model.MockDataSummary;
import com.marketlens.backend.model.StockMemo;
import com.marketlens.backend.model.WatchlistStock;
import com.marketlens.backend.service.AlertEvaluationService;
import com.marketlens.backend.service.AlertHistoryService;
import com.marketlens.backend.service.AlertRuleService;
import com.marketlens.backend.service.MarketReviewService;
import com.marketlens.backend.service.MockDataService;
import com.marketlens.backend.service.StockMemoService;
import com.marketlens.backend.service.WatchlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MarketLensApiIntegrationTest {
    private MockMvc mockMvc;

    @Mock
    private WatchlistService watchlistService;

    @Mock
    private StockMemoService stockMemoService;

    @Mock
    private AlertRuleService alertRuleService;

    @Mock
    private AlertHistoryService alertHistoryService;

    @Mock
    private AlertEvaluationService alertEvaluationService;

    @Mock
    private MockDataService mockDataService;

    @Mock
    private MarketReviewService marketReviewService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new HealthController(),
                new WatchlistController(watchlistService),
                new StockMemoController(stockMemoService),
                new NotesController(stockMemoService),
                new AlertRuleController(alertRuleService),
                new AlertHistoryController(alertHistoryService),
                new AlertsController(alertRuleService, alertHistoryService),
                new MarketReviewController(marketReviewService),
                new AdminTestController(alertEvaluationService, mockDataService)
        ).build();
    }

    @Test
    void healthCheckReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    void addAndQueryWatchlist() throws Exception {
        when(watchlistService.create(any())).thenReturn(new WatchlistStock(1L, "NVDA", "NVIDIA", "US", LocalDateTime.now()));
        when(watchlistService.list()).thenReturn(List.of(new WatchlistStock(1L, "NVDA", "NVIDIA", "US", LocalDateTime.now())));

        mockMvc.perform(post("/api/watchlist")
                        .contentType("application/json")
                        .content("{\"code\":\"NVDA\",\"name\":\"NVIDIA\",\"market\":\"US\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("NVDA"));

        mockMvc.perform(get("/api/watchlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value("NVDA"));
    }

    @Test
    void addAndUpdateMemo() throws Exception {
        when(stockMemoService.create(any())).thenReturn(new StockMemo(1L, "NVDA", "First note", "Watch earnings", LocalDateTime.now(), LocalDateTime.now()));
        when(stockMemoService.update(any(), any())).thenReturn(new StockMemo(1L, "NVDA", "Updated note", "Raise alert", LocalDateTime.now(), LocalDateTime.now()));
        when(stockMemoService.listByStockCode("NVDA")).thenReturn(List.of(new StockMemo(1L, "NVDA", "Updated note", "Raise alert", LocalDateTime.now(), LocalDateTime.now())));

        mockMvc.perform(post("/api/notes")
                        .contentType("application/json")
                        .content("{\"symbol\":\"NVDA\",\"note\":\"Watch earnings\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockCode").value("NVDA"));

        mockMvc.perform(get("/api/notes/NVDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].stockCode").value("NVDA"));

        mockMvc.perform(put("/api/notes/1")
                        .contentType("application/json")
                        .content("{\"symbol\":\"NVDA\",\"note\":\"Raise alert\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Raise alert"));
    }

    @Test
    void addAlertRuleTriggerMockQuoteAndQueryAlertEvents() throws Exception {
        when(alertRuleService.create(any())).thenReturn(new AlertRule(1L, "MOCK1", "PRICE", "GTE", new BigDecimal("100"), true, true, LocalDateTime.now(), LocalDateTime.now()));
        when(alertEvaluationService.evaluateEnabledRules()).thenReturn(1);
        when(alertHistoryService.list(null)).thenReturn(List.of(new AlertHistory(1L, 1L, "MOCK1", new BigDecimal("101"), new BigDecimal("12.5"), new BigDecimal("12.5"), "LOG", "MOCK1 triggered PRICE GTE 100", LocalDateTime.now())));

        mockMvc.perform(post("/api/alerts/rules")
                        .contentType("application/json")
                        .content("{\"symbol\":\"MOCK1\",\"ruleType\":\"PRICE\",\"operator\":\">=\",\"thresholdValue\":100,\"enabled\":true,\"oncePerDay\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockCode").value("MOCK1"));

        mockMvc.perform(post("/api/admin/alerts/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.triggered").value(1));

        mockMvc.perform(get("/api/alerts/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].stockCode").value("MOCK1"))
                .andExpect(jsonPath("$.data[0].channel").value("LOG"));
    }

    @Test
    void generateDailyMarketReview() throws Exception {
        MarketReview review = new MarketReview(LocalDate.now(), "Mock daily market review", List.of("AI led"), List.of("MOCK1"), "development mock");
        when(mockDataService.seedDevelopmentData()).thenReturn(new MockDataSummary(3, 3, review));
        when(marketReviewService.getLatest()).thenReturn(review);

        mockMvc.perform(post("/api/admin/market-review/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Mock daily market review"))
                .andExpect(jsonPath("$.data.leadingStocks[0]").value("MOCK1"));

        mockMvc.perform(get("/api/market/daily-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Mock daily market review"));
    }
}
