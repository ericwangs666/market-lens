package com.marketlens.backend.service;

import com.marketlens.backend.dto.AlertRuleRequest;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.mapper.AlertRuleMapper;
import com.marketlens.backend.mapper.StockMapper;
import com.marketlens.backend.model.AlertRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertRuleServiceTest {
    @Mock
    private AlertRuleMapper alertRuleMapper;

    @Mock
    private StockMapper stockMapper;

    @InjectMocks
    private AlertRuleService alertRuleService;

    @Test
    void createStoresEnabledPriceRule() {
        com.marketlens.backend.entity.AlertRule[] inserted = new com.marketlens.backend.entity.AlertRule[1];
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setSymbol("MOCK1");
        when(stockMapper.selectOne(any())).thenReturn(stock);
        when(stockMapper.selectById(1L)).thenReturn(stock);
        doAnswer(invocation -> {
            com.marketlens.backend.entity.AlertRule rule = invocation.getArgument(0);
            rule.setId(5L);
            inserted[0] = rule;
            return 1;
        }).when(alertRuleMapper).insert(any(com.marketlens.backend.entity.AlertRule.class));
        when(alertRuleMapper.selectById(5L)).thenAnswer(invocation -> inserted[0]);

        AlertRule result = alertRuleService.create(
                new AlertRuleRequest("mock1", "price", "gte", new BigDecimal("100"), true, true)
        );

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.stockCode()).isEqualTo("MOCK1");
        assertThat(result.operator()).isEqualTo("GTE");
        assertThat(result.threshold()).isEqualByComparingTo("100");
        assertThat(result.oncePerDay()).isTrue();
    }
}
