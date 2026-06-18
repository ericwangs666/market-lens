package com.marketlens.backend.service;

import com.marketlens.backend.entity.AlertEvent;
import com.marketlens.backend.entity.AlertRule;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.mapper.AlertEventMapper;
import com.marketlens.backend.mapper.AlertRuleMapper;
import com.marketlens.backend.mapper.StockMapper;
import com.marketlens.backend.model.MarketQuote;
import com.marketlens.backend.model.NotificationMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertEngineTest {
    @Mock
    private AlertRuleMapper alertRuleMapper;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private AlertDedupeService alertDedupeService;

    @Mock
    private AlertEventMapper alertEventMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AlertEvaluationService alertEvaluationService;

    @Test
    void evaluateEnabledRulesCreatesEventWhenQuoteTriggersRule() {
        AlertRule rule = new AlertRule();
        rule.setId(7L);
        rule.setUserId(1L);
        rule.setStockId(9L);
        rule.setRuleType("PRICE");
        rule.setCompareOperator("GTE");
        rule.setThresholdValue(new BigDecimal("100"));
        rule.setEnabled(true);
        rule.setOncePerDay(true);
        Stock stock = new Stock();
        stock.setId(9L);
        stock.setSymbol("MOCK1");
        MarketQuote quote = new MarketQuote(9L, "MOCK1", LocalDate.of(2026, 6, 17), new BigDecimal("101"), BigDecimal.ONE, BigDecimal.TEN);

        when(alertRuleMapper.selectList(any())).thenReturn(List.of(rule));
        when(stockMapper.selectById(9L)).thenReturn(stock);
        when(marketDataService.findLatestQuote(stock)).thenReturn(Optional.of(quote));
        when(alertDedupeService.markIfFirstTrigger(7L, quote.quoteDate())).thenReturn(true);

        int triggered = alertEvaluationService.evaluateEnabledRules();

        assertThat(triggered).isEqualTo(1);
        ArgumentCaptor<AlertEvent> eventCaptor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(alertEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTriggerValue()).isEqualByComparingTo("101");
        verify(notificationService).send(any(NotificationMessage.class));
    }
}
